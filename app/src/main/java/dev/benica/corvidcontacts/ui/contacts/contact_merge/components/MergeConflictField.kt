// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox

/**
 * A text field for a single-value field the two contacts disagree on: pre-filled (typically with
 * the survivor's value), freely editable, and offers [survivorValue]/[absorbedValue] as one-tap
 * alternatives via the dropdown. Meant to be wrapped in a [ConflictContainer].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeConflictField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    survivorValue: String,
    absorbedValue: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var expanded by remember { mutableStateOf(false) }

    CCExposedDropdownMenuBox(
        textBoxLabel = label,
        currentValue = value,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onValueChange = onValueChange,
        readOnly = false,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
    ) {
        listOf(survivorValue, absorbedValue)
            .distinct()
            .forEach { candidate ->
                DropdownMenuItem(
                    text = {
                        Text(
                            candidate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onValueChange(candidate)
                        expanded = false
                    }
                )
            }
    }
}
