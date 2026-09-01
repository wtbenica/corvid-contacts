// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.annotation.ColorInt
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.benica.corvidcontacts.data.repository.ContactsRepository.Companion.LOCAL_ADDRESS_BOOK_PREFIX

@Entity(tableName = "address_books")
data class AddressBookEntity(
    @PrimaryKey val href: String,
    val displayName: String?,
    val isVisible: Boolean = true,
    @ColorInt val colorInt: Int,
    val sortOrder: Int = 0,
    val iconName: String? = null,
) {
    val isLocal: Boolean
        get() = href.startsWith(LOCAL_ADDRESS_BOOK_PREFIX)
}
