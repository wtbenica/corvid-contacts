// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.contactDatePickerColors
import dev.benica.corvidcontacts.ui.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A birthday input field that opens a date picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainBirthdayField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate
                .parse(value)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                CCTextButton(
                    text = android.R.string.ok,
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onValueChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showDatePicker = false
                    }
                )
            },
            dismissButton = {
                CCTextButton(
                    text = android.R.string.cancel,
                    onClick = { showDatePicker = false }
                )
            },
            colors = contactDatePickerColors()
        ) {
            DatePicker(
                state = datePickerState,
                colors = contactDatePickerColors()
            )
        }
    }

    val focusManager = LocalFocusManager.current
    val displayBirthday = remember(value) {
        try {
            LocalDate
                .parse(value)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        } catch (_: Exception) {
            value
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CCIconButton(
            icon = Icons.Rounded.Cake,
            contentDescription = R.string.common_birthday,
            onClick = { showDatePicker = true },
            modifier = Modifier.size(40.dp),
        )

        CCOutlinedTextField(
            value = displayBirthday,
            onValueChange = { },
            label = stringResource(R.string.common_birthday),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    if (it.isFocused) {
                        showDatePicker = true
                        focusManager.clearFocus()
                    }
                },
            trailingIcon = if (value.isNotBlank()) {
                {
                    CCIconButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = R.string.action_remove,
                        onClick = { onValueChange("") },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else null,
        )
    }
}
