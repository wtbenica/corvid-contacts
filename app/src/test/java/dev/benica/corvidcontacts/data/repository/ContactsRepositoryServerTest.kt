// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The repository's writes to a CardDAV server: a contact or book changes locally only if the server accepted it. */
class ContactsRepositoryServerTest : RepositoryTestBase() {

    private lateinit var server: MockWebServer
    private val requests = mutableListOf<RecordedRequest>()

    private val principalHref = "/remote.php/dav/principals/testuser/"
    private val homeSetHref = "/remote.php/dav/addressbooks/testuser/"
    private val bookHref = "${homeSetHref}contacts/"

    private var putStatus = 201
    private var deleteStatus = 204
    private var proppatchStatus = 207
    private var mkcolStatuses = ArrayDeque(listOf(201))
    private var serverBooks = mutableListOf(AddressBookFixture(bookHref, "Contacts"))

    @Before
    fun startServer() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val body = request.body.clone().readUtf8()
                return when (request.method) {
                    "PUT" -> MockResponse().setResponseCode(putStatus)
                    "DELETE" -> MockResponse().setResponseCode(deleteStatus)
                    "PROPPATCH" -> MockResponse().setResponseCode(proppatchStatus)
                    "MKCOL" -> {
                        val status = mkcolStatuses.removeFirstOrNull() ?: 201
                        if (status == 201) {
                            serverBooks += AddressBookFixture(request.path.orEmpty(), "Created")
                        }
                        MockResponse().setResponseCode(status)
                    }
                    "PROPFIND" -> MockResponse().setResponseCode(207).setBody(
                        when {
                            "current-user-principal" in body -> principalPropfindResponse(principalHref)
                            "addressbook-home-set" in body -> homeSetPropfindResponse(principalHref, homeSetHref)
                            else -> addressBookListResponse(*serverBooks.toTypedArray())
                        }
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        runBlocking {
            authRepository.saveCredentials(
                NextcloudCredentials(server.url("/").toString(), "testuser", "app-password")
            )
        }
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    private fun localBook() = booksInRoom().single { it.href == localHref }

    private fun requestsOf(method: String) = requests.filter { it.method == method }

    private val localHref = ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF

    // --- Contacts -----------------------------------------------------------------------------

    @Test
    fun `a saved contact is uploaded to its book as a vCard and then kept`() {
        seedBooks(book(bookHref))

        val result = runBlocking { repository.saveContact(contact("c1", bookHref, "Ada Lovelace")) }

        assertTrue(result.isSuccess)
        val put = requestsOf("PUT").single()
        assertEquals("${bookHref}c1.vcf", put.path)
        assertTrue(put.body.clone().readUtf8().contains("FN:Ada Lovelace"))
        assertEquals("${bookHref}c1.vcf", contactInRoom("c1")?.contactHref)
    }

    @Test
    fun `a contact the server refuses is not kept`() {
        seedBooks(book(bookHref))
        putStatus = 500

        val result = runBlocking { repository.saveContact(contact("c1", bookHref)) }

        assertTrue(result.isFailure)
        assertNull(contactInRoom("c1"))
    }

    @Test
    fun `a contact in a local book is saved without contacting the server`() {
        seedBooks(book(localHref))

        val result = runBlocking { repository.saveContact(contact("c1", localHref)) }

        assertTrue(result.isSuccess)
        assertTrue(requests.isEmpty())
        assertNotNull(contactInRoom("c1"))
    }

    @Test
    fun `a contact with no book goes to the first visible server book`() {
        seedBooks(book(bookHref))

        runBlocking { repository.saveContact(contact("c1", bookHref = null)) }

        assertEquals(bookHref, contactInRoom("c1")?.addressBookHref)
    }

    @Test
    fun `deleting a server contact deletes it on the server and then locally`() {
        seedBooks(book(bookHref))
        seedContacts(contact("c1", bookHref).copy(contactHref = "${bookHref}c1.vcf"))

        val result = runBlocking { repository.deleteContact(contactInRoom("c1")!!) }

        assertTrue(result.isSuccess)
        assertEquals("${bookHref}c1.vcf", requestsOf("DELETE").single().path)
        assertNull(contactInRoom("c1"))
    }

    @Test
    fun `a contact the server will not delete stays`() {
        seedBooks(book(bookHref))
        seedContacts(contact("c1", bookHref).copy(contactHref = "${bookHref}c1.vcf"))
        deleteStatus = 403

        val result = runBlocking { repository.deleteContact(contactInRoom("c1")!!) }

        assertTrue(result.isFailure)
        assertNotNull(contactInRoom("c1"))
    }

    @Test
    fun `a server contact with no path cannot be deleted`() {
        seedBooks(book(bookHref))
        seedContacts(contact("c1", bookHref))

        val result = runBlocking { repository.deleteContact(contactInRoom("c1")!!) }

        assertTrue(result.isFailure)
        assertTrue(requestsOf("DELETE").isEmpty())
        assertNotNull(contactInRoom("c1"))
    }

    // --- Address books ------------------------------------------------------------------------

    @Test
    fun `renaming a server book renames it on the server and then locally`() {
        seedBooks(book(bookHref, "Old"))

        val result = runBlocking { repository.renameAddressBook(booksInRoom().single(), "New") }

        assertTrue(result.isSuccess)
        assertTrue(requestsOf("PROPPATCH").single().body.clone().readUtf8().contains("New"))
        assertEquals("New", booksInRoom().single().displayName)
    }

    @Test
    fun `a server book the server will not rename keeps its name`() {
        seedBooks(book(bookHref, "Old"))
        proppatchStatus = 403

        val result = runBlocking { repository.renameAddressBook(booksInRoom().single(), "New") }

        assertTrue(result.isFailure)
        assertEquals("Old", booksInRoom().single().displayName)
    }

    @Test
    fun `deleting a server book deletes it on the server, with its contacts here`() {
        seedBooks(book(bookHref))
        seedContacts(contact("c1", bookHref))

        val result = runBlocking { repository.deleteAddressBook(booksInRoom().single()) }

        assertTrue(result.isSuccess)
        assertEquals(1, requestsOf("DELETE").size)
        assertTrue(booksInRoom().isEmpty())
        assertNull(contactInRoom("c1"))
    }

    @Test
    fun `a server book the server will not delete is kept with its contacts`() {
        seedBooks(book(bookHref))
        seedContacts(contact("c1", bookHref))
        deleteStatus = 403

        val result = runBlocking { repository.deleteAddressBook(booksInRoom().single()) }

        assertTrue(result.isFailure)
        assertEquals(1, booksInRoom().size)
        assertNotNull(contactInRoom("c1"))
    }

    @Test
    fun `a new server book is made under the home set with a name taken from its title, and takes the colour given`() {
        val created = runBlocking { repository.createAddressBook("Family", 0xFF112233.toInt(), iconName = "people") }.getOrThrow()

        assertEquals("${homeSetHref}family/", requestsOf("MKCOL").single().path)
        assertFalse(created.isLocal)
        assertEquals(0xFF112233.toInt(), created.colorInt)
        assertEquals("people", created.iconName)
        assertEquals(created, booksInRoom().first { it.href == created.href })
    }

    @Test
    fun `a taken book name is retried with a suffix`() {
        mkcolStatuses = ArrayDeque(listOf(405, 201))

        val created = runBlocking { repository.createAddressBook("Family", 0xFF112233.toInt()) }

        assertTrue(created.isSuccess)
        val paths = requestsOf("MKCOL").map { it.path }
        assertEquals(2, paths.size)
        assertEquals("${homeSetHref}family/", paths[0])
        assertTrue(paths[1]!!.startsWith("${homeSetHref}family-"))
    }

    @Test
    fun `a book the server refuses for any other reason is not retried or kept`() {
        mkcolStatuses = ArrayDeque(listOf(403))

        val result = runBlocking { repository.createAddressBook("Family", 0xFF112233.toInt()) }

        assertTrue(result.isFailure)
        assertEquals(1, requestsOf("MKCOL").size)
    }

    @Test
    fun `a book whose three names are all taken fails`() {
        mkcolStatuses = ArrayDeque(listOf(405, 405, 405))

        val result = runBlocking { repository.createAddressBook("Family", 0xFF112233.toInt()) }

        assertTrue(result.isFailure)
        assertEquals(3, requestsOf("MKCOL").size)
    }

    @Test
    fun `a book made on request as local never contacts the server`() {
        val created = runBlocking { repository.createAddressBook("Family", 0, forceLocal = true) }.getOrThrow()

        assertTrue(created.isLocal)
        assertTrue(requests.isEmpty())
    }

    // --- Uploading a local book ---------------------------------------------------------------

    @Test
    fun `uploading a local book makes a server book, uploads every contact and removes the local book`() {
        seedBooks(book(localHref, "Local"), book(bookHref))
        seedContacts(contact("c1", localHref, "Ada"), contact("c2", localHref, "Grace"))

        val result = runBlocking { repository.uploadLocalAddressBook(localBook(), "Uploaded") }.getOrThrow()

        assertEquals(2, result.uploadedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.fullyCompleted)
        assertEquals(2, requestsOf("PUT").size)
        assertTrue(booksInRoom().none { it.href == localHref })
        val newBook = booksInRoom().single { it.href != bookHref }
        assertEquals(2, booksInRoom().size)
        assertEquals(setOf(newBook.href), listOf("c1", "c2").map { contactInRoom(it)?.addressBookHref }.toSet())
    }

    @Test
    fun `when some contacts fail to upload the local book is kept`() {
        seedBooks(book(localHref, "Local"), book(bookHref))
        seedContacts(contact("c1", localHref), contact("c2", localHref))
        putStatus = 500

        val result = runBlocking { repository.uploadLocalAddressBook(localBook(), "Uploaded") }.getOrThrow()

        assertEquals(0, result.uploadedCount)
        assertEquals(2, result.failedCount)
        assertFalse(result.fullyCompleted)
        assertTrue(booksInRoom().any { it.href == localHref })
        assertEquals(localHref, contactInRoom("c1")?.addressBookHref)
    }

    @Test
    fun `an empty local book is just removed on upload`() {
        seedBooks(book(localHref))

        val result = runBlocking { repository.uploadLocalAddressBook(booksInRoom().single(), "Uploaded") }.getOrThrow()

        assertTrue(result.fullyCompleted)
        assertTrue(booksInRoom().isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `uploading while signed out fails`() = runBlocking {
        authRepository.clearCredentials()
        seedBooks(book(localHref))

        assertTrue(repository.uploadLocalAddressBook(booksInRoom().single(), "Uploaded").isFailure)
    }
}
