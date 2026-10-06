// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.onboarding

import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.repository.AddressBookFixture
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.addressBookListResponse
import dev.benica.corvidcontacts.data.repository.homeSetPropfindResponse
import dev.benica.corvidcontacts.data.repository.principalPropfindResponse
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

/**
 * Which onboarding step comes next, and what each step saves: with no account (local-only), with
 * an account that was onboarded before (it only resolves local data), and with a new account
 * (which syncs in the background, against a mock server).
 */
class OnboardingViewModelTest : ViewModelTestBase() {

    private val localHref = ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF
    private var server: MockWebServer? = null
    private val serverBookHref = "/remote.php/dav/addressbooks/testuser/contacts/"

    @After
    fun stopServer() {
        server?.shutdown()
    }

    private fun onboarding() =
        createViewModel { OnboardingViewModel(repository, settingsRepository, authRepository) }

    private fun OnboardingViewModel.step(predicate: (OnboardingUiState) -> Boolean) =
        uiState.await(predicate = predicate)

    private fun runSetup(viewModel: OnboardingViewModel) =
        viewModel.saveSetupPreferences(
            alwaysAddCountryCode = true,
            addressLookupMode = AddressLookupMode.OFF,
            birthdayReminders = false,
        )

    private fun selfContactId() = runBlocking { settingsRepository.selfContactId.first() }

    private fun signIn(url: String = "https://cloud.example.org/") =
        NextcloudCredentials(url, "testuser", "app-password").also {
            runBlocking { authRepository.saveCredentials(it) }
        }

