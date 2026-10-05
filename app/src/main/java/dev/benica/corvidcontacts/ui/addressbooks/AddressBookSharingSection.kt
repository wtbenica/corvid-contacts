// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.HiddenContact
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingRadioButton

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/**
 * The rows that decide how a book is shared with the phone's contacts: the switch, how much of each
 * contact to share, and, when there are any, the contacts kept out of the copy. The level is always
 * shown and only dimmed while the book isn't shared, so the page doesn't change shape when sharing
 * is switched on or off.
 */
@Composable
fun AddressBookSharingSection(
    isShared: Boolean,
    level: SystemContactsLevel,
    hiddenCount: Int,
    onShareChanged: (Boolean) -> Unit,
    onLevelChanged: (SystemContactsLevel) -> Unit,
    onHiddenClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.address_book_setting_share)) },
        supportingContent = { Text(stringResource(R.string.address_book_setting_share_description)) },
        trailingContent = { Switch(checked = isShared, onCheckedChange = onShareChanged) },
    )

    val levelAlpha = if (isShared) 1f else DISABLED_ALPHA
    Text(
        text = stringResource(R.string.system_contacts_level_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .alpha(levelAlpha)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
    SystemContactsLevel.entries.forEach { option ->
        ListItem(
            headlineContent = { Text(stringResource(option.titleRes())) },
            supportingContent = { Text(stringResource(option.descriptionRes())) },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = option == level,
                    onClick = { onLevelChanged(option) },
                    enabled = isShared,
                )
            },
            modifier = Modifier
                .alpha(levelAlpha)
                .clickable(enabled = isShared) { onLevelChanged(option) },
        )
    }

    if (hiddenCount > 0) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.address_book_setting_hidden)) },
            supportingContent = { Text(stringResource(R.string.address_book_setting_hidden_description)) },
            trailingContent = { TrailingText(hiddenCount.toString()) },
            modifier = Modifier.clickable(onClick = onHiddenClick),
        )
    }
}

/** The contacts kept out of the phone's contacts, each with a way to put it back, or all at once. */
@Composable
fun HiddenContactsDialog(
    hiddenContacts: List<HiddenContact>,
    onShow: (List<ContactId>) -> Unit,
    onDismiss: () -> Unit,
) {
    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.address_book_setting_hidden,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.address_book_hidden_dialog_message))
                hiddenContacts.forEach { contact ->
                    ListItem(
                        headlineContent = { Text(contact.name) },
                        trailingContent = {
                            TextButton(onClick = { onShow(listOf(contact.id)) }) {
                                Text(stringResource(R.string.detail_hidden_from_system_show))
                            }
                        },
                    )
                }
            }
        },
        confirmButton = R.string.address_book_hidden_show_all,
        onConfirm = {
            onShow(hiddenContacts.map { it.id })
            onDismiss()
        },
        dismissButton = R.string.common_done,
    )
}

/** A value at the end of a row, in the same box as the row's other trailing controls. */
@Composable
private fun TrailingText(text: String) {
    Box(
        modifier = Modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
