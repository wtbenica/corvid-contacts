// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.annotation.ColorInt
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.ContactsRepository.Companion.LOCAL_ADDRESS_BOOK_PREFIX

@Entity(tableName = "address_books")
data class AddressBookEntity(
    @PrimaryKey val href: String,
    val displayName: String?,
    val isVisible: Boolean = true,
    @ColorInt val colorInt: Int,
    val sortOrder: Int = 0,
    val iconName: String? = null,
    /**
     * Whether this book's contacts are mirrored to the system contacts (see
     * [dev.benica.corvidcontacts.data.system.SystemContactsMirror]). Off by default: sharing is
     * opt-in per book, and a book that is removed and re-synced starts private again.
     */
    @ColumnInfo(defaultValue = "0") val shareWithSystem: Boolean = false,
    /**
     * How much of each contact is mirrored while [shareWithSystem] is on. Starts at the most
     * private level and is kept when sharing is switched off, so switching it back on restores
     * the previous choice.
     */
    @ColumnInfo(defaultValue = "CALLER_ID") val systemContactsLevel: SystemContactsLevel = SystemContactsLevel.CALLER_ID,
) {
    val isLocal: Boolean
        get() = href.startsWith(LOCAL_ADDRESS_BOOK_PREFIX)
}
