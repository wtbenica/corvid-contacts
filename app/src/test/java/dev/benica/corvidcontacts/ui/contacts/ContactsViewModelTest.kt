// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.system.SystemContactVisibility
import dev.benica.corvidcontacts.ui.ViewModelTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the contact list shows and how it is narrowed, selected and picked from. */
class ContactsViewModelTest : ViewModelTestBase() {

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

    /** The ids of the contacts the list shows, once they satisfy [predicate]. */
    private fun ContactsViewModel.shown(predicate: (List<String>) -> Boolean = { true }): List<String> {
        val state = uiState.await {
            it is ContactsUiState.Success && predicate(it.contacts.map { c -> c.contact.id })
        }
        return (state as ContactsUiState.Success).contacts.map { it.contact.id }
    }

    private fun entity(
        id: String,
        name: String,
        book: String = "/a/",
        categories: List<String> = emptyList(),
        archived: Boolean = false,
        emails: List<Email> = emptyList(),
    ) = ContactEntity(
        id = id,
        displayName = name,
        addressBookHref = book,
        categories = categories,
        isArchived = archived,
        emails = emails,
    )

    private fun seedTwoBooks() {
        seedBooks(book("/a/", "Friends"), book("/b/", "Work"))
    }

    // --- What is shown ------------------------------------------------------------------------

    @Test
    fun `with no contacts the list is empty`() {
        assertEquals(emptyList<String>(), viewModel().shown())
    }

    @Test
    fun `contacts are shown from every visible book, sorted by name`() {
        seedTwoBooks()
        seedContacts(entity("1", "Charles", "/a/"), entity("2", "Ada", "/b/"), entity("3", "Babbage", "/a/"))

        assertEquals(listOf("2", "3", "1"), viewModel().shown { it.size == 3 })
    }

    @Test
    fun `contacts in a hidden book are left out of the list`() {
        seedBooks(book("/a/", "Friends"), book("/b/", "Work").copy(isVisible = false))
        seedContacts(entity("1", "Ada", "/a/"), entity("2", "Charles", "/b/"))

        assertEquals(listOf("1"), viewModel().shown { it.isNotEmpty() })
    }

    @Test
    fun `archived contacts are kept out of the list and shown in the archive view`() {
        seedTwoBooks()
        seedContacts(entity("active", "Ada"), entity("old", "Charles", archived = true))
        val viewModel = viewModel()
        assertEquals(listOf("active"), viewModel.shown { it.isNotEmpty() })

        viewModel.toggleShowArchived()

        assertEquals(listOf("old"), viewModel.shown { it == listOf("old") })
        assertTrue(viewModel.showArchived.value)
    }

    @Test
    fun `search narrows the list and clearing it brings everyone back`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada Lovelace"), entity("2", "Charles Babbage"))
        val viewModel = viewModel()
        viewModel.shown { it.size == 2 }

        viewModel.updateSearchQuery("babbage")
        assertEquals(listOf("2"), viewModel.shown { it.size == 1 })

