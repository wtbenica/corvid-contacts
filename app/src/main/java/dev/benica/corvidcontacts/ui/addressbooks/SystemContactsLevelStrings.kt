// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import androidx.annotation.StringRes
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.SystemContactsLevel

/** The name of a sharing level, e.g. "Caller ID". */
@StringRes
fun SystemContactsLevel.titleRes(): Int = when (this) {
    SystemContactsLevel.CALLER_ID -> R.string.system_contacts_level_caller_id
    SystemContactsLevel.FULL -> R.string.system_contacts_level_full
    SystemContactsLevel.EVERYTHING -> R.string.system_contacts_level_everything
}

/** What a sharing level adds on top of the one before it. */
@StringRes
fun SystemContactsLevel.descriptionRes(): Int = when (this) {
    SystemContactsLevel.CALLER_ID -> R.string.system_contacts_level_caller_id_description
    SystemContactsLevel.FULL -> R.string.system_contacts_level_full_description
    SystemContactsLevel.EVERYTHING -> R.string.system_contacts_level_everything_description
}
