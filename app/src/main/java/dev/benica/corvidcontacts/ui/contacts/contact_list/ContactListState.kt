// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list

import androidx.compose.ui.graphics.Color
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.AddressBookUploadResult
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState

/**
 * Which single-contact picker mode (if any) the list is currently being used for.
 */
enum class PickerMode { NONE, SELF, EXTERNAL, MERGE_TARGET }

/**
 * Groups all observable states for the contacts list.
 *
 * @property uiState The current state of the contact list (Loading, Success, Error).
 * @property isRefreshing Whether the list is currently being refreshed.
 * @property selectedIds The IDs of the currently selected contacts.
 * @property isSelectionMode Whether multi-select mode is active, independent of whether anything is currently checked.
 * @property showArchived Whether to show archived contacts.
 * @property searchQuery The current search query.
 * @property selectedGroup The currently selected group filter, if any.
 * @property selectedAddressBookHrefs The HREFs of the currently selected address books.
 * @property allGroups The list of all groups available in the current selection.
 * @property allGroupsGlobal The list of all groups available globally.
 * @property groupsAvailableForSelectedBooks The groups available for the selected address books.
 * @property selfContact The user's own contact information.
 * @property pickerMode The current picker mode.
 * @property isLocalOnlyMode Whether the app is in local-only mode.
 */
data class ContactListUiStateData(
    val uiState: ContactsUiState,
    val isRefreshing: Boolean,
    val selectedIds: Set<String>,
    val isSelectionMode: Boolean,
    val showArchived: Boolean,
    val searchQuery: String,
    val selectedGroup: String?,
    val selectedAddressBookHrefs: Set<String>,
    val allGroups: List<String>,
    val allGroupsGlobal: List<String>,
    val groupsAvailableForSelectedBooks: Set<String>,
    val selfContact: ContactWithAddressBook?,
    val pickerMode: PickerMode,
    val isLocalOnlyMode: Boolean,
)

/**
 * Groups all user interaction callbacks for the contacts list.
 */
data class ContactListActions(
    val onToggleSelection: (String) -> Unit,
    val onClearSelection: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onSelectAll: () -> Unit,
    val onArchiveSelected: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onMoveSelected: (String) -> Unit,
    val onShareSelected: suspend () -> String,
    val onRefresh: () -> Unit,
    val onSetUpSync: () -> Unit,
    val onLogout: () -> Unit,
    val onDeleteLocalData: () -> Unit,
    val onExportContacts: suspend () -> String,
    val onToggleArchived: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onGroupSelected: (String?) -> Unit,
    val onSelectAddressBook: (String?) -> Unit,
    val onUpdateAddressBookAppearance: (AddressBookEntity, Color, String?) -> Unit,
    val onUpdateAddressBookOrder: (List<AddressBookEntity>) -> Unit,
    val onCreateAddressBook: suspend (String, Color, Boolean) -> Result<AddressBookEntity>,
    val onRenameAddressBook: suspend (AddressBookEntity, String) -> Result<Unit>,
    val onDeleteAddressBook: suspend (AddressBookEntity) -> Result<Unit>,
    val onUploadLocalAddressBook: suspend (AddressBookEntity, String) -> Result<AddressBookUploadResult>,
    val onToggleAddressBookVisibility: (AddressBookEntity) -> Unit,
    val onUpdateGroupOrder: (List<String>) -> Unit,
    val onRenameGroup: (String, String) -> Unit,
    val onAddSelectedToGroup: (String) -> Unit,
    val onRemoveSelectedFromGroup: (String) -> Unit,
    val onStartSelectingContact: () -> Unit,
    val onCancelSelectingContact: () -> Unit,
    val onClearSelectingContact: () -> Unit,
    val onContactSelected: (ContactWithAddressBook?) -> Unit,
    val onContactClick: (ContactWithAddressBook) -> Unit,
    val onAddContact: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onShareSelf: (ContactWithAddressBook) -> Unit,
    val onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
)
