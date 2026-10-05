// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the first-run onboarding flow shown after a user's first successful login: performs an
 * initial contact sync, then walks the user through phone-formatting preference, address-lookup
 * preference, birthday notifications opt-in, sharing address books with the system contacts, and
 * "self" contact selection, before marking setup complete.
 *
 * Note: completion is tracked per-account ([SettingsRepository.lastOnboardedAccountKey]), so this
 * flow plays again after logging into a *different* account, but not when logging back into the
 * same one. It can also be re-triggered for the current account without logging out via
 * [dev.benica.corvidcontacts.ui.settings.SettingsViewModel.resetOnboarding] (exposed in Settings as
 * "Redo Initial Setup", primarily for testing).
 */
class OnboardingViewModel(
    private val contactsRepository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Which step of the onboarding flow is currently shown. */
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Setup)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** The background sync task, started immediately on initialization. */
    private var syncJob: Deferred<Unit>? = null

    private val _isBackgroundSyncing = MutableStateFlow(false)

    /** Whether the background sync started in [Setup] is still running. */
    val isBackgroundSyncing: StateFlow<Boolean> = _isBackgroundSyncing.asStateFlow()

    /** Whether a server account is currently connected. */
    val hasServerConnection: StateFlow<Boolean> = authRepository.credentials
        .map { it != null }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    /**
     * Whether [resolveLocalDataMigration] is currently uploading books - lets the migration step
     * disable its Continue button and show progress, since a repeated tap before it finishes
     * would otherwise fire the upload again (each call creates its own new server-side book).
     */
    private val _isMigratingLocalData = MutableStateFlow(false)
    val isMigratingLocalData: StateFlow<Boolean> = _isMigratingLocalData.asStateFlow()

    /** All synced contacts, sorted alphabetically, offered as candidates for "self" contact selection. */
    val contacts: StateFlow<List<ContactWithAddressBook>> = contactsRepository.allContacts
        .map { list ->
            list.sortedBy {
                it.contact
                    .getEffectiveDisplayName()
                    .lowercase()
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Address books offered when creating a brand-new "self" contact (see [OnboardingUiState.CreatingSelfContact]). */
    val addressBooks: StateFlow<List<AddressBookEntity>> =
        contactsRepository.userManageableAddressBooks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /** Existing group names, offered when creating a brand-new "self" contact. */
    val allGroups: StateFlow<List<String>> = contacts
        .map { list ->
            list
                .flatMap { it.contact.categories ?: emptyList() }
                .distinct()
                .sorted()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Whether the local country code should be auto-prepended to phone numbers. */
    val alwaysAddCountryCode: StateFlow<Boolean> = settingsRepository.alwaysAddCountryCode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    /** The current address lookup mode. */
    val addressLookupMode: StateFlow<AddressLookupMode> = settingsRepository.addressLookupMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AddressLookupMode.PHOTON
        )

    /**
     * Whether birthday reminders are already enabled from a prior run of this flow - relevant when
     * this screen is reached via "Redo Initial Setup" rather than a true first run, so the setup
     * switch starts where the user left it.
     */
    val currentBirthdayNotificationsEnabled: StateFlow<Boolean> =
        settingsRepository.birthdayNotificationsEnabled
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            )

    /**
     * Whether this run was reached for an account that has already completed onboarding before -
     * i.e. entered purely because [ContactsRepository.hasLocalAddressBooks] flagged local data
     * needing a decision (see [dev.benica.corvidcontacts.MainViewModel]). When true, every other
     * onboarding question is skipped - the user already answered them - and finishing the
     * local-data-migration step (or finding no local books at all) goes straight back to the
     * contact list instead of walking through phone/address/birthday/self-contact again.
     */
    private var isResumingAlreadyOnboardedAccount = false

    init {
        viewModelScope.launch {
            val credentials = authRepository.credentials.first()
            if (credentials != null) {
                isResumingAlreadyOnboardedAccount =
                    credentials.accountKey == settingsRepository.lastOnboardedAccountKey.first()

                if (isResumingAlreadyOnboardedAccount) {
                    // If resuming purely for migration, we don't start a background sync here;
                    // we'll just show the migration step immediately.
                    advanceToMigrationOr { completeOnboarding() }
                } else {
                    // Start background sync immediately for new accounts.
                    _isBackgroundSyncing.value = true
                    syncJob = async {
                        contactsRepository.syncContacts()
                        _isBackgroundSyncing.value = false
                    }
                }
            } else {
                // Local-only mode: no background sync needed.
            }
        }
    }

    /**
     * Saves the combined setup preferences and advances to the next step (Migration or wait-for-sync).
     */
    fun saveSetupPreferences(
        alwaysAddCountryCode: Boolean,
        addressLookupMode: AddressLookupMode,
        birthdayReminders: Boolean,
    ) {
        viewModelScope.launch {
            settingsRepository.saveAlwaysAddCountryCode(alwaysAddCountryCode)
            settingsRepository.saveAddressLookupMode(addressLookupMode)
            settingsRepository.saveBirthdayNotificationsEnabled(birthdayReminders)

            advanceToMigrationOr { advanceToSyncJunction() }
        }
    }

    /** Shows the migration step if there are local books to decide about, and otherwise runs [next]. */
    private suspend fun advanceToMigrationOr(next: suspend () -> Unit) {
        val credentials = authRepository.credentials.first()
        cleanupEmptyDefaultLocalBook()
        val localBooks = contactsRepository.getLocalAddressBooks()
        if (credentials != null && localBooks.isNotEmpty()) {
            _uiState.value = OnboardingUiState.LocalDataMigration(localBooks)
        } else {
            next()
        }
    }

    private suspend fun cleanupEmptyDefaultLocalBook() {
        authRepository.credentials.first() ?: return // Don't delete the default book if we're in local-only mode

        val localBooks = contactsRepository.getLocalAddressBooks()
        val defaultBook =
            localBooks.find { it.href == ContactsRepository.DEFAULT_LOCAL_ADDRESS_BOOK_HREF }
        if (defaultBook != null) {
            val count = contactsRepository.getContactCountInAddressBook(defaultBook.href)
            if (count == 0) {
                contactsRepository.deleteAddressBook(defaultBook)
            }
        }
    }

    /**
     * Waits for the background sync to finish before advancing to data-dependent steps.
     */
    private suspend fun advanceToSyncJunction() {
        if (syncJob != null && syncJob?.isCompleted == false) {
            _uiState.value = OnboardingUiState.FinalizingSync
            syncJob?.await()
        }
        advancePastSync()
    }

    /** Offers sharing once at least one address book exists, and otherwise moves on. */
    private suspend fun advancePastSync() {
        if (contactsRepository.userManageableAddressBooks.first().isNotEmpty()) {
            _uiState.value = OnboardingUiState.SystemContactsSharing
        } else {
            advanceToSelfContactOrComplete()
        }
    }

    private suspend fun advanceToSelfContactOrComplete() {
        val selfContactSet = settingsRepository.selfContactId.first() != null
        if (!selfContactSet) {
            _uiState.value = OnboardingUiState.SelfContactSelection
        } else {
            completeOnboarding()
        }
    }

    /**
     * Makes the sharing of every address book match [selected] and moves on: books in it are
     * shared (at the default Caller ID level, changed later on each book's page), and books not in
     * it are unshared, including ones shared before when setup is redone. The screen only calls
     * this once the contacts permission is held, or when nothing needs it, so there is no partial
     * result to explain.
     */
    fun saveSharingChoices(selected: Set<String>) {
        viewModelScope.launch {
            contactsRepository.userManageableAddressBooks.first().forEach { book ->
                val wanted = book.href in selected
                if (wanted != book.shareWithSystem) {
                    contactsRepository.setAddressBookSharedWithSystem(book.href, wanted)
                }
            }
            advanceToSelfContactOrComplete()
        }
    }

    /**
     * Applies the chosen upload/keep/discard decisions from the local-data-migration step and
     * advances to the sync junction - or, when resuming an already-onboarded account (see
     * [isResumingAlreadyOnboardedAccount]), completes onboarding immediately instead.
     * [booksToUpload] maps each book being uploaded to its (possibly renamed) new display name;
     * [booksToDelete] is a set of books to be permanently discarded. Any local book not present in
     * either is simply left as-is, kept local rather than uploaded or deleted.
     */
    fun resolveLocalDataMigration(
        booksToUpload: Map<AddressBookEntity, String>,
        booksToDelete: Set<AddressBookEntity>,
    ) {
        if (_isMigratingLocalData.value) return
        val shownBooks =
            (_uiState.value as? OnboardingUiState.LocalDataMigration)?.localBooks ?: return
        _isMigratingLocalData.value = true
        viewModelScope.launch {
            for ((book, newName) in booksToUpload) {
                contactsRepository.uploadLocalAddressBook(
                    book,
                    newName
                )
            }
            for (book in booksToDelete) {
                contactsRepository.deleteAddressBook(book)
            }
            // Every book shown here just got a decision either way (kept or uploaded) - mark them
            // all resolved so MainViewModel doesn't route back into this step for the same ones.
            settingsRepository.markLocalBooksResolved(shownBooks.map { it.href })
            _isMigratingLocalData.value = false
            if (isResumingAlreadyOnboardedAccount) {
                completeOnboarding()
            } else {
                advanceToSyncJunction()
            }
        }
    }

    /** Saves [contactId] as the user's "My Card" contact (or clears it if `null`) and completes onboarding. */
    fun setSelfContact(contactId: String?) {
        viewModelScope.launch {
            settingsRepository.saveSelfContactId(contactId)
            completeOnboarding()
        }
    }

    /** Switches to the "create a new contact" form, for when the user isn't in the list yet. */
    fun startCreatingSelfContact() {
        _uiState.value = OnboardingUiState.CreatingSelfContact
    }

    /** Backs out of the "create a new contact" form without saving anything. */
    fun cancelCreatingSelfContact() {
        _uiState.value = OnboardingUiState.SelfContactSelection
    }

    /** Saves a brand-new contact and marks it as the user's own, completing onboarding on success. */
    suspend fun saveNewSelfContact(contact: ContactEntity): Boolean {
        val result = contactsRepository.saveContact(contact)
        if (result.isSuccess) {
            setSelfContact(contact.id)
        }
        return result.isSuccess
    }

    /**
     * Marks the onboarding flow as complete, routing subsequent launches straight to the contact
     * list - for the current account, or, in local-only mode (no credentials), via
     * [SettingsRepository.localOnboardingCompleted] instead, since there's no account to key it to.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            val credentials = authRepository.credentials.first()
            if (credentials != null) {
                settingsRepository.saveLastOnboardedAccountKey(credentials.accountKey)
            } else {
                settingsRepository.saveLocalOnboardingCompleted(true)
            }
        }
    }

    /** Logs the user out mid-onboarding (e.g. if they signed into the wrong account). */
    fun logout() {
        viewModelScope.launch {
            contactsRepository.logout()
        }
    }
}

