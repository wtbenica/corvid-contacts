// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A contact that is kept out of the system contacts on this device, because it was deleted there
 * from another app or hidden from the contact's own screen. The contact itself, in Corvid and on
 * the server, is untouched. Lives in its
 * own table, and is never synced, for the same reason as [SystemContactMirrorEntity]: sync
 * replaces contact rows wholesale, and hiding a contact here says nothing about other devices.
 */
@Entity(tableName = "system_contact_hidden")
data class SystemContactHiddenEntity(
    @PrimaryKey val contactId: ContactId,
    /** Whether the notice on the contact's screen has been dismissed, or was never needed. */
    val noticeDismissed: Boolean = false,
)
