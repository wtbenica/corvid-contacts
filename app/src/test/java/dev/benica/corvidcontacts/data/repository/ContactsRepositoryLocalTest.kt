// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import dev.benica.corvidcontacts.data.model.Phone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The repository with no server signed in: everything stays on the device. */
class ContactsRepositoryLocalTest : RepositoryTestBase() {

    private val localHref = ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF

    // --- Saving, importing, deleting ----------------------------------------------------------

    @Test
    fun `saving a contact keeps it in its book and formats its phone numbers`() {
        seedBooks(book(localHref))

        val result = runBlocking {
            repository.saveContact(contact("c1", localHref).copy(phones = listOf(Phone("5551234567", "CELL", region = "US"))))
        }

        assertTrue(result.isSuccess)
        val saved = contactInRoom("c1")!!
        assertEquals(localHref, saved.addressBookHref)
        assertNotEquals("5551234567", saved.phones?.single()?.value)
    }

    @Test
    fun `a contact saved with no book goes in the first visible one`() {
        seedBooks(book(localHref))

        runBlocking { repository.saveContact(contact("c1", bookHref = null)) }

        assertEquals(localHref, contactInRoom("c1")?.addressBookHref)
    }

    @Test
    fun `importing vCards makes a new contact of each in the chosen book`() {
        seedBooks(book(localHref))
        val vcards = """
            BEGIN:VCARD
            VERSION:4.0
            FN:Ada Lovelace
            TEL;TYPE=cell:5551234567
            END:VCARD
            BEGIN:VCARD
            VERSION:4.0
            FN:Charles Babbage
            END:VCARD
        """.trimIndent()

        val result = runBlocking { repository.importVCardText(vcards, localHref, downloadRemotePhotos = false) }

        assertEquals(2, result.imported)
        assertEquals(0, result.failed)
        val names = runBlocking { database.contactDao().getAllContactsSync() }.map { it.contact.displayName }.sorted()
        assertEquals(listOf("Ada Lovelace", "Charles Babbage"), names)
        assertTrue(runBlocking { database.contactDao().getAllContactsSync() }.all { it.contact.addressBookHref == localHref })
    }

    @Test
    fun `importing text with no vCards imports nothing`() {
        seedBooks(book(localHref))

        val result = runBlocking { repository.importVCardText("not a vCard", localHref, downloadRemotePhotos = false) }

        assertEquals(0, result.imported)
        assertEquals(0, result.failed)
    }

    @Test
    fun `a vCard with a remote photo is recognised, and one with an embedded photo is not`() {
        val remote = "BEGIN:VCARD\nVERSION:4.0\nFN:A\nPHOTO:https://example.org/a.jpg\nEND:VCARD\n"
        val embedded = "BEGIN:VCARD\nVERSION:4.0\nFN:A\nPHOTO:data:image/jpeg;base64,/9j/4AAQ\nEND:VCARD\n"

        assertTrue(repository.vCardTextHasRemotePhotoUrls(remote))
        assertFalse(repository.vCardTextHasRemotePhotoUrls(embedded))
    }

    @Test
    fun `deleting a local contact removes it`() {
        seedBooks(book(localHref))
        seedContacts(contact("c1", localHref))

        val result = runBlocking { repository.deleteContact(contactInRoom("c1")!!) }

        assertTrue(result.isSuccess)
        assertNull(contactInRoom("c1"))
    }

    @Test
    fun `archiving moves a contact out of the list and into the archive, and unarchiving brings it back`() {
        seedBooks(book(localHref))
        seedContacts(contact("c1", localHref))

        runBlocking { repository.archiveContact(contactInRoom("c1")!!) }
        assertEquals(emptyList<String>(), runBlocking { repository.allContacts.first() }.map { it.contact.id })
        assertEquals(listOf("c1"), runBlocking { repository.archivedContacts.first() }.map { it.contact.id })

        runBlocking { repository.unarchiveContact(contactInRoom("c1")!!) }
        assertEquals(listOf("c1"), runBlocking { repository.allContacts.first() }.map { it.contact.id })
    }

    // --- Moving contacts between books --------------------------------------------------------

