// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.model.Phone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
    private lateinit var database: AppDatabase
    private lateinit var repository: ContactsRepository

    private val principalHref = "/remote.php/dav/principals/testuser/"
    private val homeSetHref = "/remote.php/dav/addressbooks/testuser/"
    private val bookHref = "/remote.php/dav/addressbooks/testuser/contacts/"

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val authRepository = AuthRepository(context)
        val settingsRepository = SettingsRepository(context)

        repository = ContactsRepository(
            context = context,
            contactDao = database.contactDao(),
            addressBookDao = database.addressBookDao(),
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
        database.close()
    }

    @Test
    fun `sync does not restore phones and emails that were deleted on another client`() {
        // Seed the local cache as if a previous sync had already pulled this contact down with
        // 2 emails and 2 phones.
        seedBooks(
            AddressBookEntity(
                href = bookHref,
                displayName = "Contacts",
                colorInt = 0xFF010101.toInt(),
                isVisible = true,
            )
        )
        seedContacts(
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

        val synced = runBlocking { database.contactDao().getContactById("contact-1") }
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

        seedBooks(
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
        seedContacts(
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
            runBlocking { database.addressBookDao().getAllAddressBooks().first() }.none { it.href == orphanedBookHref }
        )
        val orphanedContact = runBlocking { database.contactDao().getContactById("orphan-contact") }
        assertNull(
            "Contacts belonging to an orphaned address book should have been deleted locally",
            orphanedContact
        )
    }

    // --- Fixture helpers -----------------------------------------------------------------

    private fun seedBooks(vararg books: AddressBookEntity) = runBlocking {
        database.addressBookDao().insertAddressBooks(books.toList())
    }

    private fun seedContacts(vararg contacts: ContactEntity) = runBlocking {
        database.contactDao().insertContacts(contacts.toList())
    }

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
