// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Reorderable list of every address book with quick access to appearance, rename, and delete -
 * the same capabilities Settings offers, reachable directly from the filter sheet where books
 * are actually being browsed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressBooksDialog(
    addressBooks: List<AddressBookEntity>,
    onUpdateOrder: (List<AddressBookEntity>) -> Unit,
    onRequestAppearance: (AddressBookEntity) -> Unit,
    onRequestRename: (AddressBookEntity) -> Unit,
    onRequestDelete: (AddressBookEntity) -> Unit,
    onRequestUpload: (AddressBookEntity) -> Unit,
    onToggleVisibility: (AddressBookEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val list = addressBooks.toMutableList()
        list.add(
            to.index,
            list.removeAt(from.index)
        )
        onUpdateOrder(list)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    CCAlertDialog(
        onDismissRequest = onDismiss,
        content = {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(
                    addressBooks,
                    key = { _, book -> book.href }
                ) { _, book ->
                    ReorderableItem(
                        reorderableState,
                        key = book.href
                    ) { isDragging ->
                        val elevation = if (isDragging) 4.dp else 0.dp
                        Surface(
                            shadowElevation = elevation,
                            modifier = Modifier.alpha(if (book.isVisible) 1f else 0.5f)
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        book.displayName
                                            ?: stringResource(R.string.settings_address_book_unnamed)
                                    )
                                },
                                // Synced is expected so only the local-only or hidden state is labeled.
                                supportingContent = if (book.isVisible.not() || book.isLocal) {
                                    {
                                        Text(
                                            listOfNotNull(
                                                if (book.isLocal) stringResource(
                                                    R.string
                                                        .settings_address_book_local_badge
                                                ) else null,
                                                if (book.isVisible.not()) stringResource(R.string.settings_address_book_hidden_label) else null
                                            ).joinToString(" · ")
                                        )
                                    }
                                } else null,
                                leadingContent = {
                                    val bookColor = Color(book.colorInt)
                                    Surface(
                                        shape = CircleShape,
                                        color = bookColor.copy(alpha = 0.15f),
                                    ) {
                                        CCIconButton(
                                            icon = ContactColors.getIconForAddressBook(
                                                book.displayName,
                                                book.iconName
                                            ),
                                            contentDescription = R.string.settings_address_book_change_appearance,
                                            onClick = { onRequestAppearance(book) },
                                            color = bookColor
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        var showBookMenu by remember { mutableStateOf(false) }
                                        Box {
                                            CCIconButton(
                                                icon = Icons.Rounded.MoreVert,
                                                contentDescription = R.string.settings_address_book_more_options,
                                                onClick = { showBookMenu = true },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            DropdownMenu(
                                                expanded = showBookMenu,
                                                onDismissRequest = { showBookMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.settings_address_book_rename)) },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Rounded.Edit,
                                                            null
                                                        )
                                                    },
                                                    onClick = {
                                                        onRequestRename(book)
                                                        showBookMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(
                                                                if (book.isVisible) {
                                                                    R.string.settings_address_book_hide
                                                                } else {
                                                                    R.string.settings_address_book_show
                                                                }
                                                            )
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            if (book.isVisible) {
                                                                Icons.Rounded.VisibilityOff
                                                            } else {
                                                                Icons.Rounded.Visibility
                                                            },
                                                            null
                                                        )
                                                    },
                                                    onClick = {
                                                        onToggleVisibility(book)
                                                        showBookMenu = false
                                                    }
                                                )
                                                if (book.isLocal) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.settings_address_book_upload)) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Rounded.CloudUpload,
                                                                null
                                                            )
                                                        },
                                                        onClick = {
                                                            onRequestUpload(book)
                                                            showBookMenu = false
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.settings_address_book_delete)) },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Rounded.Delete,
                                                            null
                                                        )
                                                    },
                                                    onClick = {
                                                        onRequestDelete(book)
                                                        showBookMenu = false
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            Icons.Rounded.DragHandle,
                                            contentDescription = stringResource(R.string.settings_address_book_reorder),
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureThresholdActivate
                                                    )
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureEnd
                                                    )
                                                }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = R.string.common_done,
        onConfirm = onDismiss,
        title = R.string.settings_section_address_books,
    )
}