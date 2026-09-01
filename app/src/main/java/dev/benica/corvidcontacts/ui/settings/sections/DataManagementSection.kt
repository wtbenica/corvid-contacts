// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingIcon
import dev.benica.corvidcontacts.ui.settings.SettingsSection

private val VCF_MIME_TYPES = arrayOf("text/vcard", "text/x-vcard", "text/directory")

@Composable
fun DataManagementSection(
    exportLauncher: ManagedActivityResultLauncher<String, Uri?>,
    importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    onAboutClick: () -> Unit,
    onResetOnboardingDialog: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_data)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_title)) },
            supportingContent = { Text(stringResource(R.string.settings_export_description)) },
            modifier = Modifier.clickable {
                exportLauncher.launch("corvid_contacts_export.vcf")
            },
            trailingContent = {
                SettingsLeadingIcon(
                    icon = Icons.Outlined.Download,
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_import_title)) },
            supportingContent = { Text(stringResource(R.string.settings_import_description)) },
            modifier = Modifier.clickable {
                importLauncher.launch(VCF_MIME_TYPES)
            },
            trailingContent = {
                SettingsLeadingIcon(
                    icon = Icons.Outlined.Upload,
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reset_onboarding_title)) },
            supportingContent = { Text(stringResource(R.string.settings_reset_onboarding_description)) },
            modifier = Modifier.clickable { onResetOnboardingDialog() },
            trailingContent = {
                SettingsLeadingIcon(
                    icon = Icons.Outlined.Restore,
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.common_about)) },
            modifier = Modifier.clickable { onAboutClick() },
            trailingContent = {
                SettingsLeadingIcon(
                    icon = Icons.Outlined.Info
                )
            },
        )
    }
}