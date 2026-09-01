// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.navigation

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.benica.corvidcontacts.BuildConfig
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.PickContent
import dev.benica.corvidcontacts.ui.contacts.contact_detail.ContactDetailScreen
import dev.benica.corvidcontacts.ui.contacts.contact_list.ContactListScreen
import dev.benica.corvidcontacts.ui.login.LoginScreen
import dev.benica.corvidcontacts.ui.login.LoginUiState
import dev.benica.corvidcontacts.ui.login.LoginViewModel
import dev.benica.corvidcontacts.ui.login.WelcomeScreen
import dev.benica.corvidcontacts.ui.onboarding.OnboardingScreen
import dev.benica.corvidcontacts.ui.onboarding.OnboardingViewModel
import dev.benica.corvidcontacts.ui.settings.AboutScreen
import dev.benica.corvidcontacts.ui.settings.LicensesScreen

/**
 * Single-pane navigation: every destination gets its own full-screen `NavEntry`. Used when the
 * screen is narrow, and as the fallback for destinations the wide-screen shell ([TabletNavigation])
 * doesn't have a case for (Welcome/Login/Onboarding/Resolving/About/Licenses).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PhoneNavigation(
    backStack: NavBackStack<Destination>,
    contactsViewModel: ContactsViewModel,
    authRepository: AuthRepository,
    contactsRepository: ContactsRepository,
    settingsRepository: SettingsRepository,
    geocoderRepository: GeocoderRepository,
    onContinueLocally: () -> Unit,
    pickType: PickContent?,
) {
    val strategy = rememberListDetailSceneStrategy<Destination>()
    val context = LocalContext.current

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        sceneStrategies = listOf(strategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
    ) { key ->
        when (key) {
            Destination.Resolving -> NavEntry(key) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Destination.Welcome -> NavEntry(key) {
                WelcomeScreen(
                    onSignInClick = { backStack.add(Destination.Login()) },
                    onUseLocallyClick = onContinueLocally
                )
            }

            is Destination.Login -> NavEntry(key) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LoginViewModel(
                                authRepository,
                                settingsRepository
                            ) as T
                        }
                    }
                )
                val uiState by loginViewModel.uiState.collectAsState()
                val savedServers by loginViewModel.savedServers.collectAsState(initial = emptySet())

                // Covers success with no destination change (e.g. reconnect while already on ContactList).
                LaunchedEffect(uiState) {
                    if (uiState is LoginUiState.Success && backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                }

                LoginScreen(
                    uiState = uiState,
                    savedServers = savedServers,
                    onRemoveSavedServer = loginViewModel::removeSavedServer,
                    onLogin = { url, user, pass, fetchPhotos ->
                        loginViewModel.login(
                            serverUrl = url,
                            username = user,
                            appPassword = pass,
                            fetchRemotePhotos = fetchPhotos
                        )
                    },
                    prefilledUrl = key.prefilledUrl,
                    onBack = if (backStack.size > 1) {
                        { backStack.removeAt(backStack.size - 1) }
                    } else {
                        null
                    }
                )
            }

            is Destination.Onboarding -> NavEntry(key) {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return OnboardingViewModel(
                                contactsRepository,
                                settingsRepository,
                                authRepository
                            ) as T
                        }
                    }
                )
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    geocoderRepository = geocoderRepository
                )
            }

            is Destination.ContactList -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                val isPickingExternal by contactsViewModel.isPickingExternal.collectAsState()
                val mergeSourceContactId by contactsViewModel.mergeSourceContactId.collectAsState()

                ContactListScreen(
                    viewModel = contactsViewModel,
                    onContactClick = { contactWithBook ->
                        backStack.add(Destination.ContactDetail(contactWithBook.contact.id))
                    },
                    onAddContact = {
                        // If exactly one address book is active, default new contacts there.
                        val singleFilteredBook =
                            contactsViewModel.selectedAddressBookHrefs.value.singleOrNull()
                        backStack.add(Destination.ContactEdit(initialAddressBookHref = singleFilteredBook))
                    },
                    onAddSelfContact = {
                        backStack.add(Destination.ContactEdit(markAsSelfOnSave = true))
                    },
                    onSetUpSync = { backStack.add(Destination.Login()) },
                    onSettingsClick = { backStack.add(Destination.Settings) },
                    onContactSelected = { contactWithBook ->
                        val sourceId = mergeSourceContactId
                        if (isPickingExternal) {
                            if (contactWithBook != null) {
                                val contact = contactWithBook.contact
                                val authority = "${BuildConfig.APPLICATION_ID}.provider"
                                val suffix =
                                    if (pickType == PickContent.EMAIL || pickType == PickContent.PHONE) "" else ".vcf"
                                val secureUri =
                                    "content://$authority/${contact.id}$suffix".toUri()

                                val resultIntent = Intent().apply {
                                    data = secureUri
                                    clipData = ClipData.newRawUri(
                                        null,
                                        secureUri
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                (context as? ComponentActivity)?.let {
                                    it.setResult(
                                        Activity.RESULT_OK,
                                        resultIntent
                                    )
                                    it.finish()
                                }
                            } else {
                                contactsViewModel.stopPickingExternal()
                            }
                        } else if (sourceId != null) {
                            if (contactWithBook != null) {
                                backStack.add(
                                    Destination.MergeReview(
                                        survivorContactId = sourceId,
                                        absorbedContactId = contactWithBook.contact.id
                                    )
                                )
                            } else {
                                // Only stop picking if the user cancelled (null selection)
                                contactsViewModel.stopPickingMergeTarget()
                            }
                        } else {
                            contactsViewModel.setSelfContactId(contactWithBook?.contact?.id)
                        }
                    },
                    onCancelSelectingContact = {
                        if (isPickingExternal) {
                            contactsViewModel.stopPickingExternal()
                            (context as? ComponentActivity)?.finish()
                        } else if (mergeSourceContactId != null) {
                            contactsViewModel.stopPickingMergeTarget()
                        } else {
                            contactsViewModel.stopPickingSelf()
                        }
                    },
                    onClearSelectingContact = {
                        contactsViewModel.setSelfContactId(null)
                    },
                    onShareSelf = { contactWithBook ->
                        backStack.add(
                            Destination.ShareSelection(
                                contactWithBook.contact.id,
                                isQr = false
                            )
                        )
                    },
                    onShareSelfViaQr = { contactWithBook ->
                        backStack.add(
                            Destination.ShareSelection(
                                contactWithBook.contact.id,
                                isQr = true
                            )
                        )
                    }
                )
            }

            is Destination.Settings -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                SettingsContent(contactsRepository, settingsRepository, authRepository, backStack)
            }

            is Destination.About -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                AboutScreen(
                    onBack = { backStack.removeAt(backStack.size - 1) },
                    onShowLicenses = { backStack.add(Destination.Licenses) })
            }

            is Destination.Licenses -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                LicensesScreen(onBack = { backStack.removeAt(backStack.size - 1) })
            }

            is Destination.ContactDetail -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                val uiState by contactsViewModel.uiState.collectAsState()
                val allContacts: List<ContactWithAddressBook> =
                    (uiState as? ContactsUiState.Success)?.contacts ?: emptyList()
                // Use direct Room lookup to ensure the contact is shown regardless of list filters.
                val contactWithBook by remember(key.contactId) {
                    contactsRepository.observeContactById(key.contactId)
                }.collectAsState(initial = allContacts.find { it.contact.id == key.contactId })
                val contact = contactWithBook?.contact

                val isFavorite = contact?.let {
                    val categories = it.categories ?: emptyList()
                    categories.any { c ->
                        c.equals(
                            ContactsRepository.FAVORITE_CATEGORY,
                            ignoreCase = true
                        )
                    }
                } ?: false

                ContactDetailScreen(
                    contactWithBook = contactWithBook,
                    allContacts = allContacts,
                    isFavorite = isFavorite,
                    onToggleFavorite = {
                        contact?.let { contactsViewModel.toggleFavorite(it) } ?: true
                    },
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                    onEdit = { backStack.add(Destination.ContactEdit(it.id)) },
                    onDelete = {
                        val success = contactsViewModel.deleteContact(it)
                        if (success && backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        success
                    },
                    onArchive = {
                        val success = contactsViewModel.archiveContact(it)
                        if (success && backStack.size > 1) backStack.removeAt(backStack.size - 1)
                        success
                    },
                    onDownloadPhoto = { contactsViewModel.downloadContactPhoto(it) },
                    onMerge = {
                        contactsViewModel.startPickingMergeTarget(it.id)
                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                    },
                    onNavigateToContact = { id ->
                        backStack.add(Destination.ContactDetail(id))
                    },

                    onShare = { isQr ->
                        backStack.add(
                            Destination.ShareSelection(
                                key.contactId,
                                isQr
                            )
                        )
                    }
                )
            }

            is Destination.MergeReview -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                MergeReviewContent(
                    key,
                    contactsViewModel,
                    contactsRepository,
                    geocoderRepository,
                    backStack
                )
            }

            is Destination.ShareSelection -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                ShareSelectionContent(
                    key,
                    contactsViewModel,
                    contactsRepository,
                    backStack,
                    context
                )
            }

            is Destination.QrDisplay -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                QrDisplayContent(key, contactsViewModel, backStack)
            }

            is Destination.ContactEdit -> NavEntry(
                key = key,
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                ContactEditContent(key, contactsViewModel, geocoderRepository, backStack)
            }
        }
    }
}
