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
    ],
    version = 21,
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

        internal val MIGRATIONS = arrayOf(MIGRATION_19_20, MIGRATION_20_21)

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
