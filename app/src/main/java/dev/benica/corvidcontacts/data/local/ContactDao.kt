// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    /**
     * Fetches all visible contacts. 
     * Note: Omit photoUrl to avoid SQLiteBlobTooBigException in lists.
     */
    @androidx.room.Transaction
    @Query(
        """
        SELECT id, displayName, firstName, lastName, middleName, prefix, suffix, 
               emails, phones, NULL as photoUrl, hasPhoto, etag, 
               addressBookHref, contactHref, colorInt, isArchived, categories, 
               company, jobTitle, birthday, nickname, notes, websites, socialProfiles,
               relationships, structuredAddresses 
        FROM contacts 
        WHERE addressBookHref IN (SELECT href FROM address_books WHERE isVisible = 1)
          AND isArchived = 0
        ORDER BY CASE WHEN displayName != '' THEN displayName ELSE TRIM(COALESCE(firstName, '') || ' ' || COALESCE(lastName, '')) END COLLATE NOCASE ASC
    """
    )
    fun getAllVisibleContacts(): Flow<List<ContactWithAddressBook>>

    /**
     * Fetches all archived contacts.
     * Note: Omit photoUrl to avoid SQLiteBlobTooBigException in lists.
     */
    @androidx.room.Transaction
    @Query(
        """
        SELECT id, displayName, firstName, lastName, middleName, prefix, suffix, 
               emails, phones, NULL as photoUrl, hasPhoto, etag, 
               addressBookHref, contactHref, colorInt, isArchived, categories, 
               company, jobTitle, birthday, nickname, notes, websites, socialProfiles,
               relationships, structuredAddresses
        FROM contacts
        WHERE isArchived = 1
        ORDER BY CASE WHEN displayName != '' THEN displayName ELSE TRIM(COALESCE(firstName, '') || ' ' || COALESCE(lastName, '')) END COLLATE NOCASE ASC
    """
    )
    fun getArchivedContacts(): Flow<List<ContactWithAddressBook>>

    @androidx.room.Transaction
    @Query("SELECT * FROM contacts ORDER BY CASE WHEN displayName != '' THEN displayName ELSE TRIM(COALESCE(firstName, '') || ' ' || COALESCE(lastName, '')) END COLLATE NOCASE ASC")
    fun getAllContacts(): Flow<List<ContactWithAddressBook>>

    @androidx.room.Transaction
    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsSync(): List<ContactWithAddressBook>

    @androidx.room.Transaction
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): ContactWithAddressBook?

    @Query("SELECT COUNT(*) FROM contacts WHERE addressBookHref = :addressBookHref")
    suspend fun getContactCountInAddressBook(addressBookHref: String): Int

    @Query("SELECT * FROM contacts WHERE photoUrl LIKE 'http%'")
    suspend fun getContactsWithPendingRemotePhotos(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE addressBookHref = :addressBookHref")
    suspend fun deleteContactsByAddressBook(addressBookHref: String)

    @Query("DELETE FROM contacts")
    suspend fun clearAll()

    // Keep the 'local://' prefix here in sync with ContactsRepository.LOCAL_ADDRESS_BOOK_PREFIX -
    // Room queries can't reference the Kotlin constant directly.
    @Query("DELETE FROM contacts WHERE addressBookHref IS NULL OR addressBookHref NOT LIKE 'local://%'")
    suspend fun clearAllExceptLocal()

    @androidx.room.Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getContactByIdFlow(contactId: ContactId): Flow<ContactWithAddressBook?>
}