        viewModel.updateSearchQuery("")
        assertEquals(listOf("1", "2"), viewModel.shown { it.size == 2 })
    }

    @Test
    fun `the group filter narrows the list`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada", categories = listOf("Family")), entity("2", "Charles", categories = listOf("Work")))
        val viewModel = viewModel()
        viewModel.shown { it.size == 2 }

        viewModel.updateSelectedGroup("Family")

        assertEquals(listOf("1"), viewModel.shown { it.size == 1 })
        assertEquals("Family", viewModel.selectedGroup.value)
    }

    @Test
    fun `selecting one address book narrows the list to it`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada", "/a/"), entity("2", "Charles", "/b/"))
        val viewModel = viewModel()
        viewModel.shown { it.size == 2 }

        viewModel.selectAddressBook("/b/")

        assertEquals(listOf("2"), viewModel.shown { it.size == 1 })
        assertEquals(setOf("/b/"), viewModel.selectedAddressBookHrefs.await { it == setOf("/b/") })

        viewModel.selectAddressBook(null)
        assertEquals(2, viewModel.shown { it.size == 2 }.size)
    }

    @Test
    fun `by default every visible book is selected, and hidden books are still manageable`() {
        seedBooks(book("/a/", "Friends"), book("/b/", "Work").copy(isVisible = false), book("/z/", "Archived"))
        val viewModel = viewModel()

        assertEquals(setOf("/a/"), viewModel.selectedAddressBookHrefs.await { it.isNotEmpty() })
        assertEquals(listOf("/a/"), viewModel.addressBooks.await { it.isNotEmpty() }.map { it.href })
        assertEquals(setOf("/a/", "/b/"), viewModel.allManageableAddressBooks.await { it.size == 2 }.map { it.href }.toSet())
    }

    // --- Groups -------------------------------------------------------------------------------

    @Test
    fun `groups follow the saved order, with new ones added alphabetically and archive left out`() {
        seedTwoBooks()
        seedContacts(
            entity("1", "A", categories = listOf("Work", "Zebra", "Archived")),
            entity("2", "B", categories = listOf("Family", "Apple")),
        )
        runBlocking { settingsRepository.saveGroupOrder(listOf("Work", "Family", "Gone")) }

        // The saved order loads a moment after the groups, so wait for it to be applied.
        val groups = viewModel().allGroups.await { it.firstOrNull() == "Work" }

        assertEquals(listOf("Work", "Family", "Apple", "Zebra"), groups)
    }

    @Test
    fun `the groups shown are those of the selected books, and global groups cover every book`() {
        seedTwoBooks()
        seedContacts(
            entity("1", "A", "/a/", categories = listOf("Family")),
            entity("2", "B", "/b/", categories = listOf("Colleagues")),
        )
        val viewModel = viewModel()
        viewModel.allGroupsGlobal.await { it.size == 2 }

        viewModel.selectAddressBook("/b/")

        assertEquals(listOf("Colleagues"), viewModel.allGroups.await { it == listOf("Colleagues") })
        assertEquals(setOf("Colleagues"), viewModel.groupsAvailableForSelectedBooks.await { it == setOf("Colleagues") })
        assertEquals(setOf("Colleagues", "Family"), viewModel.allGroupsGlobal.await { it.size == 2 }.toSet())
    }

    // --- Selection ----------------------------------------------------------------------------

    @Test
    fun `selecting all picks the contacts that are shown, not hidden ones`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada"), entity("2", "Charles"), entity("3", "Babbage", archived = true))
        val viewModel = viewModel()
        viewModel.shown { it.size == 2 }

        viewModel.selectAll()

        assertEquals(setOf("1", "2"), viewModel.selectedContactIds.value)
        assertTrue(viewModel.isSelectionMode.value)
    }

    @Test
    fun `selection can be toggled, emptied and left`() {
        val viewModel = viewModel()

        viewModel.toggleSelection("1")
        viewModel.toggleSelection("2")
        assertEquals(setOf("1", "2"), viewModel.selectedContactIds.value)

        viewModel.deselectAll()
        assertTrue(viewModel.selectedContactIds.value.isEmpty())
        assertTrue(viewModel.isSelectionMode.value)

        viewModel.clearSelection()
        assertFalse(viewModel.isSelectionMode.value)
    }

    // --- Picking ------------------------------------------------------------------------------

    @Test
    fun `an external pick for an email shows only contacts with one, and stopping restores the list`() {
        seedTwoBooks()
        seedContacts(
            entity("1", "Ada", emails = listOf(Email("ada@example.org", null))),
            entity("2", "Charles"),
        )
        val viewModel = viewModel()
        viewModel.shown { it.size == 2 }

        viewModel.startPickingExternal(PickContent.EMAIL)
        assertEquals(listOf("1"), viewModel.shown { it.size == 1 })
        assertTrue(viewModel.isPickingExternal.value)
        assertEquals(PickContent.EMAIL, viewModel.requiredPickType.value)

        viewModel.stopPickingExternal()
        assertEquals(2, viewModel.shown { it.size == 2 }.size)
        assertFalse(viewModel.isPickingExternal.value)
        assertNull(viewModel.requiredPickType.value)
    }

    @Test
    fun `picking a merge target leaves out the contact being merged and anything not editable`() {
        seedBooks(book("/a/", "Friends"), book("/system/", "System"))
        seedContacts(entity("source", "Ada", "/a/"), entity("other", "Charles", "/a/"), entity("managed", "Babbage", "/system/"))
        val viewModel = viewModel()
        viewModel.shown { it.size == 3 }

        viewModel.startPickingMergeTarget("source")

        assertEquals(listOf("other"), viewModel.shown { it.size == 1 })
        assertTrue(viewModel.isPickingMergeTarget.await { it })
        assertEquals("source", viewModel.mergeSourceContactId.value)

        viewModel.stopPickingMergeTarget()
        assertEquals(3, viewModel.shown { it.size == 3 }.size)
        assertFalse(viewModel.isPickingMergeTarget.await { !it })
    }

    @Test
    fun `choosing my card saves it and ends picking`() {
        seedTwoBooks()
        seedContacts(entity("me", "Ada"))
        val viewModel = viewModel()
        viewModel.startPickingSelf()
        assertTrue(viewModel.isPickingSelf.value)

        viewModel.setSelfContactId("me")

        assertEquals("me", viewModel.selfContactId.await { it == "me" })
        assertEquals("me", viewModel.selfContact.await { it != null }?.contact?.id)
        assertFalse(viewModel.isPickingSelf.value)
    }

    @Test
    fun `my card is found even when it is not in the visible list`() {
        seedTwoBooks()
        seedContacts(entity("me", "Ada", archived = true))
        runBlocking { settingsRepository.saveSelfContactId("me") }

        assertEquals("me", viewModel().selfContact.await { it != null }?.contact?.id)
    }

    @Test
    fun `backing out of picking my card changes nothing`() {
        val viewModel = viewModel()
        viewModel.startPickingSelf()

        viewModel.stopPickingSelf()

        assertFalse(viewModel.isPickingSelf.value)
        assertNull(runBlocking { settingsRepository.selfContactId.first() })
    }

    // --- Mode and the system contacts ---------------------------------------------------------

    @Test
    fun `local only mode needs the flag and no account`() {
        val viewModel = viewModel()
        assertFalse(viewModel.isLocalOnlyMode.await { true })

        runBlocking { settingsRepository.saveLocalOnlyMode(true) }
        assertTrue(viewModel.isLocalOnlyMode.await { it })

        signedInAccount()
        assertFalse(viewModel.isLocalOnlyMode.await { !it })
    }

    @Test
    fun `a contact can be kept out of the system contacts and shown there again`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada"), entity("2", "Charles"))
        val viewModel = viewModel()

        viewModel.hideFromSystemContacts("1")
        val hidden = viewModel.hiddenFromSystemContacts.await { it.isNotEmpty() }
        assertEquals(listOf("1"), hidden.map { it.id })
        assertTrue("hiding on purpose needs no notice", hidden.single().noticeDismissed)

        viewModel.showInSystemContacts(listOf("1"))
        assertTrue(viewModel.hiddenFromSystemContacts.await { it.isEmpty() }.isEmpty())
    }

    @Test
    fun `the hidden notice can be dismissed without showing the contact again`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada"))
        runBlocking { database.systemContactMirrorDao().hide(listOf(dev.benica.corvidcontacts.data.local.SystemContactHiddenEntity("1", noticeDismissed = false))) }
        val viewModel = viewModel()
        assertFalse(viewModel.hiddenFromSystemContacts.await { it.isNotEmpty() }.single().noticeDismissed)

        viewModel.dismissHiddenNotice("1")

        assertTrue(viewModel.hiddenFromSystemContacts.await { it.single().noticeDismissed }.single().noticeDismissed)
    }

    // --- Syncing ------------------------------------------------------------------------------

    @Test
    fun `refreshing with no account quietly does nothing`() {
        val viewModel = viewModel()
        val events = viewModel.events.record()

        viewModel.refresh()

        assertFalse(viewModel.isRefreshing.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `signing in remembers the server address and starts a sync`() {
        val mock = MockWebServer().apply { start() }
        server = mock
        mock.enqueue(MockResponse().setResponseCode(500))
        val address = "${mock.hostName}:${mock.port}"
        runBlocking { authRepository.saveCredentials(NextcloudCredentials("http://$address/", "ada", "x")) }

        viewModel()

        assertTrue(address in settingsRepository.savedServers.await { address in it })
        assertTrue("a sync was attempted", mock.requestCount > 0 || run { awaitUntil { mock.requestCount > 0 }; true })
    }

    @Test
    fun `a failed sync with nothing to show replaces the list with an error`() {
        val mock = MockWebServer().apply { start() }
        server = mock
        mock.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = MockResponse().setResponseCode(500)
        }
        runBlocking { authRepository.saveCredentials(NextcloudCredentials(mock.url("/").toString(), "ada", "x")) }

        val viewModel = viewModel()

        val state = viewModel.uiState.await { it is ContactsUiState.Error } as ContactsUiState.Error
        assertEquals(R.string.common_error_sync_title, state.resId)
        assertFalse(viewModel.isRefreshing.await { !it })
    }

    @Test
    fun `a failed sync with contacts on screen is shown as a message instead`() {
        seedTwoBooks()
        seedContacts(entity("1", "Ada"))
        val mock = MockWebServer().apply { start() }
        server = mock
        mock.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = MockResponse().setResponseCode(500)
        }
        runBlocking { authRepository.saveCredentials(NextcloudCredentials(mock.url("/").toString(), "ada", "x")) }
        val viewModel = viewModel()
        val events = viewModel.events.record()
        viewModel.shown { it.size == 1 }

        viewModel.refresh()

        awaitUntil { events.any { it is ContactsEvent.Error } }
        val error = events.filterIsInstance<ContactsEvent.Error>().first()
        assertEquals(R.string.common_error_sync_title, error.resId)
        assertTrue(viewModel.uiState.value is ContactsUiState.Success)
    }
}
