// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.People
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
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableTypedValue
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueFieldList
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueListController
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.SimpleValueField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.TypedValueField
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * Section for editing online presence like social profiles and websites.
 */
@Composable
fun EditNetworksSection(
    socialProfileController: MultiValueListController<EditableTypedValue>,
    websiteController: MultiValueListController<EditableTypedValue>,
    isReadOnly: Boolean,
    pendingFocusId: String?,
    onFocusRequested: (String?) -> Unit,
) {
    ContactSection(
        title = stringResource(R.string.edit_section_networks),
        content = {
            // Social Profiles Sub-section
            Section(
                title = stringResource(R.string.common_social),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.People,
                itemSpacing = 0.dp
            ) {
                MultiValueFieldList(
                    controller = socialProfileController,
                    isReadOnly = isReadOnly,
                    addLabel = stringResource(R.string.common_social),
                    pendingFocusId = pendingFocusId,
                    onFocusRequested = onFocusRequested,
                ) { index, profile, onDelete, onMoveUp, onMoveDown, requestFocus ->
                    TypedValueField(
                        value = profile.value,
                        onValueChange = {
                            socialProfileController.update(
                                index,
                                profile.copy(value = it)
                            )
                        },
                        type = profile.type,
                        onTypeChange = {
                            socialProfileController.update(
                                index,
                                profile.copy(type = it)
                            )
                        },
                        types = VCardType.commonSocialTypes,
                        label = stringResource(R.string.common_social),
                        onDelete = onDelete,
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        requestInitialFocus = requestFocus
                    )
                }
            }

            // Website Sub-section
            Section(
                title = stringResource(R.string.common_website),
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                icon = Icons.Outlined.Language,
                itemSpacing = 0.dp
            ) {
                MultiValueFieldList(
                    controller = websiteController,
                    isReadOnly = isReadOnly,
                    addLabel = stringResource(R.string.common_website),
                    pendingFocusId = pendingFocusId,
                    onFocusRequested = onFocusRequested,
                ) { index, site, onDelete, onMoveUp, onMoveDown, requestFocus ->
                    SimpleValueField(
                        value = site.value,
                        onValueChange = {
                            websiteController.update(
                                index,
                                site.copy(value = it)
                            )
                        },
                        label = stringResource(R.string.edit_label_url),
                        onDelete = onDelete,
                        enabled = !isReadOnly,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
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
        }
    )
}

@ThemePreview
@Composable
fun EditNetworksSectionPreview() {
    CorvidContactsTheme {
        EditNetworksSection(
            socialProfileController = MultiValueListController(
                items = remember {
                    listOf(
                        EditableTypedValue(
                            type = "TWITTER",
                            value = "@johndoe"
                        ),
                        EditableTypedValue(
                            type = "FACEBOOK",
                            value = "johndoe420"
                        )
                    ).toMutableStateList()
                },
                newItem = { EditableTypedValue() },
                onItemAdded = {}
            ),
            websiteController = MultiValueListController(
                items = remember {
                    emptyList<EditableTypedValue>().toMutableStateList()
                },
                newItem = { EditableTypedValue() },
                onItemAdded = {}
            ),
            isReadOnly = false,
            pendingFocusId = null,
            onFocusRequested = {}
        )
    }
}
