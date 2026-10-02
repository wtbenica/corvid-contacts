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
 * A link to the address book list, where each book's appearance, visibility and sharing are set.
 * The contact list's filter sheet is the primary way in; this is the secondary one.
 */
@Composable
fun AddressBooksLinkSection(onClick: () -> Unit) {
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
    }
}

@Preview(showBackground = true)
@Composable
fun AddressBooksLinkSectionPreview() {
    CorvidContactsTheme {
        AddressBooksLinkSection(onClick = {})
    }
}
