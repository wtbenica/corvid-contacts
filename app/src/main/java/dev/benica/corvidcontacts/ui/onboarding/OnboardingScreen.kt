// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.benica.corvidcontacts.ui.addressbooks.rememberContactsPermissionRequest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.addressbooks.rememberHasContactsPermission
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCBottomActionBar
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen
import dev.benica.corvidcontacts.ui.settings.SettingsHeader
import dev.benica.corvidcontacts.ui.settings.sections.AddressLookupSection
import dev.benica.corvidcontacts.ui.settings.sections.BirthdayRemindersSection
import dev.benica.corvidcontacts.ui.settings.sections.PhoneFormattingSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.PhonePreview
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

private enum class MigrationDecision { KEEP, UPLOAD, DISCARD }

private val LocalOnboardingSignOut = staticCompositionLocalOf { {} }

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    geocoderRepository: GeocoderRepository?,
) {
    val uiState by viewModel.uiState.collectAsState()
    val alwaysAddCountryCode by viewModel.alwaysAddCountryCode.collectAsState()
    val addressLookupMode by viewModel.addressLookupMode.collectAsState()
    val hasServerConnection by viewModel.hasServerConnection.collectAsState()
    val isBackgroundSyncing by viewModel.isBackgroundSyncing.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val currentBirthdayNotificationsEnabled by viewModel.currentBirthdayNotificationsEnabled.collectAsState()
    val isMigratingLocalData by viewModel.isMigratingLocalData.collectAsState()
    val addressBooks by viewModel.addressBooks.collectAsState()

    // Rendered standalone because it has its own scaffold.
    if (uiState is OnboardingUiState.CreatingSelfContact) {
        val allGroups by viewModel.allGroups.collectAsState()

        ContactEditScreen(
            contactWithBook = null,
            addressBooks = addressBooks,
            allGroups = allGroups,
            includeCountryCode = alwaysAddCountryCode,
            geocoderRepository = geocoderRepository,
            onSave = { viewModel.saveNewSelfContact(it) },
            onBack = viewModel::cancelCreatingSelfContact,
            allContacts = contacts
        )
        return
    }

    CompositionLocalProvider(LocalOnboardingSignOut provides viewModel::logout) {
        when (val currentUiState = uiState) {
            OnboardingUiState.Setup -> SetupStep(
                initialAlwaysAdd = alwaysAddCountryCode,
                initialAddressMode = addressLookupMode,
                initialBirthdayReminders = currentBirthdayNotificationsEnabled,
                isSyncing = isBackgroundSyncing,
                hasServerConnection = hasServerConnection,
                onComplete = viewModel::saveSetupPreferences
            )

            OnboardingUiState.FinalizingSync -> FinalizingSyncStep()

            is OnboardingUiState.LocalDataMigration -> LocalDataMigrationStep(
                localBooks = currentUiState.localBooks,
                isSubmitting = isMigratingLocalData,
                hasServerConnection = hasServerConnection,
                onSelection = viewModel::resolveLocalDataMigration
            )

            OnboardingUiState.SystemContactsSharing -> SystemContactsSharingStep(
                addressBooks = addressBooks,
                onSave = viewModel::saveSharingChoices
            )

            OnboardingUiState.SelfContactSelection -> SelfContactSelectionStep(
                contacts = contacts,
                hasServerConnection = hasServerConnection,
                onSelection = viewModel::setSelfContact,
                onCreateNew = viewModel::startCreatingSelfContact
            )

            OnboardingUiState.CreatingSelfContact -> Unit
        }
    }
}

/**
 * The layout every onboarding step shares: the step's [title] in the top bar, an optional
 * [description] under it, [content] in a column with one padding rule, and [actions] pinned in a
 * bottom bar that rides above the keyboard. Scrolling is decided here too: [scrollable] steps
 * scroll as a whole, and the rest give their own list the remaining height.
 */
