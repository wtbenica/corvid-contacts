// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.alt_layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopAppBar
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_detail.ContactDetailScreen
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.AddToGroupDialog
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.BottomFilterSheetContent
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.ContactList
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.NormalTopBar
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.SearchBar
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.SelectAllRow
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.SingleSelectTopBar
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.LocalThemeColor
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.utils.shareVCard
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * The wide-screen "shell": a persistent list pane plus a detail/edit pane, sharing a single top
 * bar across list browsing, contact detail, contact editing, and secondary destinations (Settings,
 * ShareSelection, etc.) instead of each one drawing its own.
 *
 * [editPaneContent], when non-null, renders inline in the detail pane so contact editing sits
 * beside the list. [singlePaneContent], when non-null, takes over the whole body instead of the
 * list+detail split, and its [singlePaneChrome] replaces the top bar entirely - used for
 * destinations that don't make sense split across two panes. Otherwise the top bar reflects
 * multi-select mode, contact-picking mode, or the normal list/profile-menu bar with the current
 * contact detail's actions folded in.
 *
 * The detail/edit pane's body is tinted to the shown/edited contact's color, same as the phone
 * screens. For [editPaneContent] that color has to be passed in as [editPaneTintColor] - the
 * caller knows which contact/address book the edit targets, this composable doesn't.
 *
 * @param snackbarHostState shared with embedded panes so their save/merge-failure snackbars surface here
 */
