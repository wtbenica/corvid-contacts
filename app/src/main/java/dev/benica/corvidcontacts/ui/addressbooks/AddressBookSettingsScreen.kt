// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.content.res.Resources
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.HiddenContact
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.AddressBookUploadResult
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScreenFrame
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.AddressBookAppearanceDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.ConfirmAddressBookDeletionDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.RenameAddressBookDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.UploadAddressBookDialog
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingIcon
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Everything about one address book in one place: appearance, name, whether it is shown in the
 * contact list, how it is shared with the phone's contacts, uploading a local book to the server,
 * and deleting it.
 *
 * The first time sharing is turned on without the contacts permission, a short explanation comes
 * first and the permission is requested.
 *
 * @param book The book to show, or `null` once it no longer exists (for example just after it was
 * deleted), in which case [onBack] is invoked.
 */
@Composable
fun AddressBookSettingsScreen(
    book: AddressBookEntity?,
    contactCount: Int,
    hasServerConnection: Boolean,
    onBack: () -> Unit,
    onUpdateAppearance: (AddressBookEntity, Color, String?) -> Unit,
    onRename: suspend (AddressBookEntity, String) -> Result<Unit>,
    onToggleVisibility: (AddressBookEntity) -> Unit,
    onShareWithSystemChanged: (AddressBookEntity, Boolean) -> Unit,
    onSystemContactsLevelChanged: (AddressBookEntity, SystemContactsLevel) -> Unit,
    hiddenContacts: List<HiddenContact>,
    onShowInSystem: (List<ContactId>) -> Unit,
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
    val submit = rememberSubmitState()
    val genericErrorMessage = stringResource(R.string.settings_address_book_generic_error)
    val bookName = book.displayName ?: stringResource(R.string.settings_address_book_unnamed)
    fun showError() = Toast.makeText(context, genericErrorMessage, Toast.LENGTH_SHORT).show()

    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showSetUpSyncPrompt by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareExplanation by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }

    val initiallyHasPermission = rememberHasContactsPermission()
    var hasPermission by remember { mutableStateOf(initiallyHasPermission) }
    val permissionRequest = rememberContactsPermissionRequest { granted ->
        hasPermission = granted
        if (granted) onShareWithSystemChanged(book, true)
    }
    val isShared = book.shareWithSystem && hasPermission

    CCScreenFrame(
        title = bookName,
        onBack = onBack,
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                // The same inset the app Settings page gets from its sections, without section headers.
                .padding(horizontal = Dimens.lgSpacing, vertical = Dimens.lgSpacing)
        ) {
            val bookColor = Color(book.colorInt)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_address_book_change_appearance)) },
                // Shows the book's current icon and color, so the row reads as "tap to change this".
                trailingContent = {
                    TrailingIcon(
                        icon = ContactColors.getIconForAddressBook(book.displayName, book.iconName),
                        tint = bookColor,
                    )
                },
                modifier = Modifier.clickable { showAppearanceDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_address_book_rename)) },
                supportingContent = { Text(bookName) },
                trailingContent = { SettingsLeadingIcon(icon = Icons.Outlined.Edit) },
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

            AddressBookSharingSection(
                isShared = isShared,
                level = book.systemContactsLevel,
                hiddenCount = hiddenContacts.size,
                onShareChanged = { wantsOn ->
                    when {
                        !wantsOn -> onShareWithSystemChanged(book, false)
                        hasPermission -> onShareWithSystemChanged(book, true)
                        else -> showShareExplanation = true
                    }
                },
                onLevelChanged = { onSystemContactsLevelChanged(book, it) },
                onHiddenClick = { showHiddenDialog = true },
            )

            HorizontalDivider()

            if (book.isLocal) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_address_book_upload)) },
                    trailingContent = { SettingsLeadingIcon(icon = Icons.Outlined.CloudUpload) },
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
                trailingContent = {
                    TrailingIcon(icon = Icons.Outlined.Delete, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable { showDeleteDialog = true },
            )
        }
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
            isSubmitting = submit.isSubmitting,
            onConfirm = { newName ->
                submit.run {
                    if (onRename(book, newName).isSuccess) showRenameDialog = false else showError()
                }
            },
            onDismiss = { if (!submit.isSubmitting) showRenameDialog = false }
        )
    }

    // First share without the contacts permission: explain what sharing does before the system
    // permission prompt appears.
    if (showShareExplanation) {
        CCAlertDialog(
            onDismissRequest = { showShareExplanation = false },
            title = R.string.system_contacts_title,
            content = { Text(stringResource(R.string.system_contacts_description)) },
            confirmButton = R.string.ok,
            onConfirm = {
                showShareExplanation = false
                permissionRequest.launch()
            },
            dismissButton = R.string.action_cancel,
        )
    }

    if (showHiddenDialog && hiddenContacts.isNotEmpty()) {
        HiddenContactsDialog(
            hiddenContacts = hiddenContacts,
            onShow = onShowInSystem,
            onDismiss = { showHiddenDialog = false },
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
            isSubmitting = submit.isSubmitting,
            onConfirm = { newName ->
                submit.run {
                    val result = onUpload(book, newName)
                    showUploadDialog = false
                    result.fold(
                        onSuccess = { outcome ->
                            Toast.makeText(context, uploadResultMessage(resources, outcome), Toast.LENGTH_LONG).show()
                        },
                        onFailure = { showError() }
                    )
                }
            },
            onDismiss = { if (!submit.isSubmitting) showUploadDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmAddressBookDeletionDialog(
            addressBook = book,
            contactCount = contactCount,
            onDismissRequest = { if (!submit.isSubmitting) showDeleteDialog = false },
            onConfirm = {
                submit.run {
                    val result = onDelete(book)
                    showDeleteDialog = false
                    if (result.isFailure) showError()
                }
            },
            isSubmittingAddressBookAction = submit.isSubmitting,
        )
    }
}

private fun uploadResultMessage(resources: Resources, outcome: AddressBookUploadResult): String {
    val total = outcome.uploadedCount + outcome.failedCount
    return if (outcome.fullyCompleted) {
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
}

/** A 24 dp trailing icon with a tint, sized like [SettingsLeadingIcon] so the rows line up. */
@Composable
private fun TrailingIcon(icon: ImageVector, tint: Color) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}
