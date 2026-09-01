// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.ExpandFieldsRow
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.ContactAvatarPicker
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * Section for editing contact identity information like name, photo, and address book.
 */
@Composable
fun EditIdentitySection(
    wideScreen: Boolean,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    prefix: String,
    onPrefixChange: (String) -> Unit,
    middleName: String,
    onMiddleNameChange: (String) -> Unit,
    suffix: String,
    onSuffixChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    photoUrl: String?,
    onPhotoUrlChange: (String?) -> Unit,
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    currentBookColor: Color,
    stableColorSeed: String,
    isReadOnly: Boolean,
    addressBooks: List<AddressBookEntity>,
    onSelectedAddressBookChange: (String?) -> Unit,
    effectiveAddressBookHref: String?,
    displayNameError: String?,
    photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
) {
    // Defaults to closed unless one of the hidden fields has a value
    var moreFieldsExpanded by remember {
        mutableStateOf(
            displayName.isNotBlank() || prefix.isNotBlank() || middleName.isNotBlank() || suffix.isNotBlank()
        )
    }
    // If displayNameError is set, expand the fields so error shows
    LaunchedEffect(displayNameError) {
        if (displayNameError != null) moreFieldsExpanded = true
    }

    ContactSection(
        title = stringResource(R.string.edit_section_identity),
    ) {
        if (wideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)
                ) {
                    Section(
                        title = stringResource(R.string.edit_section_name_photo),
                        modifier = Modifier.padding(vertical = Dimens.medSpacing),
                        icon = Icons.Outlined.AccountBox,
                        itemSpacing = Dimens.medSpacing
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ContactAvatarPicker(
                                selectedColor = selectedColor,
                                displayName = displayName.ifBlank { "$firstName $lastName" },
                                defaultColor = currentBookColor,
                                photoUrl = photoUrl,
                                onColorSelected = onColorSelected,
                                onPhotoClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemovePhoto = {
                                    onPhotoUrlChange(null)
                                },
                                enabled = !isReadOnly,
                                seed = stableColorSeed,
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing),
                                modifier = Modifier.weight(1f)
                            ) {
                                CCOutlinedTextField(
                                    value = firstName,
                                    onValueChange = onFirstNameChange,
                                    label = stringResource(R.string.common_first_name),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isReadOnly,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next,
                                    ),
                                )

                                CCOutlinedTextField(
                                    value = lastName,
                                    onValueChange = onLastNameChange,
                                    label = stringResource(R.string.common_last_name),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isReadOnly,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next,
                                    ),
                                )
                            }
                        }

                        CCOutlinedTextField(
                            value = nickname,
                            onValueChange = onNicknameChange,
                            label = stringResource(R.string.common_nickname),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                    }

                    // Address Book Selection
                    if (addressBooks.size > 1 && !isReadOnly) {
                        var addressBookExpanded by remember { mutableStateOf(false) }
                        val currentBook = addressBooks.find { it.href == effectiveAddressBookHref }

                        Section(
                            title = stringResource(R.string.common_address_book),
                            modifier = Modifier.padding(vertical = Dimens.medSpacing),
                            icon = Icons.Outlined.ImportContacts,
                            itemSpacing = 0.dp,
                            headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                        ) {

                            CCExposedDropdownMenuBox(
                                textBoxLabel = stringResource(R.string.edit_label_target_address_book),
                                currentValue = currentBook?.displayName
                                    ?: stringResource(R.string.common_unknown),
                                expanded = addressBookExpanded,
                                onExpandedChange = { addressBookExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                addressBooks.forEach { book ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                book.displayName
                                                    ?: stringResource(R.string.common_unknown)
                                            )
                                        },
                                        onClick = {
                                            onSelectedAddressBookChange(book.href)
                                            addressBookExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)
                ) {
                    Section(
                        title = stringResource(R.string.edit_section_additional_fields),
                        modifier = Modifier.padding(vertical = Dimens.medSpacing),
                        icon = Icons.Outlined.Person,
                        itemSpacing = Dimens.medSpacing
                    ) {
                        CCOutlinedTextField(
                            value = displayName,
                            onValueChange = onDisplayNameChange,
                            label = stringResource(R.string.common_display_name),
                            modifier = Modifier.fillMaxWidth(),
                            isError = displayNameError != null,
                            supportingText = displayNameError,
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )

                        CCOutlinedTextField(
                            value = prefix,
                            onValueChange = onPrefixChange,
                            label = stringResource(R.string.common_prefix),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )

                        CCOutlinedTextField(
                            value = middleName,
                            onValueChange = onMiddleNameChange,
                            label = stringResource(R.string.common_middle_name),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )

                        CCOutlinedTextField(
                            value = suffix,
                            onValueChange = onSuffixChange,
                            label = stringResource(R.string.common_suffix),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                }
            }
        } else {
            Section(
                title = stringResource(R.string.edit_section_name_photo),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.AccountBox,
                itemSpacing = Dimens.medSpacing
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ContactAvatarPicker(
                        selectedColor = selectedColor,
                        displayName = displayName.ifBlank { "$firstName $lastName" },
                        defaultColor = currentBookColor,
                        photoUrl = photoUrl,
                        onColorSelected = onColorSelected,
                        onPhotoClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemovePhoto = {
                            onPhotoUrlChange(null)
                        },
                        enabled = !isReadOnly,
                        seed = stableColorSeed,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing),
                        modifier = Modifier.weight(1f)
                    ) {
                        CCOutlinedTextField(
                            value = firstName,
                            onValueChange = onFirstNameChange,
                            label = stringResource(R.string.common_first_name),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )

                        CCOutlinedTextField(
                            value = lastName,
                            onValueChange = onLastNameChange,
                            label = stringResource(R.string.common_last_name),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isReadOnly,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                    }
                }

                CCOutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    label = stringResource(R.string.common_nickname),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReadOnly,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            // Address Book Selection
            if (addressBooks.size > 1 && !isReadOnly) {
                var addressBookExpanded by remember { mutableStateOf(false) }
                val currentBook = addressBooks.find { it.href == effectiveAddressBookHref }

                Section(
                    title = stringResource(R.string.common_address_book),
                    modifier = Modifier.padding(vertical = Dimens.medSpacing),
                    icon = Icons.Outlined.ImportContacts,
                    itemSpacing = 0.dp,
                    headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                ) {

                    CCExposedDropdownMenuBox(
                        textBoxLabel = stringResource(R.string.edit_label_target_address_book),
                        currentValue = currentBook?.displayName
                            ?: stringResource(R.string.common_unknown),
                        expanded = addressBookExpanded,
                        onExpandedChange = { addressBookExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        addressBooks.forEach { book ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        book.displayName
                                            ?: stringResource(R.string.common_unknown)
                                    )
                                },
                                onClick = {
                                    onSelectedAddressBookChange(book.href)
                                    addressBookExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (moreFieldsExpanded) {
                Section(
                    title = stringResource(R.string.edit_section_additional_fields),
                    modifier = Modifier.padding(vertical = Dimens.medSpacing),
                    icon = Icons.Outlined.Person,
                    itemSpacing = Dimens.medSpacing,
                    headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
                ) {
                    CCOutlinedTextField(
                        value = displayName,
                        onValueChange = onDisplayNameChange,
                        label = stringResource(R.string.common_display_name),
                        modifier = Modifier.fillMaxWidth(),
                        isError = displayNameError != null,
                        supportingText = displayNameError,
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    CCOutlinedTextField(
                        value = prefix,
                        onValueChange = onPrefixChange,
                        label = stringResource(R.string.common_prefix),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    CCOutlinedTextField(
                        value = middleName,
                        onValueChange = onMiddleNameChange,
                        label = stringResource(R.string.common_middle_name),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    CCOutlinedTextField(
                        value = suffix,
                        onValueChange = onSuffixChange,
                        label = stringResource(R.string.common_suffix),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
            }

            ExpandFieldsRow(
                expanded = moreFieldsExpanded,
                onToggle = { moreFieldsExpanded = !moreFieldsExpanded },
            )
        }
    }
}

@ThemePreview
@Composable
fun EditIdentitySectionPreview() {

    val mockPhotoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            // No-op
        }

    CorvidContactsTheme {
        EditIdentitySection(
            wideScreen = false,
            firstName = "David",
            onFirstNameChange = {},
            lastName = "Bowie",
            onLastNameChange = {},
            displayName = "",
            onDisplayNameChange = {},
            prefix = "",
            onPrefixChange = {},
            middleName = "",
            onMiddleNameChange = {},
            suffix = "",
            onSuffixChange = {},
            nickname = "",
            onNicknameChange = {},
            photoUrl = "",
            onPhotoUrlChange = {},
            selectedColor = null,
            onColorSelected = {},
            currentBookColor = Color(0xFF708090),
            stableColorSeed = "wonderful",
            isReadOnly = false,
            addressBooks = listOf(
                AddressBookEntity(
                    href = "",
                    displayName = "Work",
                    colorInt = 0xFF708090.toInt()
                ),
                AddressBookEntity(
                    href = "",
                    displayName = "Home",
                    colorInt = 0xFF708090.toInt()
                )
            ),
            onSelectedAddressBookChange = {},
            effectiveAddressBookHref = "",
            displayNameError = null,
            photoPickerLauncher = mockPhotoPickerLauncher,
        )
    }
}
