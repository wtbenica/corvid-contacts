// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens an old database through every migration up to the current schema. Room validates
 * the migrated tables against the entities on open, so a migration that doesn't match the schema
 * fails here instead of crashing the app at launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    @Test
    fun `migrates from version 19 keeping address books private by default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // The contacts table hasn't changed since version 19, so borrow its DDL from the current
        // schema rather than hand-copying it.
        val scratch = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val contactsDdl = scratch.openHelper.writableDatabase
            .query("SELECT sql FROM sqlite_master WHERE name = 'contacts'")
            .use {
                it.moveToFirst()
                it.getString(0)
            }
        scratch.close()

        val dbName = "migration-test.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(contactsDdl)
            db.execSQL(
                "CREATE TABLE `address_books` (`href` TEXT NOT NULL, `displayName` TEXT, " +
                    "`isVisible` INTEGER NOT NULL, `colorInt` INTEGER NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, `iconName` TEXT, PRIMARY KEY(`href`))"
            )
            db.execSQL("INSERT INTO address_books VALUES ('local://contacts', 'My Contacts', 1, 5, 0, NULL)")
            db.version = 19
        }

        val migrated = Room
            .databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val books = migrated.addressBookDao().getAllAddressBooks().first()
                assertEquals(listOf("local://contacts"), books.map { it.href })
                assertFalse(books.single().shareWithSystem)

                val mirror = migrated.systemContactMirrorDao()
                mirror.upsert(listOf(SystemContactMirrorEntity("c1", 10, "h")))
                mirror.upsertGroups(listOf(SystemGroupMirrorEntity("book:local://contacts", 3, "My Contacts")))
                assertEquals(1, mirror.getAll().size)
                assertEquals(1, mirror.getAllGroups().size)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun `group mapping rows from version 21 keep working under a book key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scratch = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val contactsDdl = scratch.openHelper.writableDatabase
            .query("SELECT sql FROM sqlite_master WHERE name = 'contacts'")
            .use {
                it.moveToFirst()
                it.getString(0)
            }
        scratch.close()

        val dbName = "migration-test-21.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(contactsDdl)
            db.execSQL(
                "CREATE TABLE `address_books` (`href` TEXT NOT NULL, `displayName` TEXT, " +
                    "`isVisible` INTEGER NOT NULL, `colorInt` INTEGER NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, `iconName` TEXT, " +
                    "`shareWithSystem` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`href`))"
            )
            db.execSQL(
                "CREATE TABLE `system_contact_mirror` (`contactId` TEXT NOT NULL, " +
                    "`rawContactId` INTEGER NOT NULL, `hash` TEXT NOT NULL, PRIMARY KEY(`contactId`))"
            )
            db.execSQL(
                "CREATE TABLE `system_group_mirror` (`bookHref` TEXT NOT NULL, " +
                    "`groupId` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`bookHref`))"
            )
            db.execSQL("INSERT INTO system_group_mirror VALUES ('/books/main/', 42, 'Main')")
            db.version = 21
        }

        val migrated = Room
            .databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val groups = migrated.systemContactMirrorDao().getAllGroups()
                assertEquals(listOf("book:/books/main/"), groups.map { it.groupKey })
                assertEquals(42L, groups.single().groupId)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun `books from version 22 start at the most private level and the level can be changed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scratch = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val contactsDdl = scratch.openHelper.writableDatabase
            .query("SELECT sql FROM sqlite_master WHERE name = 'contacts'")
            .use {
                it.moveToFirst()
                it.getString(0)
            }
        scratch.close()

        val dbName = "migration-test-22.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(contactsDdl)
            db.execSQL(
                "CREATE TABLE `address_books` (`href` TEXT NOT NULL, `displayName` TEXT, " +
                    "`isVisible` INTEGER NOT NULL, `colorInt` INTEGER NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, `iconName` TEXT, " +
                    "`shareWithSystem` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`href`))"
            )
            db.execSQL(
                "CREATE TABLE `system_contact_mirror` (`contactId` TEXT NOT NULL, " +
                    "`rawContactId` INTEGER NOT NULL, `hash` TEXT NOT NULL, PRIMARY KEY(`contactId`))"
            )
            db.execSQL(
                "CREATE TABLE `system_group_mirror` (`groupKey` TEXT NOT NULL, " +
                    "`groupId` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`groupKey`))"
            )
            db.execSQL("INSERT INTO address_books VALUES ('local://shared', 'Shared', 1, 5, 0, NULL, 1)")
            db.execSQL("INSERT INTO address_books VALUES ('local://private', 'Private', 1, 6, 1, NULL, 0)")
            db.version = 22
        }

        val migrated = Room
            .databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val dao = migrated.addressBookDao()
                val books = dao.getAllAddressBooks().first().associateBy { it.href }
                assertEquals(SystemContactsLevel.CALLER_ID, books.getValue("local://shared").systemContactsLevel)
                assertEquals(SystemContactsLevel.CALLER_ID, books.getValue("local://private").systemContactsLevel)
                // Existing sharing choices are untouched.
                assertTrue(books.getValue("local://shared").shareWithSystem)
                assertFalse(books.getValue("local://private").shareWithSystem)

                dao.updateSystemContactsLevel("local://shared", SystemContactsLevel.EVERYTHING)
                val after = dao.getAllAddressBooks().first().associateBy { it.href }
                assertEquals(SystemContactsLevel.EVERYTHING, after.getValue("local://shared").systemContactsLevel)
                assertEquals(SystemContactsLevel.CALLER_ID, after.getValue("local://private").systemContactsLevel)

                // The mirror's view of shared books carries each one's own level.
                val shared = migrated.systemContactMirrorDao().observeSharedBooks().first()
                assertEquals(listOf("local://shared"), shared.map { it.href })
                assertEquals(SystemContactsLevel.EVERYTHING, shared.single().systemContactsLevel)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }
}
