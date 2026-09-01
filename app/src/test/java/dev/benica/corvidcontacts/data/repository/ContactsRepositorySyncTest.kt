// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AddressBookDao
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactDao
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [ContactsRepository.syncContacts] against a mock HTTP server.
 *
 * These tests verify the "trust the server" policy for deletions (see SyncRepairStats).
 *
 * Runs under Robolectric to support Android API dependencies (Context, Base64, libphonenumber).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsRepositorySyncTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeAddressBookDao: FakeAddressBookDao
    private lateinit var fakeContactDao: FakeContactDao
    private lateinit var repository: ContactsRepository

    private val principalHref = "/remote.php/dav/principals/testuser/"
    private val homeSetHref = "/remote.php/dav/addressbooks/testuser/"
    private val bookHref = "/remote.php/dav/addressbooks/testuser/contacts/"

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        fakeAddressBookDao = FakeAddressBookDao()
        fakeContactDao = FakeContactDao(addressBooks = { fakeAddressBookDao.books.value })

        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepository = AuthRepository(context)
        val settingsRepository = SettingsRepository(context)

        repository = ContactsRepository(
            context = context,
            contactDao = fakeContactDao,
            addressBookDao = fakeAddressBookDao,
            authRepository = authRepository,
            settingsRepository = settingsRepository,
        )

        runBlocking {
            authRepository.saveCredentials(
                NextcloudCredentials(
                    serverUrl = mockWebServer
                        .url("/")
                        .toString(),
                    username = "testuser",
                    appPassword = "app-password",
                )
            )
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `sync does not restore phones and emails that were deleted on another client`() {
        // Seed the local cache as if a previous sync had already pulled this contact down with
        // 2 emails and 2 phones.
        fakeAddressBookDao.seed(
            AddressBookEntity(
                href = bookHref,
                displayName = "Contacts",
                colorInt = 0xFF010101.toInt(),
                isVisible = true,
            )
        )
        fakeContactDao.seed(
            ContactEntity(
                id = "contact-1",
                displayName = "",
                firstName = "Jane",
                lastName = "Doe",
                emails = listOf(
                    Email(
                        "old1@example.com",
                        "HOME"
                    ),
                    Email(
                        "old2@example.com",
                        "WORK"
                    ),
                ),
                phones = listOf(
                    Phone(
                        "+15551234567",
                        "MOBILE"
                    ),
                    Phone(
                        "+15559876543",
                        "HOME"
                    ),
                ),
                photoUrl = null,
                etag = "old-etag",
                addressBookHref = bookHref,
                contactHref = "${bookHref}contact-1.vcf",
            )
        )

        // The server reports zero emails/phones, simulating a deletion on another client.
        val serverVCard = """
            BEGIN:VCARD
            VERSION:4.0
            UID:contact-1
            FN:Jane Doe
            N:Doe;Jane;;;
            END:VCARD
        """.trimIndent()

        enqueueStandardDiscovery()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(
                    contactsReportResponse(
                        href = "${bookHref}contact-1.vcf",
                        etag = "\"new-etag\"",
                        vcard = serverVCard,
                    )
                )
        )

        val result = runBlocking { repository.syncContacts() }

        assertTrue(
            "Expected syncContacts() to succeed, but it failed: ${result.exceptionOrNull()}",
            result.isSuccess
        )

        val synced = runBlocking { fakeContactDao.getContactById("contact-1") }
        assertEquals(
            "Emails deleted on another client must not be restored from the local cache",
            emptyList<Email>(),
            synced?.contact?.emails ?: emptyList<Email>()
        )
        assertEquals(
            "Phones deleted on another client must not be restored from the local cache",
            emptyList<Phone>(),
            synced?.contact?.phones ?: emptyList<Phone>()
        )
    }

    @Test
    fun `sync removes address books and their contacts once no longer reported by the server`() {
        val orphanedBookHref = "/remote.php/dav/addressbooks/testuser/orphaned/"

        fakeAddressBookDao.seed(
            AddressBookEntity(
                href = bookHref,
                displayName = "Contacts",
                isVisible = true,
                colorInt = 0xFF010101.toInt()
            ),
            AddressBookEntity(
                href = orphanedBookHref,
                displayName = "Deleted Book",
                isVisible = true,
                colorInt = 0xFFFEFEFE.toInt()
            )
        )
        fakeContactDao.seed(
            ContactEntity(
                id = "orphan-contact",
                displayName = "Old Contact",
                firstName = null,
                lastName = null,
                emails = null,
                phones = null,
                photoUrl = null,
                etag = "etag-x",
                addressBookHref = orphanedBookHref,
                contactHref = "${orphanedBookHref}orphan-contact.vcf",
            )
        )

        enqueuePrincipalAndHomeSet()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(
                    addressBookListResponse(
                        AddressBookFixture(
                            bookHref,
                            "Contacts"
                        )
                    )
                )
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(emptyMultistatusResponse())
        )

        val result = runBlocking { repository.syncContacts() }

        assertTrue(
            "Expected syncContacts() to succeed, but it failed: ${result.exceptionOrNull()}",
            result.isSuccess
        )

        assertTrue(
            "Orphaned address book should have been deleted locally",
            fakeAddressBookDao.books.value.none { it.href == orphanedBookHref }
        )
        val orphanedContact = runBlocking { fakeContactDao.getContactById("orphan-contact") }
        assertNull(
            "Contacts belonging to an orphaned address book should have been deleted locally",
            orphanedContact
        )
    }

    @Test
    fun `PhoneFormatter reformats a raw number under Robolectric`() {
        // Verifies libphonenumber-android metadata loads correctly.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val formatted = PhoneFormatter.format(
            "5551234567",
            includeCountryCode = true,
            context = context,
            region = "US"
        )
        assertNotEquals(
            "Expected the raw number to be reformatted, not passed through unchanged",
            "5551234567",
            formatted
        )
    }

    // --- Fixture helpers -----------------------------------------------------------------

    private fun enqueuePrincipalAndHomeSet() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(principalPropfindResponse(principalHref))
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(
                    homeSetPropfindResponse(
                        principalHref,
                        homeSetHref
                    )
                )
        )
    }

    /** [enqueuePrincipalAndHomeSet] + the standard single-book listing pointing at [bookHref]. */
    private fun enqueueStandardDiscovery() {
        enqueuePrincipalAndHomeSet()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(
                    addressBookListResponse(
                        AddressBookFixture(
                            bookHref,
                            "Contacts"
                        )
                    )
                )
        )
    }
}

