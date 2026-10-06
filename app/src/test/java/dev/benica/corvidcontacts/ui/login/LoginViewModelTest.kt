// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.login

import dev.benica.corvidcontacts.R
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
import org.junit.Before
import org.junit.Test

/** Signing in: credentials are saved only after the server accepted them. Runs against a mock server. */
class LoginViewModelTest : ViewModelTestBase() {

    private lateinit var server: MockWebServer
    private val requests = mutableListOf<RecordedRequest>()

    // What the mock server answers, set per test.
    private var ocsStatus = 200
    private var ocsBody = """{"ocs":{"meta":{"status":"ok","statuscode":200,"message":"OK"},"data":{"id":"ada","displayname":"Ada"}}}"""
    private var principalStatus = 207
    private var genericPrincipalStatus = 207

    private val address get() = "${server.hostName}:${server.port}"

    @Before
    fun startServer() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when {
                    request.method == "GET" && request.path.orEmpty().startsWith("/ocs/v2.php/cloud/user") ->
                        MockResponse().setResponseCode(ocsStatus).setBody(ocsBody)

                    request.method == "PROPFIND" && request.path == "/remote.php/dav/" ->
                        MockResponse().setResponseCode(principalStatus)

                    request.method == "PROPFIND" -> MockResponse().setResponseCode(genericPrincipalStatus)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    private fun loginViewModel() =
        createViewModel { LoginViewModel(authRepository, settingsRepository, scheme = "http") }

    private fun LoginViewModel.signIn(fetchRemotePhotos: Boolean = false, serverAddress: String = address) {
        login(serverAddress, "ada", "app-password", fetchRemotePhotos)
    }

    private fun LoginViewModel.result() =
        uiState.await { it is LoginUiState.Success || it is LoginUiState.Error }

    private fun savedCredentials() = runBlocking { authRepository.credentials.first() }

    @Test
    fun `starts idle`() {
        assertEquals(LoginUiState.Idle, loginViewModel().uiState.value)
    }

    @Test
    fun `a server that accepts the Nextcloud user check signs in and saves the account`() {
        val viewModel = loginViewModel()

        viewModel.signIn()

        assertEquals(LoginUiState.Success, viewModel.result())
        val credentials = savedCredentials()!!
        assertEquals("http://$address/", credentials.serverUrl)
        assertEquals("ada", credentials.username)
        assertEquals("app-password", credentials.appPassword)
    }

    @Test
    fun `the login request carries the app password`() {
        loginViewModel().apply { signIn(); result() }

        val authorization = requests.first().getHeader("Authorization").orEmpty()
        assertTrue(authorization.startsWith("Basic "))
    }

    @Test
    fun `an address that already ends in a slash is not given a second one`() {
        val viewModel = loginViewModel()

        viewModel.signIn(serverAddress = "$address/")
        viewModel.result()

        assertEquals("http://$address/", savedCredentials()!!.serverUrl)
    }

    @Test
    fun `a Nextcloud status code of 100 also counts as signed in`() {
        ocsBody = """{"ocs":{"meta":{"status":"ok","statuscode":100,"message":"OK"},"data":{"id":"ada","displayname":"Ada"}}}"""
        val viewModel = loginViewModel()

        viewModel.signIn()

        assertEquals(LoginUiState.Success, viewModel.result())
    }

    @Test
    fun `a server without the Nextcloud check falls back to CardDAV discovery`() {
        ocsStatus = 404
        val viewModel = loginViewModel()

        viewModel.signIn()

        assertEquals(LoginUiState.Success, viewModel.result())
        assertNotNull(savedCredentials())
        assertTrue(requests.any { it.method == "PROPFIND" })
    }

    @Test
    fun `the generic CardDAV root is tried when the Nextcloud path fails`() {
        ocsStatus = 404
        principalStatus = 404
        val viewModel = loginViewModel()

        viewModel.signIn()

        assertEquals(LoginUiState.Success, viewModel.result())
        assertEquals(listOf("/remote.php/dav/", "/"), requests.filter { it.method == "PROPFIND" }.map { it.path })
    }

    @Test
    fun `refused credentials give an authentication error and save nothing`() {
        ocsStatus = 401
        principalStatus = 401
        genericPrincipalStatus = 401
        val viewModel = loginViewModel()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_generic, error.resId)
        assertEquals(401, error.formatArgs)
        assertNull(savedCredentials())
        assertTrue(runBlocking { settingsRepository.savedServers.first() }.isEmpty())
    }

    @Test
    fun `a server that forbids the sign-in is reported with its own status`() {
        ocsStatus = 403
        principalStatus = 403
        genericPrincipalStatus = 403
        val viewModel = loginViewModel()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_generic, error.resId)
        assertEquals(403, error.formatArgs)
    }

    @Test
    fun `a server error is reported as a server error with its status, not as bad credentials`() {
        ocsStatus = 500
        principalStatus = 500
        genericPrincipalStatus = 503
        val viewModel = loginViewModel()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_server, error.resId)
        assertEquals(503, error.formatArgs)
        assertNull(savedCredentials())
    }

    @Test
    fun `an address with no Nextcloud or CardDAV server says what the server answered`() {
        ocsStatus = 404
        principalStatus = 404
        genericPrincipalStatus = 404
        val viewModel = loginViewModel()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_server, error.resId)
        assertEquals(404, error.formatArgs)
    }

    @Test
    fun `a refusal of the credentials wins over a 404 from the last path tried`() {
        ocsStatus = 401
        principalStatus = 401
        genericPrincipalStatus = 404
        val viewModel = loginViewModel()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_generic, error.resId)
        assertEquals(401, error.formatArgs)
    }

    @Test
    fun `a Nextcloud answer that is not a success is not accepted`() {
        ocsBody = """{"ocs":{"meta":{"status":"failure","statuscode":997,"message":"Unauthorised"}}}"""
        principalStatus = 401
        genericPrincipalStatus = 401
        val viewModel = loginViewModel()

        viewModel.signIn()

        assertTrue(viewModel.result() is LoginUiState.Error)
        assertNull(savedCredentials())
    }

    @Test
    fun `a server that cannot be reached is reported as a connection problem`() {
        val viewModel = loginViewModel()
        server.shutdown()

        viewModel.signIn()

        val error = viewModel.result() as LoginUiState.Error
        assertEquals(R.string.login_error_connection, error.resId)
        assertNull(savedCredentials())
    }

    @Test
    fun `signing in remembers the server and the photo choice, and leaves local only mode`() {
        runBlocking { settingsRepository.saveLocalOnlyMode(true) }
        val viewModel = loginViewModel()

        viewModel.signIn(fetchRemotePhotos = true)
        viewModel.result()

        runBlocking {
            assertEquals(setOf(address), settingsRepository.savedServers.first())
            assertTrue(settingsRepository.autoLoadRemotePhotos.first())
            assertFalse(settingsRepository.localOnlyMode.first())
        }
    }

    @Test
    fun `a saved server can be removed from the list`() {
        runBlocking {
            settingsRepository.addSavedServer("a.example.org")
            settingsRepository.addSavedServer("b.example.org")
        }
        val viewModel = loginViewModel()

        viewModel.removeSavedServer("a.example.org")

        assertEquals(setOf("b.example.org"), viewModel.savedServers.await { "a.example.org" !in it })
    }
}
