// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.settings.SettingsSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * Opt-in toggle for mirroring contacts into Android's system contacts (names and photos for
 * caller ID in Messages and the dialer). Turning it on asks for the contacts-write permission
 * first; the switch only shows as on while the permission is actually held, so revoking it in
 * system settings is reflected here. Once on, each address book is shared individually, and none
 * is shared until the user picks it.
 */
@Composable
fun SystemContactsSection(
    enabled: Boolean,
    onToggled: (Boolean) -> Unit,
    addressBooks: List<AddressBookEntity>,
    onAddressBookToggled: (href: String, share: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        onToggled(granted)
    }

    SettingsSection(
        title = stringResource(R.string.system_contacts_title)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.system_contacts_title)) },
            supportingContent = { Text(stringResource(R.string.system_contacts_description)) },
            trailingContent = {
                Switch(
                    checked = enabled && hasPermission,
                    onCheckedChange = { wantsOn ->
                        if (!wantsOn) {
                            onToggled(false)
                        } else if (hasPermission) {
                            onToggled(true)
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                        }
                    }
                )
            },
        )

        if (enabled && hasPermission) {
            Text(
                text = stringResource(R.string.system_contacts_books_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            addressBooks.forEach { book ->
                ListItem(
                    headlineContent = {
                        Text(book.displayName ?: stringResource(R.string.settings_address_book_unnamed))
                    },
                    supportingContent = if (book.isLocal) {
                        { Text(stringResource(R.string.settings_address_book_local_badge)) }
                    } else null,
                    trailingContent = {
                        Switch(
                            checked = book.shareWithSystem,
                            onCheckedChange = { onAddressBookToggled(book.href, it) }
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SystemContactsSectionPreview() {
    CorvidContactsTheme {
        SystemContactsSection(
            enabled = false,
            onToggled = {},
            addressBooks = emptyList(),
            onAddressBookToggled = { _, _ -> }
        )
    }
}
