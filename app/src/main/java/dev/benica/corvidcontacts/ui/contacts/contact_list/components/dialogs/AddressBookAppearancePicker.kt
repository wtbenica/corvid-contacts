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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider

/**
 * The color and icon controls shared by the create and edit address book dialogs: a
 * [HueSlider] over a row of the icon choices from [ContactColors.iconPalette]. The selected icon
 * is highlighted in [selectedColor], so it doubles as the preview of the color being picked -
 * callers pass the color for [hue] rather than this recomputing it, since they need the same
 * value on confirm.
 */
@Composable
fun AddressBookAppearancePicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
    selectedColor: Color,
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HueSlider(
            hue = hue,
            onHueChange = onHueChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContactColors.iconPalette.forEach { (name, icon) ->
                val isSelected = name == selectedIconName
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) selectedColor.copy(alpha = 0.18f) else Color.Transparent,
                ) {
                    CCIconButton(
                        icon = icon,
                        contentDescription = ContactColors.iconPaletteLabels[name]
                            ?: R.string.common_unknown,
                        onClick = { onIconSelected(name) },
                        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
