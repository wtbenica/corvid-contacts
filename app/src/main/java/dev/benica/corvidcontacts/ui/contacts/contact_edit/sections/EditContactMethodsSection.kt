// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableAddress
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableTypedValue
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueFieldList
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueListController
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.PhoneValueField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.StructuredAddressField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.TypedValueField
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * Section for editing contact information like phone numbers, emails, and addresses.
 */
@Composable
fun EditContactMethodsSection(
    phoneController: MultiValueListController<EditableTypedValue>,
    emailController: MultiValueListController<EditableTypedValue>,
    addressController: MultiValueListController<EditableAddress>,
    geocoderRepository: GeocoderRepository?,
    isReadOnly: Boolean,
    pendingFocusId: String?,
    onFocusRequested: (String?) -> Unit,
) {
    ContactSection(
        title = stringResource(R.string.edit_section_contact),
        content = {
            // Phone Sub-section
            Section(
                title = stringResource(R.string.common_phone),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.Phone,
                itemSpacing = 0.dp
            ) {
                MultiValueFieldList(
                    controller = phoneController,
                    isReadOnly = isReadOnly,
                    addLabel = stringResource(R.string.common_phone),
                    pendingFocusId = pendingFocusId,
                    onFocusRequested = onFocusRequested,
                ) { index, phone, onDelete, onMoveUp, onMoveDown, requestFocus ->
                    PhoneValueField(
                        value = phone.value,
                        region = phone.region,
                        onUpdate = { v, r ->
                            phoneController.update(
                                index,
                                phone.copy(value = v, region = r)
                            )
                        },
                        type = phone.type,
                        onTypeChange = {
                            phoneController.update(
                                index,
                                phone.copy(type = it)
                            )
                        },
                        types = VCardType.commonPhoneTypes,
                        onDelete = onDelete,
                        enabled = !isReadOnly,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        requestInitialFocus = requestFocus
                    )
                }
            }

            // Email Sub-section
            Section(
                title = stringResource(R.string.common_email),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.Email,
                itemSpacing = 0.dp
            ) {
                MultiValueFieldList(
                    controller = emailController,
                    isReadOnly = isReadOnly,
                    addLabel = stringResource(R.string.common_email),
                    pendingFocusId = pendingFocusId,
                    onFocusRequested = onFocusRequested,
                ) { index, email, onDelete, onMoveUp, onMoveDown, requestFocus ->
                    TypedValueField(
                        value = email.value,
                        onValueChange = {
                            emailController.update(
                                index,
                                email.copy(value = it)
                            )
                        },
                        type = email.type,
                        onTypeChange = {
                            emailController.update(
                                index,
                                email.copy(type = it)
                            )
                        },
                        types = VCardType.commonEmailTypes,
                        label = stringResource(R.string.common_email),
                        onDelete = onDelete,
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        requestInitialFocus = requestFocus
                    )
                }
            }

            // Address Sub-section
            Section(
                title = stringResource(R.string.common_address),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.Map,
                itemSpacing = 0.dp
            ) {
                MultiValueFieldList(
                    controller = addressController,
                    isReadOnly = isReadOnly,
                    addLabel = stringResource(R.string.common_address),
                    pendingFocusId = pendingFocusId,
                    onFocusRequested = onFocusRequested,
                ) { index, addr, onDelete, onMoveUp, onMoveDown, requestFocus ->
                    StructuredAddressField(
                        address = addr.value,
                        geocoderRepository = geocoderRepository,
                        onAddressChange = {
                            addressController.update(
                                index,
                                addr.copy(value = it)
                            )
                        },
                        onDelete = onDelete,
                        enabled = !isReadOnly,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        requestInitialFocus = requestFocus
                    )
                }
            }
        }
    )
}

@ThemePreview
@Composable
fun EditContactMethodsSectionPreview() {
    CorvidContactsTheme {
        EditContactMethodsSection(
            phoneController = MultiValueListController(
                items = remember {
                    listOf(
                        EditableTypedValue(
                            type = "CELL",
                            value = "+1 212-555-1234",
                            region = "US"
                        ),
                        EditableTypedValue(
                            type = "CELL",
                            value = "+1 212-555-1234",
                            region = "US"
                        ),
                        EditableTypedValue(
                            type = "CELL",
                            value = "+1 212-555-1234",
                            region = "US"
                        )
                    ).toMutableStateList()
                },
                newItem = { EditableTypedValue() },
                onItemAdded = {}
            ),
            emailController = MultiValueListController(
                items = remember {
                    listOf(
                        EditableTypedValue(
                            type = "WORK",
                            value = "example@example.com",
                            region = "US"
                        )
                    ).toMutableStateList()
                },
                newItem = { EditableTypedValue() },
                onItemAdded = {}
            ),
            addressController = MultiValueListController(
                items = remember {
                    listOf(
                        EditableAddress(
                            value = StructuredAddress(
                                type = "HOME",
                                street = "123 Main St",
                                city = "City",
                                state = "State",
                                postalCode = "12345",
                                country = "US"
                            )
                        )
                    ).toMutableStateList()
                },
                newItem = { EditableAddress() },
                onItemAdded = {}
            ),
            geocoderRepository = null,
            isReadOnly = false,
            pendingFocusId = null,
            onFocusRequested = {},
        )
    }
}
