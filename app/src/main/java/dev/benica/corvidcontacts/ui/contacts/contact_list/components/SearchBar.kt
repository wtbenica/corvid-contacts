// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.extensions.active
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.contactTextFieldColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * Bottom navigation bar for searching and filtering contacts.
 *
 * @param query The current search string.
 * @param onQueryChange Callback when the search text changes.
 * @param onFilterClick Callback to open the filter bottom sheet.
 * @param selectedBook The currently selected address book (if only one is active).
 * @param modifier Modifier for the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null,
    selectedBook: AddressBookEntity? = null,
) {
    val baseColor = currentThemeColor()
    val focusManager = LocalFocusManager.current
    val mod = if (modifier == Modifier) modifier.fillMaxWidth() else modifier
    Box {
        Row(
            modifier = mod
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Search Bar with custom trailing shape
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f),
                placeholder = {
                    Text(
                        stringResource(R.string.list_search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        CCIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = R.string.list_search_clear,
                            onClick = { onQueryChange("") }
                        )
                    }
                } else null,
                shape = if (onFilterClick != null) {
                    MaterialTheme.shapes.medium.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    )
                } else {
                    MaterialTheme.shapes.medium
                },
                singleLine = true,
                colors = contactTextFieldColors(baseColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            // Filter Button
            if (onFilterClick != null) {
                Surface(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .height(56.dp)
                        .offset(x = (-1).dp), // Match OutlinedTextField height
                    shape = MaterialTheme.shapes.medium.copy(
                        topStart = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp)
                    ),
                    color = if (isDarkTheme()) baseColor.surface() else baseColor.copy(alpha = 0.1f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDarkTheme()) baseColor.surfaceVariant() else baseColor.copy(
                            alpha = 0.25f
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (selectedBook == null) {
                            Icons.Rounded.FilterList
                        } else {
                            ContactColors.getIconForAddressBook(
                                displayName = selectedBook.displayName,
                                iconName = selectedBook.iconName
                            )
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.list_filter_button_description),
                            modifier = Modifier.size(32.dp),
                            tint = baseColor.active()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding the currently selected contacts to a group.
 *
 * Provides suggestions based on existing groups.
 *
 * @param allGroups List of all existing group names for suggestions.
 * @param onConfirm Callback when a group name is submitted.
 * @param onDismissRequest Callback to close the dialog without action.
 */
@Composable
fun AddToGroupDialog(
    allGroups: List<String>,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    fun submit() {
        if (text.isNotBlank()) onConfirm(text.trim())
    }

    CCAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
            ) {
                Text(stringResource(R.string.list_group_dialog_description))
                Box(modifier = Modifier.fillMaxWidth()) {
                    val filtered = allGroups
                        .asSequence()
                        .filter {
                            it.contains(
                                text,
                                ignoreCase = true
                            ) && !it.equals(
                                ContactsRepository.FAVORITE_CATEGORY,
                                ignoreCase = true
                            )
                        }
                        .take(5)
                        .toList()

                    CCExposedDropdownMenuBox(
                        textBoxLabel = stringResource(R.string.list_group_name_label),
                        currentValue = text,
                        onValueChange = {
                            text = it
                            expanded = it.isNotEmpty()
                        },
                        expanded = expanded && filtered.isNotEmpty(),
                        onExpandedChange = { expanded = it },
                        readOnly = false,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    ) {
                        filtered.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    text = g
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = R.string.list_group_action_add,
        onConfirm = { submit() },
        confirmEnabled = text.isNotBlank(),
        dismissButton = R.string.list_group_action_cancel,
        title = R.string.list_group_dialog_title,
    )
}

@ThemePreview
@Composable
fun SearchBarPreview() {
    CorvidContactsTheme {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            SearchBar(
                query = "",
                onQueryChange = { _ -> },
                onFilterClick = {}
            )
        }
    }
}
