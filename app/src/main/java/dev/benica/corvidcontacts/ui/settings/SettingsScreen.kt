// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.ThemeMode
import dev.benica.corvidcontacts.data.repository.ImportResult
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScrollableColumn
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.common_ui.SecondaryHeader
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.CreateAddressBookDialog
import dev.benica.corvidcontacts.ui.settings.sections.AccountSection
import dev.benica.corvidcontacts.ui.settings.sections.AddressLookupSection
import dev.benica.corvidcontacts.ui.settings.sections.DataManagementSection
import dev.benica.corvidcontacts.ui.settings.sections.ExternalPhotosSection
import dev.benica.corvidcontacts.ui.settings.sections.PhoneFormattingSection
import dev.benica.corvidcontacts.ui.settings.sections.ThemeSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val alwaysAddCountryCode by viewModel.alwaysAddCountryCode.collectAsState()
    val addressLookupMode by viewModel.addressLookupMode.collectAsState()
    val autoLoadRemotePhotos by viewModel.autoLoadRemotePhotos.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val addressBooks by viewModel.addressBooks.collectAsState()

    SettingsScreen(
        serverUrl = serverUrl,
        username = username,
        alwaysAddCountryCode = alwaysAddCountryCode,
        addressLookupMode = addressLookupMode,
        autoLoadRemotePhotos = autoLoadRemotePhotos,
        themeMode = themeMode,
        addressBooks = addressBooks,
        onAlwaysAddCountryCodeToggled = { alwaysAdd -> viewModel.setAlwaysAddCountryCode(alwaysAdd) },
        onAddressLookupModeSelected = { mode -> viewModel.setAddressLookupMode(mode) },
        onAutoLoadRemotePhotosToggled = { autoLoad -> viewModel.setAutoLoadRemotePhotos(autoLoad) },
        onThemeModeSelected = { theme -> viewModel.setThemeMode(theme) },
        onLogout = { viewModel.logout() },
        onExport = { viewModel.getExportData() },
        onCheckImportHasRemotePhotos = { text -> viewModel.importFileHasRemotePhotos(text) },
        onImport = { text, href, downloadRemotePhotos ->
            viewModel.importContacts(text, href, downloadRemotePhotos)
        },
        onCreateAddressBook = { name, color, forceLocal -> viewModel.createAddressBook(name, color, forceLocal) },
        onResetOnboarding = { viewModel.resetOnboarding() },
        onAboutClick = onAboutClick,
        onBack = onBack,
        modifier = modifier,
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    serverUrl: String?,
    username: String?,
    alwaysAddCountryCode: Boolean,
    addressLookupMode: AddressLookupMode,
    autoLoadRemotePhotos: Boolean,
    themeMode: ThemeMode,
    addressBooks: List<AddressBookEntity>,
    onAlwaysAddCountryCodeToggled: (Boolean) -> Unit,
    onAddressLookupModeSelected: (AddressLookupMode) -> Unit,
    onAutoLoadRemotePhotosToggled: (Boolean) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onExport: suspend () -> String,
    onCheckImportHasRemotePhotos: (String) -> Boolean,
    onImport: suspend (String, String, Boolean) -> ImportResult,
    onCreateAddressBook: suspend (String, Color, Boolean) -> Result<AddressBookEntity>,
    onResetOnboarding: () -> Unit,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var showResetOnboardingDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = onExport()
                context.contentResolver
                    .openOutputStream(uri)
                    ?.use { outputStream ->
                        outputStream.write(data.toByteArray())
                    }
            }
        }
    }

    // Import flow: read the picked file, then (if it references any online-hosted photos)
    // ask once whether to fetch them, then resolve a destination address book before actually
    // importing - skipping the picker entirely when there's only one book already.
    var pendingImportText by remember { mutableStateOf<String?>(null) }
    var pendingDownloadRemotePhotos by remember { mutableStateOf(false) }
    var showRemotePhotoPrompt by remember { mutableStateOf(false) }
    var showPickBookDialog by remember { mutableStateOf(false) }
    var showCreateBookDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var createBookError by remember { mutableStateOf(false) }

    fun runImport(text: String, targetHref: String, downloadRemotePhotos: Boolean) {
        scope.launch {
            isBusy = true
            importResult = onImport(text, targetHref, downloadRemotePhotos)
            isBusy = false
            pendingImportText = null
        }
    }

    fun resolveImportDestination(text: String) {
        when {
            addressBooks.isEmpty() -> showCreateBookDialog = true
            addressBooks.size == 1 ->
                runImport(text, addressBooks.first().href, pendingDownloadRemotePhotos)

            else -> showPickBookDialog = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                if (text != null) {
                    pendingImportText = text
                    if (onCheckImportHasRemotePhotos(text)) {
                        showRemotePhotoPrompt = true
                    } else {
                        pendingDownloadRemotePhotos = autoLoadRemotePhotos
                        resolveImportDestination(text)
                    }
                }
            }
        }
    }

    val chromeTitle = stringResource(R.string.common_settings)
    val chromeNavigationIcon: @Composable () -> Unit = { BackNavButton(onBack) }

    if (!showScaffold) {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                )
            )
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 840.dp

            val accountThemeAndData: @Composable () -> Unit = {
                if (serverUrl != null && username != null) {
                    AccountSection(serverUrl, username) {
                        showLogoutDialog = true
                    }
                }

                ThemeSection(themeMode, onThemeModeSelected)

                DataManagementSection(exportLauncher, importLauncher, onAboutClick) {
                    showResetOnboardingDialog = true
                }
            }

            val phoneAddressAndPhotos: @Composable () -> Unit = {
                PhoneFormattingSection(alwaysAddCountryCode, onAlwaysAddCountryCodeToggled)

                AddressLookupSection(
                    mode = addressLookupMode,
                    onModeSelected = onAddressLookupModeSelected,
                    uriHandler = uriHandler
                )

                ExternalPhotosSection(
                    autoLoadRemotePhotos = autoLoadRemotePhotos,
                    onAutoLoadRemotePhotosToggled = onAutoLoadRemotePhotosToggled
                )
            }

            CCWidthClampedBox(
                maxWidth = if (wideLayout) Dp.Unspecified else 600.dp,
            ) {
                CCScrollableColumn(
                    systemPadding = padding,
                    padding = PaddingValues(vertical = Dimens.xxlSpacing),
                ) {
                    if (wideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Spacer(modifier = Modifier.weight(0.05f))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                            ) {
                                accountThemeAndData()
                            }

                            Spacer(modifier = Modifier.weight(0.05f))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                            ) {
                                phoneAddressAndPhotos()
                            }

                            Spacer(modifier = Modifier.weight(0.05f))
                        }
                    } else {
                        accountThemeAndData()
                        phoneAddressAndPhotos()
                    }

                    // Reset Onboarding Confirmation Dialog
                    if (showResetOnboardingDialog) {
                        CCAlertDialog(
                            onDismissRequest = { showResetOnboardingDialog = false },
                            title = R.string.settings_reset_onboarding_title,
                            content = {
                                Text(
                                    stringResource(R.string.settings_reset_onboarding_description),
                                )
                            },
                            confirmButton = R.string.settings_reset_onboarding_title,
                            onConfirm = {
                                onResetOnboarding()
                                showResetOnboardingDialog = false
                            },
                            dismissButton = R.string.action_cancel,
                        )
                    }

                    // Logout Confirmation Dialog
                    if (showLogoutDialog) {
                        CCAlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = R.string.logout_confirm_title,
                            content = {
                                Text(stringResource(R.string.logout_confirm_message))
                            },
                            confirmButton = R.string.list_menu_logout,
                            onConfirm = {
                                onLogout()
                                showLogoutDialog = false
                            },
                            dismissButton = R.string.action_cancel,
                        )
                    }

                    // Import: this file has online-hosted photos - ask once whether to fetch them
                    if (showRemotePhotoPrompt) {
                        CCAlertDialog(
                            onDismissRequest = {
                                showRemotePhotoPrompt = false
                                pendingDownloadRemotePhotos = false
                                onAutoLoadRemotePhotosToggled(false)
                                pendingImportText?.let { resolveImportDestination(it) }
                            },
                            title = R.string.settings_import_remote_photos_title,
                            content = {
                                Text(stringResource(R.string.settings_import_remote_photos_description))
                            },
                            confirmButton = R.string.settings_import_remote_photos_download,
                            onConfirm = {
                                showRemotePhotoPrompt = false
                                pendingDownloadRemotePhotos = true
                                onAutoLoadRemotePhotosToggled(true)
                                pendingImportText?.let { resolveImportDestination(it) }
                            },
                            dismissButton = R.string.settings_import_remote_photos_skip,
                        )
                    }

                    // Import: choose a destination address book
                    if (showPickBookDialog) {
                        CCAlertDialog(
                            onDismissRequest = {
                                showPickBookDialog = false
                                pendingImportText = null
                            },
                            title = R.string.settings_import_pick_book_title,
                            content = {
                                Column {
                                    addressBooks.forEach { book ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    book.displayName
                                                        ?: stringResource(R.string.settings_address_book_unnamed)
                                                )
                                            },
                                            supportingContent = if (book.isLocal) {
                                                { Text(stringResource(R.string.settings_address_book_local_badge)) }
                                            } else null,
                                            modifier = Modifier.clickable {
                                                showPickBookDialog = false
                                                pendingImportText?.let {
                                                    runImport(it, book.href, pendingDownloadRemotePhotos)
                                                }
                                            }
                                        )
                                    }
                                    ListItem(
                                        headlineContent = {
                                            Text(stringResource(R.string.settings_import_create_new_book))
                                        },
                                        modifier = Modifier.clickable {
                                            showPickBookDialog = false
                                            showCreateBookDialog = true
                                        }
                                    )
                                }
                            },
                            confirmButton = R.string.action_cancel,
                            onConfirm = {
                                showPickBookDialog = false
                                pendingImportText = null
                            },
                        )
                    }

                    // Import: create a new destination address book on the spot
                    if (showCreateBookDialog) {
                        CreateAddressBookDialog(
                            isSubmitting = isBusy,
                            hasServerConnection = serverUrl != null,
                            onConfirm = { name, color, forceLocal ->
                                scope.launch {
                                    isBusy = true
                                    val result = onCreateAddressBook(name, color, forceLocal)
                                    isBusy = false
                                    result.onSuccess { book ->
                                        showCreateBookDialog = false
                                        pendingImportText?.let {
                                            runImport(it, book.href, pendingDownloadRemotePhotos)
                                        }
                                    }.onFailure {
                                        createBookError = true
                                    }
                                }
                            },
                            onDismiss = {
                                showCreateBookDialog = false
                                pendingImportText = null
                            }
                        )
                    }

                    if (createBookError) {
                        CCAlertDialog(
                            onDismissRequest = { createBookError = false },
                            title = R.string.common_error_unknown,
                            content = { Text(stringResource(R.string.settings_address_book_generic_error)) },
                            confirmButton = R.string.ok,
                            onConfirm = { createBookError = false },
                        )
                    }

                    // Import: result summary
                    importResult?.let { result ->
                        CCAlertDialog(
                            onDismissRequest = { importResult = null },
                            title = R.string.settings_import_result_title,
                            content = {
                                val message = if (result.failed == 0) {
                                    pluralStringResource(
                                        R.plurals.settings_import_result_full,
                                        result.imported,
                                        result.imported
                                    )
                                } else {
                                    val total = result.imported + result.failed
                                    pluralStringResource(
                                        R.plurals.settings_import_result_partial,
                                        total,
                                        result.imported,
                                        total,
                                        result.failed
                                    )
                                }
                                Text(message)
                            },
                            confirmButton = R.string.ok,
                            onConfirm = { importResult = null },
                        )
                    }
                }
            }
        }
    }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            title = chromeTitle,
            navigationIcon = chromeNavigationIcon,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}

