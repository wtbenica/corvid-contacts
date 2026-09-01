// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ContactWithAddressBook(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "addressBookHref",
        entityColumn = "href"
    )
    val addressBook: AddressBookEntity?,
)
