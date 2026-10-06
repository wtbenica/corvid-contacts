// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.ui.ViewModelTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Settings screen's state: stored choices, import and export, creating books, and signing out. */
class SettingsViewModelTest : ViewModelTestBase() {

    private val localHref = ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF
    private val account = NextcloudCredentials("https://cloud.example.org/", "ada", "secret")

    private fun settings() =
        createViewModel { SettingsViewModel(repository, settingsRepository, authRepository) }

    // --- Account ------------------------------------------------------------------------------

    @Test
    fun `the server and user come from the signed in account`() {
        runBlocking { authRepository.saveCredentials(account) }
        val viewModel = settings()

        assertEquals("https://cloud.example.org/", viewModel.serverUrl.await { it != null })
        assertEquals("ada", viewModel.username.await { it != null })
    }

    @Test
    fun `with no account there is no server or user`() {
        val viewModel = settings()

        assertNull(viewModel.serverUrl.await { true })
        assertNull(viewModel.username.await { true })
    }

    @Test
    fun `signing out clears the account and server data but keeps local books`() {
        runBlocking { authRepository.saveCredentials(account) }
        seedBooks(book("/server/", "Server"), book(localHref, "My Contacts"))
        seedContacts(contact("s1", "/server/"), contact("l1", localHref))
        runBlocking { settingsRepository.saveSelfContactId("s1") }
        val viewModel = settings()

        viewModel.logout()

        // Logging out is a sequence: credentials, then data, then settings. Wait for the last step.
        awaitUntil { runBlocking { settingsRepository.selfContactId.first() == null } }
        assertNull(runBlocking { authRepository.credentials.first() })
        assertEquals(listOf(localHref), booksInRoom().map { it.href })
        assertNull(runBlocking { settingsRepository.selfContactId.first() })
    }

    // --- Stored choices -----------------------------------------------------------------------

    @Test
    fun `the country code choice is read from and saved to the stored setting`() {
        val viewModel = settings()
        // The view model starts at false until the stored value (true by default) arrives.
        assertTrue(viewModel.alwaysAddCountryCode.await { it })

        viewModel.setAlwaysAddCountryCode(false)

        assertFalse(settingsRepository.alwaysAddCountryCode.await { !it })
        assertFalse(viewModel.alwaysAddCountryCode.await { !it })
    }

    @Test
    fun `the address lookup mode is saved`() {
        val viewModel = settings()
        assertEquals(AddressLookupMode.PHOTON, viewModel.addressLookupMode.await { true })

        viewModel.setAddressLookupMode(AddressLookupMode.OFF)

        assertEquals(AddressLookupMode.OFF, viewModel.addressLookupMode.await { it == AddressLookupMode.OFF })
    }

