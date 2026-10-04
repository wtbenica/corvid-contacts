// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
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
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingIcon
import dev.benica.corvidcontacts.ui.settings.SettingsLeadingRadioButton
import dev.benica.corvidcontacts.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Everything about one address book in one place: appearance, name, whether it is shown in the
 * contact list, whether it is shared with other apps, uploading a local book to the server, and
 * deleting it. Replaces the per-book dialogs the filter sheet used to stack up.
 *
 * @param book The book to show, or `null` once it no longer exists (for example just after it was
 * deleted), in which case [onBack] is invoked.
 * Sharing with other apps is decided here, per book: the switch, and how much of each contact to
 * share. The first time it is turned on, with the contacts permission not yet granted, a short
 * explanation comes first and the permission is requested.
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
    var showShareExplanation by remember { mutableStateOf(false) }

    val initiallyHasPermission = rememberHasContactsWritePermission()
    var hasPermission by remember { mutableStateOf(initiallyHasPermission) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onShareWithSystemChanged(book, true)
    }
    val isShared = book.shareWithSystem && hasPermission

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

            ListItem(
                headlineContent = { Text(stringResource(R.string.address_book_setting_share)) },
                supportingContent = { Text(stringResource(R.string.address_book_setting_share_description)) },
                trailingContent = {
                    Switch(
                        checked = isShared,
                        onCheckedChange = { wantsOn ->
                            when {
                                !wantsOn -> onShareWithSystemChanged(book, false)
                                hasPermission -> onShareWithSystemChanged(book, true)
                                else -> showShareExplanation = true
                            }
                        },
                    )
                },
            )
            // Always shown and only dimmed while the book isn't shared, so the page doesn't change
            // shape when sharing is switched on or off.
            val levelAlpha = if (isShared) 1f else DISABLED_ALPHA
            Text(
                text = stringResource(R.string.system_contacts_level_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .alpha(levelAlpha)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            SystemContactsLevel.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(stringResource(option.titleRes())) },
                    supportingContent = { Text(stringResource(option.descriptionRes())) },
                    trailingContent = {
                        SettingsLeadingRadioButton(
                            selected = option == book.systemContactsLevel,
                            onClick = { onSystemContactsLevelChanged(book, option) },
                            enabled = isShared,
                        )
                    },
                    modifier = Modifier
                        .alpha(levelAlpha)
                        .clickable(enabled = isShared) { onSystemContactsLevelChanged(book, option) },
                )
            }

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
                permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            },
            dismissButton = R.string.action_cancel,
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

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

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
