// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.oklch
import dev.benica.corvidcontacts.extensions.toOklch
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults

/**
 * Lets the user pick a new permanent color (via [dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider]) and icon override for an address
 * book in one place, rather than two separate dialogs. Both changes are staged locally and only
 * applied together via [onConfirm] - Cancel discards both, matching normal dialog expectations.
 * There's no "reset to the name-guessed icon" affordance: that guess was only ever a starting
 * point, and if a user wants it back they can just pick it from the palette like any other icon.
 * There's also no separate color swatch - the currently selected icon is always highlighted in
 * the current hue, so it doubles as the color preview.
 */
@Composable
fun AddressBookAppearanceDialog(
    currentColor: Color,
    currentIconName: String,
    onConfirm: (Color, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var hue by remember { mutableFloatStateOf(currentColor.toOklch().h) }
    var iconName by remember { mutableStateOf(currentIconName) }
    val selectedColor = oklch(
        HueSliderDefaults.LIGHTNESS,
        HueSliderDefaults.CHROMA,
        hue
    )

    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.settings_address_book_appearance_dialog_title,
        content = {
            Column {
                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactColors.iconPalette.forEach { (name, icon) ->
                        val isSelected = name == iconName
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) selectedColor.copy(alpha = 0.18f) else Color.Transparent,
                        ) {
                            CCIconButton(
                                icon = icon,
                                contentDescription = ContactColors.iconPaletteLabels[name]
                                    ?: R.string.common_unknown,
                                onClick = { iconName = name },
                                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = R.string.ok,
        onConfirm = {
            onConfirm(
                oklch(
                    HueSliderDefaults.LIGHTNESS,
                    HueSliderDefaults.CHROMA,
                    hue
                ),
                iconName
            )
            onDismiss()
        },
        dismissButton = R.string.action_cancel,
    )
}