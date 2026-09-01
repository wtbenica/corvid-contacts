// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField

@Composable
fun RenameGroupDialog(
    oldName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf(oldName) }

    fun submit() {
        if (newName.isNotBlank() && newName != oldName) onConfirm(newName.trim())
    }

    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.settings_group_rename_dialog_title,
        content = {
            CCOutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = stringResource(R.string.settings_group_name_label),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = R.string.settings_group_action_rename,
        onConfirm = { submit() },
        confirmEnabled = newName.isNotBlank() && newName != oldName,
        dismissButton = R.string.action_cancel,
    )
}