// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.navigation

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import dev.benica.corvidcontacts.BuildConfig
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.PickContent
import dev.benica.corvidcontacts.ui.contacts.alt_layout.AdaptiveMasterScreen
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_edit.NavigationGuard

/**
 * Wide-screen shell dispatch: builds the list-pane/detail-pane/single-pane content for whichever
 * destination is on top of the backstack and hands it to [AdaptiveMasterScreen], which draws the
 * single, persistent top bar. Only understands destinations [isMainAppDestination][AppNavigation]
 * covers - everything else falls back to [PhoneNavigation].
 */
@Composable
fun TabletNavigation(
    backStack: NavBackStack<Destination>,
    contactsViewModel: ContactsViewModel,
    authRepository: AuthRepository,
    contactsRepository: ContactsRepository,
    settingsRepository: SettingsRepository,
    geocoderRepository: GeocoderRepository,
    pickType: PickContent?,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val current = backStack.lastOrNull()
    val selectedContactId =
        backStack.filterIsInstance<Destination.ContactDetail>().lastOrNull()?.contactId
    val isPickingSelf by contactsViewModel.isPickingSelf.collectAsState()
    val isPickingExternal by contactsViewModel.isPickingExternal.collectAsState()
    val mergeSourceContactId by contactsViewModel.mergeSourceContactId.collectAsState()

    // Replaces the detail pane's contact rather than stacking; re-clicking it deselects.
    fun navigateToContactDetail(id: String) {
        when (val top = backStack.lastOrNull()) {
            is Destination.ContactDetail if top.contactId == id -> backStack.removeAt(backStack.size - 1)
            is Destination.ContactDetail -> backStack[backStack.size - 1] =
                Destination.ContactDetail(id)

            else ->
                backStack.add(Destination.ContactDetail(id))
        }
    }

    // Reset per destination so stale chrome can't linger after a pop.
    var chrome by remember(current) { mutableStateOf<ScreenChrome?>(null) }
    val onChromeChange: (ScreenChrome) -> Unit = { chrome = it }

    // Guards unsaved ContactEdit changes, same as the edit screen's own back button.
    var navigationGuard by remember(current) { mutableStateOf<NavigationGuard?>(null) }

    fun guardedNavigate(action: () -> Unit) {
        val proceed = {
            if (backStack.lastOrNull() is Destination.ContactEdit) {
                backStack.removeAt(backStack.size - 1)
            }
            action()
        }
        val guard = navigationGuard
        if (current is Destination.ContactEdit && guard != null) {
            guard.requestNavigation(proceed)
        } else {
            proceed()
        }
    }

    // Overrides the synchronous default below once the form reports a live selection.
    var liveEditTintColor by remember(current) { mutableStateOf<Color?>(null) }

    val editPaneContent: (@Composable () -> Unit)? = if (current is Destination.ContactEdit) {
        {
            ContactEditContent(
                key = current,
                contactsViewModel = contactsViewModel,
                geocoderRepository = geocoderRepository,
                backStack = backStack,
                showScaffold = false,
                onChromeChange = onChromeChange,
                onNavigationGuardReady = { navigationGuard = it },
                snackbarHostState = snackbarHostState,
                onEffectiveColorChange = { liveEditTintColor = it },
            )
        }
    } else null

    // Synchronous so the edit pane never flashes the default color before settling.
    val editPaneTintColor: Color? = if (current is Destination.ContactEdit) {
        liveEditTintColor ?: run {
            val uiState by contactsViewModel.uiState.collectAsState()
            val addressBooks by contactsViewModel.addressBooks.collectAsState()
            val allContacts = (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
            val editContactWithBook: ContactWithAddressBook? = current.contactId?.let { id ->
                allContacts.find { it.contact.id == id }
            } ?: current.initialContact?.let { ContactWithAddressBook(it, null) }

            ContactColors.resolveEditDefaultColor(
                contactWithBook = editContactWithBook,
                addressBooks = addressBooks,
                initialAddressBookHref = current.initialAddressBookHref,
            )
        }
    } else null

    // Which list row (if any) reflects what's currently shown in the detail/edit pane.
    val activeContactId: String? = when (current) {
        is Destination.ContactEdit -> current.contactId
        is Destination.ContactDetail -> current.contactId
        else -> selectedContactId
    }
    val showNewContactPlaceholder =
        current is Destination.ContactEdit && current.contactId == null

    val singlePaneContent: (@Composable () -> Unit)? = when (current) {
        is Destination.Settings -> {
            {
                SettingsContent(
                    contactsRepository = contactsRepository,
                    settingsRepository = settingsRepository,
                    authRepository = authRepository,
                    backStack = backStack,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                )
            }
        }

        is Destination.ShareSelection -> {
            {
                ShareSelectionContent(
                    key = current,
                    contactsViewModel = contactsViewModel,
                    contactsRepository = contactsRepository,
                    backStack = backStack,
                    context = context,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                )
            }
        }

        is Destination.QrDisplay -> {
            {
                QrDisplayContent(
                    key = current,
                    contactsViewModel = contactsViewModel,
                    backStack = backStack,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                )
            }
        }

        is Destination.MergeReview -> {
            {
                MergeReviewContent(
                    key = current,
                    contactsViewModel = contactsViewModel,
                    contactsRepository = contactsRepository,
                    geocoderRepository = geocoderRepository,
                    backStack = backStack,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        is Destination.About -> {
            {
                AboutContent(
                    backStack = backStack,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                )
            }
        }

        is Destination.Licenses -> {
            {
                LicensesContent(
                    backStack = backStack,
                    showScaffold = false,
                    onChromeChange = onChromeChange,
                )
            }
        }

        else -> null
    }

    AdaptiveMasterScreen(
        viewModel = contactsViewModel,
        selectedContactId = selectedContactId,
        onContactClick = { contactWithBook ->
            val sourceId = mergeSourceContactId
            when {
                isPickingExternal -> {
                    val contact = contactWithBook.contact
                    val authority = "${BuildConfig.APPLICATION_ID}.provider"
                    val suffix =
                        if (pickType == PickContent.EMAIL || pickType == PickContent.PHONE) "" else ".vcf"
                    val secureUri = "content://$authority/${contact.id}$suffix".toUri()
                    val resultIntent = Intent().apply {
                        data = secureUri
                        clipData = ClipData.newRawUri(null, secureUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    (context as? ComponentActivity)?.let {
                        it.setResult(Activity.RESULT_OK, resultIntent)
                        it.finish()
                    }
                }

                sourceId != null -> {
                    backStack.add(
                        Destination.MergeReview(
                            survivorContactId = sourceId,
                            absorbedContactId = contactWithBook.contact.id
                        )
                    )
                }

                isPickingSelf -> {
                    contactsViewModel.setSelfContactId(contactWithBook.contact.id)
                }

                else -> guardedNavigate { navigateToContactDetail(contactWithBook.contact.id) }
            }
        },
        onAddContact = {
            val singleFilteredBook =
                contactsViewModel.selectedAddressBookHrefs.value.singleOrNull()
            guardedNavigate {
                backStack.add(Destination.ContactEdit(initialAddressBookHref = singleFilteredBook))
            }
        },
        onEditContact = { contact ->
            backStack.add(Destination.ContactEdit(contact.id))
        },
        onMergeContact = { contact ->
            contactsViewModel.startPickingMergeTarget(contact.id)
        },
        onNavigateToContact = { id -> navigateToContactDetail(id) },
        onSettingsClick = { backStack.add(Destination.Settings) },
        onSetUpSync = { backStack.add(Destination.Login()) },
        onShare = { id, isQr ->
            backStack.add(Destination.ShareSelection(id, isQr))
        },
        onContactRemovedFromView = {
            if (backStack.lastOrNull() is Destination.ContactDetail) {
                backStack.removeAt(backStack.size - 1)
            }
        },
        snackbarHostState = snackbarHostState,
        singlePaneChrome = chrome,
        singlePaneContent = singlePaneContent,
        editPaneContent = editPaneContent,
        editPaneTintColor = editPaneTintColor,
        activeContactId = activeContactId,
        showNewContactPlaceholder = showNewContactPlaceholder,
    )
}
