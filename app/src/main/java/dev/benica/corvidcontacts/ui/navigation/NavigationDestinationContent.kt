// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.navigation

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_detail.QrDisplayScreen
import dev.benica.corvidcontacts.ui.contacts.contact_detail.ShareSelectionScreen
import dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen
import dev.benica.corvidcontacts.ui.contacts.contact_edit.NavigationGuard
import dev.benica.corvidcontacts.ui.contacts.contact_merge.MergeReviewScreen
import dev.benica.corvidcontacts.ui.settings.AboutScreen
import dev.benica.corvidcontacts.ui.settings.LicensesScreen
import dev.benica.corvidcontacts.ui.settings.SettingsScreen
import dev.benica.corvidcontacts.ui.settings.SettingsViewModel
import dev.benica.corvidcontacts.utils.shareVCard

/**
 * Per-destination content shared by [PhoneNavigation] (wrapped in its own `NavEntry`, default
 * full-screen chrome) and [TabletNavigation] (embedded in the wide-screen shell with
 * `showScaffold = false`), so each destination's data-fetching and callback wiring has a single
 * source of truth instead of being hand-duplicated across both navigation strategies.
 *
 * `ContactList` and `ContactDetail` deliberately have no equivalent here. `ContactList` is
 * genuinely different between the two hosts - phone renders it via
 * [dev.benica.corvidcontacts.ui.contacts.contact_list.ContactListScreen], while the wide-screen
 * shell draws its own list pane directly. `ContactDetail` does end up at the same
 * [dev.benica.corvidcontacts.ui.contacts.contact_detail.ContactDetailScreen] either way, but each
 * host still derives the contact and wires `showScaffold`/`onChromeChange` on its own rather than
 * through a shared function here, since the two hosts observe the selected contact differently.
 */

@Composable
@Suppress("UNCHECKED_CAST")
internal fun ContactEditContent(
    key: Destination.ContactEdit,
    contactsViewModel: ContactsViewModel,
    geocoderRepository: GeocoderRepository,
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
    onNavigationGuardReady: (NavigationGuard) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onEffectiveColorChange: (Color) -> Unit = {},
) {
    val uiState by contactsViewModel.uiState.collectAsState()
    val alwaysAddCountryCode by contactsViewModel.alwaysAddCountryCode.collectAsState()
    val addressBooks by contactsViewModel.addressBooks.collectAsState()
    val allGroups by contactsViewModel.allGroups.collectAsState()
    val allContactsWithBook = (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()

    val contactWithBook = remember(
        key.contactId,
        allContactsWithBook,
        key.initialContact
    ) {
        key.contactId?.let { id ->
            allContactsWithBook.find { it.contact.id == id }
        } ?: key.initialContact?.let { ContactWithAddressBook(it, null) }
    }

    ContactEditScreen(
        contactWithBook = contactWithBook,
        addressBooks = addressBooks,
        allGroups = allGroups,
        includeCountryCode = alwaysAddCountryCode,
        geocoderRepository = geocoderRepository,
        initialAddressBookHref = key.initialAddressBookHref,
        allContacts = allContactsWithBook,
        onSave = { savedContact ->
            val success = contactsViewModel.saveContact(savedContact)
            if (success && key.markAsSelfOnSave) {
                contactsViewModel.setSelfContactId(savedContact.id)
            }
            success
        },
        onBack = { backStack.removeAt(backStack.size - 1) },
        onSaveComplete = { contactId ->
            backStack.removeAt(backStack.size - 1)
            // Land on the saved contact's detail screen - already there if Edit was reached from
            // Detail (avoid pushing a duplicate), otherwise push it fresh so the user can verify
            // the save instead of dropping back to whatever else was underneath.
            val top = backStack.lastOrNull()
            if (top !is Destination.ContactDetail || top.contactId != contactId) {
                backStack.add(Destination.ContactDetail(contactId))
            }
        },
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
        onNavigationGuardReady = onNavigationGuardReady,
        snackbarHostState = snackbarHostState,
        onEffectiveColorChange = onEffectiveColorChange,
    )
}

@Composable
@Suppress("UNCHECKED_CAST")
internal fun SettingsContent(
    contactsRepository: ContactsRepository,
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    contactsRepository,
                    settingsRepository,
                    authRepository
                ) as T
            }
        }
    )

    SettingsScreen(
        viewModel = settingsViewModel,
        onBack = { backStack.removeAt(backStack.size - 1) },
        onAboutClick = { backStack.add(Destination.About) },
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
    )
}

