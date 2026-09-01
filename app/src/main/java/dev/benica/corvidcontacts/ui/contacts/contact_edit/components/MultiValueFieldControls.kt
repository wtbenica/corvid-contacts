// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableTypedValue
import dev.benica.corvidcontacts.ui.contacts.contact_edit.HasId
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * The type selector + reorder + delete row shared by every multi-value contact field
 * (phone, email, address, social, etc.).
 *
 * Move buttons are always shown, disabled rather than hidden at the list boundaries - a hidden
 * button gives no indication reordering is even possible; a visibly-disabled one does. Delete
 * asks for confirmation first, since it's a single tap with no undo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypedFieldControlsRow(
    type: String,
    onTypeChange: (String) -> Unit,
    types: List<VCardType>,
    enabled: Boolean,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CCExposedDropdownMenuBox(
            textBoxLabel = stringResource(R.string.common_type),
            currentValue = VCardType.getLabel(type),
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            types.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.getDisplayName()) },
                    onClick = {
                        onTypeChange(t.id)
                        expanded = false
                    }
                )
            }
        }

        if (enabled) {
            CCIconButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                onClick = { onMoveUp?.invoke() },
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onSurface,
                enabled = onMoveUp != null,
            )
            CCIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                onClick = { onMoveDown?.invoke() },
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onSurface,
                enabled = onMoveDown != null,
            )
        }

        if (onDelete != null) {
            CCIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = R.string.detail_menu_delete,
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                enabled = enabled
            )
        }

        if (showDeleteConfirm && onDelete != null) {
            CCAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = R.string.edit_delete_field_confirm_title,
                content = { Text(stringResource(R.string.edit_delete_field_confirm_message)) },
                confirmButton = R.string.action_remove,
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                },
                dismissButton = R.string.action_cancel,
            )
        }
    }
}

/**
 * A [Modifier] that requests focus once, the first time [requestInitialFocus] is true, and
 * explicitly scrolls itself into view when it does - the pattern every multi-value field uses
 * to focus (and reveal) a newly-added row. Apply directly to the field's own modifier chain.
 *
 * The scroll is explicit rather than relying on a scrollable ancestor's implicit
 * focus-follows-into-view behavior, because a single hardcoded scroll target (e.g. always
 * scrolling to the bottom) would be correct only for whichever section renders last, and
 * silently wrong for every section above it. [BringIntoViewRequester] scrolls to wherever this
 * specific field actually ends up, regardless of what else is on the screen.
 */
@Composable
fun Modifier.rememberInitialFocusModifier(requestInitialFocus: Boolean): Modifier {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            focusRequester.requestFocus()
            bringIntoViewRequester.bringIntoView()
        }
    }
    return this
        .focusRequester(focusRequester)
        .bringIntoViewRequester(bringIntoViewRequester)
}

/**
 * Owns add/remove/reorder for one multi-value field list (phones, emails, addresses, social
 * profiles, websites, relationships). [newItem] builds a blank row (e.g. a phone defaulting to
 * CELL/the device region); [onItemAdded] is how the controller reports the new row's id back
 * out, since focusing it is the caller's concern (a single `pendingFocusId` shared across every
 * list on the screen), not the controller's.
 */
@Stable
class MultiValueListController<T : HasId>(
    val items: SnapshotStateList<T>,
    private val newItem: () -> T,
    private val onItemAdded: (id: String) -> Unit,
) {
    fun add() {
        val item = newItem()
        items.add(item)
        onItemAdded(item.id)
    }

    fun remove(index: Int) {
        items.removeAt(index)
    }

    fun update(
        index: Int,
        value: T,
    ) {
        items[index] = value
    }

    fun moveUp(index: Int) = move(
        index,
        up = true
    )

    fun moveDown(index: Int) = move(
        index,
        up = false
    )

    private fun move(
        index: Int,
        up: Boolean,
    ) {
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in items.indices) {
            val item = items.removeAt(index)
            items.add(
                targetIndex,
                item
            )
        }
    }
}

/**
 * Renders one row per item in a [controller]'s list, followed by an "Add [addLabel]" row -
 * gating delete/move-up/move-down (delete on [isReadOnly], the moves at the list boundaries),
 * clearing [pendingFocusId] once the newly-added row has consumed it, and the add affordance
 * itself.
 *
 * The add row lives at the *bottom* of the list rather than as a "+" in the section header - a
 * "+" sitting directly above the first row's delete button invited mis-taps, and visually it's
 * clearer for "add another" to appear where the new item will actually go. [itemContent] only
 * needs to render the field itself for a given item.
 */
@Composable
fun <T : HasId> MultiValueFieldList(
    controller: MultiValueListController<T>,
    isReadOnly: Boolean,
    addLabel: String,
    pendingFocusId: String?,
    onFocusRequested: (String?) -> Unit,
    itemContent: @Composable (
        index: Int,
        item: T,
        onDelete: (() -> Unit)?,
        onMoveUp: (() -> Unit)?,
        onMoveDown: (() -> Unit)?,
        requestInitialFocus: Boolean,
    ) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.xlSpacing),
    ) {
        controller.items.forEachIndexed { index, item ->
            itemContent(
                index,
                item,
                if (!isReadOnly) {
                    { controller.remove(index) }
                } else null,
                if (index > 0) {
                    { controller.moveUp(index) }
                } else null,
                if (index < controller.items.size - 1) {
                    { controller.moveDown(index) }
                } else null,
                item.id == pendingFocusId
            )

            if (item.id == pendingFocusId) {
                LaunchedEffect(item.id) { onFocusRequested(null) }
            }
        }
    }

    if (!isReadOnly) {
        CCTextButton(
            text = stringResource(
                R.string.action_add_item,
                addLabel
            ),
            onClick = controller::add,
            icon = Icons.Rounded.Add,
        )
    }
}

@ThemePreview
@Composable
fun MultiValueFieldListPreview() {
    CorvidContactsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Dimens.innerSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
            ) {
                MultiValueFieldList(
                    controller = MultiValueListController(
                        items = remember {
                            listOf(
                                EditableTypedValue(
                                    type = "Item layout is customizeable",
                                    value = "Hare Krishna",
                                    region = "MX"
                                ), EditableTypedValue(
                                    type = "Carrot",
                                    value = "Hare Hare",
                                ), EditableTypedValue(
                                    type = "Apple",
                                    value = "Vanity 6",
                                ), EditableTypedValue(
                                    type = "Kale",
                                    value = "Overthrow",
                                    region = "TX"
                                )

                            )
                                .toMutableStateList()
                        },
                        newItem = { EditableTypedValue() },
                        onItemAdded = {}
                    ),
                    isReadOnly = false,
                    addLabel = "Monkey",
                    pendingFocusId = null,
                    onFocusRequested = { },
                    itemContent = { _, item, _, _, _, _ ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CutCornerShape(Dimens.xlSpacing)
                        ) {
                            Surface(
                                modifier = Modifier.padding(Dimens.xsSpacing),
                                color = ContactColors.palette[6].text(),
                                shape = CutCornerShape(Dimens.medSpacing)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.medSpacing)
                                ) {
                                    Row {
                                        Text(
                                            item.type,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(item.region ?: "No region")
                                    }

                                    Text(item.value)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

