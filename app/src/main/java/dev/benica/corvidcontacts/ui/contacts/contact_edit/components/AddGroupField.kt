// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.border
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupField(
    allGroups: List<String>,
    currentGroups: List<String>,
    onAdd: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    val filteredGroups = allGroups.filter {
        it.contains(
            text,
            ignoreCase = true
        ) && !currentGroups.contains(it)
    }

    fun submit(group: String) {
        onAdd(group)
        text = ""
        expanded = false
    }

    CCExposedDropdownMenuBox(
        textBoxLabel = stringResource(
            R.string.action_add_item,
            stringResource(R.string.common_group)
        ),
        currentValue = text,
        onValueChange = { text = it },
        expanded = expanded && filteredGroups.isNotEmpty(),
        onExpandedChange = { expanded = it },
        readOnly = false,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) submit(text.trim()) }),
        trailingIcon = {
            // Not CCIconButton - matches the dropdown's own muted trailing-arrow color instead.
            val tint = currentThemeColor().border()
            IconButton(
                onClick = { submit(text.trim()) },
                modifier = Modifier.size(48.dp),
                enabled = text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = tint,
                    disabledContentColor = tint.copy(alpha = 0.3f)
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(
                        R.string.action_add_item,
                        stringResource(R.string.common_group)
                    ),
                )
            }
        }
    ) {
        filteredGroups.forEach { group ->
            DropdownMenuItem(
                text = { Text(group) },
                onClick = { submit(group) }
            )
        }
    }
}