@Composable
fun AdaptiveMasterScreen(
    viewModel: ContactsViewModel,
    selectedContactId: String?,
    onContactClick: (ContactWithAddressBook) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onMergeContact: (ContactEntity) -> Unit,
    onNavigateToContact: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSetUpSync: () -> Unit,
    onShare: (String, Boolean) -> Unit,
    onContactRemovedFromView: () -> Unit = {},
    singlePaneChrome: ScreenChrome? = null,
    singlePaneContent: (@Composable () -> Unit)? = null,
    editPaneContent: (@Composable () -> Unit)? = null,
    editPaneTintColor: Color? = null,
    activeContactId: String? = null,
    showNewContactPlaceholder: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scope = rememberCoroutineScope()
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val allGroupsGlobal by viewModel.allGroupsGlobal.collectAsState()
    val shareSelectedTitle = stringResource(R.string.common_share)

    val contactWithBook by if (selectedContactId != null) {
        remember(selectedContactId) {
            viewModel.repository.observeContactById(selectedContactId)
        }.collectAsState(initial = null)
    } else {
        remember { flowOf(null) }.collectAsState(initial = null)
    }

    val isFavorite by remember(contactWithBook?.contact?.id) {
        contactWithBook?.contact?.let { viewModel.isFavorite(it) } ?: flowOf(false)
    }.collectAsState(initial = false)

    // Only the actions row comes from ContactDetailScreen's chrome.
    var detailChrome by remember(contactWithBook?.contact?.id) { mutableStateOf<ScreenChrome?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()

    val isPickingSelf by viewModel.isPickingSelf.collectAsState()
    val isPickingExternal by viewModel.isPickingExternal.collectAsState()
    val mergeSourceContactId by viewModel.mergeSourceContactId.collectAsState()
    val isPickingAny = isPickingSelf || isPickingExternal || mergeSourceContactId != null
    val context = LocalContext.current

    val selectedIds by viewModel.selectedContactIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val availableAddressBooks by viewModel.addressBooks.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedAddressBookHrefs by viewModel.selectedAddressBookHrefs.collectAsState()

    // Matches phone: chrome tints to the one filtered address book's color.
    val shellBaseColor = if (selectedAddressBookHrefs.size == 1 && availableAddressBooks.size > 1) {
        ContactColors.resolveAddressBookColor(availableAddressBooks, selectedAddressBookHrefs.first())
    } else {
        MaterialTheme.colorScheme.primary
    }

    CCScaffold(
        modifier = Modifier.fillMaxSize(),
        baseColor = shellBaseColor,
        topBar = {
            if (singlePaneContent != null || editPaneContent != null) {
                // Not gated on singlePaneChrome, so it doesn't flash NormalTopBar first.
                CCTopAppBar(
                    title = singlePaneChrome?.title ?: "",
                    navigationIcon = singlePaneChrome?.navigationIcon ?: {},
                    actions = singlePaneChrome?.actions ?: {},
                )
            } else if (isSelectionMode) {
                // Bulk actions live in MultiSelectPane, not this bar - unlike phone.
                SingleSelectTopBar(
                    title = pluralStringResource(
                        R.plurals.list_title_contacts_selected, selectedIds.size
                    ),
                    onCancelPickingSelf = { viewModel.clearSelection() },
                )
            } else if (isPickingAny) {
                SingleSelectTopBar(
                    title = if (mergeSourceContactId != null) {
                        stringResource(R.string.list_title_select_merge_target)
                    } else {
                        stringResource(R.string.list_title_select_contact)
                    }, onCancelPickingSelf = {
                        when {
                            isPickingExternal -> {
                                viewModel.stopPickingExternal()
                                (context as? ComponentActivity)?.finish()
                            }

                            mergeSourceContactId != null -> viewModel.stopPickingMergeTarget()
                            else -> viewModel.stopPickingSelf()
                        }
                    }, onClearSelf = if (isPickingSelf) {
                        { viewModel.setSelfContactId(null) }
                    } else null)
            } else {
                NormalTopBar(
                    showArchived = viewModel.showArchived.collectAsState().value,
                    selfContact = viewModel.selfContact.collectAsState().value,
                    onToggleProfileMenu = { profileMenuExpanded = it },
                    profileMenuExpanded = profileMenuExpanded,
                    onToggleArchived = { viewModel.toggleShowArchived() },
                    onRefresh = { viewModel.refresh() },
                    onSettingsClick = onSettingsClick,
                    isLocalOnlyMode = viewModel.isLocalOnlyMode.collectAsState().value,
                    onSetUpSync = onSetUpSync,
                    onLogout = { viewModel.logout() },
                    onStartPickingSelf = { viewModel.startPickingSelf() },
                    onShareSelf = { self -> onShare(self.contact.id, false) },
                    onShareSelfViaQr = { self -> onShare(self.contact.id, true) },
                    detailActions = detailChrome?.actions,
                )
            }
        }, snackbarHost = { SnackbarHost(snackbarHostState) }, onFabClick = when {
            singlePaneContent != null -> singlePaneChrome?.onFabClick
            isPickingAny -> null
            else -> onAddContact
        }, fabContent = if (singlePaneContent != null) {
            singlePaneChrome?.fabContent ?: {}
        } else {
            { Icon(Icons.Rounded.Add, null) }
        }) { padding ->
        if (showGroupDialog) {
            AddToGroupDialog(allGroups = allGroupsGlobal, onConfirm = { groupName ->
                viewModel.addSelectedToGroup(groupName)
                showGroupDialog = false
            }, onDismissRequest = {
                showGroupDialog = false
            })
        }

        if (showDeleteConfirm) {
            CCAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        pluralStringResource(
                            R.plurals.list_delete_confirm_title, selectedIds.size, selectedIds.size
                        )
                    )
                },
                content = {
                    Text(
                        pluralStringResource(
                            R.plurals.list_delete_confirm_message,
                            selectedIds.size,
                            selectedIds.size
                        ),
                    )
                },
                confirmButton = R.string.detail_menu_delete,
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.deleteSelectedContacts()
                },
                dismissButton = R.string.action_cancel,
            )
        }

        if (singlePaneContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                singlePaneContent()
            }
            return@CCScaffold
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // List Pane
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                val uiState by viewModel.uiState.collectAsState()
                val selectedIds by viewModel.selectedContactIds.collectAsState()

                Surface(
                    color = currentThemeColor().background(),
                    shape = RoundedCornerShape(bottomEnd = Dimens.medSpacing),
                ) {
                    if (isSelectionMode) {
                        val allContactsSelected = remember(uiState, selectedIds) {
                            val contacts =
                                (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
                            contacts.isNotEmpty() && contacts.all {
                                selectedIds.contains(it.contact.id)
                            }
                        }

                        SelectAllRow(
                            allSelected = allContactsSelected,
                            onToggle = {
                                if (allContactsSelected) viewModel.deselectAll() else viewModel.selectAll()
                            },
                        )
                    } else {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                        )
                    }
                }

                AdaptiveFilterButton(viewModel)

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        is ContactsUiState.Success -> {
                            ContactList(
                                contacts = state.contacts,
                                selectedIds = selectedIds,
                                isSelectionMode = isSelectionMode,
                                onToggleSelection = { viewModel.toggleSelection(it) },
                                onContactClick = onContactClick,
                                showSimpleItems = true,
                                activeContactId = activeContactId,
                                showNewContactPlaceholder = showNewContactPlaceholder,
                            )
                        }

                        else -> { /* Loading/Error handled by parent or simplified here */
                        }
                    }
                }
            }

            // Detail Pane
            val paneTintColor = if (editPaneContent != null) {
                editPaneTintColor
            } else {
                ContactColors.resolveContactColor(contactWithAddressBook = contactWithBook)
            }

            CompositionLocalProvider(
                LocalThemeColor provides (paneTintColor ?: currentThemeColor())
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Only peeks through the inner Surface's rounded corner.
                        .background(shellBaseColor.background())
                ) {
                    Surface(shape = RoundedCornerShape(topStart = Dimens.lgSpacing)) {
                        if (isSelectionMode) {
                            val uiState by viewModel.uiState.collectAsState()
                            val allContacts =
                                (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
                            val selectedContacts = remember(allContacts, selectedIds) {
                                allContacts
                                    .map { it.contact }
                                    .filter { selectedIds.contains(it.id) }
                            }

                            MultiSelectPane(
                                selectedContacts = selectedContacts,
                                onToggleSelection = { viewModel.toggleSelection(it) },
                                selectedGroup = selectedGroup,
                                onRemoveFromCurrentGroup = {
                                    selectedGroup?.let { viewModel.removeSelectedFromGroup(it) }
                                },
                                onShowAddToGroupDialog = { showGroupDialog = true },
                                onArchiveSelected = { viewModel.archiveSelectedContacts() },
                                showArchived = showArchived,
                                availableAddressBooks = availableAddressBooks,
                                onMoveSelected = { viewModel.moveSelectedContactsToAddressBook(it) },
                                onShareSelected = {
                                    scope.launch {
                                        val vcard = viewModel.getSelectedContactsVCardString()
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
                        } else if (editPaneContent != null) {
                            editPaneContent()
                        } else {
                            val uiState by viewModel.uiState.collectAsState()
                            val allContacts =
                                (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()

                            ContactDetailScreen(
                                contactWithBook = contactWithBook,
                                allContacts = allContacts,
                                isFavorite = isFavorite,
                                onToggleFavorite = {
                                    contactWithBook?.contact?.let { viewModel.toggleFavorite(it) }
                                        ?: false
                                },
                                onBack = {},
                                onEdit = onEditContact,
                                onDelete = { c ->
                                    viewModel.deleteContact(c).also { success ->
                                        if (success) onContactRemovedFromView()
                                    }
                                },
                                onArchive = { c ->
                                    viewModel.archiveContact(c).also { success ->
                                        if (success) onContactRemovedFromView()
                                    }
                                },
                                onDownloadPhoto = { viewModel.downloadContactPhoto(it) },
                                onMerge = onMergeContact,
                                onNavigateToContact = onNavigateToContact,
                                onShare = { isQr ->
                                    contactWithBook?.contact?.id?.let { id -> onShare(id, isQr) }
                                },
                                showScaffold = false,
                                onChromeChange = { detailChrome = it },
                                snackbarHostState = snackbarHostState,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Trigger button showing the active book/group filter (or [R.string.common_all_books] when
 * nothing's set), opening a popover beside the list pane with the same filtering and
 * address-book/group management [BottomFilterSheetContent] gives the phone's bottom sheet -
 * unchanged, just hosted in a [Popup] instead of a [androidx.compose.material3.ModalBottomSheet].
 */
@Composable
private fun AdaptiveFilterButton(viewModel: ContactsViewModel) {
    val availableAddressBooks by viewModel.addressBooks.collectAsState()
    val manageableAddressBooks by viewModel.allManageableAddressBooks.collectAsState()
    val selectedHrefs by viewModel.selectedAddressBookHrefs.collectAsState()
    val allGroupsGlobal by viewModel.allGroupsGlobal.collectAsState()
    val availableGroups by viewModel.groupsAvailableForSelectedBooks.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isLocalOnlyMode by viewModel.isLocalOnlyMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }

    val currentBook = if (selectedHrefs.size == 1) {
        availableAddressBooks.find { it.href == selectedHrefs.first() }
    } else null

    val filterSummary = listOfNotNull(currentBook?.displayName, selectedGroup)
        .joinToString(" · ")
        .ifBlank { stringResource(R.string.common_all_books) }

    val contactCountByAddressBook = remember(uiState) {
        (uiState as? ContactsUiState.Success)?.contacts
            ?.groupBy { it.contact.addressBookHref }
            ?.mapValues { it.value.size }
            ?: emptyMap()
    }

    Box(modifier = Modifier.padding(8.dp)) {
        CCButton(
            onClick = { showFilterMenu = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = stringResource(R.string.list_filter_button_description),
            )
            Spacer(Modifier.width(Dimens.medSpacing))
            Text(
                text = filterSummary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
        }

        // Popup, not DropdownMenu: DropdownMenu's measurement pass crashes on LazyRows here.
        if (showFilterMenu) {
            val offsetPx = with(LocalDensity.current) {
                IntOffset(x = 360.dp.roundToPx(), y = 0)
            }
            Popup(
                alignment = Alignment.TopStart,
                offset = offsetPx,
                onDismissRequest = { showFilterMenu = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .heightIn(max = 600.dp),
                    color = currentThemeColor().surface(),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = Dimens.xsSpacing,
                    border = BorderStroke(
                        width = 2.dp,
                        color = currentThemeColor().surfaceVariant(),
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.outerSpacing)
                    ) {
                        BottomFilterSheetContent(
                            selectedAddressBookHrefs = selectedHrefs,
                            availableAddressBooks = availableAddressBooks,
                            manageableAddressBooks = manageableAddressBooks,
                            hasServerConnection = !isLocalOnlyMode,
                            onSelectAddressBook = { viewModel.selectAddressBook(it) },
                            allGroups = allGroupsGlobal,
                            availableGroups = availableGroups,
                            selectedGroup = selectedGroup,
                            onGroupSelected = { viewModel.updateSelectedGroup(it) },
                            onUpdateAddressBookAppearance = { book, color, iconName ->
                                viewModel.updateAddressBookAppearance(book, color, iconName)
                            },
                            onUpdateAddressBookOrder = { viewModel.updateAddressBookOrder(it) },
                            onCreateAddressBook = { name, color, forceLocal ->
                                viewModel.createAddressBook(name, color, forceLocal)
                            },
                            onRenameAddressBook = { book, newName ->
                                viewModel.renameAddressBook(book, newName)
                            },
                            onDeleteAddressBook = { viewModel.deleteAddressBook(it) },
                            onUploadLocalAddressBook = { book, newName ->
                                viewModel.uploadLocalAddressBook(book, newName)
                            },
                            onToggleAddressBookVisibility = {
                                viewModel.toggleAddressBookVisibility(it)
                            },
                            onUpdateGroupOrder = { viewModel.updateGroupOrder(it) },
                            onRenameGroup = { oldName, newName ->
                                viewModel.renameGroup(oldName, newName)
                            },
                            contactCountByAddressBook = contactCountByAddressBook,
                        )
                    }
                }
            }
        }
    }
}
