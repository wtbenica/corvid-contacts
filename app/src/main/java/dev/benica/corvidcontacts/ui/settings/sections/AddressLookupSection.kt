// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingRadioButton
import dev.benica.corvidcontacts.ui.settings.SettingsSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

@Composable
fun AddressLookupSection(
    mode: AddressLookupMode,
    onModeSelected: (AddressLookupMode) -> Unit,
    uriHandler: UriHandler,
) {
    SettingsSection(
        title = stringResource(R.string.common_settings_address_lookup_title)
    ) {
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.common_settings_address_lookup_description))
            },
        )

        // Photon (Privacy-focused)
        ListItem(
            headlineContent = { Text(stringResource(R.string.brand_photon)) },
            supportingContent = {
                Column {
                    Text(stringResource(R.string.common_address_lookup_photon_description))
                    Text(
                        text = stringResource(R.string.address_lookup_photon_source_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(GeocoderRepository.PHOTON_DATA_HANDLING_URL)
                        }
                    )
                }
            },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = mode == AddressLookupMode.PHOTON,
                    onClick = { onModeSelected(AddressLookupMode.PHOTON) }
                )
            },
            modifier = Modifier.clickable { onModeSelected(AddressLookupMode.PHOTON) },
        )

        // Google (More accurate)
        ListItem(
            headlineContent = { Text(stringResource(R.string.brand_google_places)) },
            supportingContent = {
                Text(stringResource(R.string.common_address_lookup_google_description))
            },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = mode == AddressLookupMode.GOOGLE,
                    onClick = { onModeSelected(AddressLookupMode.GOOGLE) }
                )
            },
            modifier = Modifier.clickable { onModeSelected(AddressLookupMode.GOOGLE) },
        )

        // Off
        ListItem(
            headlineContent = { Text(stringResource(R.string.onboarding_address_lookup_off_title)) },
            supportingContent = { Text(stringResource(R.string.onboarding_address_lookup_off_description)) },
            trailingContent = {
                SettingsLeadingRadioButton(
                    selected = mode == AddressLookupMode.OFF,
                    onClick = { onModeSelected(AddressLookupMode.OFF) }
                )
            },
            modifier = Modifier.clickable { onModeSelected(AddressLookupMode.OFF) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddressLookupSectionPreview() {
    CorvidContactsTheme {
        AddressLookupSection(
            mode = AddressLookupMode.PHOTON,
            onModeSelected = {},
            uriHandler = LocalUriHandler.current
        )
    }
}
