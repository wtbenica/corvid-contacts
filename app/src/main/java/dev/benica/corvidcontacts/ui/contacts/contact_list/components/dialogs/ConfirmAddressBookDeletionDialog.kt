// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog

@Composable
fun ConfirmAddressBookDeletionDialog(
    addressBook: AddressBookEntity,
    contactCount: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    isSubmittingAddressBookAction: Boolean,
) {
    val bookName = addressBook.displayName ?: stringResource(R.string.settings_address_book_unnamed)

    CCAlertDialog(
        onDismissRequest = onDismissRequest,
        title = R.string.settings_address_book_delete_dialog_title,
        content = {
            Column {
                Text(
                    stringResource(
                        when {
                            addressBook.isLocal && contactCount > 0 -> R.string.settings_address_book_delete_dialog_description_local
                            addressBook.isLocal -> R.string.settings_address_book_delete_dialog_empty_description_local
                            contactCount > 0 -> R.string.settings_address_book_delete_dialog_description
                            else -> R.string.settings_address_book_delete_dialog_empty_description
                        },
                        bookName
                    )
                )
                if (isSubmittingAddressBookAction) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.settings_address_book_deleting))
                    }
                }
            }
        },
        confirmButton = R.string.settings_address_book_delete,
        confirmEnabled = isSubmittingAddressBookAction.not(),
        onConfirm = onConfirm,
        dismissButton = R.string.action_cancel,
        dismissEnabled = isSubmittingAddressBookAction.not(),
    )
}