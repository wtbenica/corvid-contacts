// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records which system (`ContactsContract`) group mirrors a shared address book or a contact
 * category, and the title it was last given, so the group can be renamed or removed without
 * reading the system contacts. [groupKey] is built by `MirrorPlan.bookGroupKey` or
 * `MirrorPlan.categoryGroupKey`.
 */
@Entity(tableName = "system_group_mirror")
data class SystemGroupMirrorEntity(
    @PrimaryKey val groupKey: String,
    val groupId: Long,
    val title: String,
)