@Composable
fun SettingsHeader(title: String, icon: ImageVector? = null) {
    SecondaryHeader(
        title = title,
        icon = icon,
        textStyle = MaterialTheme.typography.labelLarge
    )
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        SettingsHeader(
            title = title,
            icon = icon
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.lgSpacing)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsLeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SettingsLeadingRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
    }
}

@ThemePreview
@Composable
fun SettingsScreenPreview() {
    CorvidContactsTheme {
        SettingsScreen(
            serverUrl = "https://cloud.example.com",
            username = "user",
            alwaysAddCountryCode = true,
            addressLookupMode = AddressLookupMode.PHOTON,
            autoLoadRemotePhotos = false,
            themeMode = ThemeMode.SYSTEM,
            addressBooks = emptyList(),
            onAlwaysAddCountryCodeToggled = { },
            onAddressLookupModeSelected = {},
            onAutoLoadRemotePhotosToggled = {},
            onThemeModeSelected = {},
            onLogout = {},
            onExport = { "potato salad" },
            onCheckImportHasRemotePhotos = { false },
            onImport = { _, _, _ -> ImportResult(imported = 0, failed = 0) },
            onCreateAddressBook = { _, _, _ -> Result.success(AddressBookEntity(href = "", displayName = null, colorInt = 0)) },
            onResetOnboarding = {},
            onAboutClick = {},
            onBack = {},
        )
    }
}