@Composable
internal fun AboutContent(
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    AboutScreen(
        onBack = { backStack.removeAt(backStack.size - 1) },
        onShowLicenses = { backStack.add(Destination.Licenses) },
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
    )
}

@Composable
internal fun LicensesContent(
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    LicensesScreen(
        onBack = { backStack.removeAt(backStack.size - 1) },
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
    )
}

@Composable
internal fun ShareSelectionContent(
    key: Destination.ShareSelection,
    contactsViewModel: ContactsViewModel,
    contactsRepository: ContactsRepository,
    backStack: NavBackStack<Destination>,
    context: Context,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val uiState by contactsViewModel.uiState.collectAsState()
    val allContacts: List<ContactWithAddressBook> =
        (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
    val contactWithBook = allContacts.find { it.contact.id == key.contactId }
    val shareContactTitle = stringResource(R.string.common_share_contact)

    if (contactWithBook != null) {
        ShareSelectionScreen(
            contactWithBook = contactWithBook,
            isQr = key.isQr,
            onBack = { backStack.removeAt(backStack.size - 1) },
            onComplete = { filteredContact ->
                val vcard = contactsRepository.getVCardString(filteredContact)

                if (key.isQr) {
                    backStack.removeAt(backStack.size - 1)
                    backStack.add(
                        Destination.QrDisplay(
                            vcard,
                            filteredContact.id
                        )
                    )
                } else {
                    shareVCard(
                        context = context,
                        vcard = vcard,
                        fileName = "shared_contact.vcf",
                        chooserTitle = shareContactTitle
                    )
                    backStack.removeAt(backStack.size - 1)
                }
            },
            showScaffold = showScaffold,
            onChromeChange = onChromeChange,
        )
    }
}

@Composable
internal fun QrDisplayContent(
    key: Destination.QrDisplay,
    contactsViewModel: ContactsViewModel,
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val uiState by contactsViewModel.uiState.collectAsState()
    val allContacts: List<ContactWithAddressBook> =
        (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
    val contactWithBook = allContacts.find { it.contact.id == key.contactId }

    QrDisplayScreen(
        contactWithBook = contactWithBook,
        vcard = key.vcard,
        onBack = { backStack.removeAt(backStack.size - 1) },
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
    )
}

@Composable
internal fun MergeReviewContent(
    key: Destination.MergeReview,
    contactsViewModel: ContactsViewModel,
    contactsRepository: ContactsRepository,
    geocoderRepository: GeocoderRepository,
    backStack: NavBackStack<Destination>,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by contactsViewModel.uiState.collectAsState()
    val allContacts: List<ContactWithAddressBook> =
        (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
    val allGroups by contactsViewModel.allGroups.collectAsState()
    val addressBooks by contactsViewModel.addressBooks.collectAsState()
    val survivor by remember(key.survivorContactId) {
        contactsRepository.observeContactById(key.survivorContactId)
    }.collectAsState(initial = allContacts.find { it.contact.id == key.survivorContactId })
    val absorbed by remember(key.absorbedContactId) {
        contactsRepository.observeContactById(key.absorbedContactId)
    }.collectAsState(initial = allContacts.find { it.contact.id == key.absorbedContactId })

    val survivorNonNull = survivor
    val absorbedNonNull = absorbed
    if (survivorNonNull != null && absorbedNonNull != null) {
        val alwaysAddCountryCode by contactsViewModel.alwaysAddCountryCode.collectAsState()
        MergeReviewScreen(
            survivor = survivorNonNull,
            absorbed = absorbedNonNull,
            onConfirm = { merged, absorbedContact, useAbsorbedPhoto ->
                val success = contactsViewModel.mergeContacts(
                    merged,
                    absorbedContact,
                    useAbsorbedPhoto
                )
                if (success) {
                    contactsViewModel.stopPickingMergeTarget()
                    backStack.removeAt(backStack.size - 1)
                    backStack.add(Destination.ContactDetail(merged.id))
                }
                success
            },
            onCancel = {
                contactsViewModel.stopPickingMergeTarget()
                backStack.removeAt(backStack.size - 1)
            },
            geocoderRepository = geocoderRepository,
            includeCountryCode = alwaysAddCountryCode,
            allContacts = allContacts,
            allGroups = allGroups,
            addressBooks = addressBooks,
            showScaffold = showScaffold,
            onChromeChange = onChromeChange,
            snackbarHostState = snackbarHostState,
        )
    }
}
