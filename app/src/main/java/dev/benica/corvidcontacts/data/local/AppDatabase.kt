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
    entities = [ContactEntity::class, AddressBookEntity::class, SystemContactMirrorEntity::class],
    version = 20,
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
                    .addMigrations(MIGRATION_19_20)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