@Composable
private fun OnboardingStepFrame(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    scrollable: Boolean = false,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val onSignOut = LocalOnboardingSignOut.current

    CCScaffold(
        title = title,
        modifier = modifier,
        topBarActions = {
            CCIconButton(
                icon = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = R.string.list_menu_logout,
                onClick = onSignOut,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        bottomBar = { if (actions != null) CCBottomActionBar(content = actions) },
    ) { padding ->
        CCWidthClampedBox(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = Dimens.smSpacing, vertical = Dimens.lgSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
            ) {
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.lgSpacing)
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun SetupStep(
    initialAlwaysAdd: Boolean,
    initialAddressMode: AddressLookupMode,
    initialBirthdayReminders: Boolean,
    isSyncing: Boolean,
    hasServerConnection: Boolean,
    onComplete: (
        alwaysAddCountryCode: Boolean,
        addressLookupMode: AddressLookupMode,
        birthdayReminders: Boolean,
    ) -> Unit,
) {
    var selectedAlwaysAdd by remember(initialAlwaysAdd) { mutableStateOf(initialAlwaysAdd) }
    var selectedAddressMode by remember(initialAddressMode) { mutableStateOf(initialAddressMode) }
    var selectedBirthdayReminders by remember(initialBirthdayReminders) {
        mutableStateOf(initialBirthdayReminders)
    }
    val uriHandler = LocalUriHandler.current

    OnboardingStepFrame(
        title = stringResource(R.string.onboarding_title),
        scrollable = true,
        actions = {
            if (hasServerConnection) {
                Surface(
                    color = currentThemeColor().surfaceVariant(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (isSyncing) {
                            stringResource(R.string.onboarding_setup_description)
                        } else {
                            stringResource(R.string.onboarding_setup_description_complete)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.medSpacing)
                    )
                }
            }

            CCButton(
                text = R.string.onboarding_action_continue,
                onClick = {
                    onComplete(
                        selectedAlwaysAdd,
                        selectedAddressMode,
                        selectedBirthdayReminders
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        PhoneFormattingSection(alwaysAddCountryCode = selectedAlwaysAdd) {
            selectedAlwaysAdd = it
        }

        AddressLookupSection(
            mode = selectedAddressMode,
            onModeSelected = { selectedAddressMode = it },
            uriHandler = uriHandler
        )

        BirthdayRemindersSection(
            enabled = selectedBirthdayReminders,
            onToggled = { selectedBirthdayReminders = it }
        )
    }
}

@Composable
private fun FinalizingSyncStep() {
    OnboardingStepFrame(title = stringResource(R.string.onboarding_title)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Text(
                    text = stringResource(R.string.onboarding_setup_syncing),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

/**
 * Offers, per local-only address book, to keep it local to this device or upload it to the
 * server just logged into (with an editable name, defaulting to its current one). Books not
 * chosen for upload are simply left alone - "keep separate" already works with no action needed.
 */
@Composable
private fun LocalDataMigrationStep(
    localBooks: List<AddressBookEntity>,
    isSubmitting: Boolean,
    hasServerConnection: Boolean,
    onSelection: (booksToUpload: Map<AddressBookEntity, String>, booksToDelete: Set<AddressBookEntity>) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    var decisions by remember(localBooks) {
        mutableStateOf(localBooks.associateWith { MigrationDecision.KEEP })
    }
    var uploadNames by remember(localBooks) {
        mutableStateOf(localBooks.associate { it.href to (it.displayName ?: "") })
    }

    OnboardingStepFrame(
        title = stringResource(R.string.onboarding_local_migration_title),
        description = stringResource(R.string.onboarding_local_migration_description),
        actions = {
            if (isSubmitting) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.medSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = Dimens.smSpacing)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.onboarding_local_migration_uploading))
                }
            }

            CCButton(
                text = R.string.onboarding_action_continue,
                enabled = !isSubmitting,
                onClick = {
                    val booksToUpload = localBooks
                        .filter { decisions[it] == MigrationDecision.UPLOAD }
                        .associateWith { book ->
                            (uploadNames[book.href] ?: "")
                                .trim()
                                .ifBlank { book.displayName ?: "" }
                        }
                    val booksToDelete = localBooks
                        .filter { decisions[it] == MigrationDecision.DISCARD }
                        .toSet()

                    onSelection(
                        booksToUpload,
                        booksToDelete
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)
        ) {
            items(
                localBooks,
                key = { it.href }) { book ->
                val decision = decisions[book] ?: MigrationDecision.KEEP
                val icon = ContactColors.getIconForAddressBook(
                    displayName = book.displayName,
                    iconName = book.iconName
                )

                Column {
                    SettingsHeader(
                        title = book.displayName
                            ?: stringResource(R.string.settings_address_book_unnamed),
                        icon = icon,
                    )

                    Column(modifier = Modifier.padding(vertical = Dimens.smSpacing)) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.onboarding_local_migration_keep_separate)) },
                            leadingContent = {
                                RadioButton(
                                    selected = decision == MigrationDecision.KEEP,
                                    enabled = !isSubmitting,
                                    onClick = {
                                        decisions = decisions + (book to MigrationDecision.KEEP)
                                    })
                            },
                            modifier = Modifier.clickable(enabled = !isSubmitting) {
                                decisions = decisions + (book to MigrationDecision.KEEP)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )

                        if (hasServerConnection) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.onboarding_local_migration_upload)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = decision == MigrationDecision.UPLOAD,
                                        enabled = !isSubmitting,
                                        onClick = {
                                            decisions =
                                                decisions + (book to MigrationDecision.UPLOAD)
                                        })
                                },
                                modifier = Modifier.clickable(enabled = !isSubmitting) {
                                    decisions = decisions + (book to MigrationDecision.UPLOAD)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.onboarding_local_migration_discard)) },
                            leadingContent = {
                                RadioButton(
                                    selected = decision == MigrationDecision.DISCARD,
                                    enabled = !isSubmitting,
                                    onClick = {
                                        decisions = decisions + (book to MigrationDecision.DISCARD)
                                    })
                            },
                            modifier = Modifier.clickable(enabled = !isSubmitting) {
                                decisions = decisions + (book to MigrationDecision.DISCARD)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )

                        if (decision == MigrationDecision.UPLOAD && hasServerConnection) {
                            CCOutlinedTextField(
                                value = uploadNames[book.href] ?: "",
                                onValueChange = { uploadNames = uploadNames + (book.href to it) },
                                label = stringResource(R.string.settings_address_book_name_label),
                                supportingText = stringResource(R.string.onboarding_local_migration_rename_hint),
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Offers to share address books with the system contacts so other apps (Messages, the dialer) can
 * show names for calls and texts. The switches are the whole answer and Continue is the only way
 * out: on a first run nothing is preselected, and when setup is redone each book starts as it
 * currently is (shared only if its flag is on and the permission is held).
 *
 * Sharing a book needs the contacts permission, so Continue asks for it when it would newly share
 * one. If it is denied the step does not move on: the books that needed it are switched back off
 * and a note says why, so nothing is left quietly unshared. Turning everything off is always a
 * valid way forward and never asks. The level stays at Caller ID and is changed later on each
 * book's page.
 */
@Composable
internal fun SystemContactsSharingStep(
    addressBooks: List<AddressBookEntity>,
    onSave: (selected: Set<String>) -> Unit,
) {
    val context = LocalContext.current
    val hasPermission = rememberHasContactsPermission()
    // A book is only really shared while the permission is held too, so with it revoked every
    // switch starts off, matching what the address book pages and Settings show.
    var selected by remember {
        mutableStateOf(addressBooks.filter { it.shareWithSystem && hasPermission }.map { it.href }.toSet())
    }
    var permissionDenied by remember { mutableStateOf(false) }

    fun needsPermission(books: Set<String>) = addressBooks.any {
        it.href in books && !(it.shareWithSystem && hasPermission)
    }

    val permissionRequest = rememberContactsPermissionRequest { granted ->
        if (granted) {
            onSave(selected)
        } else {
            permissionDenied = true
            // Back to a clean page: the books that needed the permission are off again.
            selected = addressBooks
                .filter { it.href in selected && it.shareWithSystem && hasPermission }
                .map { it.href }
                .toSet()
        }
    }

    OnboardingStepFrame(
        title = stringResource(R.string.onboarding_sharing_title),
        description = stringResource(R.string.onboarding_sharing_description),
        scrollable = true,
        actions = {
            CCButton(
                text = R.string.onboarding_action_continue,
                onClick = {
                    if (needsPermission(selected)) {
                        permissionRequest.launch()
                    } else {
                        onSave(selected)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        if (permissionDenied) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.lgSpacing)
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.medSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_sharing_permission_denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (!permissionRequest.canAskAgain) {
                        CCTextButton(
                            text = R.string.onboarding_sharing_open_settings,
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        addressBooks.forEach { book ->
            val isOn = book.href in selected
            val toggle = {
                permissionDenied = false
                selected = if (isOn) selected - book.href else selected + book.href
            }
            ListItem(
                headlineContent = {
                    Text(book.displayName ?: stringResource(R.string.settings_address_book_unnamed))
                },
                supportingContent = if (book.isLocal) {
                    { Text(stringResource(R.string.settings_address_book_local_badge)) }
                } else null,
                trailingContent = { Switch(checked = isOn, onCheckedChange = { toggle() }) },
                modifier = Modifier.clickable(onClick = toggle),
            )
        }
    }
}

@Composable
private fun SelfContactSelectionStep(
    contacts: List<ContactWithAddressBook>,
    hasServerConnection: Boolean,
    onSelection: (String?) -> Unit,
    onCreateNew: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val filteredContacts = remember(
        contacts,
        searchQuery
    ) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.contact
                .getEffectiveDisplayName()
                .contains(
                    searchQuery,
                    ignoreCase = true
                )
        }
    }

    OnboardingStepFrame(
        title = stringResource(R.string.onboarding_self_contact_title),
        description = stringResource(R.string.onboarding_self_contact_description),
        actions = {
            CCButton(
                text = R.string.onboarding_self_contact_create_new,
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth()
            )

            CCTextButton(
                text = R.string.onboarding_action_skip,
                onClick = { onSelection(null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        if (contacts.isNotEmpty()) {
            CCOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = stringResource(R.string.onboarding_self_contact_search_label),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    filteredContacts,
                    key = { it.contact.id }) { contactWithBook ->
                    ListItem(
                        headlineContent = { Text(contactWithBook.contact.getEffectiveDisplayName()) },
                        leadingContent = {
                            ContactAvatar(
                                displayName = contactWithBook.contact.getEffectiveDisplayName(),
                                hasPhoto = contactWithBook.contact.hasPhoto,
                                id = contactWithBook.contact.id
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                null
                            )
                        },
                        modifier = Modifier.clickable { onSelection(contactWithBook.contact.id) }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    if (hasServerConnection) R.string.onboarding_self_contact_empty_syncing
                    else R.string.onboarding_self_contact_empty_local
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@ThemePreview
@Composable
private fun SetupStepPreview() {
    CorvidContactsTheme {
        SetupStep(
            initialAlwaysAdd = true,
            initialAddressMode = AddressLookupMode.PHOTON,
            initialBirthdayReminders = false,
            isSyncing = true,
            hasServerConnection = true,
            onComplete = { _, _, _ -> }
        )
    }
}

@PhonePreview
@Composable
private fun FinalizingSyncStepPreview() {
    CorvidContactsTheme {
        FinalizingSyncStep()
    }
}

@PhonePreview
@Composable
private fun LocalDataMigrationStepPreview() {
    CorvidContactsTheme {
        LocalDataMigrationStep(
            localBooks = listOf(
                AddressBookEntity(
                    href = "local://contacts",
                    displayName = "My Contacts",
                    colorInt = 0xFF349583.toInt()
                )
            ),
            isSubmitting = false,
            hasServerConnection = true,
            onSelection = { _, _ -> }
        )
    }
}


@PhonePreview
@Composable
private fun SelfContactSelectionPreview() {
    CorvidContactsTheme {
        SelfContactSelectionStep(
            contacts = emptyList(),
            hasServerConnection = true,
            onSelection = {},
            onCreateNew = {}
        )
    }
}
