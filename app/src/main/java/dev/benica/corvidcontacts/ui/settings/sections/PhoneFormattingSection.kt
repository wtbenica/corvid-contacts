// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.settings.SettingsSection

@Composable
fun PhoneFormattingSection(
    alwaysAddCountryCode: Boolean,
    onAlwaysAddCountryCodeToggled: (Boolean) -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.common_settings_phone_formatting),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.common_settings_country_code_title)) },
            supportingContent = { Text(stringResource(R.string.common_settings_country_code_description)) },
            trailingContent = {
                Switch(
                    checked = alwaysAddCountryCode,
                    onCheckedChange = onAlwaysAddCountryCodeToggled
                )
            },
        )
    }
}