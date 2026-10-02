// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records which system (`ContactsContract`) raw contact mirrors a Corvid contact, and a hash of
 * what was last written to it, so the mirror can be updated in place without ever reading the
 * system contacts. Lives in its own table rather than on [ContactEntity] because sync replaces
 * contact rows wholesale, which would wipe the mapping.
 */
@Entity(tableName = "system_contact_mirror")
data class SystemContactMirrorEntity(
    @PrimaryKey val contactId: ContactId,
    val rawContactId: Long,
    val hash: String,
)
