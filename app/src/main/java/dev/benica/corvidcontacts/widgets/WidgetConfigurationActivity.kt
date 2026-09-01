// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benica.corvidcontacts.CorvidContactsApplication
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.ui.contacts.ContactsViewModel
import dev.benica.corvidcontacts.ui.contacts.PickContent
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.contact_list.ContactListScreen
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The configuration gateway activity launched by the Android system when a user adds a new home-screen widget.
 * Coordinates a multi-step Jetpack Compose wizard (Contact selection -> Action selection) and persists
 * the settings before passing control back to the launcher.
 */
class WidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var resultValue: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default to canceled in case user abandons the flow early
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        resultValue = Intent().putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(
            RESULT_CANCELED,
            resultValue
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(
                "WidgetConfig",
                "Invalid appWidgetId, finishing activity"
            )
            finish()
            return
        }

        val app = application as CorvidContactsApplication

        setContent {
            CorvidContactsTheme {
                val contactsViewModel: ContactsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ContactsViewModel(
                                app.container.contactsRepository,
                                app.container.settingsRepository,
                                app.container.authRepository
                            ) as T
                        }
                    }
                )

                val configViewModel: WidgetConfigViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return WidgetConfigViewModel() as T
                        }
                    }
                )

                val configState by configViewModel.state.collectAsState()

                LaunchedEffect(Unit) {
                    contactsViewModel.startPickingExternal(PickContent.ALL)
                }

                when (configState.step) {
                    ConfigStep.SELECT_CONTACT -> {
                        ContactSelectionScreen(
                            contactsViewModel = contactsViewModel,
                            onContactSelected = { contact ->
                                configViewModel.setContact(contact)
                            },
                            onCancel = {
                                finish()
                            }
                        )
                    }

                    ConfigStep.SELECT_ACTION -> {
                        val contact = configState.selectedContact
                        var showConfirmationDialog by remember { mutableStateOf(false) }
                        var selectedAction by remember { mutableStateOf<WidgetAction?>(null) }

                        ActionSelectionScreen(
                            contactName = contact?.displayName ?: "",
                            onActionSelected = { action ->
                                selectedAction = action
                                showConfirmationDialog = true
                            },
                            onBack = {
                                configViewModel.backToContactSelection()
                            }
                        )

                        if (showConfirmationDialog && contact != null && selectedAction != null) {
                            if (selectedAction == WidgetAction.VIEW) {
                                saveConfiguration(
                                    contact,
                                    selectedAction!!,
                                    "" // No detail value needed for VIEW
                                )
                                showConfirmationDialog = false
                            } else {
                                val options = when (selectedAction) {
                                    WidgetAction.CALL, WidgetAction.TEXT -> {
                                        contact.phones?.map { it.value }
                                            ?: emptyList()
                                    }

                                    WidgetAction.EMAIL -> {
                                        contact.emails?.map { it.value }
                                            ?: emptyList()
                                    }

                                    WidgetAction.DIRECTIONS -> {
                                        contact.structuredAddresses?.mapNotNull { address ->
                                            address
                                                .toSingleLine()
                                                .takeIf { it.isNotBlank() }
                                        } ?: emptyList()
                                    }

                                    else -> {
                                        emptyList()
                                    }
                                }

                                if (options.isEmpty()) {
                                    // Show error or handle no data
                                    showConfirmationDialog = false
                                } else {
                                    DetailConfirmationDialog(
                                        action = selectedAction!!,
                                        options = options,
                                        onConfirm = { detailValue ->
                                            configViewModel.setAction(selectedAction!!)
                                            saveConfiguration(
                                                contact,
                                                selectedAction!!,
                                                detailValue
                                            )
                                            showConfirmationDialog = false
                                        },
                                        onDismiss = { showConfirmationDialog = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Persists the configuration into Glance DataStore and confirms placement to the system launcher.
     *
     * @param contact The chosen destination contact.
     * @param action The action the widget will perform (Call, Text, Email, Directions, or View).
     * @param detailValue The specific phone/email/address selected for that action.
     */
    private fun saveConfiguration(
        contact: ContactEntity?,
        action: WidgetAction?,
        detailValue: String,
    ) {
        if (contact != null && action != null) {
            lifecycleScope.launch {
                SingleContactWidget.saveConfig(
                    applicationContext,
                    appWidgetId,
                    contact.id,
                    action,
                    detailValue,
                    contact.colorInt
                )

                setResult(
                    RESULT_OK,
                    resultValue
                )

                finish()
            }
        } else {
            Log.e(
                "WidgetConfig",
                "saveConfiguration called with NULL contact or action"
            )
        }
    }
}

/** Wraps [ContactListScreen] in picker mode for the widget's contact-selection step. */
@Composable
private fun ContactSelectionScreen(
    contactsViewModel: ContactsViewModel,
    onContactSelected: (ContactEntity) -> Unit,
    onCancel: () -> Unit,
) {
    ContactListScreen(
        viewModel = contactsViewModel,
        onContactClick = { _ -> },
        onAddContact = { },
        onAddSelfContact = { },
        onSetUpSync = { },
        onSettingsClick = { },
        onContactSelected = { contactWithBook ->
            contactWithBook?.contact?.let {
                onContactSelected(
                    it
                )
            }
        },
        onCancelSelectingContact = onCancel,
        onClearSelectingContact = { },
        onShareSelf = { },
        onShareSelfViaQr = {}
    )
}

/** Screen for choosing which action (Call, Text, Email, Directions, View) the widget performs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSelectionScreen(
    contactName: String,
    onActionSelected: (WidgetAction) -> Unit,
    onBack: () -> Unit,
) {
    CCScaffold(
        title = stringResource(R.string.widget_config_select_action),
        navigationIcon = {
            BackNavButton(onBack)
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.innerSpacing),
                verticalArrangement = Arrangement.spacedBy(
                    Dimens.medSpacing,
                    Alignment.CenterVertically
                )
            ) {
                Text(
                    text = stringResource(
                        R.string.widget_config_action_prompt,
                        contactName
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = Dimens.smSpacing)
                )

                ActionButton(
                    text = stringResource(R.string.widget_action_call),
                    onClick = { onActionSelected(WidgetAction.CALL) }
                )

                ActionButton(
                    text = stringResource(R.string.widget_action_text),
                    onClick = { onActionSelected(WidgetAction.TEXT) }
                )

                ActionButton(
                    text = stringResource(R.string.widget_action_email),
                    onClick = { onActionSelected(WidgetAction.EMAIL) }
                )

                ActionButton(
                    text = stringResource(R.string.widget_action_directions),
                    onClick = { onActionSelected(WidgetAction.DIRECTIONS) }
                )

                ActionButton(
                    text = stringResource(R.string.widget_action_view),
                    onClick = { onActionSelected(WidgetAction.VIEW) }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailConfirmationDialog(
    action: WidgetAction,
    options: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOption by remember { mutableStateOf(options.firstOrNull() ?: "") }

    val labelRes = when (action) {
        WidgetAction.CALL -> R.string.common_phone
        WidgetAction.TEXT -> R.string.common_phone
        WidgetAction.EMAIL -> R.string.common_email
        WidgetAction.DIRECTIONS -> R.string.common_address
        WidgetAction.VIEW -> R.string.detail_title // Should not be reachable
    }

    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.widget_confirm_title,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.smSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
            ) {
                Text(
                    text = stringResource(
                        R.string.widget_config_select_detail,
                        stringResource(labelRes).lowercase()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (options.size > 1) {
                    Column(Modifier.selectableGroup()) {
                        options.forEach { text ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = (text == selectedOption),
                                        onClick = { selectedOption = text },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = Dimens.smSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (text == selectedOption),
                                    onClick = null // null recommended for accessibility with selectable modifier
                                )
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = Dimens.smSpacing)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = selectedOption,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = Dimens.smSpacing)
                    )
                }
            }
        },
        confirmButton = R.string.widget_confirm_save,
        onConfirm = { onConfirm(selectedOption) },
        dismissButton = R.string.action_cancel,
    )
}

/** A full-width button for one of the wizard's action choices. */
@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
) {
    CCButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text)
    }
}

/** Tracks the widget-creation wizard's current step and the choices made so far. */
class WidgetConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConfigState())

    val state: StateFlow<ConfigState> = _state.asStateFlow()

    /**
     * Records the user's selected contact choice and automatically bumps the state forward
     * to the action picking step.
     */
    fun setContact(contact: ContactEntity) {
        _state.value = _state.value.copy(
            selectedContact = contact,
            step = ConfigStep.SELECT_ACTION
        )
    }

    /**
     * Commits the requested shortcut action intent to state.
     */
    fun setAction(action: WidgetAction) {
        _state.value = _state.value.copy(selectedAction = action)
    }

    /**
     * Shifts state backward to allow the user to modify their selected contact.
     */
    fun backToContactSelection() {
        _state.value = _state.value.copy(step = ConfigStep.SELECT_CONTACT)
    }
}

/**
 * State for the widget-creation wizard.
 *
 * @property step The current wizard step.
 * @property selectedContact The contact chosen in the contact-selection step, if any.
 * @property selectedAction The action chosen in the action-selection step, if any.
 */
data class ConfigState(
    val step: ConfigStep = ConfigStep.SELECT_CONTACT,
    val selectedContact: ContactEntity? = null,
    val selectedAction: WidgetAction? = null,
)

/** The creation wizard's screen steps. */
enum class ConfigStep {
    /** Phase 1: Contact filtering and search picker. */
    SELECT_CONTACT,

    /** Phase 2: Action assignment screen. */
    SELECT_ACTION
}

@Preview
@Composable
fun WidgetConfigPreview() {
    CorvidContactsTheme {
        ActionSelectionScreen(
            contactName = "John Doe",
            onActionSelected = {},
            onBack = {}
        )
    }
}