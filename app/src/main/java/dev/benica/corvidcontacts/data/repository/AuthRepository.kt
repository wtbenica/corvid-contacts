// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Persists and exposes the Nextcloud login credentials (server URL, username, and app password)
 * used to authenticate CardDAV requests.
 *
 * Backed by a Jetpack DataStore Preferences file (`auth_prefs`), separate from
 * [SettingsRepository]'s general app-preferences store.
 *
 * @param context Application context used to access the DataStore.
 */
class AuthRepository(private val context: Context) {

    private object PreferencesKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val APP_PASSWORD = stringPreferencesKey("app_password")
    }

    /**
     * Emits the currently saved [NextcloudCredentials], or `null` if the user isn't logged in
     * (i.e. any of server URL, username, or app password is missing).
     */
    val credentials: Flow<NextcloudCredentials?> = context.dataStore.data.map { preferences ->
        val serverUrl = preferences[PreferencesKeys.SERVER_URL]
        val username = preferences[PreferencesKeys.USERNAME]
        val appPassword = preferences[PreferencesKeys.APP_PASSWORD]

        if (serverUrl != null && username != null && appPassword != null) {
            NextcloudCredentials(
                serverUrl,
                username,
                appPassword
            )
        } else {
            null
        }
    }

    /**
     * Saves the given [credentials], overwriting any previously stored login details.
     */
    suspend fun saveCredentials(credentials: NextcloudCredentials) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            preferences[PreferencesKeys.USERNAME] = credentials.username
            preferences[PreferencesKeys.APP_PASSWORD] = credentials.appPassword
        }
    }

    /**
     * Clears all saved credentials, effectively logging the user out.
     */
    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