private data class AddressBookFixture(
    val href: String,
    val displayName: String,
)

private fun principalPropfindResponse(principalHref: String): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\">" +
            "<d:response>" +
            "<d:href>/remote.php/dav/</d:href>" +
            "<d:propstat><d:prop>" +
            "<d:current-user-principal><d:href>$principalHref</d:href></d:current-user-principal>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

private fun homeSetPropfindResponse(
    principalHref: String,
    homeSetHref: String,
): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "<d:response>" +
            "<d:href>$principalHref</d:href>" +
            "<d:propstat><d:prop>" +
            "<card:addressbook-home-set><d:href>$homeSetHref</d:href></card:addressbook-home-set>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

private fun addressBookListResponse(vararg books: AddressBookFixture): String {
    val responses = books.joinToString("") { book ->
        "<d:response>" +
                "<d:href>${book.href}</d:href>" +
                "<d:propstat><d:prop>" +
                "<d:resourcetype><d:collection/><card:addressbook/></d:resourcetype>" +
                "<d:displayname>${book.displayName}</d:displayname>" +
                "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
                "</d:response>"
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\" " +
            "xmlns:ical=\"http://apple.com/ns/ical/\">" +
            responses +
            "</d:multistatus>"
}

private fun contactsReportResponse(
    href: String,
    etag: String,
    vcard: String,
): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "<d:response>" +
            "<d:href>$href</d:href>" +
            "<d:propstat><d:prop>" +
            "<d:getetag>$etag</d:getetag>" +
            "<card:address-data>$vcard</card:address-data>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

private fun emptyMultistatusResponse(): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "</d:multistatus>"
}

