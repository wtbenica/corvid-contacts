// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.system.SystemContactVisibility
import dev.benica.corvidcontacts.ui.ViewModelTestBase
import kotlinx.coroutines.flow.first
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
import org.junit.Test

/** What the contact list does to the user's data: saving, deleting, archiving, moving, grouping and merging. */
class ContactsViewModelActionsTest : ViewModelTestBase() {

    private val friends = "local://friends"
    private val work = "local://work"
    private var server: MockWebServer? = null

    @After
    fun stopServer() {
        server?.shutdown()
    }

    private fun viewModel() = createViewModel {
        ContactsViewModel(
            repository,
            settingsRepository,
            authRepository,
            SystemContactVisibility(database.systemContactMirrorDao()),
        )
    }

    private fun entity(
        id: String,
        name: String = id,
        book: String = friends,
        categories: List<String> = emptyList(),
        archived: Boolean = false,
        relationships: List<Relationship> = emptyList(),
    ) = ContactEntity(
        id = id,
        displayName = name,
        addressBookHref = book,
        categories = categories,
        isArchived = archived,
        relationships = relationships,
    )

    private fun seed(vararg contacts: ContactEntity) {
        seedBooks(book(friends, "Friends"), book(work, "Work"))
        seedContacts(*contacts)
    }

    private fun ContactsViewModel.waitForShown(count: Int) {
        uiState.await { it is ContactsUiState.Success && it.contacts.size == count }
    }

    private fun stored(id: String) = contactInRoom(id)

    private fun ContactsViewModel.messages() = events.record()

    private fun List<ContactsEvent>.messages() =
        filterIsInstance<ContactsEvent.Message>().map { it.message }

    // --- One contact --------------------------------------------------------------------------

