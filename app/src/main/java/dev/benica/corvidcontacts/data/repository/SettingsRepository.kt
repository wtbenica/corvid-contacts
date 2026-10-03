// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.sync.BirthdayWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

/**
 * Persists and exposes general app preferences (as opposed to login credentials, which live in
 * [AuthRepository]) via a dedicated DataStore Preferences file (`settings_prefs`).
 *
 * Covers phone-number formatting, address-geocoding provider choice, the first-run onboarding
 * flag, birthday-notification opt-in (which also toggles [BirthdayWorker]'s scheduled work),
 * saved-server history for the login screen, and contact-group ordering.
 *
 * @param context Application context used to access the DataStore.
 */
class SettingsRepository(private val context: Context) {

    private val moshi = Moshi
        .Builder()
        .build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(
            List::class.java,
            String::class.java
        )
    )

    private object PreferencesKeys {
        val ALWAYS_ADD_COUNTRY_CODE = booleanPreferencesKey("always_add_country_code")
        val SELF_CONTACT_ID = stringPreferencesKey("self_contact_id")
        val GROUP_ORDER = stringPreferencesKey("group_order")
        val ADDRESS_LOOKUP_MODE = stringPreferencesKey("address_lookup_mode")
        val LAST_ONBOARDED_ACCOUNT_KEY = stringPreferencesKey("last_onboarded_account_key")
        val BIRTHDAY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("birthday_notifications_enabled")
        val SAVED_SERVERS = stringPreferencesKey("saved_servers")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val LOCAL_ONBOARDING_COMPLETED = booleanPreferencesKey("local_onboarding_completed")
        val RESOLVED_LOCAL_BOOK_HREFS = stringPreferencesKey("resolved_local_book_hrefs")
        val AUTO_LOAD_REMOTE_PHOTOS = booleanPreferencesKey("auto_load_remote_photos")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /** Whether the local country code should be auto-prepended to phone numbers. Defaults to `true`. */
    val alwaysAddCountryCode: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.ALWAYS_ADD_COUNTRY_CODE] ?: true
    }

    /** The contact ID the user has designated as their own "My Card", or `null` if unset. */
    val selfContactId: Flow<String?> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELF_CONTACT_ID]
    }

    /** The user's preferred display order for contact groups, stored as a JSON-encoded list. */
    val groupOrder: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.GROUP_ORDER]?.let { json ->
            try {
                stringListAdapter.fromJson(json)
            } catch (_: Exception) {
                null
            }
        } ?: emptyList()
    }

    /** The user's preferred address lookup mode. Defaults to [AddressLookupMode.PHOTON]. */
    val addressLookupMode: Flow<AddressLookupMode> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADDRESS_LOOKUP_MODE]?.let { name ->
            try {
                AddressLookupMode.valueOf(name)
            } catch (_: Exception) {
                null
            }
        } ?: AddressLookupMode.PHOTON
    }

    /**
     * Whether a contact's externally-hosted photo (e.g. from a Google Contacts import, referenced
     * by URL rather than embedded in the vCard) should be automatically downloaded and cached
     * during sync. When `false`, such a contact simply has no photo (falls back to initials) until
     * this is turned on or the user manually downloads it for that one contact - see
     * [dev.benica.corvidcontacts.data.repository.ContactsRepository.downloadContactPhoto]. Defaults
     * to `false`: downloading means contacting whatever third-party server hosts that photo, which
     * shouldn't happen without consent.
     */
    val autoLoadRemotePhotos: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_LOAD_REMOTE_PHOTOS] ?: false
    }

    /** The user's preferred theme mode. Defaults to [ThemeMode.SYSTEM]. */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE]?.let { name ->
            try {
                ThemeMode.valueOf(name)
            } catch (_: Exception) {
                null
            }
        } ?: ThemeMode.SYSTEM
    }

    /**
     * The [dev.benica.corvidcontacts.data.model.NextcloudCredentials.accountKey] of the account that most recently completed the
     * first-run onboarding flow, or `null` if none has. `MainViewModel` compares this against the
     * currently logged-in account's key to decide whether to route to
     * [Destination.Onboarding][dev.benica.corvidcontacts.navigation.Destination.Onboarding] or the
     * contact list - so onboarding runs once per account, not once per device, and logging out
     * doesn't need to (and doesn't) touch this value.
     */
    val lastOnboardedAccountKey: Flow<String?> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_ONBOARDED_ACCOUNT_KEY]
    }

    /**
     * Whether daily birthday-reminder notifications are enabled. Defaults to `false`. Toggling
     * this via [saveBirthdayNotificationsEnabled] schedules or cancels [BirthdayWorker]'s
     * periodic work accordingly.
     */
    val birthdayNotificationsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.BIRTHDAY_NOTIFICATIONS_ENABLED] ?: false
        }

    /**
     * Whether the user chose to use the app without connecting to a CardDAV server - contacts are
     * stored only in the local Room database. Independent of [AuthRepository.credentials], which
     * remains `null` either way before login; this flag is what distinguishes "hasn't logged in
     * yet" from "chose not to." Defaults to `false`.
     */
    val localOnlyMode: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOCAL_ONLY_MODE] ?: false
    }

    /**
     * Whether the first-run onboarding flow has been completed for local-only mode - the
     * local-only equivalent of [lastOnboardedAccountKey], which doesn't apply since there's no
     * account to key it to. Defaults to `false`, and is reset alongside [saveLocalOnlyMode] by
     * [dev.benica.corvidcontacts.data.repository.ContactsRepository.deleteAllLocalData], so
     * onboarding plays again the next time local-only mode is entered.
     */
    val localOnboardingCompleted: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.LOCAL_ONBOARDING_COMPLETED] ?: false
        }

    /** The set of server URLs the user has previously logged into, offered as suggestions on the login screen. */
    val savedServers: Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVED_SERVERS]?.let { json ->
            try {
                stringListAdapter
                    .fromJson(json)
                    ?.toSet()
            } catch (_: Exception) {
                null
            }
        } ?: emptySet()
    }

    /**
     * Hrefs of local-only address books that have
     * already been offered a keep-local-or-upload decision, so [dev.benica.corvidcontacts.MainViewModel]
     * knows not to route back into onboarding for them again. A book lands here either by going
     * through onboarding's local-data-migration step, or by being created as local-only in the
     * first place (see [dev.benica.corvidcontacts.data.repository.ContactsRepository.createAddressBook]) -
     * that choice IS the decision, so it doesn't need asking about again either. Only additive
     * (see [markLocalBooksResolved]): a book kept local stays in this set indefinitely, since
     * "resolved" tracks whether it's been dealt with, not whether it still exists.
     */
    val resolvedLocalBookHrefs: Flow<Set<String>> =
        context.settingsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.RESOLVED_LOCAL_BOOK_HREFS]?.let { json ->
                try {
                    stringListAdapter
                        .fromJson(json)
                        ?.toSet()
                } catch (_: Exception) {
                    null
                }
            } ?: emptySet()
        }

    /** Sets whether the local country code should be auto-prepended to phone numbers. */
    suspend fun saveAlwaysAddCountryCode(alwaysAdd: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ALWAYS_ADD_COUNTRY_CODE] = alwaysAdd
        }
    }

    /** Sets the contact ID representing the user's own "My Card", or clears it if [id] is `null`. */
    suspend fun saveSelfContactId(id: String?) {
        context.settingsDataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(PreferencesKeys.SELF_CONTACT_ID)
            } else {
                preferences[PreferencesKeys.SELF_CONTACT_ID] = id
            }
        }
    }

    /** Persists the user's preferred display [order] for contact groups. */
    suspend fun saveGroupOrder(order: List<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.GROUP_ORDER] = stringListAdapter.toJson(order)
        }
    }

    /** Sets the preferred address lookup mode. */
    suspend fun saveAddressLookupMode(mode: AddressLookupMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ADDRESS_LOOKUP_MODE] = mode.name
        }
    }

    /** Sets whether externally-hosted contact photos are downloaded automatically (see [autoLoadRemotePhotos]). */
    suspend fun saveAutoLoadRemotePhotos(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOAD_REMOTE_PHOTOS] = enabled
        }
    }

    /** Sets the user's preferred theme mode. */
    suspend fun saveThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    /**
     * Records [accountKey] as having completed the first-run onboarding flow, or clears the
     * record entirely when `null` (which re-triggers onboarding regardless of which account is
     * currently logged in - see [SettingsViewModel.resetOnboarding][dev.benica.corvidcontacts.ui.settings.SettingsViewModel.resetOnboarding]).
     */
    suspend fun saveLastOnboardedAccountKey(accountKey: String?) {
        context.settingsDataStore.edit { preferences ->
            if (accountKey != null) {
                preferences[PreferencesKeys.LAST_ONBOARDED_ACCOUNT_KEY] = accountKey
            } else {
                preferences.remove(PreferencesKeys.LAST_ONBOARDED_ACCOUNT_KEY)
            }
        }
    }

    /**
     * Sets whether daily birthday-reminder notifications are [enabled], and schedules or cancels
     * [BirthdayWorker]'s periodic work to match.
     */
    suspend fun saveBirthdayNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.BIRTHDAY_NOTIFICATIONS_ENABLED] = enabled
        }
        if (enabled) {
            BirthdayWorker.scheduleDailyCheck(context)
        } else {
            BirthdayWorker.cancelDailyCheck(context)
        }
    }

    /** Sets whether the user is using the app in local-only mode (see [localOnlyMode]). */
    suspend fun saveLocalOnlyMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_ONLY_MODE] = enabled
        }
    }

    /** Sets whether local-only mode's first-run onboarding flow has been completed (see [localOnboardingCompleted]). */
    suspend fun saveLocalOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_ONBOARDING_COMPLETED] = completed
        }
    }

    /** Adds [url] to the set of saved servers offered on the login screen. */
    suspend fun addSavedServer(url: String) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SAVED_SERVERS]?.let { json ->
                try {
                    stringListAdapter
                        .fromJson(json)
                        ?.toMutableSet()
                } catch (_: Exception) {
                    null
                }
            } ?: mutableSetOf()
            current.add(url)
            preferences[PreferencesKeys.SAVED_SERVERS] = stringListAdapter.toJson(current.toList())
        }
    }

    /** Removes [url] from the set of saved servers offered on the login screen. */
    suspend fun removeSavedServer(url: String) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SAVED_SERVERS]?.let { json ->
                try {
                    stringListAdapter
                        .fromJson(json)
                        ?.toMutableSet()
                } catch (_: Exception) {
                    null
                }
            } ?: mutableSetOf()
            current.remove(url)
            preferences[PreferencesKeys.SAVED_SERVERS] = stringListAdapter.toJson(current.toList())
        }
    }

    /** Adds [hrefs] to the set of local address books already offered a keep-local-or-upload decision (see [resolvedLocalBookHrefs]). */
    suspend fun markLocalBooksResolved(hrefs: Collection<String>) {
        if (hrefs.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.RESOLVED_LOCAL_BOOK_HREFS]?.let { json ->
                try {
                    stringListAdapter
                        .fromJson(json)
                        ?.toMutableSet()
                } catch (_: Exception) {
                    null
                }
            } ?: mutableSetOf()
            current.addAll(hrefs)
            preferences[PreferencesKeys.RESOLVED_LOCAL_BOOK_HREFS] =
                stringListAdapter.toJson(current.toList())
        }
    }

    /**
     * Forgets every local address book resolution recorded by [markLocalBooksResolved].
     */
    suspend fun clearResolvedLocalBookHrefs() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.RESOLVED_LOCAL_BOOK_HREFS)
        }
    }

}
