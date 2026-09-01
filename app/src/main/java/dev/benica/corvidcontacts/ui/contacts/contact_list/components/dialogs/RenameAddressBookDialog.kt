// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField

/** Prompts for a new display name for an existing address book. */
@Composable
fun RenameAddressBookDialog(
    oldName: String,
    isSubmitting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf(oldName) }

    fun submit() {
        if (newName.isNotBlank() && newName != oldName && isSubmitting.not()) onConfirm(newName.trim())
    }

    CCAlertDialog(
        onDismissRequest = { if (isSubmitting.not()) onDismiss() },
        title = R.string.settings_address_book_rename_dialog_title,
        content = {
            Column {
                CCOutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(R.string.settings_address_book_name_label),
                    singleLine = true,
                    enabled = isSubmitting.not(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                if (isSubmitting) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.settings_address_book_renaming))
                    }
                }
            }
        },
        confirmButton = R.string.settings_address_book_rename,
        onConfirm = { submit() },
        confirmEnabled = newName.isNotBlank() && newName != oldName && isSubmitting.not(),
        dismissButton = R.string.action_cancel,
        dismissEnabled = isSubmitting.not()
    )
}