// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableRelationship
import dev.benica.corvidcontacts.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipField(
    relationship: EditableRelationship,
    onRelationshipChange: (EditableRelationship) -> Unit,
    onDelete: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    requestInitialFocus: Boolean = false,
    contacts: List<ContactEntity> = emptyList(),
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasInteracted by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(true) }

    val initialFocusModifier = Modifier.rememberInitialFocusModifier(requestInitialFocus)

    // Linked relationships (isUid) store a contact ID; show the name instead.
    val displayValue = if (relationship.isUid) {
        contacts
            .find { it.id == relationship.value }
            ?.getEffectiveDisplayName()
            ?: relationship.value
    } else {
        relationship.value
    }

    // Suggest matching contacts only while actively typing free text.
    val suggestions = if (
        !relationship.isUid &&
        isFocused &&
        hasInteracted &&
        relationship.value.length >= 2
    ) {
        contacts
            .filter {
                it
                    .getEffectiveDisplayName()
                    .contains(
                        relationship.value,
                        ignoreCase = true
                    )
            }
            .take(5)
    } else {
        emptyList()
    }

    LaunchedEffect(relationship.value) {
        if (hasInteracted) {
            showSuggestions = true
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
    ) {
        TypedFieldControlsRow(
            type = relationship.type,
            onTypeChange = { onRelationshipChange(relationship.copy(type = it)) },
            types = VCardType.commonRelationshipTypes,
            enabled = enabled,
            onDelete = onDelete,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )

        // Row 2: Value - free text, or linked to a known contact via suggestions below
        CCExposedDropdownMenuBox(
            textBoxLabel = stringResource(R.string.common_contact),
            currentValue = displayValue,
            expanded = showSuggestions && suggestions.isNotEmpty() && enabled,
            onExpandedChange = { showSuggestions = it },
            onValueChange = {
                hasInteracted = true
                onRelationshipChange(
                    relationship.copy(
                        value = it,
                        isUid = false
                    )
                )
            },
            readOnly = false,
            enabled = enabled,
            showTrailingIcon = false,
            textFieldModifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .then(initialFocusModifier),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        ) {
            suggestions.forEach { contact ->
                DropdownMenuItem(
                    text = { Text(contact.getEffectiveDisplayName()) },
                    onClick = {
                        hasInteracted = false
                        onRelationshipChange(
                            relationship.copy(
                                value = contact.id,
                                isUid = true
                            )
                        )
                    }
                )
            }
        }
    }
}
