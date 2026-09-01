// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.ImportResult
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Settings screen: phone-formatting and address-geocoding preferences, vCard export,
 * and the "Redo Initial Setup" dev/testing aid. Address book and group management (appearance,
 * rename, delete, reorder, visibility) lives in the contact list's filter sheet instead - see
 * [dev.benica.corvidcontacts.ui.contacts.ContactsViewModel].
 */
class SettingsViewModel(
    private val repository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** The currently connected server's URL, or `null` in local-only mode / while logged out. */
    val serverUrl: StateFlow<String?> = authRepository.credentials
        .map { it?.serverUrl }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** The signed-in username on [serverUrl], or `null` in local-only mode / while logged out. */
    val username: StateFlow<String?> = authRepository.credentials
        .map { it?.username }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** Whether the local country code should be auto-prepended to phone numbers. */
    val alwaysAddCountryCode: StateFlow<Boolean> = settingsRepository.alwaysAddCountryCode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /** The current address lookup mode. */
    val addressLookupMode: StateFlow<AddressLookupMode> = settingsRepository.addressLookupMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AddressLookupMode.PHOTON
        )

    /** Whether externally-hosted contact photos are downloaded automatically during sync. */
    val autoLoadRemotePhotos: StateFlow<Boolean> = settingsRepository.autoLoadRemotePhotos
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /** The user's preferred theme mode. */
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ThemeMode.SYSTEM
        )

    /** Address books available as an import destination. */
    val addressBooks: StateFlow<List<AddressBookEntity>> = repository.userManageableAddressBooks
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Sets whether the local country code should be auto-prepended to phone numbers. */
    fun setAlwaysAddCountryCode(alwaysAdd: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAlwaysAddCountryCode(alwaysAdd)
        }
    }

    /** Sets the address lookup mode. */
    fun setAddressLookupMode(mode: AddressLookupMode) {
        viewModelScope.launch {
            settingsRepository.saveAddressLookupMode(mode)
        }
    }

    /** Sets whether externally-hosted contact photos are downloaded automatically during sync. */
    fun setAutoLoadRemotePhotos(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAutoLoadRemotePhotos(enabled)
            if (enabled) {
                repository.downloadAllPendingRemotePhotos()
            }
        }
    }

    /** Sets the user's preferred theme mode. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.saveThemeMode(mode)
        }
    }

    /** Clears the record of which account (or, in local-only mode, which device) last completed onboarding, so the app shows the onboarding flow again on next launch, regardless of which account is logged in. */
    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.saveLastOnboardedAccountKey(null)
            settingsRepository.saveLocalOnboardingCompleted(false)
        }
    }

    /** Exports every locally cached contact as a single merged multi-vCard text document. */
    suspend fun getExportData(): String {
        return repository.exportAllContacts()
    }

    /** Whether [vcardText] has any photo that's a URL reference rather than embedded data. */
    fun importFileHasRemotePhotos(vcardText: String): Boolean =
        repository.vCardTextHasRemotePhotoUrls(vcardText)

    /** Imports every vCard in [vcardText] into [targetAddressBookHref] as new contacts. */
    suspend fun importContacts(
        vcardText: String,
        targetAddressBookHref: String,
        downloadRemotePhotos: Boolean,
    ): ImportResult = repository.importVCardText(vcardText, targetAddressBookHref, downloadRemotePhotos)

    /** Creates a new address book with [displayName] and [color], see [ContactsRepository.createAddressBook]. */
    suspend fun createAddressBook(
        displayName: String,
        color: Color,
        forceLocal: Boolean = false,
    ): Result<AddressBookEntity> = repository.createAddressBook(
        displayName,
        color.toArgb(),
        forceLocal
    )

    /** Logs the user out, clearing credentials and that account's local data. */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
