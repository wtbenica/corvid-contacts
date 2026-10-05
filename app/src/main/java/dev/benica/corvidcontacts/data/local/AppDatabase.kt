// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ContactEntity::class,
        AddressBookEntity::class,
        SystemContactMirrorEntity::class,
        SystemGroupMirrorEntity::class,
        SystemContactHiddenEntity::class,
    ],
    version = 25,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun addressBookDao(): AddressBookDao
    abstract fun systemContactMirrorDao(): SystemContactMirrorDao

    companion object {
        /** Adds the table that maps contacts to their mirrored system raw contacts. */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `system_contact_mirror` (" +
                        "`contactId` TEXT NOT NULL, " +
                        "`rawContactId` INTEGER NOT NULL, " +
                        "`hash` TEXT NOT NULL, " +
                        "PRIMARY KEY(`contactId`))"
                )
            }
        }

        /**
         * Adds the per-book sharing flag (off by default) and the table that maps shared address
         * books to their system contact groups.
         */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `address_books` ADD COLUMN `shareWithSystem` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `system_group_mirror` (" +
                        "`bookHref` TEXT NOT NULL, " +
                        "`groupId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "PRIMARY KEY(`bookHref`))"
                )
            }
        }

        /**
         * Generalizes the group mapping from address books only to address books and contact
         * categories: the key column is renamed and existing rows keep working under a `book:`
         * prefix.
         */
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `system_group_mirror_new` (" +
                        "`groupKey` TEXT NOT NULL, " +
                        "`groupId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "PRIMARY KEY(`groupKey`))"
                )
                db.execSQL(
                    "INSERT INTO `system_group_mirror_new` (`groupKey`, `groupId`, `title`) " +
                        "SELECT 'book:' || `bookHref`, `groupId`, `title` FROM `system_group_mirror`"
                )
                db.execSQL("DROP TABLE `system_group_mirror`")
                db.execSQL("ALTER TABLE `system_group_mirror_new` RENAME TO `system_group_mirror`")
            }
        }

        /**
         * Adds the per-book sharing level, replacing the single level that used to apply to every
         * shared book. Every book starts at the most private level.
         */
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `address_books` ADD COLUMN `systemContactsLevel` TEXT NOT NULL DEFAULT 'CALLER_ID'"
                )
            }
        }

        /**
         * Keeps what was last written to each mirrored contact, so edits made in other apps can be
         * read back. Existing rows have none, and are rewritten on the next reconcile.
         */
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `system_contact_mirror` ADD COLUMN `snapshot` TEXT")
                db.execSQL("UPDATE `system_contact_mirror` SET `hash` = ''")
            }
        }

        /** Adds the table of contacts hidden from the system contacts on this device. */
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `system_contact_hidden` " +
                        "(`contactId` TEXT NOT NULL, PRIMARY KEY(`contactId`))"
                )
            }
        }

        internal val MIGRATIONS = arrayOf(
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "corvid_contacts_db"
                    )
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
