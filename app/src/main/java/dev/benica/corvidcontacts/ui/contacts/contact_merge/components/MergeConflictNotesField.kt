// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Notes is a multi-line field where the two contacts disagree, and is the only field with a
 * "keep both" option - a combo-box doesn't suit a paragraph of text, so this offers three
 * quick-fill actions above a directly-editable text area instead.
 */
@Composable
fun MergeConflictNotesField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    survivorValue: String,
    absorbedValue: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    ConflictContainer(
        modifier = modifier,
        color = color
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
        ) {
            AssistChip(
                onClick = { onValueChange(survivorValue) },
                label = { Text(stringResource(R.string.merge_notes_use_survivor)) }
            )
            AssistChip(
                onClick = { onValueChange(absorbedValue) },
                label = { Text(stringResource(R.string.merge_notes_use_absorbed)) }
            )
            AssistChip(
                onClick = { onValueChange("${survivorValue.trim()}\n\n${absorbedValue.trim()}") },
                label = { Text(stringResource(R.string.merge_review_keep_both)) }
            )
        }

        CCOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
    }
}
