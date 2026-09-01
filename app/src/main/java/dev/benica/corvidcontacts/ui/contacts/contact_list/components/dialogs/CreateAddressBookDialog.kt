// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.oklch
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults

/**
 * Prompts for a name and initial color for a brand-new address book, plus - when [hasServerConnection]
 * is true - whether it should be a synced (server) book or a deliberately local-only one. With no
 * server connected at all, there's nothing to ask: the new book is always local, same as today.
 */
@Composable
fun CreateAddressBookDialog(
    isSubmitting: Boolean,
    hasServerConnection: Boolean,
    onConfirm: (name: String, color: Color, forceLocal: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var hue by remember { mutableFloatStateOf(0f) }
    var forceLocal by remember { mutableStateOf(false) }

    fun submit() {
        if (name.isNotBlank() && !isSubmitting) {
            onConfirm(
                name.trim(),
                oklch(
                    HueSliderDefaults.LIGHTNESS,
                    HueSliderDefaults.CHROMA,
                    hue
                ),
                forceLocal
            )
        }
    }

    CCAlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = R.string.settings_address_book_create_dialog_title,
        content = {
            Column {
                CCOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.settings_address_book_name_label),
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueSlider(
                        hue = hue,
                        onHueChange = { hue = it },
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = CircleShape,
                        color = oklch(
                            HueSliderDefaults.LIGHTNESS,
                            HueSliderDefaults.CHROMA,
                            hue
                        ),
                        modifier = Modifier
                            .size(48.dp)
                    ) {}
                }

                if (hasServerConnection) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_address_book_location_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) { forceLocal = false }
                    ) {
                        RadioButton(
                            selected = !forceLocal,
                            onClick = { forceLocal = false },
                            enabled = !isSubmitting
                        )
                        Text(stringResource(R.string.settings_address_book_location_synced))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) { forceLocal = true }
                    ) {
                        RadioButton(
                            selected = forceLocal,
                            onClick = { forceLocal = true },
                            enabled = !isSubmitting
                        )
                        Text(stringResource(R.string.settings_address_book_location_local))
                    }
                }
                if (isSubmitting) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.settings_address_book_creating))
                    }
                }
            }
        },
        confirmButton = R.string.settings_address_book_create,
        onConfirm = { submit() },
        confirmEnabled = name.isNotBlank() && !isSubmitting,
        dismissButton = R.string.action_cancel,
        dismissEnabled = !isSubmitting
    )
}