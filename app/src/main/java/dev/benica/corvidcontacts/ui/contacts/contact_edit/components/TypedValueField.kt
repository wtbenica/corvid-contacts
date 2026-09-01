// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.data.repository.AddressSuggestion
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructuredAddressField(
    address: StructuredAddress,
    geocoderRepository: GeocoderRepository?,
    onAddressChange: (StructuredAddress) -> Unit,
    onDelete: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    requestInitialFocus: Boolean = false,
) {
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isProgrammaticChange by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var hasInteracted by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val initialFocusModifier = Modifier.rememberInitialFocusModifier(requestInitialFocus)

    // Use the street field for autocomplete
    val query = address.street ?: ""
    LaunchedEffect(
        query,
        isFocused
    ) {
        if (!isFocused || isProgrammaticChange) {
            if (isProgrammaticChange) isProgrammaticChange = false
            suggestions = emptyList()
            return@LaunchedEffect
        }

        if (!hasInteracted && query.isNotEmpty()) {
            return@LaunchedEffect
        }

        // Show suggestions again if query changes while focused
        showSuggestions = true

        if (geocoderRepository != null && query.length >= 3) {
            delay(500.milliseconds) // Debounce
            isSearching = true
            suggestions = try {
                geocoderRepository.getAutocompleteSuggestions(query)
            } catch (_: Exception) {
                emptyList()
            } finally {
                isSearching = false
            }
        } else {
            suggestions = emptyList()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
    ) {
        TypedFieldControlsRow(
            type = address.type ?: "HOME",
            onTypeChange = { onAddressChange(address.copy(type = it)) },
            types = VCardType.commonAddressTypes,
            enabled = enabled,
            onDelete = onDelete,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )

        CCExposedDropdownMenuBox(
            textBoxLabel = stringResource(R.string.common_street),
            currentValue = address.street ?: "",
            expanded = showSuggestions && suggestions.isNotEmpty() && enabled,
            onExpandedChange = { showSuggestions = it },
            onValueChange = {
                hasInteracted = true
                onAddressChange(address.copy(street = it))
            },
            readOnly = false,
            enabled = enabled,
            showTrailingIcon = false,
            textFieldModifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .then(initialFocusModifier),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                suggestion.displayTitle,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (suggestion.displaySubtitle.isNotBlank()) {
                                Text(
                                    suggestion.displaySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        scope.launch {
                            isProgrammaticChange = true
                            val resolved = geocoderRepository?.resolveSuggestion(suggestion)
                            if (resolved != null) {
                                onAddressChange(resolved.copy(type = address.type))
                            }
                            suggestions = emptyList()
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
        ) {
            CCOutlinedTextField(
                value = address.city ?: "",
                onValueChange = { onAddressChange(address.copy(city = it)) },
                label = stringResource(R.string.common_city),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            CCOutlinedTextField(
                value = address.state ?: "",
                onValueChange = { onAddressChange(address.copy(state = it)) },
                label = stringResource(R.string.common_state_province),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
        ) {
            CCOutlinedTextField(
                value = address.postalCode ?: "",
                onValueChange = { onAddressChange(address.copy(postalCode = it)) },
                label = stringResource(R.string.common_postal_code),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            CCOutlinedTextField(
                value = address.country ?: "",
                onValueChange = { onAddressChange(address.copy(country = it)) },
                label = stringResource(R.string.common_country),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypedValueField(
    value: String,
    onValueChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    types: List<VCardType>,
    label: String,
    onDelete: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    requestInitialFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val initialFocusModifier = Modifier.rememberInitialFocusModifier(requestInitialFocus)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
    ) {
        TypedFieldControlsRow(
            type = type,
            onTypeChange = onTypeChange,
            types = types,
            enabled = enabled,
            onDelete = onDelete,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )

        // Row 2: Value
        CCOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .then(initialFocusModifier),
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@ThemePreview
@Composable
fun TypedValueFieldsPreviews() {
    CorvidContactsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Dimens.xxlSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
            ) {
                StructuredAddressField(
                    address = StructuredAddress(
                        street = "123 Main St",
                        city = "Anytown",
                        state = "CA",
                        postalCode = "12345",
                        country = "USA"
                    ),
                    geocoderRepository = null,
                    onAddressChange = {},
                    onDelete = {},
                    enabled = true,
                    onMoveDown = {},
                )

                PhoneValueField(
                    value = "123-456-7890",
                    region = "US",
                    onUpdate = { _, _ -> },
                    type = "CELL",
                    onTypeChange = {},
                    types = VCardType.commonPhoneTypes,
                    onDelete = {},
                    enabled = true,
                    onMoveUp = {},
                    onMoveDown = {}
                )

                TypedValueField(
                    value = "john@example.com",
                    onValueChange = {},
                    type = "HOME",
                    onTypeChange = {},
                    types = VCardType.commonEmailTypes,
                    label = "Email",
                    onDelete = {},
                    enabled = true,
                    onMoveUp = {}
                )
            }
        }
    }
}
