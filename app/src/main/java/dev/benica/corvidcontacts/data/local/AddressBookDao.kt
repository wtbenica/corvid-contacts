// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressBookDao {
    @Query("SELECT * FROM address_books ORDER BY sortOrder ASC")
    fun getAllAddressBooks(): Flow<List<AddressBookEntity>>

    @Query("SELECT * FROM address_books WHERE displayName NOT LIKE '%Archived%' AND displayName NOT LIKE '%archived%' ORDER BY sortOrder ASC")
    fun getUserManageableAddressBooks(): Flow<List<AddressBookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAddressBooks(addressBooks: List<AddressBookEntity>)

    @Update
    suspend fun updateAddressBook(addressBook: AddressBookEntity)

    @Delete
    suspend fun deleteAddressBooks(addressBooks: List<AddressBookEntity>)

    @Query("DELETE FROM address_books")
    suspend fun clearAll()

    // Keep the 'local://' prefix here in sync with [ContactsRepository.LOCAL_ADDRESS_BOOK_PREFIX] -
    // Room queries can't reference the Kotlin constant directly.
    @Query("DELETE FROM address_books WHERE href NOT LIKE 'local://%'")
    suspend fun clearAllExceptLocal()

    @Query("UPDATE address_books SET displayName = :displayName WHERE href = :href")
    suspend fun updateDisplayName(
        href: String,
        displayName: String?,
    )
}
