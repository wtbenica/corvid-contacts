// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.KnownRelative
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relative
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.UnknownRelative
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScrollableColumn
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopAppBar
import dev.benica.corvidcontacts.ui.contacts.common_ui.EmptyState
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_detail.components.ContactDetailContent
import dev.benica.corvidcontacts.ui.contacts.contact_detail.components.rememberContactDetailActions
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.launch

/**
 * Screen for viewing a contact's details. Used both as its own full-screen destination (phone)
 * and embedded in the wide-screen shell's detail pane with [showScaffold] = false, in which case
 * [onChromeChange] receives this screen's title/actions instead of it drawing its own top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactWithBook: ContactWithAddressBook?,
    allContacts: List<ContactWithAddressBook>,
    isFavorite: Boolean,
    onToggleFavorite: suspend () -> Boolean,
    onBack: () -> Unit,
    onEdit: (ContactEntity) -> Unit,
    onDelete: suspend (ContactEntity) -> Boolean,
    onArchive: suspend (ContactEntity) -> Boolean,
    onDownloadPhoto: suspend (ContactEntity) -> Boolean,
    onMerge: (ContactEntity) -> Unit,
    onNavigateToContact: (String) -> Unit,
    onShare: (Boolean) -> Unit, // true for QR, false for normal share
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
    // Only rendered here when showScaffold is true; embedders pass their own otherwise.
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val contact = contactWithBook?.contact
    val baseColor = ContactColors.resolveContactColor(contactWithAddressBook = contactWithBook)
    val errorActionFailed = stringResource(R.string.detail_error_action_failed)
    val scope = rememberCoroutineScope()

    val handlers = rememberContactDetailActions(
        contact = contact,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onEdit = onEdit,
        onArchive = onArchive,
        onMerge = onMerge,
        onDelete = onDelete,
        snackbarHostState = snackbarHostState,
        errorMessage = errorActionFailed,
    )

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        if (contactWithBook == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.smSpacing),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Rounded.PersonSearch,
                    title = stringResource(R.string.detail_no_contact_title),
                    description = stringResource(R.string.detail_no_contact_description),
                )
            }
        } else {
            val relationships = contactWithBook.contact.relationships ?: emptyList()
            val relatives: List<Relative> = relationships.mapNotNull { rel ->
                if (rel.isUid) {
                    allContacts.find { it.contact.id == rel.value }?.contact?.let {
                        KnownRelative(rel.type, it)
                    }
                } else {
                    UnknownRelative(rel.type, rel.value)
                }
            }

            CCScrollableColumn(
                systemPadding = padding,
                modifier = modifier,
                content = {
                    ContactDetailContent(
                        contact = contactWithBook.contact,
                        relatives = relatives,
                        onNavigateToContact = onNavigateToContact,
                        onShowQr = { onShare(true) },
                        onShare = { onShare(false) },
                        onDownloadPhoto = { downloaded ->
                            scope.launch {
                                if (!onDownloadPhoto(downloaded)) {
                                    snackbarHostState.showSnackbar(errorActionFailed)
                                }
                            }
                        }
                    )
                }
            )
        }
        handlers.deleteDialog()
    }

    val chromeTitle = stringResource(R.string.detail_title)
    val chromeNavigationIcon: @Composable () -> Unit = { BackNavButton(onBack) }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            baseColor = baseColor,
            topBar = {
                CCTopAppBar(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                    actions = handlers.actions,
                    baseColor = baseColor
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = bodyContent
        )
    } else {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                    actions = handlers.actions,
                )
            )
        }
        bodyContent(PaddingValues())
    }
}

@ThemePreview
@Composable
private fun ContactDetailScreenPreview() {
    val contact = ContactEntity(
        id = "A",
        displayName = "Aunty Em",
        firstName = "Emily",
        middleName = "Potato",
        lastName = "Giviono",
        jobTitle = "Gardener",
        company = "Emerald City Gardens",
        nickname = "Em",
        photoUrl = null,
        emails = listOf(
            Email(
                "emily@example.com",
                "home"
            )
        ),
        phones = listOf(
            Phone(
                "+1234567890",
                "mobile"
            )
        ),
        //        address = "123 Main St, Anytown, State, 12345, Country",
        etag = "etag",
        structuredAddresses = listOf(
            StructuredAddress(
                street = "123 Main St",
                city = "Anytown",
                state = "State",
                postalCode = "12345",
                country = "Country"
            )
        )
    )

    CorvidContactsTheme {
        ContactDetailScreen(
            contactWithBook = ContactWithAddressBook(
                contact,
                null
            ),
            allContacts = emptyList(),
            isFavorite = true,
            onToggleFavorite = { true },
            onBack = {},
            onEdit = {},
            onDelete = { true },
            onArchive = { true },
            onDownloadPhoto = { true },
            onMerge = {},
            onNavigateToContact = {},
            onShare = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@ThemePreview
@Composable
private fun ContactDetailScreenNoContactPreview() {
    CorvidContactsTheme {
        ContactDetailScreen(
            null,
            emptyList(),
            false,
            { true },
            {},
            {},
            { true },
            { true },
            { true },
            {},
            {},
            {},
        )
    }
}