    @Test
    fun `the theme is saved`() {
        val viewModel = settings()

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.await { it == ThemeMode.LIGHT })
    }

    @Test
    fun `birthday reminders can be turned on and off`() {
        val viewModel = settings()

        viewModel.setBirthdayNotificationsEnabled(true)
        assertTrue(settingsRepository.birthdayNotificationsEnabled.await { it })

        viewModel.setBirthdayNotificationsEnabled(false)
        assertFalse(settingsRepository.birthdayNotificationsEnabled.await { !it })
    }

    @Test
    fun `remote photos are off until chosen, and the choice is saved`() {
        val viewModel = settings()
        assertFalse(viewModel.autoLoadRemotePhotos.await { true })

        viewModel.setAutoLoadRemotePhotos(true)

        assertTrue(viewModel.autoLoadRemotePhotos.await { it })
    }

    @Test
    fun `redoing setup forgets which account was onboarded`() {
        runBlocking {
            settingsRepository.saveLastOnboardedAccountKey(account.accountKey)
            settingsRepository.saveLocalOnboardingCompleted(true)
        }
        val viewModel = settings()

        viewModel.resetOnboarding()

        assertNull(settingsRepository.lastOnboardedAccountKey.await { it == null })
        assertFalse(settingsRepository.localOnboardingCompleted.await { !it })
    }

    // --- Address books ------------------------------------------------------------------------

    @Test
    fun `the books on offer are the ones the user manages, not archive books`() {
        seedBooks(book("/a/", "Friends"), book("/z/", "Archived"))
        val viewModel = settings()

        val books = viewModel.addressBooks.await { it.isNotEmpty() }

        assertEquals(listOf("/a/"), books.map { it.href })
    }

    @Test
    fun `a new local address book keeps its name, color and icon`() {
        val viewModel = settings()
        val color = Color(0xFF336699)

        val result = runBlocking { viewModel.createAddressBook("Family", color, forceLocal = true, iconName = "home") }

        assertTrue(result.isSuccess)
        val created = booksInRoom().single { it.displayName == "Family" }
        assertTrue(created.isLocal)
        assertEquals(color.toArgb(), created.colorInt)
        assertEquals("home", created.iconName)
    }

    // --- Import and export --------------------------------------------------------------------

    @Test
    fun `an export holds every contact as vCards`() {
        seedBooks(book(localHref))
        seedContacts(contact("c1", localHref, "Ada Lovelace"), contact("c2", localHref, "Charles Babbage"))
        val viewModel = settings()

        val text = runBlocking { viewModel.getExportData() }

        assertTrue(text.contains("FN:Ada Lovelace"))
        assertTrue(text.contains("FN:Charles Babbage"))
        assertEquals(2, "BEGIN:VCARD".toRegex().findAll(text).count())
    }

    @Test
    fun `importing makes a new contact of each vCard and counts them`() {
        seedBooks(book(localHref))
        val viewModel = settings()
        val vcards = """
            BEGIN:VCARD
            VERSION:4.0
            FN:Ada Lovelace
            END:VCARD
            BEGIN:VCARD
            VERSION:4.0
            FN:Charles Babbage
            END:VCARD
        """.trimIndent()

        val result = runBlocking { viewModel.importContacts(vcards, localHref, downloadRemotePhotos = false) }

        assertEquals(2, result.imported)
        assertEquals(0, result.failed)
        val names = runBlocking { database.contactDao().getAllContactsSync() }.map { it.contact.displayName }
        assertEquals(setOf("Ada Lovelace", "Charles Babbage"), names.toSet())
    }

    @Test
    fun `importing text with no vCards imports nothing`() {
        seedBooks(book(localHref))

        val result = runBlocking { settings().importContacts("not a vcard", localHref, downloadRemotePhotos = false) }

        assertEquals(0, result.imported)
        assertTrue(runBlocking { database.contactDao().getAllContactsSync() }.isEmpty())
    }

    @Test
    fun `a file with a photo link is flagged so the user can be asked about fetching it`() {
        val viewModel = settings()
        val withLink = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada\nPHOTO:https://example.org/ada.jpg\nEND:VCARD"
        val embedded = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada\nPHOTO:data:image/jpeg;base64,/9j/4AAQSkZJRg==\nEND:VCARD"
        val none = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada\nEND:VCARD"

        assertTrue(viewModel.importFileHasRemotePhotos(withLink))
        assertFalse(viewModel.importFileHasRemotePhotos(embedded))
        assertFalse(viewModel.importFileHasRemotePhotos(none))
    }

    @Test
    fun `an imported contact lands in the chosen book`() {
        seedBooks(book(localHref), book("/other/", "Other"))

        runBlocking {
            settings().importContacts("BEGIN:VCARD\nVERSION:4.0\nFN:Ada\nEND:VCARD", "/other/", false)
        }

        val saved = runBlocking { database.contactDao().getAllContactsSync() }.single().contact
        assertEquals("/other/", saved.addressBookHref)
        assertNotNull(saved.id)
    }
}
