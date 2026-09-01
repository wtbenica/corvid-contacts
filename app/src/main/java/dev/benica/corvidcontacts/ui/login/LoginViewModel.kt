// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.remote.AuthInterceptor
import dev.benica.corvidcontacts.data.remote.NextcloudService
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Handles authenticating against a Nextcloud server for the login screen.
 *
 * Verifies credentials by probing the server (Nextcloud's OCS user-info endpoint first, falling
 * back to generic CardDAV principal discovery for non-Nextcloud/OCS-disabled servers) before
 * persisting them via [AuthRepository]. Also surfaces and manages the [SettingsRepository]-backed
 * list of previously-used servers shown as suggestions on the login screen.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Server URLs the user has previously logged into successfully, for the saved-servers dropdown. */
    val savedServers = settingsRepository.savedServers

    /**
     * Attempts to log in to [serverUrl] with the given [username]/[appPassword], updating [uiState]
     * with the result. On success, saves credentials and remembers [serverUrl] for future logins.
     */
    fun login(
        serverUrl: String,
        username: String,
        appPassword: String,
        fetchRemotePhotos: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                // Ensure server URL has trailing slash and add https:// prefix
                val formattedUrl =
                    "https://${if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"}"

                // Create a temporary Retrofit instance to check credentials
                val client = OkHttpClient
                    .Builder()
                    .addInterceptor(
                        AuthInterceptor(
                            username,
                            appPassword
                        )
                    )
                    .build()

                val moshi = Moshi
                    .Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val retrofit = Retrofit
                    .Builder()
                    .baseUrl(formattedUrl)
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                val service = retrofit.create(NextcloudService::class.java)

                // Try Nextcloud OCS API first
                var isAuthorized = false
                try {
                    val response = service.getUserInfo()
                    if (response.ocs.meta.statusCode in listOf(
                            100,
                            200
                        )
                    ) {
                        isAuthorized = true
                    }
                } catch (_: Exception) {
                    // Not a Nextcloud server or OCS API disabled
                }

                // If OCS failed, try generic principal discovery
                if (!isAuthorized) {
                    try {
                        val body =
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:current-user-principal /></d:prop></d:propfind>"
                        val requestBody = body.toRequestBody("application/xml".toMediaType())

                        // Try Nextcloud path first
                        var response = service.getPrincipal(body = requestBody)
                        if (response.isSuccessful) {
                            isAuthorized = true
                        } else {
                            // Try root path
                            response = service.getPrincipalGeneric(body = requestBody)
                            if (response.isSuccessful) {
                                isAuthorized = true
                            }
                        }
                    } catch (_: Exception) {
                        // Not a CardDAV server or invalid credentials
                    }
                }

                if (isAuthorized) {
                    val credentials = NextcloudCredentials(
                        formattedUrl,
                        username,
                        appPassword
                    )
                    // Save preferences first. If we save credentials first, MainViewModel may 
                    // navigate away and cancel this ViewModel's scope before these finishes.
                    settingsRepository.addSavedServer(serverUrl)
                    settingsRepository.saveAutoLoadRemotePhotos(fetchRemotePhotos)
                    settingsRepository.saveLocalOnlyMode(false)
                    
                    authRepository.saveCredentials(credentials)
                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error(
                        R.string.login_error_generic,
                        401
                    )
                }
            } catch (_: java.net.UnknownServiceException) {
                _uiState.value = LoginUiState.Error(R.string.login_error_security)
            } catch (_: java.net.ConnectException) {
                _uiState.value = LoginUiState.Error(R.string.login_error_connection)
            } catch (_: Exception) {
                _uiState.value = LoginUiState.Error(R.string.login_error_unknown)
            }
        }
    }

    /** Removes [url] from the saved-servers list. */
    fun removeSavedServer(url: String) {
        viewModelScope.launch {
            settingsRepository.removeSavedServer(url)
        }
    }
}

/** UI state for the login screen. */
sealed class LoginUiState {
    /** No login attempt in progress. */
    object Idle : LoginUiState()

    /** A login attempt is currently being verified against the server. */
    object Loading : LoginUiState()

    /** Login succeeded and credentials have been saved. */
    object Success : LoginUiState()

    /**
     * Login failed.
     *
     * @param resId String resource describing the error to show the user.
     * @param formatArgs Optional formatting argument(s) for [resId] (e.g. an HTTP status code).
     */
    data class Error(
        val resId: Int,
        val formatArgs: Any? = null,
    ) : LoginUiState()
}
