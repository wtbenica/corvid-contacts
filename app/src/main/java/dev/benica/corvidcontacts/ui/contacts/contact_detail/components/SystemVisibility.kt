// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.local.HiddenContact

/**
 * How a contact stands with the system contacts, for its screen: whether it is hidden from them,
 * and whether the notice about that has been dismissed.
 */
data class SystemVisibility(val isHidden: Boolean, val noticeDismissed: Boolean) {
    val showsNotice: Boolean get() = isHidden && !noticeDismissed

    companion object {
        /**
         * The state for [contact], or `null` when the system contacts have nothing to do with it:
         * its book isn't shared and it isn't hidden.
         */
        fun of(contact: ContactWithAddressBook?, hidden: List<HiddenContact>): SystemVisibility? {
            val row = hidden.find { it.id == contact?.contact?.id }
            return when {
                row != null -> SystemVisibility(isHidden = true, noticeDismissed = row.noticeDismissed)
                contact?.addressBook?.shareWithSystem == true -> SystemVisibility(isHidden = false, noticeDismissed = false)
                else -> null
            }
        }
    }
}
