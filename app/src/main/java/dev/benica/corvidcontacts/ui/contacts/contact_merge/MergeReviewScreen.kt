// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.merge.ConflictField
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCDropdownMenuItem
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScrollableColumn
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableAddress
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableRelationship
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableTypedValue
import dev.benica.corvidcontacts.ui.contacts.contact_edit.HasId
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.AddGroupField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueFieldList
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueListController
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.PhoneValueField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.RelationshipField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.SimpleValueField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.StructuredAddressField
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.TypedValueField
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.ConflictAwareTextField
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.MergeConflictBirthdayField
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.MergeConflictNotesField
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.MergeConflictPhotoField
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.PhotoChoice
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.PlainBirthdayField
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import dev.benica.corvidcontacts.data.model.Phone as ModelPhone

/**
 * Screen for reviewing and confirming the merge of [absorbed] into [survivor].
 */
@Composable
fun MergeReviewScreen(
    survivor: ContactWithAddressBook,
    absorbed: ContactWithAddressBook,
    onConfirm: suspend (mergedSurvivor: ContactEntity, absorbed: ContactEntity, useAbsorbedPhoto: Boolean) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    includeCountryCode: Boolean = true,
    geocoderRepository: GeocoderRepository? = null,
    allContacts: List<ContactWithAddressBook> = emptyList(),
    allGroups: List<String> = emptyList(),
    addressBooks: List<AddressBookEntity> = emptyList(),
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
    // Only rendered here when showScaffold is true; embedders pass their own otherwise.
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val state = rememberMergeReviewState(
        survivor,
        absorbed
    )
    val conflictMap = remember(state.conflicts) { state.conflicts.associateBy { it.field } }

    val scope = rememberCoroutineScope()
    val errorMessage = stringResource(R.string.common_error_unknown)
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var isMerging by remember { mutableStateOf(false) }
    var showMergingIndicator by remember { mutableStateOf(false) }

    var pendingFocusId by remember { mutableStateOf<String?>(null) }

    val phoneController = remember(state.phones) {
        MultiValueListController(
            items = state.phones,
            newItem = { EditableTypedValue(type = "CELL") },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val emailController = remember(state.emails) {
        MultiValueListController(
            items = state.emails,
            newItem = { EditableTypedValue(type = "HOME") },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val addressController = remember(state.structuredAddresses) {
        MultiValueListController(
            items = state.structuredAddresses,
            newItem = { EditableAddress() },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val socialProfileController = remember(state.socialProfiles) {
        MultiValueListController(
            items = state.socialProfiles,
            newItem = { EditableTypedValue(type = "OTHER") },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val websiteController = remember(state.websites) {
        MultiValueListController(
            items = state.websites,
            newItem = { EditableTypedValue() },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val relationshipController = remember(state.relationships) {
        MultiValueListController(
            items = state.relationships,
            newItem = { EditableRelationship() },
            onItemAdded = { pendingFocusId = it }
        )
    }

    LaunchedEffect(isMerging) {
        if (isMerging) {
            delay(200.milliseconds)
            showMergingIndicator = true
        } else {
            showMergingIndicator = false
        }
    }

    val currentBookColor: Color = remember(
        state.selectedAddressBookHref,
        addressBooks
    ) {
        val book = addressBooks.find { it.href == state.selectedAddressBookHref }
        ContactColors.getAddressBookColorWithFallback(
            customColor = book?.colorInt,
            href = state.selectedAddressBookHref ?: ""
        )
    }

    val relationshipCandidates = remember(
        allContacts,
        survivor.contact.id,
        absorbed.contact.id
    ) {
        allContacts
            .map { it.contact }
            .filter { it.id != survivor.contact.id && it.id != absorbed.contact.id }
    }

    @Composable
    fun MergeSection(
        titleRes: Int,
        icon: ImageVector,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Section(
            title = stringResource(titleRes),
            modifier = Modifier.padding(vertical = Dimens.medSpacing),
            icon = icon,
            itemSpacing = Dimens.smSpacing,
            content = content
        )
    }

    @Composable
    fun MergeTextField(
        labelRes: Int,
        value: String?,
        onValueChange: (String) -> Unit,
        field: ConflictField,
        keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    ) {
        ConflictAwareTextField(
            label = stringResource(labelRes),
            value = value ?: "",
            onValueChange = onValueChange,
            conflict = conflictMap[field],
            keyboardOptions = keyboardOptions,
            color = currentBookColor
        )
    }

    @Composable
    fun <T : HasId> MergeMultiValueList(
        controller: MultiValueListController<T>,
        addLabelRes: Int,
        itemContent: @Composable (
            index: Int,
            item: T,
            onDelete: (() -> Unit)?,
            onMoveUp: (() -> Unit)?,
            onMoveDown: (() -> Unit)?,
            requestFocus: Boolean,
        ) -> Unit,
    ) {
        MultiValueFieldList(
            controller = controller,
            isReadOnly = false,
            addLabel = stringResource(addLabelRes),
            pendingFocusId = pendingFocusId,
            onFocusRequested = { pendingFocusId = it },
            itemContent = itemContent
        )
    }

    BackHandler {
        if (state.hasChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onCancel()
        }
    }

    val chromeTitle = stringResource(R.string.merge_review_title)
    val chromeNavigationIcon: @Composable () -> Unit = {
        BackNavButton {
            if (state.hasChanges()) {
                showUnsavedChangesDialog = true
            } else {
                onCancel()
            }
        }
    }
    val chromeActions: @Composable RowScope.() -> Unit = {
        if (showMergingIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp + Dimens.smSpacing * 2)
                    .padding(Dimens.smSpacing),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            CCIconButton(
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                contentDescription = R.string.merge_review_confirm_action,
                onClick = { showConfirmDialog = true },
                color = MaterialTheme.colorScheme.onSurface,
                enabled = !isMerging
            )
        }
    }

    if (!showScaffold) {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                    actions = chromeActions,
                )
            )
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        CCScrollableColumn(
            systemPadding = padding
        ) {
            if (addressBooks.size > 1) {
                var addressBookExpanded by remember { mutableStateOf(false) }
                val currentBook = addressBooks.find { it.href == state.selectedAddressBookHref }

                CCExposedDropdownMenuBox(
                    textBoxLabel = stringResource(R.string.edit_label_target_address_book),
                    currentValue = currentBook?.displayName
                        ?: stringResource(R.string.common_unknown),
                    expanded = addressBookExpanded,
                    onExpandedChange = { addressBookExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    addressBooks.forEach { book ->
                        CCDropdownMenuItem(
                            text = book.displayName ?: stringResource(R.string.common_unknown),
                            onClick = {
                                state.selectedAddressBookHref = book.href
                                addressBookExpanded = false
                            }
                        )
                    }
                }
            }

            ContactSection(
                title = stringResource(R.string.edit_section_identity),
                content = {
                    MergeSection(
                        titleRes = R.string.edit_section_name_photo,
                        icon = Icons.Outlined.AccountBox
                    ) {
                        conflictMap[ConflictField.PHOTO_URL]?.let {
                            MergeConflictPhotoField(
                                survivor = survivor,
                                absorbed = absorbed,
                                selected = state.photoChoice,
                                onSelect = { state.photoChoice = it },
                                color = currentBookColor
                            )
                        }
                        MergeTextField(
                            labelRes = R.string.common_first_name,
                            value = state.firstName,
                            onValueChange = { state.firstName = it },
                            field = ConflictField.FIRST_NAME,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_last_name,
                            value = state.lastName,
                            onValueChange = { state.lastName = it },
                            field = ConflictField.LAST_NAME,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_middle_name,
                            value = state.middleName,
                            onValueChange = { state.middleName = it },
                            field = ConflictField.MIDDLE_NAME,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_prefix,
                            value = state.prefix,
                            onValueChange = { state.prefix = it },
                            field = ConflictField.PREFIX,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_suffix,
                            value = state.suffix,
                            onValueChange = { state.suffix = it },
                            field = ConflictField.SUFFIX,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_display_name,
                            value = state.displayName,
                            onValueChange = { state.displayName = it },
                            field = ConflictField.DISPLAY_NAME,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        MergeTextField(
                            labelRes = R.string.common_nickname,
                            value = state.nickname,
                            onValueChange = { state.nickname = it },
                            field = ConflictField.NICKNAME,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                }
            )

            ContactSection(
                title = stringResource(R.string.edit_section_work),
                content = {
                    MergeSection(
                        titleRes = R.string.common_company,
                        icon = Icons.Outlined.Business
                    ) {
                        MergeTextField(
                            labelRes = R.string.common_company,
                            value = state.company,
                            onValueChange = { state.company = it },
                            field = ConflictField.COMPANY,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                    }

                    MergeSection(
                        titleRes = R.string.common_job_title,
                        icon = Icons.Outlined.WorkOutline
                    ) {
                        MergeTextField(
                            labelRes = R.string.common_job_title,
                            value = state.jobTitle,
                            onValueChange = { state.jobTitle = it },
                            field = ConflictField.JOB_TITLE,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                }
            )

            ContactSection(
                title = stringResource(R.string.edit_section_contact),
                content = {
                    MergeSection(
                        titleRes = R.string.common_phone,
                        icon = Icons.Outlined.Phone
                    ) {
                        MergeMultiValueList(
                            controller = phoneController,
                            addLabelRes = R.string.common_phone
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
                                enabled = true,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                requestInitialFocus = requestFocus
                            )
                        }
                    }

                    MergeSection(
                        titleRes = R.string.common_email,
                        icon = Icons.Outlined.Email
                    ) {
                        MergeMultiValueList(
                            controller = emailController,
                            addLabelRes = R.string.common_email
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
                                enabled = true,
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

                    MergeSection(
                        titleRes = R.string.common_address,
                        icon = Icons.Outlined.Map
                    ) {
                        MergeMultiValueList(
                            controller = addressController,
                            addLabelRes = R.string.common_address
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
                                enabled = true,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                requestInitialFocus = requestFocus
                            )
                        }
                    }
                }
            )

            ContactSection(
                title = stringResource(R.string.edit_section_networks),
                content = {
                    MergeSection(
                        titleRes = R.string.common_social,
                        icon = Icons.Outlined.People
                    ) {
                        MergeMultiValueList(
                            controller = socialProfileController,
                            addLabelRes = R.string.common_social
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
                                enabled = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                requestInitialFocus = requestFocus
                            )
                        }
                    }

                    MergeSection(
                        titleRes = R.string.common_website,
                        icon = Icons.Outlined.Language
                    ) {
                        MergeMultiValueList(
                            controller = websiteController,
                            addLabelRes = R.string.common_website
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
                                enabled = true,
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

            ContactSection(
                title = stringResource(R.string.edit_section_relationships),
                content = {
                    MergeSection(
                        titleRes = R.string.common_relationship,
                        icon = Icons.Outlined.People
                    ) {
                        MergeMultiValueList(
                            controller = relationshipController,
                            addLabelRes = R.string.common_relationship
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
                                enabled = true,
                                contacts = relationshipCandidates,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                requestInitialFocus = requestFocus
                            )
                        }
                    }
                }
            )

            ContactSection(
                title = stringResource(R.string.edit_section_misc),
                content = {
                    MergeSection(
                        titleRes = R.string.common_birthday,
                        icon = Icons.Outlined.Cake
                    ) {
                        val conflict = conflictMap[ConflictField.BIRTHDAY]
                        if (conflict != null) {
                            MergeConflictBirthdayField(
                                label = stringResource(R.string.common_birthday),
                                value = state.birthday,
                                onValueChange = { state.birthday = it },
                                survivorValue = conflict.survivorValue,
                                absorbedValue = conflict.absorbedValue,
                                color = currentBookColor
                            )
                        } else {
                            PlainBirthdayField(
                                value = state.birthday,
                                onValueChange = { state.birthday = it },
                            )
                        }
                    }

                    MergeSection(
                        titleRes = R.string.common_notes,
                        icon = Icons.AutoMirrored.Outlined.Notes
                    ) {
                        val conflict = conflictMap[ConflictField.NOTES]
                        if (conflict != null) {
                            MergeConflictNotesField(
                                label = stringResource(R.string.common_notes),
                                value = state.notes,
                                onValueChange = { state.notes = it },
                                survivorValue = conflict.survivorValue,
                                absorbedValue = conflict.absorbedValue,
                                color = currentBookColor
                            )
                        } else {
                            CCOutlinedTextField(
                                value = state.notes,
                                onValueChange = { state.notes = it },
                                label = stringResource(R.string.common_notes),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            )
                        }
                    }

                    MergeSection(
                        titleRes = R.string.common_groups,
                        icon = Icons.Outlined.Groups
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (state.categories.isNotEmpty()) {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.categories.forEach { category ->
                                        InputChip(
                                            selected = false,
                                            onClick = { },
                                            label = { Text(category) },
                                            trailingIcon = {
                                                CCIconButton(
                                                    icon = Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                    onClick = { state.categories.remove(category) },
                                                    modifier = Modifier.size(18.dp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            AddGroupField(
                                allGroups = allGroups,
                                currentGroups = state.categories,
                                onAdd = { state.categories.add(it) })
                        }
                    }
                }
            )
        }
    }

    if (showScaffold) {
        CCScaffold(
            title = chromeTitle,
            navigationIcon = chromeNavigationIcon,
            topBarActions = chromeActions,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            baseColor = currentBookColor,
            modifier = modifier,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }

    if (showConfirmDialog) {
        CCAlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = R.string.merge_confirm_dialog_title,
            content = {
                Text(
                    stringResource(
                        R.string.merge_confirm_dialog_message,
                        absorbed.contact.getEffectiveDisplayName(),
                        survivor.contact.getEffectiveDisplayName()
                    )
                )
            },
            confirmButton = R.string.merge_review_confirm_action,
            onConfirm = {
                showConfirmDialog = false
                isMerging = true
                scope.launch {
                    val merged = state.buildMergedEntity()
                    // Final formatting pass to apply country code preference to all numbers
                    val formattedPhones = merged.phones?.map { p ->
                        p.copy(
                            value = PhoneFormatter.format(
                                phone = p.value,
                                includeCountryCode = includeCountryCode,
                                context = context,
                                region = p.region
                            )
                        )
                    }
                    val finalMerged = merged.copy(phones = formattedPhones)

                    val success = onConfirm(
                        finalMerged,
                        absorbed.contact,
                        state.photoChoice == PhotoChoice.ABSORBED
                    )
                    isMerging = false
                    if (!success) {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            },
            dismissButton = R.string.action_cancel,
        )
    }

    if (showUnsavedChangesDialog) {
        CCAlertDialog(
            onDismissRequest = {
                showUnsavedChangesDialog = false
                onCancel()
            },
            title = R.string.merge_unsaved_changes_title,
            content = {
                Text(stringResource(R.string.merge_unsaved_changes_message))
            },
            confirmButton = R.string.merge_action_continue_merge,
            onConfirm = {
                showUnsavedChangesDialog = false
                showConfirmDialog = true
            },
            dismissButton = R.string.action_discard,
            baseColor = currentBookColor
        )
    }
}

@ThemePreview
@Composable
private fun MergeReviewScreenPreview() {
    val survivor = ContactEntity(
        id = "A",
        displayName = "The Fish",
        firstName = "Robert",
        middleName = "Elephant",
        lastName = "Whitfield",
        prefix = "Mr.",
        suffix = "Jr.",
        nickname = "Ted",
        emails = null,
        phones = listOf(
            ModelPhone(
                "+13035550166",
                "CELL"
            )
        ),
        photoUrl = null,
        etag = "etag",
        company = "Whitfield Legal Group",
        jobTitle = "Janitor",
        birthday = "1970-01-01",
        notes = "Handles the Denver contracts."
    )
    val absorbed = ContactEntity(
        id = "B",
        displayName = "Gary",
        firstName = "Bob",
        middleName = "Robert",
        lastName = "Whitfields",
        prefix = "Dr.",
        suffix = "Esq.",
        nickname = "Bobby",
        emails = null,
        phones = listOf(
            ModelPhone(
                "+13035550165",
                "CELL"
            )
        ),
        photoUrl = null,
        etag = "etag",
        company = "Yellow Dog Dirt",
        jobTitle = "CEO",
        birthday = "1980-05-15",
        notes = "Great guy, plays in the Sunday softball league."
    )

    CorvidContactsTheme {
        MergeReviewScreen(
            survivor = ContactWithAddressBook(
                contact = survivor,
                addressBook = null
            ),
            absorbed = ContactWithAddressBook(
                contact = absorbed,
                addressBook = null
            ),
            onConfirm = { _, _, _ -> true },
            onCancel = {}
        )
    }
}