    @Test
    fun `moving a contact between local books changes only its book`() {
        val other = "${ContactsRepository.LOCAL_ADDRESS_BOOK_PREFIX}other"
        seedBooks(book(localHref), book(other))
        seedContacts(contact("c1", localHref).copy(etag = "old", contactHref = "/old.vcf"))

        val result = runBlocking { repository.moveContact(contactInRoom("c1")!!, other) }

        assertTrue(result.isSuccess)
        val moved = contactInRoom("c1")!!
        assertEquals(other, moved.addressBookHref)
        assertNull(moved.etag)
        assertEquals("Ada", moved.displayName)
    }

    // --- Address books ------------------------------------------------------------------------

    @Test
    fun `a new address book with no server is local, with the colour and icon given, and counts as decided`() {
        val created = runBlocking { repository.createAddressBook("Family", 0xFF112233.toInt(), iconName = "people") }.getOrThrow()

        assertTrue(created.isLocal)
        assertEquals("Family", created.displayName)
        assertEquals(0xFF112233.toInt(), created.colorInt)
        assertEquals("people", created.iconName)
        assertEquals(listOf(created.href), booksInRoom().map { it.href })
        assertTrue(runBlocking { settingsRepository.resolvedLocalBookHrefs.first() }.contains(created.href))
    }

    @Test
    fun `renaming a local book changes its name`() {
        seedBooks(book(localHref, "Old"))

        val result = runBlocking { repository.renameAddressBook(booksInRoom().single(), "New") }

        assertTrue(result.isSuccess)
        assertEquals("New", booksInRoom().single().displayName)
    }

    @Test
    fun `deleting a local book deletes its contacts and no others`() {
        val other = "${ContactsRepository.LOCAL_ADDRESS_BOOK_PREFIX}other"
        seedBooks(book(localHref), book(other))
        seedContacts(contact("c1", localHref), contact("c2", other))

        val result = runBlocking { repository.deleteAddressBook(booksInRoom().first { it.href == localHref }) }

        assertTrue(result.isSuccess)
        assertEquals(listOf(other), booksInRoom().map { it.href })
        assertNull(contactInRoom("c1"))
        assertNotNull(contactInRoom("c2"))
    }

    @Test
    fun `the contacts in a book can be counted`() {
        seedBooks(book(localHref))
        seedContacts(contact("c1", localHref), contact("c2", localHref))

        assertEquals(2, runBlocking { repository.getContactCountInAddressBook(localHref) })
    }

    @Test
    fun `the default local book is made once, and is listed as local`() {
        runBlocking {
            repository.ensureLocalAddressBookExists()
            repository.ensureLocalAddressBookExists()
        }

        assertEquals(listOf(localHref), runBlocking { repository.getLocalAddressBooks() }.map { it.href })
    }

    // --- Local-only mode, logging out and deleting everything --------------------------------

    @Test
    fun `entering local-only mode clears server data, keeps local data and makes the default book`() {
        val serverBook = "/remote.php/dav/addressbooks/u/contacts/"
        seedBooks(book(serverBook))
        seedContacts(contact("server", serverBook), contact("local", localHref))

        runBlocking { repository.enterLocalOnlyMode() }

        assertNull(contactInRoom("server"))
        assertNotNull(contactInRoom("local"))
        assertEquals(listOf(localHref), booksInRoom().map { it.href })
        assertTrue(runBlocking { settingsRepository.localOnlyMode.first() })
    }

    @Test
    fun `logging out clears server data and keeps local data`() {
        val serverBook = "/remote.php/dav/addressbooks/u/contacts/"
        seedBooks(book(localHref), book(serverBook))
        seedContacts(contact("server", serverBook), contact("local", localHref))

        runBlocking { repository.logout() }

        assertNull(contactInRoom("server"))
        assertNotNull(contactInRoom("local"))
        assertEquals(listOf(localHref), booksInRoom().map { it.href })
        assertNull(runBlocking { authRepository.credentials.first() })
    }

    @Test
    fun `deleting all local data clears every contact and book`() {
        seedBooks(book(localHref))
        seedContacts(contact("c1", localHref))

        runBlocking { repository.deleteAllLocalData() }

        assertEquals(emptyList<String>(), booksInRoom().map { it.href })
        assertNull(contactInRoom("c1"))
    }
}
