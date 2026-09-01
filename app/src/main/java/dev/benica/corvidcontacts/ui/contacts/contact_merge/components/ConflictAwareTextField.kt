// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.benica.corvidcontacts.data.merge.ContactMergeConflict
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField

/**
 * A text field that displays a conflict resolution UI if a [conflict] exists,
 * otherwise a standard outlined text field.
 */
@Composable
fun ConflictAwareTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    conflict: ContactMergeConflict?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    color: Color? = null,
) {
    if (conflict != null) {
        ConflictContainer(
            color = color,
            modifier = modifier
        ) {
            MergeConflictField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                survivorValue = conflict.survivorValue,
                absorbedValue = conflict.absorbedValue,
                keyboardOptions = keyboardOptions,
            )
        }
    } else {
        CCOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
        )
    }
}
