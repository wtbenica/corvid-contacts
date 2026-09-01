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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.theme.Dimens
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

/**
 * A customized Composable field specialized for capturing telephone contact metadata.
 * Includes a country code selector with flag emojis and smart paste detection for international numbers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneValueField(
    value: String,
    region: String?,
    onUpdate: (value: String, region: String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    types: List<VCardType>,
    onDelete: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    requestInitialFocus: Boolean = false,
) {
    var countryExpanded by remember { mutableStateOf(false) }
    var showAllCountries by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val countries = remember { PhoneFormatter.getCountries(context) }
    val selectedCountry =
        remember(region) {
            countries.find { it.code == region } ?: countries.find { it.code == "US" }
        }

    val initialFocusModifier = Modifier.rememberInitialFocusModifier(requestInitialFocus)

    val priorityCountries = remember {
        listOf(
            "US",
            "CA",
            "MX"
        ).mapNotNull { code -> countries.find { it.code == code } }
    }

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

        // Row 2: Country Selector and Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            // Country Selector
            CCExposedDropdownMenuBox(
                textBoxLabel = stringResource(R.string.common_country),
                currentValue = "${selectedCountry?.flag ?: ""} ${selectedCountry?.dialCode ?: "+1"}",
                expanded = countryExpanded && enabled,
                onExpandedChange = { if (enabled) countryExpanded = it },
                enabled = enabled,
                modifier = Modifier.weight(0.35f)
            ) {
                if (!showAllCountries) {
                    priorityCountries.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flag ?: ""} ${country.name} (${country.dialCode})") },
                            onClick = {
                                // We always want national format in this text field because
                                // the country code is shown in the dropdown.
                                val formatted =
                                    PhoneFormatter.format(
                                        phone = value,
                                        includeCountryCode = false,
                                        context = context,
                                        region = country.code
                                    )
                                onUpdate(formatted, country.code)
                                countryExpanded = false
                            })
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_action_more_countries)) },
                        onClick = { showAllCountries = true }
                    )
                } else {
                    countries.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flag ?: ""} ${country.name} (${country.dialCode})") },
                            onClick = {
                                val formatted =
                                    PhoneFormatter.format(
                                        value,
                                        false,
                                        context,
                                        country.code
                                    )
                                onUpdate(formatted, country.code)
                                countryExpanded = false
                                showAllCountries = false
                            })
                    }
                }
            }

            // Phone Number Input
            CCOutlinedTextField(
                value = TextFieldValue(
                    text = value,
                    selection = TextRange(value.length)
                ),
                onValueChange = { fieldValue ->
                    val text = fieldValue.text
                    val cleaned =
                        text.filter { c -> c.isDigit() || c == '+' || c == '*' || c == '#' }
                    var activeRegion = region ?: "US"
                    var textToFormat = text

                    // Smart region detection: if user types '+', try to move it to the dropdown
                    if (cleaned.startsWith("+")) {
                        try {
                            val phoneUtil = PhoneNumberUtil.createInstance(context)

                            // Check for 1, 2, and 3 digit country codes
                            for (len in 1..3) {
                                if (cleaned.length > len) {
                                    val potCodeStr = cleaned.substring(1, 1 + len)
                                    val potCode = potCodeStr.toIntOrNull() ?: continue
                                    val detectedRegion =
                                        phoneUtil.getRegionCodeForCountryCode(potCode)

                                    if (detectedRegion != "ZZ") {
                                        // If it's a valid code, switch the dropdown
                                        activeRegion = detectedRegion

                                        // Use national significant number to strip trunk prefix
                                        val subNumber = cleaned.substring(1 + len)
                                        try {
                                            val numberProto =
                                                phoneUtil.parse(cleaned, detectedRegion)
                                            textToFormat =
                                                phoneUtil.getNationalSignificantNumber(numberProto)
                                        } catch (_: Exception) {
                                            textToFormat = subNumber
                                        }
                                        break
                                    }
                                }
                            }

                            // If we have a fairly long string, try a full parse to be sure
                            if (cleaned.length > 5) {
                                val numberProto = phoneUtil.parse(cleaned, null)
                                val exactRegion = phoneUtil.getRegionCodeForNumber(numberProto)
                                if (exactRegion != null) {
                                    activeRegion = exactRegion
                                    // Extract the national significant number (strips trunk prefix like '0')
                                    textToFormat =
                                        phoneUtil.getNationalSignificantNumber(numberProto)
                                }
                            }
                        } catch (_: Exception) {
                            // Partial/invalid prefix, continue with standard formatting
                        }
                    }

                    // We format for NATIONAL display here because the country code is already
                    // visible in the dropdown. We use significantOnly = true to strip trunk
                    // prefixes (like the UK '0') that aren't used with country code.
                    val formatted = PhoneFormatter.format(
                        phone = textToFormat,
                        includeCountryCode = false,
                        context = context,
                        region = activeRegion,
                        significantOnly = true
                    )
                    onUpdate(formatted, activeRegion)
                },
                label = stringResource(R.string.common_phone),
                modifier = Modifier
                    .weight(0.65f)
                    .then(initialFocusModifier),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
    }
}

@Preview
@Composable
fun PhoneValueFieldPreview() {
    PhoneValueField(
        value = "555-555-5555",
        region = "US",
        onUpdate = { _, _ -> },
        type = "HOME",
        onTypeChange = { _ -> },
        types = emptyList(),
        onDelete = { },
        enabled = true
    )
}
