// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.StatelessContactListScreen
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main screen for displaying and managing contacts.
 *
 * @param viewModel ViewModel for managing contacts.
 * @param onContactClick Callback when a contact is clicked.
 * @param onAddContact Callback when the add contact button is clicked.
 * @param onAddSelfContact Callback to add the user's own contact.
 * @param onSetUpSync Callback to set up synchronization.
 * @param onSettingsClick Callback when the settings button is clicked.
 * @param onContactSelected Callback when a contact is selected in picker mode.
 * @param onCancelSelectingContact Callback when selecting a contact is canceled.
 * @param onClearSelectingContact Callback when the selected contact is cleared.
 * @param onShareSelf Callback when the self contact is shared.
 * @param onShareSelfViaQr Callback when the self contact is shared via QR code.
 * @param modifier Modifier for the root layout.
 */
@Composable
fun ContactListScreen(
    viewModel: ContactsViewModel,
    onContactClick: (ContactWithAddressBook) -> Unit,
    onAddContact: () -> Unit,
    onAddSelfContact: () -> Unit,
    onSetUpSync: () -> Unit,
    onSettingsClick: () -> Unit,
    onContactSelected: (ContactWithAddressBook?) -> Unit,
    onCancelSelectingContact: () -> Unit,
    onClearSelectingContact: () -> Unit,
    onShareSelf: (ContactWithAddressBook) -> Unit,
    onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedIds by viewModel.selectedContactIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedAddressBookHrefs by viewModel.selectedAddressBookHrefs.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val allGroupsGlobal by viewModel.allGroupsGlobal.collectAsState()
    val groupsAvailableForSelectedBooks by viewModel.groupsAvailableForSelectedBooks.collectAsState()
    val selfContact by viewModel.selfContact.collectAsState()
    val isPickingSelf by viewModel.isPickingSelf.collectAsState()
    val isPickingExternal by viewModel.isPickingExternal.collectAsState()
    val isPickingMergeTarget by viewModel.isPickingMergeTarget.collectAsState()
    val isLocalOnlyMode by viewModel.isLocalOnlyMode.collectAsState()

    val pickerMode = when {
        isPickingMergeTarget -> PickerMode.MERGE_TARGET
        isPickingExternal -> PickerMode.EXTERNAL
        isPickingSelf -> PickerMode.SELF
        else -> PickerMode.NONE
    }

    val state = ContactListUiStateData(
        uiState = uiState,
        isRefreshing = isRefreshing,
        selectedIds = selectedIds,
        isSelectionMode = isSelectionMode,
        showArchived = showArchived,
        searchQuery = searchQuery,
        selectedGroup = selectedGroup,
        selectedAddressBookHrefs = selectedAddressBookHrefs,
        allGroups = allGroups,
        allGroupsGlobal = allGroupsGlobal,
        groupsAvailableForSelectedBooks = groupsAvailableForSelectedBooks,
        selfContact = selfContact,
        pickerMode = pickerMode,
        isLocalOnlyMode = isLocalOnlyMode,
    )

    val actions = remember(viewModel) {
        ContactListActions(
            onToggleSelection = { viewModel.toggleSelection(it) },
            onClearSelection = { viewModel.clearSelection() },
            onDeselectAll = { viewModel.deselectAll() },
            onSelectAll = { viewModel.selectAll() },
            onArchiveSelected = { viewModel.archiveSelectedContacts() },
            onDeleteSelected = { viewModel.deleteSelectedContacts() },
            onMoveSelected = { viewModel.moveSelectedContactsToAddressBook(it) },
            onShareSelected = { viewModel.getSelectedContactsVCardString() },
            onRefresh = { viewModel.refresh() },
            onSetUpSync = onSetUpSync,
            onLogout = { viewModel.logout() },
            onDeleteLocalData = { viewModel.deleteAllLocalData() },
            onExportContacts = { viewModel.exportAllContacts() },
            onToggleArchived = { viewModel.toggleShowArchived() },
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onGroupSelected = { viewModel.updateSelectedGroup(it) },
            onSelectAddressBook = { viewModel.selectAddressBook(it) },
            onUpdateAddressBookAppearance = { book, color, iconName ->
                viewModel.updateAddressBookAppearance(
                    book,
                    color,
                    iconName
                )
            },
            onUpdateAddressBookOrder = { viewModel.updateAddressBookOrder(it) },
            onCreateAddressBook = { name, color, forceLocal ->
                viewModel.createAddressBook(
                    name,
                    color,
                    forceLocal
                )
            },
            onRenameAddressBook = { book, newName ->
                viewModel.renameAddressBook(
                    book,
                    newName
                )
            },
            onDeleteAddressBook = { viewModel.deleteAddressBook(it) },
            onUploadLocalAddressBook = { book, newName ->
                viewModel.uploadLocalAddressBook(
                    book,
                    newName
                )
            },
            onToggleAddressBookVisibility = { viewModel.toggleAddressBookVisibility(it) },
            onUpdateGroupOrder = { viewModel.updateGroupOrder(it) },
            onRenameGroup = { oldName, newName ->
                viewModel.renameGroup(
                    oldName,
                    newName
                )
            },
            onAddSelectedToGroup = { viewModel.addSelectedToGroup(it) },
            onRemoveSelectedFromGroup = { viewModel.removeSelectedFromGroup(it) },
            onStartSelectingContact = {
                val hasContacts =
                    (uiState as? ContactsUiState.Success)?.contacts?.isNotEmpty() == true
                if (hasContacts) {
                    viewModel.startPickingSelf()
                } else {
                    onAddSelfContact()
                }
            },
            onCancelSelectingContact = onCancelSelectingContact,
            onClearSelectingContact = onClearSelectingContact,
            onContactSelected = onContactSelected,
            onContactClick = onContactClick,
            onAddContact = onAddContact,
            onSettingsClick = onSettingsClick,
            onShareSelf = onShareSelf,
            onShareSelfViaQr = onShareSelfViaQr
        )
    }

    StatelessContactListScreen(
        state = state,
        actions = actions,
        addressBooks = viewModel.addressBooks,
        allManageableAddressBooks = viewModel.allManageableAddressBooks,
        events = viewModel.events,
        modifier = modifier,
    )
}

@ThemePreview
@Composable
private fun ContactListScreenPreview() {
    CorvidContactsTheme {
        StatelessContactListScreen(
            state = ContactListUiStateData(
                uiState = mockUiState,
                isRefreshing = false,
                selectedIds = setOf(),
                isSelectionMode = false,
                showArchived = false,
                searchQuery = "",
                selectedGroup = "Ted",
                selectedAddressBookHrefs = setOf(),
                allGroups = listOf("Ted"),
                allGroupsGlobal = emptyList(),
                groupsAvailableForSelectedBooks = emptySet(),
                selfContact = null,
                pickerMode = PickerMode.NONE,
                isLocalOnlyMode = false
            ),
            actions = mockListActions,
            addressBooks = MutableStateFlow(emptyList()),
            allManageableAddressBooks = MutableStateFlow(emptyList()),
            events = MutableSharedFlow(),
            modifier = Modifier
        )
    }
}

