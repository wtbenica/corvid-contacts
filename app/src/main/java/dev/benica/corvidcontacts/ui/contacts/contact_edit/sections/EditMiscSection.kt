// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.ExpandFieldsRow
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.contacts.common_ui.contactDatePickerColors
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableRelationship
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.AddGroupField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueFieldList
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueListController
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.RelationshipField
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Section for miscellaneous, less-common information: birthday, notes, relationships, and
 * groups. Groups stays visible since it's used often enough to warrant that; the rest starts
 * collapsed behind a single "Show more fields" toggle unless the contact already has data there.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun EditMiscSection(
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    categories: List<String>,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    allGroups: List<String>,
    relationshipController: MultiValueListController<EditableRelationship>,
    relationshipCandidates: List<ContactEntity>,
    isReadOnly: Boolean,
    currentThemeColor: Color,
    pendingFocusId: String?,
    onFocusRequested: (String?) -> Unit,
) {
    var moreFieldsExpanded by remember {
        mutableStateOf(
            birthday.isNotBlank() || notes.isNotBlank() || relationshipController.items.isNotEmpty()
        )
    }

    ContactSection(
        title = stringResource(R.string.edit_section_misc),
        content = {
            Section(
                title = stringResource(R.string.common_groups),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.Groups,
                itemSpacing = 0.dp
            ) {
                if (categories.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(category) },
                                trailingIcon = {
                                    if (!isReadOnly) {
                                        CCIconButton(
                                            icon = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            onClick = { onRemoveCategory(category) },
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                enabled = !isReadOnly
                            )
                        }
                    }
                }

                if (!isReadOnly) {
                    AddGroupField(
                        allGroups,
                        categories,
                        onAdd = onAddCategory
                    )
                }
            }

            var showDatePicker by remember { mutableStateOf(false) }

            if (moreFieldsExpanded && showDatePicker) {
                val initialDate = remember(birthday) {
                    try {
                        LocalDate
                            .parse(birthday)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    } catch (_: Exception) {
                        null
                    }
                }

                // Default to Jan 1st, 2000 if no date is set, to avoid "Today" for birthdays
                val defaultDisplayMonth = LocalDate
                    .of(
                        2000,
                        1,
                        1
                    )
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()

                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialDate,
                    initialDisplayedMonthMillis = initialDate ?: defaultDisplayMonth
                )

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
                                    onBirthdayChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                                }
                                showDatePicker = false
                            },
                            baseColor = currentThemeColor
                        )
                    },
                    dismissButton = {
                        Row {
                            CCTextButton(
                                text = R.string.edit_action_clear_birthday,
                                onClick = {
                                    onBirthdayChange("")
                                    showDatePicker = false
                                },
                                baseColor = currentThemeColor
                            )
                            CCTextButton(
                                text = android.R.string.cancel,
                                onClick = { showDatePicker = false },
                                baseColor = currentThemeColor
                            )
                        }
                    },
                    colors = contactDatePickerColors(currentThemeColor)
                ) {
                    DatePicker(
                        state = datePickerState,
                        headline = {
                            Text(
                                text = if (datePickerState.selectedDateMillis == null) {
                                    stringResource(R.string.common_birthday)
                                } else {
                                    val date = Instant
                                        .ofEpochMilli(datePickerState.selectedDateMillis!!)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                    date.format(
                                        DateTimeFormatter.ofLocalizedDate(
                                            FormatStyle.LONG
                                        )
                                    )
                                },
                                modifier = Modifier.padding(16.dp)
                            )
                        },
                        colors = contactDatePickerColors(currentThemeColor),
                        showModeToggle = false
                    )
                }
            }

            val focusManager = LocalFocusManager.current
            val displayBirthday = remember(birthday) {
                try {
                    val date = LocalDate.parse(birthday)
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                } catch (_: Exception) {
                    birthday
                }
            }

            if (moreFieldsExpanded) {
                Section(
                    title = stringResource(R.string.common_birthday),
                    modifier = Modifier.padding(vertical = Dimens.medSpacing),
                    icon = Icons.Outlined.Cake,
                    contentDescription = R.string.common_birthday,
                    itemSpacing = 0.dp,
                    headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                ) {
                    CCOutlinedTextField(
                        value = displayBirthday,
                        onValueChange = { },
                        label = stringResource(R.string.common_birthday),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused && !isReadOnly) {
                                    showDatePicker = true
                                    focusManager.clearFocus()
                                }
                            },
                        enabled = !isReadOnly,
                        trailingIcon = if (birthday.isNotBlank() && !isReadOnly) {
                            {
                                CCIconButton(
                                    icon = Icons.Outlined.Delete,
                                    contentDescription = R.string.edit_action_clear_birthday,
                                    onClick = { onBirthdayChange("") },
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else null,
                    )
                }

                Section(
                    title = stringResource(R.string.common_notes),
                    modifier = Modifier.padding(vertical = Dimens.medSpacing),
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = R.string.common_notes,
                    itemSpacing = 0.dp,
                    headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                ) {
                    CCOutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        label = stringResource(R.string.common_notes),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }

                Section(
                    title = stringResource(R.string.edit_section_relationships),
                    modifier = Modifier.padding(vertical = Dimens.medSpacing),
                    icon = Icons.Outlined.People,
                    itemSpacing = 0.dp,
                    headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                ) {
                    MultiValueFieldList(
                        controller = relationshipController,
                        isReadOnly = isReadOnly,
                        addLabel = stringResource(R.string.common_relationship),
                        pendingFocusId = pendingFocusId,
                        onFocusRequested = onFocusRequested,
                    ) { index, rel, onDelete, onMoveUp, onMoveDown, requestFocus ->
                        RelationshipField(
                            relationship = rel,
                            onRelationshipChange = {
                                relationshipController.update(
                                    index,
                                    it
                                )
                            },
                            onDelete = onDelete,
                            enabled = !isReadOnly,
                            contacts = relationshipCandidates,
                            onMoveUp = onMoveUp,
                            onMoveDown = onMoveDown,
                            requestInitialFocus = requestFocus
                        )
                    }
                }
            }

            ExpandFieldsRow(
                expanded = moreFieldsExpanded,
                onToggle = { moreFieldsExpanded = !moreFieldsExpanded },
            )
        }
    )
}

@ThemePreview
@Composable
fun EditMiscSectionPreview() {
    CorvidContactsTheme {
        EditMiscSection(
            birthday = "May 6, 1977",
            onBirthdayChange = {},
            notes = "Don't be naughty or else you might fall asleep!",
            onNotesChange = {},
            categories = listOf(
                "Three",
                "Two",
                "One"
            ),
            onAddCategory = {},
            onRemoveCategory = {},
            allGroups = listOf(
                "Three",
                "Two",
                "One",
                "Dumbass"
            ),
            relationshipController = MultiValueListController(
                items = remember { mutableStateListOf() },
                newItem = { EditableRelationship() },
                onItemAdded = {}
            ),
            relationshipCandidates = emptyList(),
            isReadOnly = false,
            currentThemeColor = ContactColors.palette[0],
            pendingFocusId = null,
            onFocusRequested = {},
        )
    }
}

