// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Determines the start destination based on login, local-only, and onboarding state.
 */
class MainViewModel(
    private val authRepository: AuthRepository,
    private val contactsRepository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * The screen to navigate to at startup, or [Destination.Resolving] while login/onboarding 
     * state is still being resolved.
     */
    private val _startDestination = MutableStateFlow<Destination?>(Destination.Resolving)
    val startDestination: StateFlow<Destination?> = _startDestination.asStateFlow()

    /** The user's preferred theme mode. */
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ThemeMode.SYSTEM
        )

    /**
     * Opts into local-only mode.
     */
    fun continueWithoutAccount() {
        viewModelScope.launch {
            contactsRepository.enterLocalOnlyMode()
        }
    }

    init {
        viewModelScope.launch {
            combine(
                authRepository.credentials,
                settingsRepository.lastOnboardedAccountKey,
                settingsRepository.localOnlyMode,
                settingsRepository.localOnboardingCompleted
            ) { credentials, lastOnboardedAccountKey, localOnlyMode, localOnboardingCompleted ->
                // Heal stray local-only flag if credentials exist.
                if (credentials != null && localOnlyMode) {
                    launch { settingsRepository.saveLocalOnlyMode(false) }
                }

                when {
                    credentials != null && credentials.accountKey == lastOnboardedAccountKey -> Destination.ContactList
                    credentials != null -> Destination.Onboarding
                    localOnlyMode && localOnboardingCompleted -> Destination.ContactList
                    localOnlyMode -> Destination.Onboarding
                    else -> Destination.Welcome
                }
            }.collect { destination ->
                _startDestination.value = destination
            }
        }
    }
}
