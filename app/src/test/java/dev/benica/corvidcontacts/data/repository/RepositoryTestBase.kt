// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.ContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A [ContactsRepository] over an in-memory Room database, for tests that exercise it as the app
 * does. Runs under Robolectric for the Context, DataStore and libphonenumber the repository uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
abstract class RepositoryTestBase {

    protected lateinit var context: Context
    protected lateinit var database: AppDatabase
    protected lateinit var authRepository: AuthRepository
    protected lateinit var settingsRepository: SettingsRepository
    protected lateinit var repository: ContactsRepository

    @Before
    fun createRepository() {
        context = ApplicationProvider.getApplicationContext()
        database = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        authRepository = AuthRepository(context, newDataStore("auth"))
        settingsRepository = SettingsRepository(context, newDataStore("settings"))
        repository = ContactsRepository(
            context = context,
            contactDao = database.contactDao(),
            addressBookDao = database.addressBookDao(),
            authRepository = authRepository,
            settingsRepository = settingsRepository,
        )
    }

    @After
    fun closeDatabase() {
        finishRoomWork()
        database.close()
        dataStoreScope.cancel()
        dataStoreDir.deleteRecursively()
    }

    // Unconfined, so a write finishes on the calling thread. On a background thread, a collector that
    // starts while a write is in flight can miss that write for good (seen in DataStore 1.2.1).
    /**
     * A view model cancelled mid-action can leave a Room call in flight. Closing the database under it
     * makes the call throw on a background thread, and the Compose test rule blames whichever test
     * starts next. Room's executors are serial or drain in order, so a marker task run on each
     * means everything queued before it has finished.
     */
    private fun finishRoomWork() {
        listOf(database.transactionExecutor, database.queryExecutor).forEach { executor ->
            val done = CountDownLatch(1)
            executor.execute { done.countDown() }
            done.await(5, TimeUnit.SECONDS)
        }
    }

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val dataStoreDir = File.createTempFile("datastore", "").also { it.delete(); it.mkdirs() }

    /** A store of this test's own: the app's are process-wide, and would carry one test's state into the next. */
    private fun newDataStore(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(dataStoreDir, "$name.preferences_pb") }

    protected fun seedBooks(vararg books: AddressBookEntity) =
        runBlocking { database.addressBookDao().insertAddressBooks(books.toList()) }

    protected fun seedContacts(vararg contacts: ContactEntity) =
        runBlocking { database.contactDao().insertContacts(contacts.toList()) }

    protected fun contactInRoom(id: String): ContactEntity? =
        runBlocking { database.contactDao().getContactById(id)?.contact }

    protected fun booksInRoom(): List<AddressBookEntity> =
        runBlocking { database.addressBookDao().getAllAddressBooks().first() }

    protected fun book(href: String, name: String = "Book", color: Int = 0xFF010101.toInt()) =
        AddressBookEntity(href = href, displayName = name, colorInt = color)

    protected fun contact(id: String, bookHref: String?, name: String = "Ada") = ContactEntity(
        id = id,
        displayName = name,
        addressBookHref = bookHref,
    )
}
