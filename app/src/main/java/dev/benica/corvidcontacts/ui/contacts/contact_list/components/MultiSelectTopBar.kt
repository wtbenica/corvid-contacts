// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.GroupRemove
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopAppBar
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopBarDropdownMenu
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * Top app bar for multi-selection mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    selectedGroup: String?,
    onRemoveFromCurrentGroup: () -> Unit,
    onShowAddToGroupDialog: () -> Unit,
    onArchiveSelected: () -> Unit,
    showArchived: Boolean,
    availableAddressBooks: List<AddressBookEntity>,
    onMoveSelected: (String) -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val selectionTitle = pluralStringResource(
        id = R.plurals.list_selected_count,
        count = selectedCount,
        selectedCount
    )
    CCTopAppBar(
        title = selectionTitle,
        navigationIcon = {
            CCIconButton(
                icon = Icons.TwoTone.Close,
                contentDescription = R.string.list_action_clear_selection,
                onClick = onClearSelection,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            CCIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = R.string.list_action_share_selected,
                onClick = onShareSelected,
                color = MaterialTheme.colorScheme.onSurface
            )

            CCIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = R.string.list_action_delete_selected,
                onClick = onDeleteSelected,
                color = MaterialTheme.colorScheme.onSurface
            )

            var overflowExpanded by remember { mutableStateOf(false) }
            var showMoveSubmenu by remember { mutableStateOf(false) }

            Box {
                CCIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = R.string.detail_action_more,
                    onClick = { overflowExpanded = true },
                    color = MaterialTheme.colorScheme.onSurface
                )

                CCTopBarDropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = {
                        overflowExpanded = false
                        showMoveSubmenu = false
                    },
                ) {
                    if (showMoveSubmenu) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_back)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = null
                                )
                            },
                            onClick = { showMoveSubmenu = false }
                        )

                        HorizontalDivider()

                        availableAddressBooks.forEach { book ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        book.displayName ?: stringResource(R.string.common_unknown)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ContactColors.getIconForAddressBook(
                                            book.displayName,
                                            book.iconName
                                        ),
                                        contentDescription = null,
                                        tint = Color(book.colorInt)
                                    )
                                },
                                onClick = {
                                    onMoveSelected(book.href)
                                    overflowExpanded = false
                                    showMoveSubmenu = false
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.list_action_add_to_group)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.GroupAdd,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onShowAddToGroupDialog()
                                overflowExpanded = false
                            }
                        )

                        if (selectedGroup != null) {
                            val groupDisplayName = if (
                                selectedGroup.equals(
                                    other = ContactsRepository.FAVORITE_CATEGORY,
                                    ignoreCase = true
                                )
                            ) {
                                stringResource(id = R.string.list_group_favorites)
                            } else {
                                selectedGroup
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.list_action_remove_from_group,
                                            groupDisplayName
                                        ),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.GroupRemove,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    onRemoveFromCurrentGroup()
                                    overflowExpanded = false
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (showArchived) R.string.list_action_unarchive_selected
                                        else R.string.list_action_archive_selected
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onArchiveSelected()
                                overflowExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.list_action_move_selected)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                                    contentDescription = null
                                )
                            },
                            onClick = { showMoveSubmenu = true }
                        )
                    }
                }
            }
        },
    )
}

@Preview
@Composable
fun MultiSelectTopBarPreview() {
    CorvidContactsTheme {
        MultiSelectTopBar(
            selectedCount = 4,
            onClearSelection = {},
            selectedGroup = null,
            onRemoveFromCurrentGroup = {},
            onShowAddToGroupDialog = {},
            onArchiveSelected = {},
            showArchived = false,
            availableAddressBooks = emptyList(),
            onMoveSelected = {},
            onShareSelected = {},
            onDeleteSelected = {}
        )
    }
}
