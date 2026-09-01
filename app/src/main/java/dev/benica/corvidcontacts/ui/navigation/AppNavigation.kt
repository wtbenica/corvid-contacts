// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.PickContent
import dev.benica.corvidcontacts.ui.theme.isWideScreen

/**
 * Top-level entry point: owns the backstack and the shared [ContactsViewModel], then picks
 * between [TabletNavigation] (wide-screen shell, one persistent top bar) and [PhoneNavigation]
 * (single-pane, one `NavEntry` per destination) based on device width and whether the current
 * destination is one the shell knows how to render.
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun AppNavigation(
    startDestination: Destination,
    authRepository: AuthRepository,
    contactsRepository: ContactsRepository,
    settingsRepository: SettingsRepository,
    geocoderRepository: GeocoderRepository,
    onContinueLocally: () -> Unit,
    initialIntentContact: ContactEntity? = null,
    pickType: PickContent? = null,
    initialContactId: String? = null,
) {
    val backStack = rememberNavBackStack(startDestination) as NavBackStack<Destination>

    LaunchedEffect(initialIntentContact) {
        if (initialIntentContact != null) {
            val current = backStack.lastOrNull()
            if (current !is Destination.ContactEdit) {
                backStack.add(Destination.ContactEdit(initialContact = initialIntentContact))
            }
        }
    }

    val contactsViewModel: ContactsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(
                    contactsRepository,
                    settingsRepository,
                    authRepository
                ) as T
            }
        }
    )

    LaunchedEffect(pickType) {
        if (pickType != null) {
            contactsViewModel.startPickingExternal(pickType)
        }
    }

    LaunchedEffect(initialContactId) {
        if (initialContactId != null) {
            backStack.add(Destination.ContactDetail(initialContactId))
        }
    }

    val wideScreen = isWideScreen()
    val current = backStack.lastOrNull()

    // Destinations the wide-screen shell has no case for fall back to single-pane, even on a wide screen.
    val isMainAppDestination = current is Destination.ContactList ||
            current is Destination.ContactDetail ||
            current is Destination.ContactEdit ||
            current is Destination.Settings ||
            current is Destination.ShareSelection ||
            current is Destination.QrDisplay ||
            current is Destination.MergeReview ||
            current is Destination.About ||
            current is Destination.Licenses

    if (wideScreen && isMainAppDestination) {
        TabletNavigation(
            backStack = backStack,
            contactsViewModel = contactsViewModel,
            authRepository = authRepository,
            contactsRepository = contactsRepository,
            settingsRepository = settingsRepository,
            geocoderRepository = geocoderRepository,
            pickType = pickType,
        )
    } else {
        PhoneNavigation(
            backStack = backStack,
            contactsViewModel = contactsViewModel,
            authRepository = authRepository,
            contactsRepository = contactsRepository,
            settingsRepository = settingsRepository,
            geocoderRepository = geocoderRepository,
            onContinueLocally = onContinueLocally,
            pickType = pickType,
        )
    }
}
