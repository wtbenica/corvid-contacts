// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCCardBordered
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.CreateAddressBookDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.ManageGroupsDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.RenameGroupDialog
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import kotlinx.coroutines.launch

/**
 * A bottom sheet for filtering contacts by address book and group.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomFilterSheet(
    setFilterState: (Boolean) -> Unit,
    filterSheetState: SheetState,
    selectedAddressBookHrefs: Set<String>,
    availableAddressBooks: List<AddressBookEntity>,
    manageableAddressBooks: List<AddressBookEntity>,
    hasServerConnection: Boolean,
    onSelectAddressBook: (String?) -> Unit,
    allGroups: List<String>,
    availableGroups: Set<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateAddressBook: suspend (String, Color, Boolean, String?) -> Result<AddressBookEntity>,
    onManageAddressBooks: (() -> Unit)?,
    onUpdateGroupOrder: (List<String>) -> Unit,
    onRenameGroup: (String, String) -> Unit,
) {
    val baseColor = currentThemeColor()

    ModalBottomSheet(
        onDismissRequest = { setFilterState(false) },
        sheetState = filterSheetState,
        containerColor = baseColor.surface()
    ) {
        BottomFilterSheetContent(
            selectedAddressBookHrefs = selectedAddressBookHrefs,
            availableAddressBooks = availableAddressBooks,
            manageableAddressBooks = manageableAddressBooks,
            hasServerConnection = hasServerConnection,
            onSelectAddressBook = onSelectAddressBook,
            allGroups = allGroups,
            availableGroups = availableGroups,
            selectedGroup = selectedGroup,
            onGroupSelected = onGroupSelected,
            onCreateAddressBook = onCreateAddressBook,
            onManageAddressBooks = onManageAddressBooks?.let { manage ->
                {
                    setFilterState(false)
                    manage()
                }
            },
            onUpdateGroupOrder = onUpdateGroupOrder,
            onRenameGroup = onRenameGroup,
            modifier = Modifier.padding(bottom = 32.dp)

        )
    }
}

@Composable
fun BottomFilterSheetContent(
    selectedAddressBookHrefs: Set<String>,
    availableAddressBooks: List<AddressBookEntity>,
    manageableAddressBooks: List<AddressBookEntity>,
    hasServerConnection: Boolean,
    onSelectAddressBook: (String?) -> Unit,
    allGroups: List<String>,
    availableGroups: Set<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateAddressBook: suspend (String, Color, Boolean, String?) -> Result<AddressBookEntity>,
    onManageAddressBooks: (() -> Unit)?,
    onUpdateGroupOrder: (List<String>) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val genericErrorMessage = stringResource(R.string.settings_address_book_generic_error)

    var showCreateAddressBookDialog by remember { mutableStateOf(false) }
    var isSubmittingAddressBookAction by remember { mutableStateOf(false) }
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<String?>(null) }

    val manageableGroups = remember(allGroups) {
        allGroups.filterNot {
            it.equals(
                ContactsRepository.FAVORITE_CATEGORY,
                ignoreCase = true
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.common_address_book),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
            )
            if (onManageAddressBooks != null) {
                CCIconButton(
                    icon = Icons.Rounded.Edit,
                    contentDescription = R.string.edit_action_manage_address_books,
                    onClick = onManageAddressBooks,
                )
            }
            CCIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = R.string.settings_address_book_add,
                onClick = { showCreateAddressBookDialog = true },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        CCCardBordered(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = Dimens.xsSpacing)
        ) {
            AddressBookFilterRow(
                selectedAddressBookHrefs,
                availableAddressBooks,
                onSelectAddressBook
            )
        }

        if (allGroups.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.medSpacing))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.common_groups),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                )
                if (manageableGroups.isNotEmpty()) {
                    CCIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = R.string.edit_action_manage_groups,
                        onClick = { showManageGroupsDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            val currentBook = if (selectedAddressBookHrefs.size == 1) {
                availableAddressBooks.find { it.href == selectedAddressBookHrefs.first() }
            } else null

            CCCardBordered(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(vertical = Dimens.xsSpacing)
            ) {
                GroupFilterRow(
                    groups = allGroups,
                    availableGroups = availableGroups,
                    selectedGroup = selectedGroup,
                    onGroupSelected = onGroupSelected,
                    selectedBook = currentBook
                )
            }
        }
    }

    if (showCreateAddressBookDialog) {
        CreateAddressBookDialog(
            isSubmitting = isSubmittingAddressBookAction,
            hasServerConnection = hasServerConnection,
            existingColors = manageableAddressBooks.map { it.colorInt },
            onConfirm = { name, color, forceLocal, iconName ->
                if (!isSubmittingAddressBookAction) {
                    isSubmittingAddressBookAction = true
                    scope.launch {
                        val result = onCreateAddressBook(
                            name,
                            color,
                            forceLocal,
                            iconName
                        )
                        isSubmittingAddressBookAction = false
                        if (result.isSuccess) {
                            showCreateAddressBookDialog = false
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    genericErrorMessage,
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
            },
            onDismiss = { if (!isSubmittingAddressBookAction) showCreateAddressBookDialog = false }
        )
    }

    if (showManageGroupsDialog) {
        ManageGroupsDialog(
            groups = manageableGroups,
            onUpdateOrder = onUpdateGroupOrder,
            onRequestRename = { groupToRename = it },
            onDismiss = { showManageGroupsDialog = false }
        )
    }

    groupToRename?.let { oldName ->
        RenameGroupDialog(
            oldName = oldName,
            onConfirm = { newName ->
                onRenameGroup(
                    oldName,
                    newName
                )
                groupToRename = null
            },
            onDismiss = { groupToRename = null }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomFilterSheetPreview() {
    CorvidContactsTheme {
        BottomFilterSheetContent(
            availableAddressBooks = listOf(
                AddressBookEntity(
                    href = "1",
                    displayName = "Personal",
                    colorInt = ContactColors.palette[3].toArgb(),
                    isVisible = true
                ),
                AddressBookEntity(
                    href = "2",
                    displayName = "Work",
                    colorInt = ContactColors.palette[7].toArgb(),
                    isVisible = true
                )
            ),
            manageableAddressBooks = listOf(
                AddressBookEntity(
                    href = "1",
                    displayName = "Personal",
                    colorInt = ContactColors.palette[3].toArgb(),
                    isVisible = true
                ),
                AddressBookEntity(
                    href = "2",
                    displayName = "Work",
                    colorInt = ContactColors.palette[7].toArgb(),
                    isVisible = true
                ),
                AddressBookEntity(
                    href = "3",
                    displayName = "Archived",
                    colorInt = ContactColors.palette[1].toArgb(),
                    isVisible = false
                )
            ),
            hasServerConnection = true,
            selectedAddressBookHrefs = setOf("1"),
            onSelectAddressBook = {},
            allGroups = listOf(
                "Friends",
                "Family",
                "Work",
                "Favorites"
            ),
            availableGroups = setOf(
                "Family",
                "Work",
                "Favorites"
            ),
            selectedGroup = "Work",
            onGroupSelected = { },
            onCreateAddressBook = { _, _, _, _ ->
                Result.success(
                    AddressBookEntity(
                        href = "preview",
                        displayName = "Preview",
                        colorInt = ContactColors.palette[3].toArgb(),
                        isVisible = true
                    )
                )
            },
            onManageAddressBooks = {},
            onUpdateGroupOrder = {},
            onRenameGroup = { _, _ -> },
        )
    }
}
