// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.settings.SettingsSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * Links to the address book list, where each book's appearance, visibility and sharing are set.
 * The contact list's filter sheet is the primary way in; this is the secondary one.
 *
 * The second row only points at where sharing with other apps is set (per book, on each book's
 * page) and summarizes it ([sharedBookCount] of [bookCount]); nothing is controlled from here, so
 * there is a single place to change it.
 */
@Composable
fun AddressBooksLinkSection(
    bookCount: Int,
    sharedBookCount: Int,
    onClick: () -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_address_books)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.edit_action_manage_address_books)) },
            supportingContent = { Text(stringResource(R.string.settings_address_books_description)) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.system_contacts_title)) },
            supportingContent = {
                Text(
                    if (sharedBookCount == 0) {
                        stringResource(R.string.system_contacts_summary_none)
                    } else {
                        stringResource(R.string.system_contacts_summary_shared, sharedBookCount, bookCount)
                    }
                )
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddressBooksLinkSectionPreview() {
    CorvidContactsTheme {
        AddressBooksLinkSection(bookCount = 3, sharedBookCount = 1, onClick = {})
    }
}
