// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.ContactsEvent
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.EmptyState
import dev.benica.corvidcontacts.ui.contacts.common_ui.FullScreenLoading
import dev.benica.corvidcontacts.ui.contacts.common_ui.FullScreenSyncFailureError
import dev.benica.corvidcontacts.ui.contacts.contact_list.ContactListActions
import dev.benica.corvidcontacts.ui.contacts.contact_list.ContactListUiStateData
import dev.benica.corvidcontacts.ui.contacts.contact_list.PickerMode
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.utils.shareVCard
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Main screen for displaying and managing contacts.
 *
 * Supports searching, filtering by group, selecting multiple contacts, and managing address books.
 *
 * @param state Current state of the contacts UI.
 * @param actions Callbacks for user interactions.
 * @param addressBooks A [StateFlow] of currently-visible address books, used for filtering.
 * @param allManageableAddressBooks A [StateFlow] of every address book regardless of visibility.
 * @param events Flow of asynchronous events like errors or repair notifications.
 * @param modifier Modifier for the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatelessContactListScreen(
    state: ContactListUiStateData,
    actions: ContactListActions,
    addressBooks: StateFlow<List<AddressBookEntity>>,
    allManageableAddressBooks: StateFlow<List<AddressBookEntity>>,
    events: SharedFlow<ContactsEvent>,
    modifier: Modifier = Modifier,
    isPane: Boolean = false,
    showScaffold: Boolean = true,
    detailActions: @Composable (RowScope.() -> Unit)? = null,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteLocalDataConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val filterSheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val shareSelectedTitle = stringResource(R.string.common_share)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = actions.onExportContacts()
                context.contentResolver
                    .openOutputStream(uri)
                    ?.use { outputStream ->
                        outputStream.write(data.toByteArray())
                    }
            }
        }
    }

    val resources = LocalResources.current

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is ContactsEvent.Error -> {
                    val message = if (event.formatArgs != null) {
                        resources.getString(
                            event.resId,
                            event.formatArgs
                        )
                    } else {
                        resources.getString(event.resId)
                    }
                    snackbarHostState.showSnackbar(message)
                }

                is ContactsEvent.RepairPerformed -> {
                    val parts = buildList {
                        if (event.stats.photoCount > 0) add("${event.stats.photoCount} photo(s)")
                        if (event.stats.phoneCount > 0) add("${event.stats.phoneCount} phone(s)")
                    }

                    val message = resources
                        .getString(R.string.list_repair_notification)
                        .format(parts.joinToString(", "))
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )
                }

                is ContactsEvent.Message -> {
                    val message = when (val m = event.message) {
                        is ContactsEvent.ContactsMessage.ContactsArchived -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_archived,
                                m.count,
                                m.count
                            )
                        }

                        is ContactsEvent.ContactsMessage.ContactsUnarchived -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_unarchived,
                                m.count,
                                m.count
                            )
                        }

                        is ContactsEvent.ContactsMessage.ContactsDeleted -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_deleted,
                                m.count,
                                m.count
                            )
                        }

                        is ContactsEvent.ContactsMessage.ContactsMoved -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_moved,
                                m.count,
                                m.count
                            )
                        }

                        is ContactsEvent.ContactsMessage.ContactsAddedToGroup -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_added_to_group,
                                m.count,
                                m.count,
                                m.groupName
                            )
                        }

                        is ContactsEvent.ContactsMessage.ContactsRemovedFromGroup -> {
                            resources.getQuantityString(
                                R.plurals.snackbar_contacts_removed_from_group,
                                m.count,
                                m.count,
                                m.groupName
                            )
                        }

                        ContactsEvent.ContactsMessage.ContactSaved -> resources.getString(R.string.snackbar_contact_saved)
                        ContactsEvent.ContactsMessage.ContactsMerged -> resources.getString(R.string.snackbar_contact_merged)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    val isSelectionMode = state.isSelectionMode

    val availableAddressBooks by addressBooks.collectAsState()
    val manageableAddressBooks by allManageableAddressBooks.collectAsState()

    val currentBook: AddressBookEntity? =
        if (state.selectedAddressBookHrefs.size == 1 && availableAddressBooks.size > 1) {
            availableAddressBooks.find { it.href == state.selectedAddressBookHrefs.first() }
        } else null

    val baseColor = if (isPane && !isSelectionMode) {
        MaterialTheme.colorScheme.primary
    } else {
        currentBook?.let {
            ContactColors.getAddressBookColorWithFallback(
                customColor = it.colorInt,
                href = it.href
            )
        } ?: MaterialTheme.colorScheme.primary
    }

    val contactCountByAddressBook = remember(state.uiState) {
        (state.uiState as? ContactsUiState.Success)?.contacts
            ?.groupBy { it.contact.addressBookHref }
            ?.mapValues { it.value.size }
            ?: emptyMap()
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        if (showGroupDialog) {
            AddToGroupDialog(
                allGroups = state.allGroups,
                onConfirm = { groupName ->
                    actions.onAddSelectedToGroup(groupName)
                    showGroupDialog = false
                },
                onDismissRequest = {
                    showGroupDialog = false
                }
            )
        }

        if (showDeleteConfirm) {
            CCAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        pluralStringResource(
                            R.plurals.list_delete_confirm_title,
                            state.selectedIds.size,
                            state.selectedIds.size
                        )
                    )
                },
                content = {
                    Text(
                        pluralStringResource(
                            R.plurals.list_delete_confirm_message,
                            state.selectedIds.size,
                            state.selectedIds.size
                        ),
                    )
                },
                confirmButton = R.string.detail_menu_delete,
                onConfirm = {
                    showDeleteConfirm = false
                    actions.onDeleteSelected()
                },
                dismissButton = R.string.action_cancel,
            )
        }

        if (showDeleteLocalDataConfirm) {
            val contactCount = (state.uiState as? ContactsUiState.Success)?.contacts?.size ?: 0
            val hasCustomAddressBooks = availableAddressBooks.any {
                it.isLocal && it.href != ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF
            }

            CCAlertDialog(
                onDismissRequest = { showDeleteLocalDataConfirm = false },
                title = R.string.list_delete_local_data_confirm_title,
                content = {
                    Column {
                        if (contactCount > 0 || hasCustomAddressBooks) {
                            Text(stringResource(R.string.list_delete_local_data_confirm_message))
                            Spacer(Modifier.height(8.dp))
                            if (contactCount > 0) {
                                CCTextButton(
                                    text = R.string.settings_export_title,
                                    onClick = { exportLauncher.launch("corvid_contacts_export.vcf") }
                                )
                            }
                        } else {
                            Text(stringResource(R.string.list_delete_local_data_empty_message))
                        }
                    }
                },
                confirmButton = R.string.list_delete_local_data_confirm_action,
                onConfirm = {
                    showDeleteLocalDataConfirm = false
                    actions.onDeleteLocalData()
                },
                dismissButton = R.string.action_cancel,
            )
        }

        if (showFilterSheet) {
            BottomFilterSheet(
                setFilterState = { showFilterSheet = it },
                filterSheetState = filterSheetState,
                selectedAddressBookHrefs = state.selectedAddressBookHrefs,
                availableAddressBooks = availableAddressBooks,
                manageableAddressBooks = manageableAddressBooks,
                hasServerConnection = !state.isLocalOnlyMode,
                onSelectAddressBook = actions.onSelectAddressBook,
                allGroups = state.allGroupsGlobal,
                availableGroups = state.groupsAvailableForSelectedBooks,
                selectedGroup = state.selectedGroup,
                onGroupSelected = actions.onGroupSelected,
                onUpdateAddressBookAppearance = actions.onUpdateAddressBookAppearance,
                onUpdateAddressBookOrder = actions.onUpdateAddressBookOrder,
                onCreateAddressBook = actions.onCreateAddressBook,
                onRenameAddressBook = actions.onRenameAddressBook,
                onDeleteAddressBook = actions.onDeleteAddressBook,
                onUploadLocalAddressBook = actions.onUploadLocalAddressBook,
                onToggleAddressBookVisibility = actions.onToggleAddressBookVisibility,
                onUpdateGroupOrder = actions.onUpdateGroupOrder,
                onRenameGroup = actions.onRenameGroup,
                contactCountByAddressBook = contactCountByAddressBook,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showScaffold) paddingValues else PaddingValues())
        ) {
            val contactListContent: @Composable () -> Unit = {
                when (val uiState = state.uiState) {
                    is ContactsUiState.Loading -> FullScreenLoading()
                    is ContactsUiState.Success -> {
                        if (uiState.contacts.isEmpty()) {
                            val isSearching =
                                state.searchQuery.isNotEmpty() || (state.selectedGroup != null)
                            val allHidden =
                                manageableAddressBooks.isNotEmpty() && state.selectedAddressBookHrefs.isEmpty()
                            val isSingleBook = currentBook != null

                            EmptyState(
                                icon = when {
                                    allHidden -> Icons.Rounded.PeopleOutline
                                    isSearching -> Icons.Rounded.SearchOff
                                    isSingleBook -> ContactColors.getIconForAddressBook(
                                        displayName = currentBook.displayName,
                                        iconName = currentBook.iconName
                                    )

                                    else -> Icons.Rounded.PeopleOutline
                                },
                                title = when {
                                    allHidden -> stringResource(R.string.list_empty_no_books)
                                    isSearching -> stringResource(R.string.list_no_results_title)
                                    isSingleBook -> stringResource(R.string.list_empty_title)
                                    else -> stringResource(R.string.list_empty_title)
                                },
                                description = when {
                                    allHidden -> stringResource(R.string.list_empty_no_books_description)
                                    isSearching -> stringResource(R.string.list_no_results_description)
                                    isSingleBook -> stringResource(
                                        R.string.list_empty_single_book_description,
                                        currentBook.displayName!!
                                    )

                                    state.isLocalOnlyMode -> stringResource(R.string.list_empty_local_description)
                                    else -> stringResource(R.string.list_empty_description)
                                },
                                action = {
                                    when {
                                        allHidden ->
                                            CCButton(
                                                text = R.string.common_all_books,
                                                onClick = { actions.onSelectAddressBook(null) }
                                            )

                                        isSingleBook ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
                                            ) {
                                                if (!currentBook.isLocal)
                                                    CCButton(
                                                        text = R.string.list_menu_sync,
                                                        onClick = { actions.onRefresh() }
                                                    )

                                                CCButton(
                                                    text = R.string.common_all_books,
                                                    onClick = { actions.onSelectAddressBook(null) }
                                                )

                                                CCButton(
                                                    onClick = { actions.onAddContact() }
                                                ) {
                                                    Text(
                                                        stringResource(
                                                            R.string.action_add_item,
                                                            stringResource(R.string.common_contact)
                                                        )
                                                    )
                                                }
                                            }

                                        state.showArchived ->
                                            CCButton(
                                                text = R.string.list_menu_show_active,
                                                onClick = { actions.onToggleArchived() }
                                            )

                                        else -> {
                                        }
                                    }
                                }
                            )
                        } else {
                            ContactList(
                                contacts = uiState.contacts,
                                selectedIds = state.selectedIds,
                                isSelectionMode = isSelectionMode,
                                onToggleSelection = actions.onToggleSelection,
                                onContactClick = { contactWithBook ->
                                    if (state.pickerMode != PickerMode.NONE) {
                                        actions.onContactSelected(contactWithBook)
                                    } else {
                                        actions.onContactClick(contactWithBook)
                                    }
                                },
                                baseColor = baseColor,
                                showSimpleItems = isPane
                            )
                        }
                    }

                    is ContactsUiState.Error -> {
                        val errorMessage = if (uiState.formatArgs != null) {
                            stringResource(
                                uiState.resId,
                                uiState.formatArgs
                            )
                        } else {
                            stringResource(uiState.resId)
                        }
                        FullScreenSyncFailureError(
                            message = errorMessage,
                            onRetry = actions.onRefresh
                        )
                    }
                }
            }

            if (state.isLocalOnlyMode) {
                Box(modifier = Modifier.weight(1f)) { contactListContent() }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = actions.onRefresh,
                    modifier = Modifier.weight(1f)
                ) { contactListContent() }
            }
        }
    }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            baseColor = baseColor,
            topBar = {
                if (isSelectionMode) {
                    MultiSelectTopBar(
                        selectedCount = state.selectedIds.size,
                        onClearSelection = actions.onClearSelection,
                        selectedGroup = state.selectedGroup,
                        onRemoveFromCurrentGroup = {
                            state.selectedGroup?.let { actions.onRemoveSelectedFromGroup(it) }
                        },
                        onShowAddToGroupDialog = {
                            showGroupDialog = true
                        },
                        onArchiveSelected = actions.onArchiveSelected,
                        showArchived = state.showArchived,
                        availableAddressBooks = availableAddressBooks,
                        onMoveSelected = actions.onMoveSelected,
                        onShareSelected = {
                            scope.launch {
                                val vcard = actions.onShareSelected()
                                shareVCard(
                                    context = context,
                                    vcard = vcard,
                                    fileName = "shared_contacts.vcf",
                                    chooserTitle = shareSelectedTitle
                                )
                            }
                        },
                        onDeleteSelected = { showDeleteConfirm = true },
                    )
                } else if (state.pickerMode != PickerMode.NONE) {
                    SingleSelectTopBar(
                        onCancelPickingSelf = actions.onCancelSelectingContact,
                        title = when (state.pickerMode) {
                            PickerMode.MERGE_TARGET -> stringResource(R.string.list_title_select_merge_target)
                            else -> stringResource(R.string.list_title_select_contact)
                        },
                        onClearSelf = if (state.pickerMode == PickerMode.SELF) actions.onClearSelectingContact else null
                    )
                } else {
                    NormalTopBar(
                        showArchived = state.showArchived,
                        selfContact = state.selfContact,
                        onToggleProfileMenu = { profileMenuExpanded = it },
                        profileMenuExpanded = profileMenuExpanded,
                        onToggleArchived = actions.onToggleArchived,
                        onRefresh = actions.onRefresh,
                        onSettingsClick = actions.onSettingsClick,
                        isLocalOnlyMode = state.isLocalOnlyMode,
                        onSetUpSync = actions.onSetUpSync,
                        onLogout = {
                            if (state.isLocalOnlyMode) {
                                showDeleteLocalDataConfirm = true
                            } else {
                                actions.onLogout()
                            }
                        },
                        onStartPickingSelf = actions.onStartSelectingContact,
                        onShareSelf = { state.selfContact?.let { actions.onShareSelf(it) } },
                        onShareSelfViaQr = { state.selfContact?.let { actions.onShareSelfViaQr(it) } },
                        detailActions = detailActions,
                    )
                }
            },
            bottomBar = {
                val currentBookInBar = if (state.selectedAddressBookHrefs.size == 1) {
                    availableAddressBooks.find { it.href == state.selectedAddressBookHrefs.first() }
                } else null

                Column {
                    state.selectedGroup?.let { group ->
                        Box(
                            modifier = Modifier.padding(
                                top = 8.dp,
                                start = 16.dp,
                                bottom = 8.dp,
                                end = 4.dp
                            )
                        ) {
                            ActiveGroupFilterChip(
                                groupName = group,
                                onDismiss = { actions.onGroupSelected(null) }
                            )
                        }
                    }

                    if (isSelectionMode) {
                        val allContactsSelected = remember(state.uiState, state.selectedIds) {
                            val contacts = (state.uiState as? ContactsUiState.Success)?.contacts
                                ?: emptyList()
                            contacts.isNotEmpty() && contacts.all {
                                state.selectedIds.contains(it.contact.id)
                            }
                        }

                        SelectAllRow(
                            allSelected = allContactsSelected,
                            onToggle = {
                                if (allContactsSelected) actions.onDeselectAll() else actions.onSelectAll()
                            },
                            modifier = Modifier
                                .background(baseColor.background())
                                // Sit above the navigation bar or IME, same as SearchBar.
                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                        )
                    } else {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = actions.onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(baseColor.background())
                                // Sit above the navigation bar or IME.
                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                            onFilterClick = { showFilterSheet = true },
                            selectedBook = if (isPane) null else currentBookInBar,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            onFabClick = if (!isSelectionMode && state.pickerMode == PickerMode.NONE)
                actions.onAddContact else null,
            fabContent = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(
                        R.string.action_add_item,
                        stringResource(R.string.common_contact)
                    )
                )
            },
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}
