// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.repository.RepositoryTestBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Before

/**
 * Base for view model tests: the real repositories over in-memory Room (see [RepositoryTestBase]),
 * with `Dispatchers.Main` replaced so `viewModelScope` runs eagerly, and the settings these tests
 * change reset first, because the DataStores outlive a test inside one JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase : RepositoryTestBase() {

    @Before
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initializeWorkManager()
        resetSettings()
    }

    /**
     * Saving the birthday setting schedules or cancels a WorkManager job, and WorkManager is not
     * initialized in a unit test. The instance is process-wide, so it is only created once.
     */
    private fun initializeWorkManager() {
        try {
            WorkManager.initialize(context, Configuration.Builder().build())
        } catch (_: IllegalStateException) {
            // Already initialized by an earlier test in this process.
        }
    }

    @After
    fun restoreMainDispatcher() {
        // Cancels every view model made here. One left running would keep reacting to the shared
        // settings and credentials, and could undo the set-up of a later test.
        recorders.cancel()
        viewModels.clear()
        localServers.forEach { it.shutdown() }
        localServers.clear()
        Dispatchers.resetMain()
    }

    private val recorders = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val localServers = mutableListOf<MockWebServer>()

    /**
     * Credentials for an account whose server is a local mock that refuses everything. For tests that
     * only need a signed-in account: some view models sync as soon as there is one, and a test must
     * not do that against the internet.
     */
    protected fun signedInAccount(): NextcloudCredentials {
        val mock = MockWebServer()
        mock.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(503)
        }
        mock.start()
        localServers += mock
        return NextcloudCredentials(mock.url("/").toString(), "ada", "app-password")
            .also { runBlocking { authRepository.saveCredentials(it) } }
    }

    /**
     * Starts collecting this flow and returns what it has emitted so far, as a list that keeps
     * growing. For one-shot events, which are dropped when nothing is collecting.
     */
    protected fun <T> Flow<T>.record(): List<T> {
        val seen = CopyOnWriteArrayList<T>()
        recorders.launch { collect { seen += it } }
        return seen
    }

    private val viewModels = ViewModelStore()
    private var viewModelCount = 0

    /** Makes a view model that is cleared, and its coroutines cancelled, when the test ends. */
    protected inline fun <reified T : ViewModel> createViewModel(crossinline create: () -> T): T =
        newViewModel(T::class.java) { create() }

    @PublishedApi
    internal fun <T : ViewModel> newViewModel(type: Class<T>, create: () -> T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        }
        // A distinct key lets one test make more than one view model of the same kind.
        return ViewModelProvider.create(viewModels, factory)["${type.name}#${viewModelCount++}", type]
    }

    /** Puts back the stored settings a view model reads, so one test's choices don't reach the next. */
    private fun resetSettings() = runBlocking {
        settingsRepository.apply {
            saveLocalOnlyMode(false)
            saveLocalOnboardingCompleted(false)
            saveLastOnboardedAccountKey(null)
            saveSelfContactId(null)
            saveAlwaysAddCountryCode(true)
            saveBirthdayNotificationsEnabled(false)
            saveAutoLoadRemotePhotos(false)
            clearResolvedLocalBookHrefs()
            saveGroupOrder(emptyList())
            savedServers.first().forEach { removeSavedServer(it) }
        }
    }

    /**
     * The first value of this flow that satisfies [predicate]. The stored settings are read on
     * another thread, so a view model that reads them settles a moment after it is created.
     */
    /** Waits until [condition] holds, polling, for results a view model produces in the background. */
    protected fun awaitUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean) = runBlocking {
        withTimeout(timeoutMillis) {
            while (!condition()) delay(20)
        }
    }

    protected fun <T> Flow<T>.await(timeoutMillis: Long = 5_000, predicate: (T) -> Boolean): T =
        runBlocking { withTimeout(timeoutMillis) { first(predicate) } }
}
