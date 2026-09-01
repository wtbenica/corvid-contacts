// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScrollableColumn
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen
import dev.benica.corvidcontacts.ui.settings.SettingsHeader
import dev.benica.corvidcontacts.ui.settings.sections.AddressLookupSection
import dev.benica.corvidcontacts.ui.settings.sections.PhoneFormattingSection
import dev.benica.corvidcontacts.ui.settings.sections.ThemeSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.PhonePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

private enum class MigrationDecision { KEEP, UPLOAD, DISCARD }

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    geocoderRepository: GeocoderRepository?,
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val alwaysAddCountryCode by viewModel.alwaysAddCountryCode.collectAsState()
    val addressLookupMode by viewModel.addressLookupMode.collectAsState()
    val hasServerConnection by viewModel.hasServerConnection.collectAsState()
    val isBackgroundSyncing by viewModel.isBackgroundSyncing.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val hasBirthdays by viewModel.hasBirthdays.collectAsState()
    val currentBirthdayNotificationsEnabled by viewModel.currentBirthdayNotificationsEnabled.collectAsState()
    val isMigratingLocalData by viewModel.isMigratingLocalData.collectAsState()

    // Rendered standalone because it has its own scaffold.
    if (uiState is OnboardingUiState.CreatingSelfContact) {
        val addressBooks by viewModel.addressBooks.collectAsState()
        val allGroups by viewModel.allGroups.collectAsState()

        ContactEditScreen(
            contactWithBook = null,
            addressBooks = addressBooks,
            allGroups = allGroups,
            includeCountryCode = alwaysAddCountryCode,
            geocoderRepository = geocoderRepository,
            allContacts = contacts,
            onSave = { viewModel.saveNewSelfContact(it) },
            onBack = viewModel::cancelCreatingSelfContact
        )
        return
    }

    CCScaffold(
        title = stringResource(R.string.onboarding_title),
        topBarActions = {
            CCIconButton(
                icon = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = R.string.list_menu_logout,
                onClick = viewModel::logout,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        content = { padding ->
            CCWidthClampedBox(modifier = Modifier.padding(padding)) {
            val currentUiState = uiState
            val scrollState = rememberScrollState()
            // Only unscrolled steps need the outer Column scrollable; the list-based ones scroll themselves.
            val needsOuterScroll = currentUiState is OnboardingUiState.Setup ||
                currentUiState is OnboardingUiState.FinalizingSync ||
                currentUiState is OnboardingUiState.BirthdayNotifications

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (needsOuterScroll) Modifier.verticalScroll(scrollState) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (currentUiState) {
                    OnboardingUiState.Setup -> SetupStep(
                        themeMode = themeMode,
                        initialAlwaysAdd = alwaysAddCountryCode,
                        initialAddressMode = addressLookupMode,
                        isSyncing = isBackgroundSyncing,
                        hasServerConnection = hasServerConnection,
                        onThemeSelection = viewModel::setThemeMode,
                        onComplete = viewModel::saveSetupPreferences
                    )

                    OnboardingUiState.FinalizingSync -> FinalizingSyncStep()

                    is OnboardingUiState.LocalDataMigration -> LocalDataMigrationStep(
                        localBooks = currentUiState.localBooks,
                        isSubmitting = isMigratingLocalData,
                        hasServerConnection = hasServerConnection,
                        onSelection = viewModel::resolveLocalDataMigration
                    )

                    OnboardingUiState.BirthdayNotifications -> BirthdayNotificationsStep(
                        hasBirthdays = hasBirthdays,
                        currentlyEnabled = currentBirthdayNotificationsEnabled,
                        onSelection = viewModel::setBirthdayNotificationsEnabled
                    )

                    OnboardingUiState.SelfContactSelection -> SelfContactSelectionStep(
                        contacts = contacts,
                        hasServerConnection = hasServerConnection,
                        onSelection = viewModel::setSelfContact,
                        onCreateNew = viewModel::startCreatingSelfContact
                    )

                    // Handled above via early return - never reached, but kept here so this `when`
                    // stays exhaustive if that changes later.
                    OnboardingUiState.CreatingSelfContact -> Unit
                }
            }
            }
        }
    )
}

