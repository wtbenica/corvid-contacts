// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import kotlinx.coroutines.flow.Flow

/**
 * The columns of a contact that can be mirrored to the system contacts, at any sharing level. The
 * level decides which of them are actually written.
 */
data class MirrorSource(
    val id: ContactId,
    val displayName: String,
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val prefix: String?,
    val suffix: String?,
    val phones: List<Phone>?,
    val emails: List<Email>?,
    val structuredAddresses: List<StructuredAddress>?,
    val websites: List<String>?,
    val socialProfiles: List<SocialProfile>?,
    val relationships: List<Relationship>?,
    val birthday: String?,
    val company: String?,
    val jobTitle: String?,
    val nickname: String?,
    val notes: String?,
    val categories: List<String>?,
    val hasPhoto: Boolean,
    val addressBookHref: String,
)

@Dao
interface SystemContactMirrorDao {
    @Query("SELECT * FROM system_contact_mirror")
    suspend fun getAll(): List<SystemContactMirrorEntity>

    /** The Corvid contact that the system raw contact [rawContactId] mirrors, if any. */
    @Query("SELECT contactId FROM system_contact_mirror WHERE rawContactId = :rawContactId")
    suspend fun getContactIdForRawContact(rawContactId: Long): ContactId?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entries: List<SystemContactMirrorEntity>)

    @Query("DELETE FROM system_contact_mirror WHERE contactId IN (:contactIds)")
    suspend fun delete(contactIds: List<ContactId>)

    @Query("DELETE FROM system_contact_mirror")
    suspend fun deleteAll()

    @Query("SELECT * FROM system_group_mirror")
    suspend fun getAllGroups(): List<SystemGroupMirrorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<SystemGroupMirrorEntity>)

    @Query("DELETE FROM system_group_mirror WHERE groupKey IN (:groupKeys)")
    suspend fun deleteGroups(groupKeys: List<String>)

    @Query("DELETE FROM system_group_mirror")
    suspend fun deleteAllGroups()

    /** Address books the user has chosen to share with the system contacts. */
    @Query("SELECT * FROM address_books WHERE shareWithSystem = 1 ORDER BY sortOrder ASC")
    fun observeSharedBooks(): Flow<List<AddressBookEntity>>

    /**
     * Non-archived contacts in shared address books, with only the columns the mirror needs.
     * Photos are read from their files, so `photoUrl` is deliberately omitted.
     */
    @Query(
        """
        SELECT c.id, c.displayName, c.firstName, c.lastName, c.middleName, c.prefix, c.suffix,
               c.phones, c.emails, c.structuredAddresses, c.websites, c.socialProfiles,
               c.relationships, c.birthday, c.company, c.jobTitle, c.nickname, c.notes,
               c.categories, c.hasPhoto, c.addressBookHref AS addressBookHref
        FROM contacts c
        INNER JOIN address_books b ON c.addressBookHref = b.href
        WHERE c.isArchived = 0 AND b.shareWithSystem = 1
        """
    )
    fun observeMirrorSources(): Flow<List<MirrorSource>>
}
