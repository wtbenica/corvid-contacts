// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.benica.corvidcontacts.data.model.Phone
import kotlinx.coroutines.flow.Flow

/** The columns of a contact that can be mirrored to the system contacts at the Caller ID level. */
data class MirrorSource(
    val id: ContactId,
    val displayName: String,
    val firstName: String?,
    val lastName: String?,
    val phones: List<Phone>?,
    val hasPhoto: Boolean,
)

@Dao
interface SystemContactMirrorDao {
    @Query("SELECT * FROM system_contact_mirror")
    suspend fun getAll(): List<SystemContactMirrorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entries: List<SystemContactMirrorEntity>)

    @Query("DELETE FROM system_contact_mirror WHERE contactId IN (:contactIds)")
    suspend fun delete(contactIds: List<ContactId>)

    @Query("DELETE FROM system_contact_mirror")
    suspend fun deleteAll()

    /**
     * Non-archived contacts, with only the columns the mirror needs. Photos are read from their
     * files, so `photoUrl` is deliberately omitted.
     */
    @Query(
        """
        SELECT id, displayName, firstName, lastName, phones, hasPhoto
        FROM contacts
        WHERE isArchived = 0
        """
    )
    fun observeMirrorSources(): Flow<List<MirrorSource>>
}