@Composable
private fun SetupStep(
    themeMode: ThemeMode,
    initialAlwaysAdd: Boolean,
    initialAddressMode: AddressLookupMode,
    isSyncing: Boolean,
    hasServerConnection: Boolean,
    onThemeSelection: (ThemeMode) -> Unit,
    onComplete: (alwaysAddCountryCode: Boolean, addressLookupMode: AddressLookupMode) -> Unit,
) {
    var selectedAlwaysAdd by remember(initialAlwaysAdd) { mutableStateOf(initialAlwaysAdd) }
    var selectedAddressMode by remember(initialAddressMode) { mutableStateOf(initialAddressMode) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.smSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.outerSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (hasServerConnection && isSyncing) {
                    stringResource(R.string.onboarding_setup_description)
                } else if (hasServerConnection) {
                    stringResource(R.string.onboarding_setup_description_complete)
                } else {
                    "" // Local only - no sync status needed
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Dimens.medSpacing))

        ThemeSection(themeMode = themeMode, onThemeModeSelected = onThemeSelection)

        PhoneFormattingSection(alwaysAddCountryCode = selectedAlwaysAdd) {
            selectedAlwaysAdd = it
        }

        AddressLookupSection(
            mode = selectedAddressMode,
            onModeSelected = { selectedAddressMode = it },
            uriHandler = uriHandler
        )

        CCButton(
            text = R.string.onboarding_action_continue,
            onClick = {
                onComplete(
                    selectedAlwaysAdd,
                    selectedAddressMode
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.lgSpacing)
        )
    }
}

@Composable
private fun FinalizingSyncStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Text(
            text = stringResource(R.string.onboarding_setup_syncing),
            style = MaterialTheme.typography.headlineSmall
        )
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
    val baseColor = currentThemeColor()
    val focusManager = LocalFocusManager.current

    var decisions by remember(localBooks) {
        mutableStateOf(localBooks.associateWith { MigrationDecision.KEEP })
    }
    var uploadNames by remember(localBooks) {
        mutableStateOf(localBooks.associate { it.href to (it.displayName ?: "") })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            Icons.Rounded.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = baseColor
        )
        Text(
            text = stringResource(R.string.onboarding_local_migration_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.onboarding_local_migration_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    Column(modifier = Modifier.padding(Dimens.outerSpacing)) {
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

        if (isSubmitting) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.onboarding_local_migration_uploading))
            }
        }

        Spacer(Modifier.height(16.dp))

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
}

@Composable
private fun BirthdayNotificationsStep(
    hasBirthdays: Boolean,
    currentlyEnabled: Boolean,
    onSelection: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onSelection(isGranted)
    }

    // Ask "keep this on?" if notifications were already enabled and permission is granted.
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    val alreadyEnabled = currentlyEnabled && permissionGranted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Rounded.Cake,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.onboarding_birthday_title),
            style = MaterialTheme.typography.headlineSmall
        )

        if (hasBirthdays) {
            Text(
                text = stringResource(R.string.onboarding_birthday_description_has_birthdays),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.onboarding_birthday_description_no_birthdays),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        if (alreadyEnabled) {
            CCButton(
                text = R.string.onboarding_action_keep_enabled,
                onClick = { onSelection(true) },
                modifier = Modifier.fillMaxWidth()
            )

            CCTextButton(
                text = R.string.onboarding_action_turn_off,
                onClick = { onSelection(false) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            CCButton(
                text = R.string.onboarding_action_enable_notifications,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onSelection(true)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            CCTextButton(
                text = R.string.onboarding_action_not_now,
                onClick = { onSelection(false) },
                modifier = Modifier.fillMaxWidth()
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.outerSpacing)
    ) {
        Text(
            text = stringResource(R.string.onboarding_self_contact_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.onboarding_self_contact_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
        }

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
}

@PhonePreview
@Composable
private fun SetupStepPreview() {
    CorvidContactsTheme {
        CCScrollableColumn(
            systemPadding = PaddingValues(vertical = Dimens.innerSpacing)
        ) {
            SetupStep(
                themeMode = ThemeMode.SYSTEM,
                initialAlwaysAdd = true,
                initialAddressMode = AddressLookupMode.PHOTON,
                isSyncing = true,
                hasServerConnection = true,
                onThemeSelection = {},
                onComplete = { _, _ -> }
            )
        }
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
private fun BirthdayNotificationsPreview() {
    CorvidContactsTheme {
        BirthdayNotificationsStep(
            hasBirthdays = true,
            currentlyEnabled = false,
            onSelection = {}
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
