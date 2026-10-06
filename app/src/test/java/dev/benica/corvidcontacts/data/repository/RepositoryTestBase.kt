// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.ContactEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A [ContactsRepository] over an in-memory Room database, for tests that exercise it as the app
 * does. Runs under Robolectric for the Context, DataStore and libphonenumber the repository uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        authRepository = AuthRepository(context)
        settingsRepository = SettingsRepository(context)
        // The DataStores outlive a test within the JVM, so a login saved by an earlier test is still there.
        runBlocking { authRepository.clearCredentials() }
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
        database.close()
    }

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
