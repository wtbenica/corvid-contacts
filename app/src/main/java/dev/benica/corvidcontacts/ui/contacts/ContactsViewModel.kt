// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.AddressBookUploadResult
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the main contact list screen, handling filtering, selection, and contact mutations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModel(
    val repository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val selectionManager = ContactSelectionManager()
    private val filterManager = ContactFilterManager()

    private val _isRefreshing = MutableStateFlow(false)

    /** Whether a manual sync (pull-to-refresh) is currently in progress. */
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _events = MutableSharedFlow<ContactsEvent>()

    /** One-shot events (snackbar messages, sync-repair notices, errors) for the list screen to consume. */
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)

    /** The current filtered/sorted contact list (or loading/error state) shown by the list screen. */
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    /** Whether the archived-contacts view is currently shown instead of the active list. */
    val showArchived = filterManager.showArchived

    /** IDs of contacts currently checked in multi-select (bulk action) mode. */
    val selectedContactIds = selectionManager.selectedContactIds

    /** Whether multi-select mode is active - independent of whether anything is currently checked. */
    val isSelectionMode = selectionManager.isSelectionMode

    /** The current search query typed into the list's search bar. */
    val searchQuery = filterManager.searchQuery

    /** The currently selected contact group filter, or `null` if no group filter is active. */
    val selectedGroup = filterManager.selectedGroup

    /** The [PickContent] restriction currently in effect when picking externally (email/phone/address/any), if any. */
    val requiredPickType = filterManager.requiredPickType

    /** Whether the local country code should be auto-prepended to phone numbers. */
    val alwaysAddCountryCode = settingsRepository.alwaysAddCountryCode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /** The contact ID the user has designated as their own "My Card", or `null` if unset. */
    val selfContactId = settingsRepository.selfContactId
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** The full contact record for [selfContactId], resolved from the live contact list or (as a fallback) a direct DB lookup. */
    val selfContact: StateFlow<ContactWithAddressBook?> =
        combine(
            selfContactId,
            repository.allContacts
        ) { id, contacts ->
            if (id == null) null
            else contacts.find { it.contact.id == id } ?: repository.getContactByIdSync(id)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** The user's saved display order for contact groups. */
    val groupOrder = settingsRepository.groupOrder
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Currently visible user-manageable address books. */
    val addressBooks: StateFlow<List<AddressBookEntity>> = repository.userManageableAddressBooks
        .map { books -> books.filter { it.isVisible } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** All user-manageable address books regardless of visibility. */
    val allManageableAddressBooks: StateFlow<List<AddressBookEntity>> =
        repository.userManageableAddressBooks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /** Hrefs of address books currently included in the list. */
    val selectedAddressBookHrefs: StateFlow<Set<String>> =
        combine(
            filterManager.selectedAddressBookHrefsState,
            addressBooks
        ) { selected, available ->
            selected ?: available
                .map { it.href }
                .toSet()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptySet()
        )

    /** All contact-group names in scope for the currently selected address book(s). */
    val allGroups: StateFlow<List<String>> =
        combine(
            repository.allContacts,
            groupOrder,
            selectedAddressBookHrefs
        ) { contacts, order, bookHrefs ->
            val existingGroups = contacts
                .categoriesFrom { bookHrefs.isEmpty() || it.contact.addressBookHref in bookHrefs }
            orderGroups(
                existingGroups,
                order
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** All contact-group names across every address book. */
    val allGroupsGlobal: StateFlow<List<String>> =
        combine(
            repository.allContacts,
            groupOrder
        ) { contacts, order ->
            val existingGroups = contacts.categoriesFrom { true }
            orderGroups(
                existingGroups,
                order
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Groups that have at least one contact in the currently selected address book(s). */
    val groupsAvailableForSelectedBooks: StateFlow<Set<String>> =
        combine(
            repository.allContacts,
            selectedAddressBookHrefs
        ) { contacts, bookHrefs ->
            contacts.categoriesFrom { bookHrefs.isEmpty() || it.contact.addressBookHref in bookHrefs }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptySet()
        )

    /** Distinct, non-"Archived" category names from contacts matching [predicate]. */
    private fun List<ContactWithAddressBook>.categoriesFrom(predicate: (ContactWithAddressBook) -> Boolean): Set<String> =
        asSequence()
            .filter(predicate)
            .flatMap { it.contact.categories ?: emptyList() }
            .distinct()
            .filter {
                it
                    .equals(
                        "Archived",
                        ignoreCase = true
                    )
                    .not()
            }
            .toSet()

    /** Combines [existingGroups] with any saved display [order], appending newly-seen groups alphabetically at the end. */
    private fun orderGroups(
        existingGroups: Set<String>,
        order: List<String>,
    ): List<String> {
        val sortedList = order
            .filter { it in existingGroups }
            .toMutableList()
        val newGroups = existingGroups
            .filter { it !in sortedList }
            .sorted()
        return sortedList + newGroups
    }

    private val _isPickingSelf = MutableStateFlow(false)

    /** Whether the list is currently being used to pick the user's own "My Card" contact. */
    val isPickingSelf: StateFlow<Boolean> = _isPickingSelf.asStateFlow()

    private val _isPickingExternal = MutableStateFlow(false)

    /** Whether the list is currently being used to serve a system-wide `ACTION_PICK`/`ACTION_GET_CONTENT` request. */
    val isPickingExternal: StateFlow<Boolean> = _isPickingExternal.asStateFlow()

    private val _mergeSourceContactId = MutableStateFlow<String?>(null)

    /** The contact ID being merged FROM while the list is being used to pick a merge target, or `null` if not currently picking one. */
    val mergeSourceContactId: StateFlow<String?> = _mergeSourceContactId.asStateFlow()

    /** Whether the list is currently being used to pick a second contact to merge [mergeSourceContactId] with. */
    val isPickingMergeTarget: StateFlow<Boolean> = mergeSourceContactId
        .map { it != null }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    init {
        Log.d(
            "ContactsViewModel",
            "Initializing ContactsViewModel"
        )
        viewModelScope.launch {
            val filterFlow = combine(
                showArchived,
                searchQuery,
                selectedGroup,
                selectedAddressBookHrefs,
                requiredPickType
            ) { archived, query, group, bookHrefs, pickType ->
                ContactFilter(
                    archived,
                    query,
                    group,
                    bookHrefs,
                    pickType
                )
            }
                .combine(filterManager.excludedContactId) { filter, excludedContactId ->
                    filter.copy(excludedContactId = excludedContactId)
                }

            // Keying on showArchived alone avoids restarting the Room query on every filter tweak.
            val contactsFlow = showArchived.flatMapLatest { archived ->
                if (archived) repository.archivedContacts else repository.allContacts
            }

            filterFlow
                .combine(contactsFlow) { filter, contacts ->
                    filter to contacts
                }
                .collect { (filter, contacts) ->
                    val filtered = filterManager.filterContacts(
                        contacts,
                        filter.query,
                        filter.group,
                        filter.bookHrefs,
                        filter.pickType,
                        filter.excludedContactId
                    )

                    _uiState.value = ContactsUiState.Success(
                        filtered
                    )
                }
        }

        viewModelScope.launch {
            authRepository.credentials
                .distinctUntilChanged()
                .filterNotNull()
                .collect { credentials ->
                    // Backfills the saved-servers list if needed.
                    settingsRepository.addSavedServer(
                        credentials.serverUrl
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removeSuffix("/")
                    )
                    refresh()
                }
        }
    }


    /** Updates the search query used to filter [uiState]. */
    fun updateSearchQuery(query: String) {
        filterManager.updateSearchQuery(query)
    }

    /** Sets the active contact-group filter, or clears it if [group] is `null`. */
    fun updateSelectedGroup(group: String?) {
        filterManager.updateSelectedGroup(group)
    }

    /**
     * Sets the currently selected address book, or clears it if [href] is null.
     *
     * @param href The href of the address book to select, or null to select all address books
     */
    fun selectAddressBook(href: String?) {
        filterManager.selectAddressBook(href)
    }

    /** Toggles whether [contactId] is checked in multi-select mode. */
    fun toggleSelection(contactId: String) {
        selectionManager.toggleSelection(contactId)
    }

    /** Exits multi-select mode, clearing all checked contacts. */
    fun clearSelection() {
        selectionManager.clearSelection()
    }

    /** Unchecks every contact without leaving multi-select mode. */
    fun deselectAll() {
        selectionManager.deselectAll()
    }

    /** Selects every contact currently visible in [uiState]. */
    fun selectAll() {
        val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return
        selectionManager.selectAll(contacts.map { it.contact.id })
    }

    /**
     * Archives (or, if [showArchived] is active, unarchives) every currently-selected contact,
     * then clears the selection and emits a confirmation message.
     */
    fun archiveSelectedContacts() {
        val selectedIds = selectedContactIds.value
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            val toProcess = contacts.filter { selectedIds.contains(it.contact.id) }
            val isArchiving = !showArchived.value

            val successCount = toProcess.count { contactWithBook ->
                val result = if (showArchived.value) {
                    repository.unarchiveContact(contactWithBook.contact)
                } else {
                    repository.archiveContact(contactWithBook.contact)
                }
                result.isSuccess
            }

            clearSelection()

            if (successCount > 0) {
                _events.emit(
                    ContactsEvent.Message(
                        if (isArchiving) ContactsEvent.ContactsMessage.ContactsArchived(successCount)
                        else ContactsEvent.ContactsMessage.ContactsUnarchived(successCount)
                    )
                )
            }
            if (successCount < toProcess.size) {
                surfaceError(R.string.list_error_bulk_partial_failure)
            }
        }
    }

    /** Deletes every currently-selected contact from the server and local cache, then clears the selection. */
    fun deleteSelectedContacts() {
        val selectedIds = selectedContactIds.value
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            val toProcess = contacts.filter { selectedIds.contains(it.contact.id) }

            val successCount = toProcess.count { contactWithBook ->
                repository.deleteContact(contactWithBook.contact).isSuccess
            }
            clearSelection()

            if (successCount > 0) {
                _events.emit(
                    ContactsEvent.Message(
                        ContactsEvent.ContactsMessage.ContactsDeleted(
                            successCount
                        )
                    )
                )
            }
            if (successCount < toProcess.size) {
                surfaceError(R.string.list_error_bulk_partial_failure)
            }
        }
    }

    /** Moves every currently-selected contact to the address book at [addressBookHref], then clears the selection. */
    fun moveSelectedContactsToAddressBook(addressBookHref: String) {
        val selectedIds = selectedContactIds.value
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            val toProcess = contacts.filter { selectedIds.contains(it.contact.id) }

            val successCount = toProcess.count { contactWithBook ->
                repository.moveContact(
                    contactWithBook.contact,
                    addressBookHref
                ).isSuccess
            }
            clearSelection()

            if (successCount > 0) {
                _events.emit(
                    ContactsEvent.Message(
                        ContactsEvent.ContactsMessage.ContactsMoved(
                            successCount
                        )
                    )
                )
            }
            if (successCount < toProcess.size) {
                surfaceError(R.string.list_error_bulk_partial_failure)
            }
        }
    }

    /** Adds every currently-selected contact (that isn't already a member) to group [groupName], then clears the selection. */
    fun addSelectedToGroup(groupName: String) {
        val selectedIds = selectedContactIds.value
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            val toProcess = contacts.filter { selectedIds.contains(it.contact.id) }
            val eligible = toProcess.filter { contactWithBook ->
                val currentGroups = contactWithBook.contact.categories ?: emptyList()
                !currentGroups.any {
                    it.equals(
                        groupName,
                        ignoreCase = true
                    )
                }
            }

            val successCount = eligible.count { contactWithBook ->
                val contact = contactWithBook.contact
                val currentGroups = (contact.categories ?: emptyList()) + groupName
                repository.saveContact(contact.copy(categories = currentGroups)).isSuccess
            }
            clearSelection()

            if (successCount > 0) {
                _events.emit(
                    ContactsEvent.Message(
                        ContactsEvent.ContactsMessage.ContactsAddedToGroup(
                            successCount,
                            groupName
                        )
                    )
                )
            }
            if (successCount < eligible.size) {
                surfaceError(R.string.list_error_bulk_partial_failure)
            }
        }
    }

    /** Removes every currently-selected contact from group [groupName], then clears the selection. */
    fun removeSelectedFromGroup(groupName: String) {
        val selectedIds = selectedContactIds.value
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            val toProcess = contacts.filter { selectedIds.contains(it.contact.id) }
            val eligible = toProcess.filter { contactWithBook ->
                (contactWithBook.contact.categories ?: emptyList()).any {
                    it.equals(
                        groupName,
                        ignoreCase = true
                    )
                }
            }

            val successCount = eligible.count { contactWithBook ->
                val contact = contactWithBook.contact
                val currentGroups = (contact.categories ?: emptyList()).toMutableList()
                currentGroups.removeIf {
                    it.equals(
                        groupName,
                        ignoreCase = true
                    )
                }
                repository.saveContact(contact.copy(categories = currentGroups)).isSuccess
            }
            clearSelection()

            if (successCount > 0) {
                _events.emit(
                    ContactsEvent.Message(
                        ContactsEvent.ContactsMessage.ContactsRemovedFromGroup(
                            successCount,
                            groupName
                        )
                    )
                )
            }
            if (successCount < eligible.size) {
                surfaceError(R.string.list_error_bulk_partial_failure)
            }
        }
    }

    /** Switches between the active-contacts list and the archived-contacts list. */
    fun toggleShowArchived() {
        filterManager.toggleShowArchived()
    }

    /** Returns a [kotlinx.coroutines.flow.Flow] indicating whether [contact] is marked as a favorite. */
    fun isFavorite(contact: ContactEntity) = repository.observeContactById(contact.id)
        .map { contactWithBook ->
            contactWithBook?.contact?.categories?.any {
                it.equals(ContactsRepository.FAVORITE_CATEGORY, ignoreCase = true)
            } ?: false
        }
        .distinctUntilChanged()

    /** Triggers a manual CardDAV sync. */
    fun refresh() {
        Log.d(
            "ContactsViewModel",
            "Refresh requested"
        )
        viewModelScope.launch {
            // Nothing to sync with in local-only mode - pull-to-refresh just resolves quietly
            // rather than surfacing a "Sync Failed" error for a server that was never configured.
            if (authRepository.credentials.first() == null) {
                return@launch
            }

            _isRefreshing.value = true
            val result = repository.syncContacts()
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Sync Failed"
                Log.e(
                    "ContactsViewModel",
                    "Sync Failed: $errorMsg"
                )

                surfaceError(R.string.common_error_sync_title)
            } else {
                Log.d(
                    "ContactsViewModel",
                    "Sync Succeeded"
                )
                val stats = result.getOrNull()
                if (stats != null && stats.total > 0) {
                    _events.emit(ContactsEvent.RepairPerformed(stats))
                }
            }
            _isRefreshing.value = false
        }
    }

    /**
     * Saves [contact] (create or update), moving it if the address book changed.
     * @return `true` if successful.
     */
    suspend fun saveContact(contact: ContactEntity): Boolean {
        val originalWithBook = repository.getContactByIdSync(contact.id)
        val original = originalWithBook?.contact
        var contactToSave = contact

        if (original != null && !original.contactHref.isNullOrBlank() &&
            original.addressBookHref != contact.addressBookHref &&
            !contact.addressBookHref.isNullOrBlank()
        ) {

            val moveResult = repository.moveContact(
                original,
                contact.addressBookHref
            )
            if (moveResult.isSuccess) {
                val updatedOriginalWithBook = repository.getContactByIdSync(contact.id)
                val updatedOriginal = updatedOriginalWithBook?.contact
                if (updatedOriginal != null) {
                    contactToSave = contact.copy(contactHref = updatedOriginal.contactHref)
                }
            } else {
                surfaceError(R.string.list_error_move_failed)
                return false
            }
        }

        val result = repository.saveContact(contactToSave)
        return if (result.isSuccess) {
            _events.emit(ContactsEvent.Message(ContactsEvent.ContactsMessage.ContactSaved))
            true
        } else {
            surfaceError(R.string.edit_error_save_failed)
            false
        }
    }

    /**
     * Deletes [contact] from server and cache.
     * @return `true` if successful.
     */
    suspend fun deleteContact(contact: ContactEntity): Boolean {
        val result = repository.deleteContact(contact)
        return if (result.isSuccess) {
            _events.emit(ContactsEvent.Message(ContactsEvent.ContactsMessage.ContactsDeleted(1)))
            true
        } else {
            surfaceError(R.string.common_error_unknown)
            false
        }
    }

    /**
     * Surfaces an error as a snackbar if contacts are present, otherwise shows a full-screen error.
     */
    private suspend fun surfaceError(
        resId: Int,
        formatArgs: Any? = null,
    ) {
        val currentUiState = _uiState.value
        if (currentUiState is ContactsUiState.Success && currentUiState.contacts.isNotEmpty()) {
            _events.emit(
                ContactsEvent.Error(
                    resId,
                    formatArgs
                )
            )
        } else {
            _uiState.value = ContactsUiState.Error(
                resId,
                formatArgs
            )
        }
    }

    /**
     * Toggles the archived state of a single [contact].
     * @return `true` if successful.
     */
    suspend fun archiveContact(contact: ContactEntity): Boolean {
        val result = if (contact.isArchived) {
            // Unarchive
            repository.unarchiveContact(contact)
        } else {
            // Archive
            repository.archiveContact(contact)
        }

        return if (result.isSuccess) {
            _events.emit(
                ContactsEvent.Message(
                    if (contact.isArchived) ContactsEvent.ContactsMessage.ContactsUnarchived(1)
                    else ContactsEvent.ContactsMessage.ContactsArchived(1)
                )
            )
            true
        } else {
            surfaceError(R.string.common_error_unknown)
            false
        }
    }

    /** Logs the user out, clearing credentials and that account's local data. */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    /** Whether the app is in local-only mode (no CardDAV server configured). */
    val isLocalOnlyMode: StateFlow<Boolean> = combine(
        settingsRepository.localOnlyMode,
        authRepository.credentials
    ) { localOnlyMode, credentials ->
        localOnlyMode && credentials == null
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /** Deletes all local-only data and disables local-only mode. */
    fun deleteAllLocalData() {
        viewModelScope.launch {
            repository.deleteAllLocalData()
        }
    }

    /** Exports every locally cached contact as a single merged multi-vCard text document. */
    suspend fun exportAllContacts(): String = repository.exportAllContacts()

    /** Builds a merged multi-vCard document for every currently-selected contact, without clearing the selection. */
    fun getSelectedContactsVCardString(): String {
        val selectedIds = selectedContactIds.value
        val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return ""
        val toShare = contacts
            .filter { selectedIds.contains(it.contact.id) }
            .map { it.contact }
        return repository.getVCardStringForContacts(toShare)
    }

    /** Updates the display color and icon for an address book. */
    fun updateAddressBookAppearance(
        addressBook: AddressBookEntity,
        color: Color,
        iconName: String?,
    ) {
        viewModelScope.launch {
            repository.updateAddressBookVisibility(
                addressBook.copy(
                    colorInt = color.toArgb(),
                    iconName = iconName
                )
            )
        }
    }

    /** Renames a group across all contacts and updates group order. */
    fun renameGroup(
        oldName: String,
        newName: String,
    ) {
        viewModelScope.launch {
            val contacts = (uiState.value as? ContactsUiState.Success)?.contacts ?: return@launch
            contacts.forEach { contactWithBook ->
                val contact = contactWithBook.contact
                val categories = (contact.categories ?: emptyList()).toMutableList()
                val index = categories.indexOfFirst {
                    it.equals(
                        oldName,
                        ignoreCase = true
                    )
                }
                if (index != -1) {
                    categories[index] = newName
                    repository.saveContact(contact.copy(categories = categories))
                }
            }

            val currentOrder = groupOrder.value
            val orderIndex = currentOrder.indexOfFirst {
                it.equals(
                    oldName,
                    ignoreCase = true
                )
            }
            if (orderIndex != -1) {
                val newOrder = currentOrder.toMutableList()
                newOrder[orderIndex] = newName
                settingsRepository.saveGroupOrder(newOrder)
            }
        }
    }

    /** Sets [id] as the user's "My Card" contact and exits self-picking mode. */
    fun setSelfContactId(id: String?) {
        viewModelScope.launch {
            settingsRepository.saveSelfContactId(id)
            _isPickingSelf.value = false
        }
    }

    /** Toggles a contact's favorite status. @return `true` if successful. */
    suspend fun toggleFavorite(contact: ContactEntity): Boolean {
        val currentGroups = (contact.categories ?: emptyList()).toMutableList()
        val isFavorite = currentGroups.any {
            it.equals(
                ContactsRepository.FAVORITE_CATEGORY,
                ignoreCase = true
            )
        }

        currentGroups.removeIf {
            it.equals(
                ContactsRepository.FAVORITE_CATEGORY,
                ignoreCase = true
            )
        }

        if (!isFavorite) {
            currentGroups.add(ContactsRepository.FAVORITE_CATEGORY)
        }
        val updatedContact = contact.copy(categories = currentGroups)
        return repository.saveContact(updatedContact).isSuccess
    }

    /** Persists a new display [newOrder] for contact groups. */
    fun updateGroupOrder(newOrder: List<String>) {
        viewModelScope.launch {
            settingsRepository.saveGroupOrder(newOrder)
        }
    }

    /** Persists a new display order for address books, taken from the position of each entry in [addressBooks]. */
    fun updateAddressBookOrder(addressBooks: List<AddressBookEntity>) {
        viewModelScope.launch {
            addressBooks.forEachIndexed { index, book ->
                repository.updateAddressBookVisibility(book.copy(sortOrder = index))
            }
        }
    }

    /** Toggles whether [addressBook] is shown in the main contact list. */
    fun toggleAddressBookVisibility(addressBook: AddressBookEntity) {
        viewModelScope.launch {
            repository.updateAddressBookVisibility(addressBook.copy(isVisible = !addressBook.isVisible))
        }
    }

    /** Creates a new address book with [displayName] and [color], see [ContactsRepository.createAddressBook]. */
    suspend fun createAddressBook(
        displayName: String,
        color: Color,
        forceLocal: Boolean = false,
    ): Result<AddressBookEntity> = repository.createAddressBook(
        displayName,
        color.toArgb(),
        forceLocal
    )

    /** Renames [addressBook] on the server, see [ContactsRepository.renameAddressBook]. */
    suspend fun renameAddressBook(
        addressBook: AddressBookEntity,
        newDisplayName: String,
    ): Result<Unit> = repository.renameAddressBook(
        addressBook,
        newDisplayName
    )

    /** Deletes [addressBook] - and every contact in it - from the server, see [ContactsRepository.deleteAddressBook]. */
    suspend fun deleteAddressBook(addressBook: AddressBookEntity): Result<Unit> =
        repository.deleteAddressBook(addressBook)

    /** Manually downloads [contact]'s externally-hosted photo, see [ContactsRepository.downloadContactPhoto]. */
    suspend fun downloadContactPhoto(contact: ContactEntity): Boolean =
        repository.downloadContactPhoto(contact)

    /** Uploads a local-only [addressBook] to the server as [newDisplayName], see [ContactsRepository.uploadLocalAddressBook]. */
    suspend fun uploadLocalAddressBook(
        addressBook: AddressBookEntity,
        newDisplayName: String,
    ): Result<AddressBookUploadResult> = repository.uploadLocalAddressBook(
        addressBook,
        newDisplayName
    )

    /** Enters self-picking mode: the list is being used to choose the user's own "My Card" contact. */
    fun startPickingSelf() {
        _isPickingSelf.value = true
    }

    /** Exits self-picking mode without making a selection. */
    fun stopPickingSelf() {
        _isPickingSelf.value = false
    }

    /**
     * Enters external-picking mode: the list is being used to serve a system-wide
     * `ACTION_PICK`/`ACTION_GET_CONTENT` request, optionally restricted to contacts matching
     * [type] (e.g. only contacts with an email address).
     */
    fun startPickingExternal(type: PickContent? = null) {
        _isPickingExternal.value = true
        filterManager.setRequiredPickType(type)
    }

    /** Exits external-picking mode and clears any pick-type restriction. */
    fun stopPickingExternal() {
        _isPickingExternal.value = false
        filterManager.setRequiredPickType(null)
    }

    /** Enters merge-target-picking mode for [sourceContactId]. */
    fun startPickingMergeTarget(sourceContactId: String) {
        _mergeSourceContactId.value = sourceContactId
        filterManager.setExcludedContactId(sourceContactId)
    }

    /** Exits merge-target-picking mode without making a selection. */
    fun stopPickingMergeTarget() {
        _mergeSourceContactId.value = null
        filterManager.setExcludedContactId(null)
    }

    /**
     * Persists a completed merge, updating relationships and deleting the absorbed contact.
     * @return `true` if successful.
     */
    suspend fun mergeContacts(
        mergedSurvivor: ContactEntity,
        absorbedContact: ContactEntity,
        useAbsorbedPhoto: Boolean = false,
    ): Boolean {
        val survivorToSave = if (useAbsorbedPhoto) {
            val copiedUrl = repository.copyContactPhoto(
                absorbedContact.id,
                mergedSurvivor.id
            )
            mergedSurvivor.copy(
                photoUrl = copiedUrl,
                hasPhoto = copiedUrl != null
            )
        } else mergedSurvivor

        val saveResult = repository.saveContact(survivorToSave)
        if (saveResult.isFailure) {
            surfaceError(R.string.common_error_unknown)
            return false
        }

        val others = (uiState.value as? ContactsUiState.Success)
            ?.contacts
            .orEmpty()
            .map { it.contact }
            .filter { it.id != mergedSurvivor.id && it.id != absorbedContact.id }

        others.forEach { other ->
            val relationships = other.relationships ?: return@forEach
            if (relationships.none { it.isUid && it.value == absorbedContact.id }) return@forEach
            val repointed = relationships.map {
                if (it.isUid && it.value == absorbedContact.id) it.copy(value = mergedSurvivor.id) else it
            }
            // Best-effort: an individual repoint failure doesn't block the merge itself and isn't
            // separately surfaced - it's incidental cleanup, not the user's primary requested action.
            repository.saveContact(other.copy(relationships = repointed))
        }

        val deleteResult = repository.deleteContact(absorbedContact)
        return if (deleteResult.isSuccess) {
            _events.emit(ContactsEvent.Message(ContactsEvent.ContactsMessage.ContactsMerged))
            true
        } else {
            surfaceError(R.string.common_error_unknown)
            false
        }
    }
}
