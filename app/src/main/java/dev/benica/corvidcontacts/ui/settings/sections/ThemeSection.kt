// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingRadioButton
import dev.benica.corvidcontacts.ui.settings.SettingsSection

@Composable
fun ThemeSection(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    SettingsSection(
        title = stringResource(R.string.common_settings_theme_title),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.common_settings_theme_system)) },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeSelected(ThemeMode.SYSTEM) }
                )
            },
            modifier = Modifier.clickable { onThemeModeSelected(ThemeMode.SYSTEM) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.common_settings_theme_light)) },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeSelected(ThemeMode.LIGHT) }
                )
            },
            modifier = Modifier.clickable { onThemeModeSelected(ThemeMode.LIGHT) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.common_settings_theme_dark)) },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeSelected(ThemeMode.DARK) }
                )
            },
            modifier = Modifier.clickable { onThemeModeSelected(ThemeMode.DARK) },
        )
    }
}