    @Test
    fun `saving a new contact keeps it and says so`() {
        seedBooks(book(friends, "Friends"))
        val viewModel = viewModel()
        val events = viewModel.messages()

        val saved = runBlocking { viewModel.saveContact(entity("1", "Ada")) }

        assertTrue(saved)
        assertEquals("Ada", stored("1")?.displayName)
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactSaved), events.messages())
    }

    @Test
    fun `saving a contact into another book moves it`() {
        seed(entity("1", "Ada", friends))
        val viewModel = viewModel()

        val saved = runBlocking { viewModel.saveContact(stored("1")!!.copy(addressBookHref = work)) }

        assertTrue(saved)
        assertEquals(work, stored("1")?.addressBookHref)
    }

    @Test
    fun `deleting a contact removes it and says so`() {
        seed(entity("1", "Ada"), entity("2", "Charles"))
        val viewModel = viewModel()
        val events = viewModel.messages()

        val deleted = runBlocking { viewModel.deleteContact(stored("1")!!) }

        assertTrue(deleted)
        assertNull(stored("1"))
        assertNotNull(stored("2"))
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsDeleted(1)), events.messages())
    }

    @Test
    fun `archiving a contact flags it, and archiving it again brings it back`() {
        seed(entity("1", "Ada"))
        val viewModel = viewModel()
        val events = viewModel.messages()

        assertTrue(runBlocking { viewModel.archiveContact(stored("1")!!) })
        assertTrue(stored("1")!!.isArchived)

        assertTrue(runBlocking { viewModel.archiveContact(stored("1")!!) })
        assertFalse(stored("1")!!.isArchived)
        assertEquals(
            listOf(ContactsEvent.ContactsMessage.ContactsArchived(1), ContactsEvent.ContactsMessage.ContactsUnarchived(1)),
            events.messages()
        )
    }

    @Test
    fun `a favorite is added, and taken away again whatever its case`() {
        seed(entity("1", "Ada", categories = listOf("Family")))
        val viewModel = viewModel()

        assertTrue(runBlocking { viewModel.toggleFavorite(stored("1")!!) })
        assertEquals(listOf("Family", "Favorites"), stored("1")!!.categories)

        runBlocking { repository.saveContact(stored("1")!!.copy(categories = listOf("favorites", "Family"))) }
        assertTrue(runBlocking { viewModel.toggleFavorite(stored("1")!!) })
        assertEquals(listOf("Family"), stored("1")!!.categories)
    }

    // --- Several contacts ---------------------------------------------------------------------

    private fun ContactsViewModel.select(vararg ids: String) {
        waitForShownAtLeast(ids.size)
        ids.forEach { toggleSelection(it) }
    }

    private fun ContactsViewModel.waitForShownAtLeast(count: Int) {
        uiState.await { it is ContactsUiState.Success && it.contacts.size >= count }
    }

    @Test
    fun `archiving the selected contacts archives them, clears the selection and says how many`() {
        seed(entity("1"), entity("2"), entity("3"))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.select("1", "2")

        viewModel.archiveSelectedContacts()

        awaitUntil { events.messages().isNotEmpty() }
        assertTrue(stored("1")!!.isArchived && stored("2")!!.isArchived)
        assertFalse(stored("3")!!.isArchived)
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsArchived(2)), events.messages())
        assertTrue(viewModel.selectedContactIds.value.isEmpty())
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `in the archive view, the same action unarchives`() {
        seed(entity("1", archived = true), entity("2", archived = true))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.toggleShowArchived()
        viewModel.select("1")

        viewModel.archiveSelectedContacts()

        awaitUntil { events.messages().isNotEmpty() }
        assertFalse(stored("1")!!.isArchived)
        assertTrue(stored("2")!!.isArchived)
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsUnarchived(1)), events.messages())
    }

    @Test
    fun `deleting the selected contacts removes only them`() {
        seed(entity("1"), entity("2"), entity("3"))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.select("1", "3")

        viewModel.deleteSelectedContacts()

        awaitUntil { events.messages().isNotEmpty() }
        assertNull(stored("1"))
        assertNotNull(stored("2"))
        assertNull(stored("3"))
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsDeleted(2)), events.messages())
    }

    @Test
    fun `moving the selected contacts puts them in the other book`() {
        seed(entity("1", book = friends), entity("2", book = friends))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.select("1")

        viewModel.moveSelectedContactsToAddressBook(work)

        awaitUntil { events.messages().isNotEmpty() }
        assertEquals(work, stored("1")!!.addressBookHref)
        assertEquals(friends, stored("2")!!.addressBookHref)
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsMoved(1)), events.messages())
    }

    @Test
    fun `adding the selected contacts to a group skips those already in it`() {
        seed(entity("in", categories = listOf("family")), entity("out"), entity("also out"))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.select("in", "out")

        viewModel.addSelectedToGroup("Family")

        awaitUntil { events.messages().isNotEmpty() }
        assertEquals(listOf("family"), stored("in")!!.categories)
        assertEquals(listOf("Family"), stored("out")!!.categories)
        assertTrue(stored("also out")!!.categories.isNullOrEmpty())
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsAddedToGroup(1, "Family")), events.messages())
    }

    @Test
    fun `removing the selected contacts from a group takes it off only the members, whatever the case`() {
        seed(
            entity("1", categories = listOf("FAMILY", "Work")),
            entity("2", categories = listOf("Work")),
            entity("3", categories = listOf("Family")),
        )
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.select("1", "2")

        viewModel.removeSelectedFromGroup("family")

        awaitUntil { events.messages().isNotEmpty() }
        assertEquals(listOf("Work"), stored("1")!!.categories)
        assertEquals(listOf("Work"), stored("2")!!.categories)
        assertEquals(listOf("Family"), stored("3")!!.categories)
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsRemovedFromGroup(1, "family")), events.messages())
    }

    @Test
    fun `a bulk action with nothing selected does nothing and says nothing`() {
        seed(entity("1"))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.waitForShown(1)

        viewModel.archiveSelectedContacts()
        viewModel.deleteSelectedContacts()
        viewModel.moveSelectedContactsToAddressBook(work)

        assertTrue(events.isEmpty())
        assertNotNull(stored("1"))
        assertFalse(stored("1")!!.isArchived)
    }

    @Test
    fun `when the server refuses a change the contact is kept as it was and an error is shown`() {
        val mock = MockWebServer().apply { start() }
        server = mock
        mock.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.method) {
                "PUT" -> MockResponse().setResponseCode(500)
                else -> MockResponse().setResponseCode(404)
            }
        }
        val serverBook = "/remote.php/dav/addressbooks/testuser/contacts/"
        runBlocking { authRepository.saveCredentials(NextcloudCredentials(mock.url("/").toString(), "testuser", "x")) }
        seedBooks(book(serverBook, "Server"))
        seedContacts(entity("1", book = serverBook).copy(contactHref = "${serverBook}1.vcf"))
        val viewModel = viewModel()
        val events = viewModel.events.record()
        viewModel.select("1")

        viewModel.archiveSelectedContacts()

        awaitUntil { events.any { it is ContactsEvent.Error && it.resId == R.string.list_error_bulk_partial_failure } }
        assertFalse("not archived locally if the server said no", stored("1")!!.isArchived)
        assertTrue(events.messages().isEmpty())
    }

    // --- Groups -------------------------------------------------------------------------------

    @Test
    fun `renaming a group changes it on every contact in it, whatever the case`() {
        seed(
            entity("1", categories = listOf("Family", "Work")),
            entity("2", categories = listOf("family")),
            entity("3", categories = listOf("Work")),
        )
        val viewModel = viewModel()
        viewModel.waitForShown(3)

        viewModel.renameGroup("Family", "Relatives")

        awaitUntil { stored("1")!!.categories == listOf("Relatives", "Work") && stored("2")!!.categories == listOf("Relatives") }
        assertEquals(listOf("Work"), stored("3")!!.categories)
    }

    @Test
    fun `renaming a group reaches contacts in other books too, even while one book is selected`() {
        seed(
            entity("a", book = friends, categories = listOf("Family")),
            entity("b", book = work, categories = listOf("Family")),
        )
        val viewModel = viewModel()
        viewModel.waitForShown(2)
        viewModel.selectAddressBook(friends)
        viewModel.waitForShown(1)

        viewModel.renameGroup("Family", "Relatives")

        awaitUntil { stored("a")!!.categories == listOf("Relatives") }
        assertEquals("a contact outside the selected book is renamed too", listOf("Relatives"), stored("b")!!.categories)
    }

    @Test
    fun `renaming a group reaches archived contacts and contacts in hidden books too`() {
        seedBooks(book(friends, "Friends"), book(work, "Work").copy(isVisible = false))
        seedContacts(
            entity("shown", book = friends, categories = listOf("Family")),
            entity("archived", book = friends, categories = listOf("Family"), archived = true),
            entity("hidden book", book = work, categories = listOf("Family")),
        )
        val viewModel = viewModel()
        viewModel.waitForShown(1)

        viewModel.renameGroup("Family", "Relatives")

        awaitUntil { stored("shown")!!.categories == listOf("Relatives") }
        assertEquals(listOf("Relatives"), stored("archived")!!.categories)
        assertEquals(listOf("Relatives"), stored("hidden book")!!.categories)
    }

    @Test
    fun `renaming a group keeps its place in the saved order`() {
        seed(entity("1", categories = listOf("Family", "Work")))
        runBlocking { settingsRepository.saveGroupOrder(listOf("Work", "Family")) }
        val viewModel = viewModel()
        viewModel.waitForShown(1)
        viewModel.groupOrder.await { it == listOf("Work", "Family") }

        viewModel.renameGroup("family", "Relatives")

        assertEquals(listOf("Work", "Relatives"), settingsRepository.groupOrder.await { "Relatives" in it })
    }

    @Test
    fun `the group order is saved`() {
        val viewModel = viewModel()

        viewModel.updateGroupOrder(listOf("B", "A"))

        assertEquals(listOf("B", "A"), settingsRepository.groupOrder.await { it.isNotEmpty() })
    }

    // --- Merging ------------------------------------------------------------------------------

    @Test
    fun `merging saves the survivor, deletes the absorbed contact and says so`() {
        seed(entity("survivor", "Ada"), entity("absorbed", "A. Lovelace"))
        val viewModel = viewModel()
        val events = viewModel.messages()
        viewModel.waitForShown(2)

        val merged = runBlocking {
            viewModel.mergeContacts(stored("survivor")!!.copy(company = "Analytical Engines"), stored("absorbed")!!)
        }

        assertTrue(merged)
        assertEquals("Analytical Engines", stored("survivor")!!.company)
        assertNull(stored("absorbed"))
        assertEquals(listOf(ContactsEvent.ContactsMessage.ContactsMerged), events.messages())
    }

    @Test
    fun `merging repoints other contacts' links to the absorbed contact at the survivor`() {
        seed(
            entity("survivor", "Ada"),
            entity("absorbed", "A. Lovelace"),
            entity(
                "friend", "Charles",
                relationships = listOf(
                    Relationship("FRIEND", "absorbed", isUid = true),
                    Relationship("SPOUSE", "absorbed", isUid = false),
                    Relationship("PARENT", "someone-else", isUid = true),
                )
            ),
            entity("unrelated", "Grace"),
        )
        val viewModel = viewModel()
        viewModel.waitForShown(4)

        runBlocking { viewModel.mergeContacts(stored("survivor")!!, stored("absorbed")!!) }

        val relationships = stored("friend")!!.relationships.orEmpty()
        assertEquals(Relationship("FRIEND", "survivor", isUid = true), relationships[0])
        assertEquals("a name that happens to match is left alone", Relationship("SPOUSE", "absorbed", isUid = false), relationships[1])
        assertEquals(Relationship("PARENT", "someone-else", isUid = true), relationships[2])
    }

    @Test
    fun `merging repoints links on contacts that are not on screen, such as another book or the archive`() {
        seed(
            entity("survivor", "Ada", book = friends),
            entity("absorbed", "A. Lovelace", book = friends),
            entity("other book", "Charles", book = work, relationships = listOf(Relationship("FRIEND", "absorbed", isUid = true))),
            entity("archived", "Grace", book = friends, archived = true, relationships = listOf(Relationship("FRIEND", "absorbed", isUid = true))),
        )
        val viewModel = viewModel()
        viewModel.waitForShown(3)
        viewModel.selectAddressBook(friends)
        viewModel.waitForShown(2)

        runBlocking { viewModel.mergeContacts(stored("survivor")!!, stored("absorbed")!!) }

        assertEquals(listOf(Relationship("FRIEND", "survivor", isUid = true)), stored("other book")!!.relationships)
        assertEquals(listOf(Relationship("FRIEND", "survivor", isUid = true)), stored("archived")!!.relationships)
    }

    @Test
    fun `merging with a photo that does not exist leaves the survivor without one`() {
        seed(entity("survivor", "Ada"), entity("absorbed", "A. Lovelace"))
        val viewModel = viewModel()
        viewModel.waitForShown(2)

        runBlocking { viewModel.mergeContacts(stored("survivor")!!, stored("absorbed")!!, useAbsorbedPhoto = true) }

        assertFalse(stored("survivor")!!.hasPhoto)
        assertNull(stored("survivor")!!.photoUrl)
    }

    // --- Address books ------------------------------------------------------------------------

    @Test
    fun `address books are put in the order they are given`() {
        seedBooks(book("/a/", "A"), book("/b/", "B"), book("/c/", "C"))
        val viewModel = viewModel()
        val reordered = booksInRoom().sortedBy { it.href }.let { listOf(it[2], it[0], it[1]) }

        viewModel.updateAddressBookOrder(reordered)

        awaitUntil { booksInRoom().sortedBy { it.sortOrder }.map { it.href } == listOf("/c/", "/a/", "/b/") }
    }

    @Test
    fun `a book can be hidden and shown`() {
        seedBooks(book("/a/", "A"))
        val viewModel = viewModel()

        viewModel.toggleAddressBookVisibility(booksInRoom().single())
        awaitUntil { !booksInRoom().single().isVisible }

        viewModel.toggleAddressBookVisibility(booksInRoom().single())
        awaitUntil { booksInRoom().single().isVisible }
    }

    @Test
    fun `a book keeps the color and icon it is given`() {
        seedBooks(book("/a/", "A"))
        val viewModel = viewModel()

        viewModel.updateAddressBookAppearance(booksInRoom().single(), Color(0xFF123456), "work")

        awaitUntil { booksInRoom().single().iconName == "work" }
        assertEquals(Color(0xFF123456).toArgb(), booksInRoom().single().colorInt)
    }

    @Test
    fun `sharing a book with the system contacts, and the level it is shared at, are saved`() {
        seedBooks(book("/a/", "A"))
        val viewModel = viewModel()

        viewModel.setAddressBookSharedWithSystem(booksInRoom().single(), true)
        viewModel.setAddressBookSystemContactsLevel(booksInRoom().single(), SystemContactsLevel.EVERYTHING)

        awaitUntil { booksInRoom().single().let { it.shareWithSystem && it.systemContactsLevel == SystemContactsLevel.EVERYTHING } }
    }

    @Test
    fun `a book can be created, renamed and deleted with its contacts`() {
        val viewModel = viewModel()

        val created = runBlocking { viewModel.createAddressBook("Family", Color(0xFF336699), forceLocal = true) }.getOrThrow()
        seedContacts(entity("1", book = created.href))
        assertTrue(runBlocking { viewModel.renameAddressBook(created, "Relatives") }.isSuccess)
        assertEquals("Relatives", booksInRoom().single { it.href == created.href }.displayName)

        assertTrue(runBlocking { viewModel.deleteAddressBook(booksInRoom().single { it.href == created.href }) }.isSuccess)
        assertTrue(booksInRoom().none { it.href == created.href })
        assertNull(stored("1"))
    }

    // --- Export, sharing, and leaving ---------------------------------------------------------

    @Test
    fun `the vCards to share are only the selected contacts`() {
        seed(entity("1", "Ada Lovelace"), entity("2", "Charles Babbage"))
        val viewModel = viewModel()
        viewModel.select("2")

        val text = viewModel.getSelectedContactsVCardString()

        assertTrue(text.contains("FN:Charles Babbage"))
        assertFalse(text.contains("Ada Lovelace"))
    }

    @Test
    fun `with nothing selected there is nothing to share`() {
        seed(entity("1"))
        val viewModel = viewModel()
        viewModel.waitForShown(1)

        assertEquals("", viewModel.getSelectedContactsVCardString().let { if ("BEGIN:VCARD" in it) it else "" })
    }

    @Test
    fun `an export holds every contact`() {
        seed(entity("1", "Ada Lovelace"), entity("2", "Charles Babbage", archived = true))

        val text = runBlocking { viewModel().exportAllContacts() }

        assertTrue(text.contains("Ada Lovelace") && text.contains("Charles Babbage"))
    }

    @Test
    fun `deleting all local data empties the books and contacts and forgets the contact card`() {
        seed(entity("1"))
        runBlocking { settingsRepository.saveSelfContactId("1"); settingsRepository.saveGroupOrder(listOf("A")) }
        val viewModel = viewModel()

        viewModel.deleteAllLocalData()

        awaitUntil { booksInRoom().isEmpty() }
        assertNull(stored("1"))
        assertNull(runBlocking { settingsRepository.selfContactId.first() })
        assertTrue(runBlocking { settingsRepository.groupOrder.first() }.isEmpty())
    }

    @Test
    fun `signing out clears the account and keeps local books`() {
        signedInAccount()
        seed(entity("1"))
        val viewModel = viewModel()

        viewModel.logout()

        assertNull(authRepository.credentials.await { it == null })
        assertNotNull(stored("1"))
    }
}
