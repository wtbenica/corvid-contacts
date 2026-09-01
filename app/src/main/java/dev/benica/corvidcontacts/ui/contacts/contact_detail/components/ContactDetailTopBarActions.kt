// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopBarDropdownMenu
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import kotlinx.coroutines.launch

/**
 * Top bar actions for the contact detail screen.
 */
@Composable
fun ContactDetailTopBarActions(
    contact: ContactEntity,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    CCIconButton(
        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = R.string.detail_menu_favorite,
        onClick = onToggleFavorite,
        color = if (isFavorite) currentThemeColor() else LocalContentColor.current
    )

    if (contact.isEditable()) {
        CCIconButton(
            icon = Icons.Rounded.Edit,
            contentDescription = R.string.detail_menu_edit,
            onClick = onEdit,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    CCIconButton(
        icon = Icons.Rounded.MoreVert,
        contentDescription = R.string.detail_action_more,
        onClick = { isMenuExpanded = true },
        color = MaterialTheme.colorScheme.onSurface
    )

    CCTopBarDropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = { isMenuExpanded = false },
    ) {
        if (contact.isEditable()) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (contact.isArchived) stringResource(R.string.detail_menu_unarchive) else stringResource(
                            R.string.detail_menu_archive
                        )
                    )
                },
                onClick = {
                    isMenuExpanded = false
                    onArchive()
                },
                leadingIcon = {
                    Icon(
                        if (contact.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_menu_merge)) },
                onClick = {
                    isMenuExpanded = false
                    onMerge()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CallMerge,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_menu_delete)) },
                onClick = {
                    isMenuExpanded = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

/**
 * The [ContactDetailTopBarActions] row plus the delete-confirmation dialog, wired up once so
 * every host (full-screen or embedded in a wide-screen shell) shares the same behavior instead
 * of re-implementing it.
 */
data class ContactDetailActionsHandlers(
    val actions: @Composable RowScope.() -> Unit,
    val deleteDialog: @Composable () -> Unit,
)

/**
 * Builds a [ContactDetailActionsHandlers] for [contact]. [onToggleFavorite]/[onArchive]/[onDelete]
 * return whether the action succeeded; on `false`, [errorMessage] is shown via [snackbarHostState]
 * instead of the caller having to check and surface it itself.
 */
@Composable
fun rememberContactDetailActions(
    contact: ContactEntity?,
    isFavorite: Boolean,
    onToggleFavorite: suspend () -> Boolean,
    onEdit: (ContactEntity) -> Unit,
    onArchive: suspend (ContactEntity) -> Boolean,
    onMerge: (ContactEntity) -> Unit,
    onDelete: suspend (ContactEntity) -> Boolean,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
): ContactDetailActionsHandlers {
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val actions: @Composable RowScope.() -> Unit = {
        contact?.let { c ->
            ContactDetailTopBarActions(
                contact = c,
                isFavorite = isFavorite,
                onToggleFavorite = {
                    scope.launch {
                        if (!onToggleFavorite()) snackbarHostState.showSnackbar(errorMessage)
                    }
                },
                onEdit = { onEdit(c) },
                onArchive = {
                    scope.launch {
                        if (!onArchive(c)) snackbarHostState.showSnackbar(errorMessage)
                    }
                },
                onMerge = { onMerge(c) }
            ) { showDeleteConfirm = true }
        }
    }

    val deleteDialog: @Composable () -> Unit = {
        if (showDeleteConfirm && contact != null) {
            CCAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = R.string.detail_delete_confirm_title,
                content = {
                    Text(
                        stringResource(
                            R.string.detail_delete_confirm_message,
                            contact.getEffectiveDisplayName()
                        )
                    )
                },
                confirmButton = R.string.detail_menu_delete,
                onConfirm = {
                    showDeleteConfirm = false
                    scope.launch {
                        if (!onDelete(contact)) snackbarHostState.showSnackbar(errorMessage)
                    }
                },
                dismissButton = R.string.action_cancel,
            )
        }
    }

    return ContactDetailActionsHandlers(actions, deleteDialog)
}
