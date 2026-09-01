// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.settings.SettingsSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

@Composable
fun ExternalPhotosSection(
    autoLoadRemotePhotos: Boolean,
    onAutoLoadRemotePhotosToggled: (Boolean) -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.common_auto_load_remote_photos_title)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.common_auto_load_remote_photos_title)) },
            supportingContent = { Text(stringResource(R.string.common_auto_load_remote_photos_description)) },
            trailingContent = {
                Switch(
                    checked = autoLoadRemotePhotos,
                    onCheckedChange = onAutoLoadRemotePhotosToggled
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExternalPhotosSectionPreview() {
    CorvidContactsTheme {
        ExternalPhotosSection(
            autoLoadRemotePhotos = true,
            onAutoLoadRemotePhotosToggled = {}
        )
    }
}
