// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records which system (`ContactsContract`) raw contact mirrors a Corvid contact, a hash of what
 * was last written to it, and that content as a [snapshot], so the mirror can be updated in place
 * and edits made in other apps can be told apart from Corvid's own. Lives in its own table rather
 * than on [ContactEntity] because sync replaces contact rows wholesale, which would wipe the mapping.
 *
 * An empty [hash] marks the row stale, which makes the next reconcile rewrite it.
 */
@Entity(tableName = "system_contact_mirror")
data class SystemContactMirrorEntity(
    @PrimaryKey val contactId: ContactId,
    val rawContactId: Long,
    val hash: String,
    /** JSON of the [dev.benica.corvidcontacts.data.system.MirrorContact] last written, if known. */
    val snapshot: String? = null,
)
