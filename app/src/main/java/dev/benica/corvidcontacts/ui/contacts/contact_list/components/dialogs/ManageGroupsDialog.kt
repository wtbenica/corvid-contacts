// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Reorderable list of every non-Favorites group with quick access to rename - Favorites is
 * always pinned first and isn't user-manageable, matching Settings' behavior.
 */
@Composable
fun ManageGroupsDialog(
    groups: List<String>,
    onUpdateOrder: (List<String>) -> Unit,
    onRequestRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val list = groups.toMutableList()
        list.add(
            to.index,
            list.removeAt(from.index)
        )
        onUpdateOrder(list)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.settings_group_order_title,
        content = {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(
                    groups,
                    key = { _, group -> group }
                ) { _, group ->
                    ReorderableItem(
                        reorderableState,
                        key = group
                    ) { isDragging ->
                        val elevation = if (isDragging) 4.dp else 0.dp
                        Surface(shadowElevation = elevation) {
                            ListItem(
                                headlineContent = { Text(group) },
                                leadingContent = {
                                    CCIconButton(
                                        icon = Icons.Rounded.Edit,
                                        contentDescription = R.string.settings_group_rename,
                                        onClick = { onRequestRename(group) },
                                    )
                                },
                                trailingContent = {
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
                            )
                        }
                    }
                }
            }
        },
        confirmButton = R.string.common_done,
        onConfirm = onDismiss
    )
}