// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.ui.ViewModelTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where the app starts: login, local-only mode and onboarding state decide the first screen. */
class MainViewModelTest : ViewModelTestBase() {

    private val account = NextcloudCredentials("https://cloud.example.org/", "ada", "secret")

    private fun startDestination(): Destination? {
        val viewModel = createViewModel { MainViewModel(authRepository, repository, settingsRepository) }
        return viewModel.startDestination.await { it != Destination.Resolving }
    }

    @Test
    fun `a first launch with nothing set goes to the welcome screen`() {
        assertEquals(Destination.Welcome, startDestination())
    }

    @Test
    fun `a signed in account that has been onboarded goes to the contact list`() {
        runBlocking {
            authRepository.saveCredentials(account)
            settingsRepository.saveLastOnboardedAccountKey(account.accountKey)
        }

        assertEquals(Destination.ContactList, startDestination())
    }

    @Test
    fun `a signed in account that has not been onboarded goes to onboarding`() {
        runBlocking { authRepository.saveCredentials(account) }

        assertEquals(Destination.Onboarding, startDestination())
    }

    @Test
    fun `signing into a different account than the onboarded one runs onboarding again`() {
        runBlocking {
            authRepository.saveCredentials(account)
            settingsRepository.saveLastOnboardedAccountKey("https://other.example.org/|someone")
        }

        assertEquals(Destination.Onboarding, startDestination())
    }

    @Test
    fun `the same account with a rotated password is still the onboarded account`() {
        runBlocking {
            authRepository.saveCredentials(account.copy(appPassword = "a-new-password"))
            settingsRepository.saveLastOnboardedAccountKey(account.accountKey)
        }

        assertEquals(Destination.ContactList, startDestination())
    }

    @Test
    fun `local only mode before onboarding goes to onboarding`() {
        runBlocking { settingsRepository.saveLocalOnlyMode(true) }

        assertEquals(Destination.Onboarding, startDestination())
    }

    @Test
    fun `local only mode after onboarding goes to the contact list`() {
        runBlocking {
            settingsRepository.saveLocalOnlyMode(true)
            settingsRepository.saveLocalOnboardingCompleted(true)
        }

        assertEquals(Destination.ContactList, startDestination())
    }

    @Test
    fun `a stray local only flag is cleared once there is an account`() {
        runBlocking {
            authRepository.saveCredentials(account)
            settingsRepository.saveLocalOnlyMode(true)
        }

        assertEquals(Destination.Onboarding, startDestination())

        // The flag is healed in the background, so wait for it to be written.
        assertFalse(settingsRepository.localOnlyMode.await { !it })
    }

    @Test
    fun `choosing to continue without an account enters local only mode and onboarding`() {
        val viewModel = createViewModel { MainViewModel(authRepository, repository, settingsRepository) }
        viewModel.startDestination.await { it == Destination.Welcome }

        viewModel.continueWithoutAccount()

        assertEquals(Destination.Onboarding, viewModel.startDestination.await { it == Destination.Onboarding })
        assertTrue(runBlocking { settingsRepository.localOnlyMode.first() })
        assertTrue(booksInRoom().any { it.href == ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF })
    }

    @Test
    fun `the theme follows the stored setting`() {
        val viewModel = createViewModel { MainViewModel(authRepository, repository, settingsRepository) }
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.await { true })

        runBlocking { settingsRepository.saveThemeMode(ThemeMode.DARK) }

        assertEquals(ThemeMode.DARK, viewModel.themeMode.await { it == ThemeMode.DARK })
    }
}
