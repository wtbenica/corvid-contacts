// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.AddressBookUploadResult
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.AddressBookAppearanceDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.ConfirmAddressBookDeletionDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.RenameAddressBookDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.UploadAddressBookDialog
import kotlinx.coroutines.launch

/**
 * Everything about one address book in one place: appearance, name, whether it is shown in the
 * contact list, whether it is shared with other apps, uploading a local book to the server, and
 * deleting it. Replaces the per-book dialogs the filter sheet used to stack up.
 *
 * @param book The book to show, or `null` once it no longer exists (for example just after it was
 * deleted), in which case [onBack] is invoked.
 * @param systemContactsActive Whether the "Show names in other apps" feature is on and permitted;
 * the sharing switch is disabled, with an explanation, when it isn't.
 */
@Composable
fun AddressBookSettingsScreen(
    book: AddressBookEntity?,
    contactCount: Int,
    hasServerConnection: Boolean,
    systemContactsActive: Boolean,
    onBack: () -> Unit,
    onUpdateAppearance: (AddressBookEntity, Color, String?) -> Unit,
    onRename: suspend (AddressBookEntity, String) -> Result<Unit>,
    onToggleVisibility: (AddressBookEntity) -> Unit,
    onShareWithSystemChanged: (AddressBookEntity, Boolean) -> Unit,
    onOpenSystemContactsSettings: () -> Unit,
    onUpload: suspend (AddressBookEntity, String) -> Result<AddressBookUploadResult>,
    onSetUpSync: () -> Unit,
    onDelete: suspend (AddressBookEntity) -> Result<Unit>,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    // A deleted book has nothing left to show, so leave the page.
    if (book == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val genericErrorMessage = stringResource(R.string.settings_address_book_generic_error)
    val bookName = book.displayName ?: stringResource(R.string.settings_address_book_unnamed)

    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showSetUpSyncPrompt by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val chromeNavigationIcon: @Composable () -> Unit = { BackNavButton(onBack) }
    if (!showScaffold) {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = bookName,
                    navigationIcon = chromeNavigationIcon,
                )
            )
        }
    }

    val body: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            val bookColor = Color(book.colorInt)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_address_book_change_appearance)) },
                leadingContent = {
                    Surface(shape = CircleShape, color = bookColor.copy(alpha = 0.15f)) {
                        Icon(
                            imageVector = ContactColors.getIconForAddressBook(book.displayName, book.iconName),
                            contentDescription = null,
                            tint = bookColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                },
                modifier = Modifier.clickable { showAppearanceDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_address_book_rename)) },
                supportingContent = { Text(bookName) },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                modifier = Modifier.clickable { showRenameDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.address_book_setting_visibility)) },
                supportingContent = { Text(stringResource(R.string.address_book_setting_visibility_description)) },
                trailingContent = {
                    Switch(checked = book.isVisible, onCheckedChange = { onToggleVisibility(book) })
                },
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.address_book_setting_share)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (systemContactsActive) {
                                R.string.address_book_setting_share_description
                            } else {
                                R.string.address_book_setting_share_off_hint
                            }
                        )
                    )
                },
                trailingContent = {
                    Switch(
                        checked = book.shareWithSystem && systemContactsActive,
                        onCheckedChange = { onShareWithSystemChanged(book, it) },
                        enabled = systemContactsActive,
                    )
                },
                // With the feature off the row leads to where it is switched on.
                modifier = if (systemContactsActive) Modifier else Modifier.clickable(onClick = onOpenSystemContactsSettings),
            )

            HorizontalDivider()

            if (book.isLocal) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_address_book_upload)) },
                    leadingContent = { Icon(Icons.Rounded.CloudUpload, contentDescription = null) },
                    modifier = Modifier.clickable {
                        if (hasServerConnection) showUploadDialog = true else showSetUpSyncPrompt = true
                    },
                )
            }
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_address_book_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                colors = ListItemDefaults.colors(),
                modifier = Modifier.clickable { showDeleteDialog = true },
            )
        }
    }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            title = bookName,
            navigationIcon = chromeNavigationIcon,
            content = body
        )
    } else {
        body(PaddingValues())
    }

    if (showAppearanceDialog) {
        AddressBookAppearanceDialog(
            currentColor = Color(book.colorInt),
            currentIconName = book.iconName ?: ContactColors.guessIconNameForAddressBook(book.displayName),
            onConfirm = { color, iconName ->
                onUpdateAppearance(book, color, iconName)
                showAppearanceDialog = false
            },
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showRenameDialog) {
        RenameAddressBookDialog(
            oldName = bookName,
            isSubmitting = isSubmitting,
            onConfirm = { newName ->
                if (!isSubmitting) {
                    isSubmitting = true
                    scope.launch {
                        val result = onRename(book, newName)
                        isSubmitting = false
                        if (result.isSuccess) {
                            showRenameDialog = false
                        } else {
                            Toast.makeText(context, genericErrorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { if (!isSubmitting) showRenameDialog = false }
        )
    }

    // Uploading needs a server; with none connected, offer to set one up instead of failing.
    if (showSetUpSyncPrompt) {
        CCAlertDialog(
            onDismissRequest = { showSetUpSyncPrompt = false },
            title = R.string.settings_address_book_upload_dialog_title,
            content = { Text(stringResource(R.string.settings_address_book_upload_needs_server)) },
            confirmButton = R.string.list_menu_set_up_sync,
            onConfirm = {
                showSetUpSyncPrompt = false
                onSetUpSync()
            },
            dismissButton = R.string.action_cancel,
        )
    }

    if (showUploadDialog) {
        UploadAddressBookDialog(
            oldName = bookName,
            isSubmitting = isSubmitting,
            onConfirm = { newName ->
                if (!isSubmitting) {
                    isSubmitting = true
                    scope.launch {
                        val result = onUpload(book, newName)
                        isSubmitting = false
                        showUploadDialog = false
                        result.fold(
                            onSuccess = { outcome ->
                                val total = outcome.uploadedCount + outcome.failedCount
                                val message = if (outcome.fullyCompleted) {
                                    resources.getQuantityString(
                                        R.plurals.settings_address_book_upload_result_full,
                                        outcome.uploadedCount,
                                        outcome.uploadedCount
                                    )
                                } else {
                                    resources.getQuantityString(
                                        R.plurals.settings_address_book_upload_result_partial,
                                        total,
                                        outcome.uploadedCount,
                                        total,
                                        outcome.failedCount
                                    )
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            },
                            onFailure = {
                                Toast.makeText(context, genericErrorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            onDismiss = { if (!isSubmitting) showUploadDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmAddressBookDeletionDialog(
            addressBook = book,
            contactCount = contactCount,
            onDismissRequest = { if (!isSubmitting) showDeleteDialog = false },
            onConfirm = {
                if (!isSubmitting) {
                    isSubmitting = true
                    scope.launch {
                        val result = onDelete(book)
                        isSubmitting = false
                        showDeleteDialog = false
                        if (result.isFailure) {
                            Toast.makeText(context, genericErrorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            isSubmittingAddressBookAction = isSubmitting,
        )
    }
}
