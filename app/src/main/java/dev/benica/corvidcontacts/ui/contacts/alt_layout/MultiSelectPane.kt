// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.alt_layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.GroupRemove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.extensions.onSurface
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * Wide-screen shell's detail-pane content while multiple contacts are selected: a scrollable
 * accounting of who's selected (tapping a name deselects it - useful once the list pane has
 * scrolled away from some of the checked rows) beside the bulk actions themselves as labeled
 * rows, instead of cramming them into [dev.benica.corvidcontacts.ui.contacts.contact_list.components.MultiSelectTopBar]'s
 * icon-only overflow menu, which stays as-is for phone.
 */
@Composable
fun MultiSelectPane(
    selectedContacts: List<ContactEntity>,
    onToggleSelection: (String) -> Unit,
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
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = pluralStringResource(
                R.plurals.list_selected_count,
                selectedContacts.size,
                selectedContacts.size
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(
                horizontal = Dimens.xlSpacing,
                vertical = Dimens.lgSpacing,
            ),
        )

        val actionsParams = MultiSelectActionsParams(
            onShowAddToGroupDialog = onShowAddToGroupDialog,
            selectedGroup = selectedGroup,
            onRemoveFromCurrentGroup = onRemoveFromCurrentGroup,
            showArchived = showArchived,
            onArchiveSelected = onArchiveSelected,
            availableAddressBooks = availableAddressBooks,
            onMoveSelected = onMoveSelected,
            onShareSelected = onShareSelected,
            onDeleteSelected = onDeleteSelected,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (maxWidth < 600.dp) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top: selected contacts
                    SelectedContactsList(
                        selectedContacts,
                        onToggleSelection,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    // Bottom: actions
                    PortraitActionsSection(
                        actionsParams, Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: selected contacts
                    SelectedContactsList(
                        selectedContacts,
                        onToggleSelection,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    // Right: actions
                    ActionsList(
                        actionsParams, Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Bundles the bulk-action callbacks/state shared by [PortraitActionsSection] and [ActionsList]. */
private data class MultiSelectActionsParams(
    val onShowAddToGroupDialog: () -> Unit,
    val selectedGroup: String?,
    val onRemoveFromCurrentGroup: () -> Unit,
    val showArchived: Boolean,
    val onArchiveSelected: () -> Unit,
    val availableAddressBooks: List<AddressBookEntity>,
    val onMoveSelected: (String) -> Unit,
    val onShareSelected: () -> Unit,
    val onDeleteSelected: () -> Unit,
)

/** Add-to-group/remove-from-group/archive rows shared by [PortraitActionsSection] and [ActionsList]. */
@Composable
private fun LeadingActionRows(params: MultiSelectActionsParams) {
    MultiSelectActionRow(
        icon = Icons.Outlined.GroupAdd,
        label = stringResource(R.string.list_action_add_to_group),
        onClick = params.onShowAddToGroupDialog,
    )
    if (params.selectedGroup != null) {
        val groupDisplayName = if (
            params.selectedGroup.equals(ContactsRepository.FAVORITE_CATEGORY, ignoreCase = true)
        ) {
            stringResource(R.string.list_group_favorites)
        } else {
            params.selectedGroup
        }
        MultiSelectActionRow(
            icon = Icons.Outlined.GroupRemove,
            label = stringResource(R.string.list_action_remove_from_group, groupDisplayName),
            onClick = params.onRemoveFromCurrentGroup,
            tint = MaterialTheme.colorScheme.error,
        )
    }
    MultiSelectActionRow(
        icon = if (params.showArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
        label = stringResource(
            if (params.showArchived) R.string.list_action_unarchive_selected
            else R.string.list_action_archive_selected
        ),
        onClick = params.onArchiveSelected,
    )
}

/** Share/delete rows shared by [PortraitActionsSection] and [ActionsList]. */
@Composable
private fun TrailingActionRows(params: MultiSelectActionsParams) {
    MultiSelectActionRow(
        icon = Icons.Outlined.Share,
        label = stringResource(R.string.list_action_share_selected),
        onClick = params.onShareSelected,
    )
    MultiSelectActionRow(
        icon = Icons.Outlined.Delete,
        label = stringResource(R.string.list_action_delete_selected),
        onClick = params.onDeleteSelected,
    )
}

@Composable
private fun SelectedContactsList(
    selectedContacts: List<ContactEntity>,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight(),
        contentPadding = PaddingValues(
            horizontal = Dimens.innerSpacing,
            vertical = Dimens.innerSpacing,
        ),
    ) {
        items(selectedContacts, key = { it.id }) { contact ->
            ListItem(
                headlineContent = { Text(contact.getEffectiveDisplayName()) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.TwoTone.Close,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(
                        R.string.list_action_deselect_contact,
                        contact.getEffectiveDisplayName()
                    )
                ) { onToggleSelection(contact.id) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

/**
 * Portrait-width layout for the bulk actions: address-book choices sit in their own permanently
 * visible, independently scrollable column instead of [ActionsList]'s expand-in-place "Move to"
 * row - there's no good place for an accordion to push its neighbors when the actions are already
 * squeezed into a single narrow column. The address-book column's height is measured from the
 * actions column (not [androidx.compose.foundation.layout.IntrinsicSize], which would force an
 * intrinsic-measurement pass that crashes against the address-book [LazyColumn] below, same as
 * intrinsics do against any layout built on `SubcomposeLayout`).
 */
@Composable
private fun PortraitActionsSection(
    params: MultiSelectActionsParams,
    modifier: Modifier = Modifier,
) {
    var actionsHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                horizontal = Dimens.innerSpacing,
                vertical = Dimens.innerSpacing,
            ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { actionsHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(Dimens.xlSpacing),
        ) {
            LeadingActionRows(params)
            TrailingActionRows(params)
        }

        VerticalDivider()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.list_action_move_selected),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = Dimens.smSpacing),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { actionsHeightPx.toDp() }),
            ) {
                items(params.availableAddressBooks, key = { it.href }) { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { params.onMoveSelected(book.href) }
                            .padding(vertical = Dimens.smSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing),
                    ) {
                        Icon(
                            imageVector = ContactColors.getIconForAddressBook(
                                book.displayName,
                                book.iconName
                            ),
                            contentDescription = null,
                            tint = Color(book.colorInt),
                        )
                        Text(
                            text = book.displayName ?: stringResource(R.string.common_unknown),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsList(
    params: MultiSelectActionsParams,
    modifier: Modifier = Modifier,
) {
    var showMoveOptions by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    horizontal = Dimens.innerSpacing,
                    vertical = Dimens.innerSpacing,
                )
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.xlSpacing)
    ) {
        LeadingActionRows(params)
        MultiSelectActionRow(
            icon = Icons.AutoMirrored.Outlined.DriveFileMove,
            label = stringResource(R.string.list_action_move_selected),
            onClick = { showMoveOptions = !showMoveOptions },
            trailing = {
                Icon(
                    imageVector = if (showMoveOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.onSurface()
                )
            }
        )
        if (showMoveOptions) {
            params.availableAddressBooks.forEach { book ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            params.onMoveSelected(book.href)
                            showMoveOptions = false
                        }
                        .padding(
                            start = Dimens.xxlSpacing + Dimens.xsSpacing,
                            end = Dimens.xlSpacing,
                            top = Dimens.smSpacing,
                            bottom = Dimens.smSpacing,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                ) {
                    Icon(
                        imageVector = ContactColors.getIconForAddressBook(
                            book.displayName,
                            book.iconName
                        ),
                        contentDescription = null,
                        tint = Color(book.colorInt),
                    )
                    Text(book.displayName ?: stringResource(R.string.common_unknown))
                }
            }
        }
        TrailingActionRows(params)
    }
}

@Composable
private fun MultiSelectActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary.onSurface(),
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.medSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(text = label, color = tint, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Spacer(Modifier.width(Dimens.xsSpacing))
            trailing()
        }
    }
}

@ThemePreview
@Composable
fun MultiSelectPanePreview() {
    CorvidContactsTheme {
        CCScaffold { pv ->
            Box(
                modifier = Modifier.padding(pv)
            ) {
                MultiSelectPane(
                    selectedContacts = listOf(
                        ContactEntity(
                            id = "a",
                            displayName = "Ferris Wheel",
                        ),
                        ContactEntity(
                            id = "b",
                            displayName = "Cali Fragi",
                        ),
                        ContactEntity(
                            id = "c",
                            displayName = "Vesper Wy",
                        )
                    ),
                    onToggleSelection = {},
                    selectedGroup = null,
                    onRemoveFromCurrentGroup = {},
                    onShowAddToGroupDialog = {},
                    onArchiveSelected = {},
                    showArchived = false,
                    availableAddressBooks = emptyList(),
                    onMoveSelected = {},
                    onShareSelected = {},
                    onDeleteSelected = {},
                )
            }
        }
    }
}