// --- In-memory fakes (unlike ContactsRepositoryVCardRoundTripTest's NoOp fakes, these actually
// store data, since these tests need to assert on DAO state after a sync cycle runs) ----------

private class FakeContactDao(
    private val addressBooks: () -> List<AddressBookEntity>,
) : ContactDao {
    private val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())

    fun seed(vararg entities: ContactEntity) {
        contacts.value += entities
    }

    private fun withBook(entity: ContactEntity): ContactWithAddressBook {
        val book = addressBooks().find { it.href == entity.addressBookHref }
        return ContactWithAddressBook(
            entity,
            book
        )
    }

    override fun getAllVisibleContacts(): Flow<List<ContactWithAddressBook>> =
        contacts.map { list ->
            list
                .filter { !it.isArchived }
                .map(::withBook)
        }

    override fun getArchivedContacts(): Flow<List<ContactWithAddressBook>> =
        contacts.map { list ->
            list
                .filter { it.isArchived }
                .map(::withBook)
        }

    override fun getAllContacts(): Flow<List<ContactWithAddressBook>> =
        contacts.map { list -> list.map(::withBook) }

    override suspend fun getAllContactsSync(): List<ContactWithAddressBook> =
        contacts.value.map(::withBook)

    override suspend fun getContactById(id: String): ContactWithAddressBook? =
        contacts.value
            .find { it.id == id }
            ?.let(::withBook)

    override suspend fun insertContacts(contacts: List<ContactEntity>) {
        val current = this.contacts.value
            .associateBy { it.id }
            .toMutableMap()
        contacts.forEach { current[it.id] = it }
        this.contacts.value = current.values.toList()
    }

    override suspend fun deleteContact(contact: ContactEntity) {
        contacts.value = contacts.value.filterNot { it.id == contact.id }
    }

    override suspend fun deleteContactsByAddressBook(addressBookHref: String) {
        contacts.value = contacts.value.filterNot { it.addressBookHref == addressBookHref }
    }

    override suspend fun clearAll() {
        contacts.value = emptyList()
    }

    override suspend fun clearAllExceptLocal() {
        contacts.value = contacts.value.filter {
            it.addressBookHref?.startsWith("local://") == true
        }
    }

    override fun getContactByIdFlow(contactId: ContactId): Flow<ContactWithAddressBook?> =
        contacts.map { list ->
            list
                .find { it.id == contactId }
                ?.let(::withBook)
        }
}

private class FakeAddressBookDao : AddressBookDao {
    val books = MutableStateFlow<List<AddressBookEntity>>(emptyList())

    fun seed(vararg entities: AddressBookEntity) {
        books.value += entities
    }

    override fun getAllAddressBooks(): Flow<List<AddressBookEntity>> = books

    override fun getUserManageableAddressBooks(): Flow<List<AddressBookEntity>> =
        books.map { list ->
            list.filterNot {
                it.displayName?.contains(
                    "Archived",
                    ignoreCase = true
                ) == true
            }
        }

    // Mirrors the real DAO's @Insert(onConflict = OnConflictStrategy.IGNORE): existing hrefs are
    // left untouched by an insert call.
    override suspend fun insertAddressBooks(addressBooks: List<AddressBookEntity>) {
        val current = books.value
            .associateBy { it.href }
            .toMutableMap()
        addressBooks.forEach { book ->
            current.putIfAbsent(
                book.href,
                book
            )
        }
        books.value = current.values.toList()
    }

    override suspend fun updateAddressBook(addressBook: AddressBookEntity) {
        books.value = books.value.map { if (it.href == addressBook.href) addressBook else it }
    }

    override suspend fun deleteAddressBooks(addressBooks: List<AddressBookEntity>) {
        val hrefs = addressBooks
            .map { it.href }
            .toSet()
        books.value = books.value.filterNot { it.href in hrefs }
    }

    override suspend fun clearAll() {
        books.value = emptyList()
    }

    override suspend fun clearAllExceptLocal() {
        books.value = books.value.filter { it.href.startsWith("local://") }
    }

    override suspend fun updateDisplayName(
        href: String,
        displayName: String?,
    ) {
        books.value = books.value.map {
            if (it.href == href) it.copy(displayName = displayName) else it
        }
    }
}