    /** A server with one empty book, so a background sync succeeds without any contacts. */
    private fun startEmptyServer(): NextcloudCredentials {
        val mock = MockWebServer()
        mock.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = request.body.clone().readUtf8()
                return when {
                    request.method == "REPORT" -> MockResponse().setResponseCode(207)
                        .setBody("<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\"/>")

                    request.method == "PROPFIND" -> MockResponse().setResponseCode(207).setBody(
                        when {
                            "current-user-principal" in body -> principalPropfindResponse("/remote.php/dav/principals/testuser/")
                            "addressbook-home-set" in body -> homeSetPropfindResponse(
                                "/remote.php/dav/principals/testuser/",
                                "/remote.php/dav/addressbooks/testuser/"
                            )
                            else -> addressBookListResponse(AddressBookFixture(serverBookHref, "Contacts"))
                        }
                    )

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        mock.start()
        server = mock
        return signIn(mock.url("/").toString())
    }

    // --- Local-only: no account ---------------------------------------------------------------

    @Test
    fun `the setup choices are saved`() {
        runBlocking { repository.ensureLocalAddressBookExists() }
        val viewModel = onboarding()

        viewModel.saveSetupPreferences(
            alwaysAddCountryCode = false,
            addressLookupMode = AddressLookupMode.GOOGLE,
            birthdayReminders = true,
        )
        viewModel.step { it == OnboardingUiState.SystemContactsSharing }

        runBlocking {
            assertFalse(settingsRepository.alwaysAddCountryCode.first())
            assertEquals(AddressLookupMode.GOOGLE, settingsRepository.addressLookupMode.first())
            assertTrue(settingsRepository.birthdayNotificationsEnabled.first())
        }
    }

    @Test
    fun `after setup the user is offered sharing when there is a book to share`() {
        runBlocking { repository.ensureLocalAddressBookExists() }
        val viewModel = onboarding()

        runSetup(viewModel)

        assertEquals(OnboardingUiState.SystemContactsSharing, viewModel.step { it != OnboardingUiState.Setup })
    }

    @Test
    fun `with no book to share, setup goes straight to picking a contact card`() {
        val viewModel = onboarding()

        runSetup(viewModel)

        assertEquals(OnboardingUiState.SelfContactSelection, viewModel.step { it != OnboardingUiState.Setup })
    }

    @Test
    fun `with a contact card already chosen and no book, setup completes onboarding`() {
        runBlocking { settingsRepository.saveSelfContactId("me") }
        val viewModel = onboarding()

        runSetup(viewModel)

        assertTrue(settingsRepository.localOnboardingCompleted.await { it })
    }

    @Test
    fun `sharing choices share the chosen books and unshare the rest`() {
        seedBooks(
            book("/a/", "A").copy(shareWithSystem = true),
            book("/b/", "B"),
            book("/c/", "C").copy(shareWithSystem = true),
        )
        val viewModel = onboarding()

        viewModel.saveSharingChoices(setOf("/b/", "/c/"))
        viewModel.step { it == OnboardingUiState.SelfContactSelection }

        val shared = booksInRoom().filter { it.shareWithSystem }.map { it.href }.toSet()
        assertEquals(setOf("/b/", "/c/"), shared)
    }

    @Test
    fun `choosing no books unshares everything and moves on`() {
        seedBooks(book("/a/", "A").copy(shareWithSystem = true))
        val viewModel = onboarding()

        viewModel.saveSharingChoices(emptySet())
        viewModel.step { it == OnboardingUiState.SelfContactSelection }

        assertTrue(booksInRoom().none { it.shareWithSystem })
    }

    @Test
    fun `sharing choices complete onboarding when a contact card is already chosen`() {
        seedBooks(book("/a/", "A"))
        runBlocking { settingsRepository.saveSelfContactId("me") }
        val viewModel = onboarding()

        viewModel.saveSharingChoices(setOf("/a/"))

        assertTrue(settingsRepository.localOnboardingCompleted.await { it })
        assertTrue(booksInRoom().single().shareWithSystem)
    }

    @Test
    fun `choosing a contact card saves it and completes onboarding`() {
        val viewModel = onboarding()

        viewModel.setSelfContact("c1")

        assertTrue(settingsRepository.localOnboardingCompleted.await { it })
        assertEquals("c1", selfContactId())
    }

    @Test
    fun `skipping the contact card clears any earlier choice and completes onboarding`() {
        runBlocking { settingsRepository.saveSelfContactId("old") }
        val viewModel = onboarding()

        viewModel.setSelfContact(null)

        assertTrue(settingsRepository.localOnboardingCompleted.await { it })
        assertNull(selfContactId())
    }

    @Test
    fun `the new contact form can be opened and backed out of`() {
        val viewModel = onboarding()

        viewModel.startCreatingSelfContact()
        assertEquals(OnboardingUiState.CreatingSelfContact, viewModel.uiState.value)

        viewModel.cancelCreatingSelfContact()
        assertEquals(OnboardingUiState.SelfContactSelection, viewModel.uiState.value)
    }

    @Test
    fun `a new contact is saved, becomes the contact card and completes onboarding`() {
        seedBooks(book(localHref))
        val viewModel = onboarding()

        val saved = runBlocking { viewModel.saveNewSelfContact(contact("me", localHref, "Ada Lovelace")) }

        assertTrue(saved)
        assertNotNull(contactInRoom("me"))
        assertTrue(settingsRepository.localOnboardingCompleted.await { it })
        assertEquals("me", selfContactId())
    }

    // --- Signed in, onboarded before ----------------------------------------------------------

    @Test
    fun `an onboarded account with local books is asked what to do with them`() {
        val account = signIn()
        runBlocking { settingsRepository.saveLastOnboardedAccountKey(account.accountKey) }
        seedBooks(book(localHref, "My Contacts"))
        seedContacts(contact("c1", localHref))

        val viewModel = onboarding()

        val step = viewModel.step { it is OnboardingUiState.LocalDataMigration } as OnboardingUiState.LocalDataMigration
        assertEquals(listOf(localHref), step.localBooks.map { it.href })
        assertFalse("no sync is started when only resolving local data", viewModel.isBackgroundSyncing.value)
    }

    @Test
    fun `an onboarded account with only an empty default local book drops it and carries on`() {
        val account = signIn()
        runBlocking { settingsRepository.saveLastOnboardedAccountKey(account.accountKey) }
        seedBooks(book(localHref, "My Contacts"))

        val viewModel = onboarding()

        awaitUntil { booksInRoom().none { it.href == localHref } }
        assertEquals(OnboardingUiState.Setup, viewModel.uiState.value)
        assertEquals(account.accountKey, runBlocking { settingsRepository.lastOnboardedAccountKey.first() })
    }

    @Test
    fun `keeping a local book while resuming marks it resolved and finishes`() {
        val account = signIn()
        runBlocking { settingsRepository.saveLastOnboardedAccountKey(account.accountKey) }
        seedBooks(book(localHref, "My Contacts"))
        seedContacts(contact("c1", localHref))
        val viewModel = onboarding()
        viewModel.step { it is OnboardingUiState.LocalDataMigration }

        viewModel.resolveLocalDataMigration(booksToUpload = emptyMap(), booksToDelete = emptySet())

        val resolved = settingsRepository.resolvedLocalBookHrefs.await { localHref in it }
        assertTrue(localHref in resolved)
        assertNotNull("kept, not deleted", contactInRoom("c1"))
        assertFalse(viewModel.isMigratingLocalData.await { !it })
    }

    @Test
    fun `discarding a local book while resuming deletes it with its contacts`() {
        val account = signIn()
        runBlocking { settingsRepository.saveLastOnboardedAccountKey(account.accountKey) }
        seedBooks(book(localHref, "My Contacts"))
        seedContacts(contact("c1", localHref))
        val viewModel = onboarding()
        val step = viewModel.step { it is OnboardingUiState.LocalDataMigration } as OnboardingUiState.LocalDataMigration

        viewModel.resolveLocalDataMigration(booksToUpload = emptyMap(), booksToDelete = step.localBooks.toSet())

        viewModel.isMigratingLocalData.await { !it }
        assertTrue(booksInRoom().none { it.href == localHref })
        assertNull(contactInRoom("c1"))
    }

    // --- Signed in, new account ---------------------------------------------------------------

    @Test
    fun `a new account syncs in the background and then offers sharing`() {
        startEmptyServer()
        val viewModel = onboarding()

        runSetup(viewModel)
        val step = viewModel.step { it == OnboardingUiState.SystemContactsSharing }

        assertEquals(OnboardingUiState.SystemContactsSharing, step)
        assertTrue("the server book was synced", booksInRoom().any { it.href == serverBookHref })
        assertFalse(viewModel.isBackgroundSyncing.await { !it })
    }

    @Test
    fun `a new account with local data is asked about it before the sync is waited on`() {
        startEmptyServer()
        seedBooks(book(localHref, "My Contacts"))
        seedContacts(contact("c1", localHref))
        val viewModel = onboarding()

        runSetup(viewModel)
        viewModel.step { it is OnboardingUiState.LocalDataMigration }

        viewModel.resolveLocalDataMigration(booksToUpload = emptyMap(), booksToDelete = emptySet())

        assertEquals(OnboardingUiState.SystemContactsSharing, viewModel.step { it == OnboardingUiState.SystemContactsSharing })
    }

    @Test
    fun `finishing onboarding with an account remembers that account`() {
        val account = startEmptyServer()
        val viewModel = onboarding()
        runSetup(viewModel)
        viewModel.step { it == OnboardingUiState.SystemContactsSharing }

        viewModel.saveSharingChoices(setOf(serverBookHref))
        viewModel.step { it == OnboardingUiState.SelfContactSelection }
        viewModel.setSelfContact(null)

        assertEquals(account.accountKey, settingsRepository.lastOnboardedAccountKey.await { it != null })
        assertTrue(booksInRoom().single { it.href == serverBookHref }.shareWithSystem)
    }

    @Test
    fun `signing out mid onboarding clears the account`() {
        // A new account syncs in the background, so give it a local server rather than the internet.
        startEmptyServer()
        val viewModel = onboarding()

        viewModel.logout()

        // Logging out runs in the background, so wait for the stored account to go.
        awaitUntil { runBlocking { authRepository.credentials.first() } == null }
        assertNull(runBlocking { authRepository.credentials.first() })
    }
}
