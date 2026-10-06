// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Starts databases from the schemas Room exported for each older version (`app/schemas`, recovered
 * from the code at those versions, with 19 being the 1.0.4 release) and migrates them to the
 * latest, which Room then checks table by table against the current schema. The data tests put rows
 * in the old tables first, to see what the migrations do to them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val latest = 26
    private val oldestRelease = 19

    private fun migrate(name: String, from: Int, seed: (SupportSQLiteDatabase) -> Unit = {}): SupportSQLiteDatabase {
        helper.createDatabase(name, from).use(seed)
        return helper.runMigrationsAndValidate(name, latest, true, *AppDatabase.MIGRATIONS)
    }

    private fun SupportSQLiteDatabase.rows(sql: String): List<List<String?>> = query(sql).use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .map { row -> (0 until row.columnCount).map { if (row.isNull(it)) null else row.getString(it) } }
            .toList()
    }

    @Test
    fun `every older version migrates to the latest schema`() {
        (oldestRelease until latest).forEach { version -> migrate("from-$version", version).close() }
    }

    @Test
    fun `the released version 19 database keeps its contacts and books, with books private`() {
        val db = migrate("released", oldestRelease) {
            it.execSQL(
                "INSERT INTO contacts (id, displayName, firstName, lastName, phones, hasPhoto, isArchived) " +
                    "VALUES ('c1', 'Ada Lovelace', 'Ada', 'Lovelace', " +
                    "'[{\"value\":\"+1 555-123-4567\",\"type\":\"CELL\"}]', 0, 0)"
            )
            it.execSQL("INSERT INTO address_books VALUES ('local://contacts', 'My Contacts', 1, 5, 0, NULL)")
        }

        assertEquals(listOf(listOf("c1", "Ada Lovelace")), db.rows("SELECT id, displayName FROM contacts"))
        assertEquals(
            listOf(listOf("local://contacts", "0", "CALLER_ID")),
            db.rows("SELECT href, shareWithSystem, systemContactsLevel FROM address_books")
        )
    }

    @Test
    fun `a migrated version 19 database opens in Room and reads its old contacts`() {
        migrate("opens", oldestRelease) {
            it.execSQL(
                "INSERT INTO contacts (id, displayName, phones, emails, hasPhoto, isArchived) VALUES " +
                    "('c1', 'Ada', '[{\"value\":\"+1 555-123-4567\",\"type\":\"CELL\"}]', " +
                    "'[{\"value\":\"ada@example.org\",\"type\":\"HOME\"}]', 0, 0)"
            )
            it.execSQL("INSERT INTO address_books VALUES ('local://contacts', 'My Contacts', 1, 5, 0, NULL)")
        }.close()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val room = Room
            .databaseBuilder(context, AppDatabase::class.java, "opens")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val contact = room.contactDao().getContactById("c1")!!.contact
                assertEquals("+1 555-123-4567", contact.phones?.single()?.value)
                assertEquals("ada@example.org", contact.emails?.single()?.value)

                val book = room.addressBookDao().getAllAddressBooks().first().single()
                assertEquals(false, book.shareWithSystem)
                assertEquals(SystemContactsLevel.CALLER_ID, book.systemContactsLevel)
                assertTrue(room.systemContactMirrorDao().getAll().isEmpty())
            }
        } finally {
            room.close()
        }
    }

    @Test
    fun `group rows from version 21 keep working under a book key`() {
        val db = migrate("groups", 21) {
            it.execSQL("INSERT INTO system_group_mirror VALUES ('/books/main/', 42, 'Main')")
        }

        assertEquals(
            listOf(listOf("book:/books/main/", "42", "Main")),
            db.rows("SELECT groupKey, groupId, title FROM system_group_mirror")
        )
    }

    @Test
    fun `books from version 22 start at the most private level and keep their sharing choice`() {
        val db = migrate("levels", 22) {
            it.execSQL("INSERT INTO address_books VALUES ('local://shared', 'Shared', 1, 5, 0, NULL, 1)")
            it.execSQL("INSERT INTO address_books VALUES ('local://private', 'Private', 1, 6, 1, NULL, 0)")
        }

        assertEquals(
            listOf(
                listOf("local://private", "0", "CALLER_ID"),
                listOf("local://shared", "1", "CALLER_ID"),
            ),
            db.rows("SELECT href, shareWithSystem, systemContactsLevel FROM address_books ORDER BY href")
        )
    }

    @Test
    fun `mirror rows from version 23 gain a snapshot slot and are marked stale`() {
        val db = migrate("snapshots", 23) {
            it.execSQL("INSERT INTO system_contact_mirror VALUES ('c1', 10, 'abc')")
        }

        assertEquals(
            listOf(listOf("c1", "10", "")),
            db.rows("SELECT contactId, rawContactId, hash FROM system_contact_mirror")
        )
        assertNull(db.rows("SELECT snapshot FROM system_contact_mirror").single().single())
    }

    @Test
    fun `hidden contacts from version 25 keep their notice until dismissed`() {
        val db = migrate("hidden", 25) {
            it.execSQL("INSERT INTO system_contact_hidden VALUES ('c1')")
        }

        assertEquals(
            listOf(listOf("c1", "0")),
            db.rows("SELECT contactId, noticeDismissed FROM system_contact_hidden")
        )
    }
}