/** The current step of the onboarding flow, shown in order. */
sealed class OnboardingUiState {
    /** App-wide setup: address lookup, phone formatting and birthday reminders. Sync runs in the background. */
    object Setup : OnboardingUiState()

    /** Wait screen shown only if background sync from [Setup] isn't finished yet. */
    object FinalizingSync : OnboardingUiState()

    /**
     * Offering to keep each local-only address book local, or upload it to the server just
     * logged into - only shown when local-only data exists at all (see
     * [ContactsRepository.getLocalAddressBooks][dev.benica.corvidcontacts.data.repository.ContactsRepository.getLocalAddressBooks]).
     */
    data class LocalDataMigration(val localBooks: List<AddressBookEntity>) : OnboardingUiState()

    /**
     * Offering to share address books with the system contacts, so other apps can show names for
     * calls and texts. Nothing is preselected on a first run, the switches are the whole answer
     * (Continue is the only way out), and the permission is only requested when a book is newly
     * shared.
     */
    object SystemContactsSharing : OnboardingUiState()

    /** Prompting the user to pick their own contact card ("My Card"). */
    object SelfContactSelection : OnboardingUiState()

    /** Creating a brand-new contact to use as "My Card", for when the user isn't in the list yet. */
    object CreatingSelfContact : OnboardingUiState()
}
