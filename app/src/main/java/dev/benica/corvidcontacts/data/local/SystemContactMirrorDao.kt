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

private const val SHARED_BOOKS_QUERY =
    "SELECT * FROM address_books WHERE shareWithSystem = 1 ORDER BY sortOrder ASC"

/**
 * Non-archived contacts in shared address books, with only the columns the mirror needs. Photos
 * are read from their files, so `photoUrl` is deliberately omitted.
 */
private const val MIRROR_SOURCES_QUERY =
    """
    SELECT c.id, c.displayName, c.firstName, c.lastName, c.middleName, c.prefix, c.suffix,
           c.phones, c.emails, c.structuredAddresses, c.websites, c.socialProfiles,
           c.relationships, c.birthday, c.company, c.jobTitle, c.nickname, c.notes,
           c.categories, c.hasPhoto, c.addressBookHref AS addressBookHref
    FROM contacts c
    INNER JOIN address_books b ON c.addressBookHref = b.href
    WHERE c.isArchived = 0 AND b.shareWithSystem = 1
    """

/** A contact hidden from the system contacts, with what the review list needs to name it. */
data class HiddenContact(
    val id: ContactId,
    val displayName: String,
    val firstName: String?,
    val lastName: String?,
    val addressBookHref: String?,
    val noticeDismissed: Boolean,
) {
    val name: String
        get() = displayName.ifBlank {
            listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        }
}

@Dao
interface SystemContactMirrorDao {
    @Query("SELECT * FROM system_contact_mirror")
    suspend fun getAll(): List<SystemContactMirrorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entries: List<SystemContactMirrorEntity>)

    /** Forces the next reconcile to rewrite these contacts, whatever Corvid's own state is. */
    @Query("UPDATE system_contact_mirror SET hash = '' WHERE contactId IN (:contactIds)")
    suspend fun markStale(contactIds: List<ContactId>)

    @Query("DELETE FROM system_contact_mirror WHERE contactId IN (:contactIds)")
    suspend fun delete(contactIds: List<ContactId>)

    @Query("DELETE FROM system_contact_mirror")
    suspend fun deleteAll()

    @Query("SELECT contactId FROM system_contact_hidden")
    suspend fun getHiddenIds(): List<ContactId>

    @Query("SELECT contactId FROM system_contact_hidden")
    fun observeHiddenIds(): Flow<List<ContactId>>

    @Query(
        """
        SELECT c.id, c.displayName, c.firstName, c.lastName, c.addressBookHref, h.noticeDismissed
        FROM contacts c
        INNER JOIN system_contact_hidden h ON h.contactId = c.id
        ORDER BY c.displayName COLLATE NOCASE ASC
        """
    )
    fun observeHiddenContacts(): Flow<List<HiddenContact>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun hide(entries: List<SystemContactHiddenEntity>)

    @Query("UPDATE system_contact_hidden SET noticeDismissed = 1 WHERE contactId = :contactId")
    suspend fun dismissHiddenNotice(contactId: ContactId)

    @Query("DELETE FROM system_contact_hidden WHERE contactId = :contactId")
    suspend fun unhide(contactId: ContactId)

    /** Forgets hidden contacts that no longer exist. */
    @Query("DELETE FROM system_contact_hidden WHERE contactId NOT IN (SELECT id FROM contacts)")
    suspend fun pruneHidden()

    @Query("SELECT * FROM system_group_mirror")
    suspend fun getAllGroups(): List<SystemGroupMirrorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<SystemGroupMirrorEntity>)

    @Query("DELETE FROM system_group_mirror WHERE groupKey IN (:groupKeys)")
    suspend fun deleteGroups(groupKeys: List<String>)

    @Query("DELETE FROM system_group_mirror")
    suspend fun deleteAllGroups()

    /** Address books the user has chosen to share with the system contacts. */
    @Query(SHARED_BOOKS_QUERY)
    fun observeSharedBooks(): Flow<List<AddressBookEntity>>

    /** The same books as [observeSharedBooks], read once. */
    @Query(SHARED_BOOKS_QUERY)
    suspend fun getSharedBooks(): List<AddressBookEntity>

    @Query(MIRROR_SOURCES_QUERY)
    fun observeMirrorSources(): Flow<List<MirrorSource>>

    /** The same contacts as [observeMirrorSources], read once. */
    @Query(MIRROR_SOURCES_QUERY)
    suspend fun getMirrorSources(): List<MirrorSource>
}
