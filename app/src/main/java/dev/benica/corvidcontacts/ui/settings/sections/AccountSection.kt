// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.settings.SettingsSection

@Composable
fun AccountSection(
    serverUrl: String,
    username: String,
    onLogout: () -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_account),
    ) {
        ListItem(
            overlineContent = { Text(stringResource(R.string.login_server_url)) },
            headlineContent = { Text(serverUrl) },
            trailingContent = {
                CCIconButton(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.size(24.dp),
                    contentDescription = R.string.list_menu_logout,
                    onClick = onLogout
                )
            },
        )

        ListItem(
            overlineContent = { Text(stringResource(R.string.login_username)) },
            headlineContent = { Text(username) },
        )
    }
}