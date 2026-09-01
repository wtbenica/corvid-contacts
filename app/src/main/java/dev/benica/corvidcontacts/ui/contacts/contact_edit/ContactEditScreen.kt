// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScrollableColumn
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueListController
import dev.benica.corvidcontacts.ui.contacts.contact_edit.sections.EditContactMethodsSection
import dev.benica.corvidcontacts.ui.contacts.contact_edit.sections.EditIdentitySection
import dev.benica.corvidcontacts.ui.contacts.contact_edit.sections.EditMiscSection
import dev.benica.corvidcontacts.ui.contacts.contact_edit.sections.EditNetworksSection
import dev.benica.corvidcontacts.ui.contacts.contact_edit.sections.EditWorkSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import dev.benica.corvidcontacts.data.model.Email as ModelEmail

/**
 * Lets a caller ask to navigate away from an in-progress edit. If there's nothing worth confirming,
 * [requestNavigation] runs [NavigationGuard.requestNavigation]'s `onProceed` immediately; otherwise
 * it shows the unsaved-changes dialog and runs `onProceed` only once the user chooses Save (and it
 * succeeds) or Discard.
 */
fun interface NavigationGuard {
    fun requestNavigation(onProceed: () -> Unit)
}

private fun combinedName(first: String, last: String) =
    listOfNotNull(first.ifBlank { null }, last.ifBlank { null }).joinToString(" ")

/**
 * Screen for creating or editing a contact.
 *
 * @param onNavigationGuardReady lets a host with no back handling of its own guard against losing unsaved changes
 * @param snackbarHostState only shown when [showScaffold] is true; pass your own if embedding with it false
 * @param onEffectiveColorChange reports the live-selected address book's color, not the contact's saved one
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    contactWithBook: ContactWithAddressBook?,
    addressBooks: List<AddressBookEntity>,
    allGroups: List<String>,
    includeCountryCode: Boolean,
    geocoderRepository: GeocoderRepository?,
    onSave: suspend (ContactEntity) -> Boolean,
    onBack: () -> Unit,
    onSaveComplete: (contactId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    initialAddressBookHref: String? = null,
    allContacts: List<ContactWithAddressBook> = emptyList(),
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
    onNavigationGuardReady: (NavigationGuard) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onEffectiveColorChange: (Color) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contactId = remember(contactWithBook?.contact?.id) {
        contactWithBook?.contact?.id ?: UUID
            .randomUUID()
            .toString()
    }

    val relationshipCandidates = remember(
        allContacts,
        contactId
    ) {
        allContacts
            .map { it.contact }
            .filter { it.id != contactId }
    }

    var firstName by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.firstName ?: ""
        )
    }
    var lastName by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.lastName ?: ""
        )
    }
    var middleName by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.middleName ?: ""
        )
    }
    var prefix by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.prefix ?: ""
        )
    }
    var suffix by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.suffix ?: ""
        )
    }
    var displayName by remember(contactWithBook?.contact) {
        mutableStateOf(contactWithBook?.contact?.displayName ?: "")
    }
    var nickname by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.nickname ?: ""
        )
    }
    var company by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.company ?: ""
        )
    }
    var jobTitle by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.jobTitle ?: ""
        )
    }
    var birthday by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.birthday ?: ""
        )
    }
    var notes by remember(contactWithBook?.contact) {
        mutableStateOf(
            contactWithBook?.contact?.notes ?: ""
        )
    }

    val socialProfiles = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.socialProfiles?.map {
            EditableTypedValue(
                type = it.type ?: "OTHER",
                value = it.value
            )
        } ?: emptyList()
        list.toMutableStateList()
    }

    // hasChanges() diffs photoUrl against this, not contact.photoUrl directly.
    val initialPhotoUrl = remember(contactWithBook?.contact) {
        if (contactWithBook?.contact?.photoUrl.isNullOrEmpty() && contactWithBook?.contact?.hasPhoto == true) {
            val file = File(
                File(
                    context.filesDir,
                    "photos"
                ),
                "$contactId.jpg"
            )
            if (file.exists()) Uri
                .fromFile(file)
                .toString() else null
        } else {
            contactWithBook?.contact?.photoUrl
        }
    }
    var photoUrl by remember(contactWithBook?.contact) { mutableStateOf(initialPhotoUrl) }

    var selectedColor: Color? by remember(contactWithBook) {
        mutableStateOf(contactWithBook?.contact?.colorInt?.let { Color(it) })
    }

    var selectedAddressBook by remember(contactWithBook?.contact) {
        mutableStateOf(contactWithBook?.contact?.addressBookHref ?: initialAddressBookHref)
    }

    val effectiveAddressBookHref: String? = selectedAddressBook ?: addressBooks.firstOrNull()?.href

    val currentBookColor: Color = remember(
        effectiveAddressBookHref,
        addressBooks
    ) {
        ContactColors.resolveAddressBookColor(addressBooks, effectiveAddressBookHref)
    }

    LaunchedEffect(currentBookColor) {
        onEffectiveColorChange(currentBookColor)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val filePath = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { input ->
                                val original = BitmapFactory.decodeStream(input) ?: return@use null
                                val maxDim = 512
                                val scale = minOf(
                                    1f,
                                    maxDim.toFloat() / maxOf(
                                        original.width,
                                        original.height
                                    )
                                )
                                val scaled = if (scale < 1f) {
                                    original.scale(
                                        (original.width * scale)
                                            .toInt()
                                            .coerceAtLeast(1),
                                        (original.height * scale)
                                            .toInt()
                                            .coerceAtLeast(1)
                                    )
                                } else original

                                val output = ByteArrayOutputStream()
                                scaled.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    80,
                                    output
                                )
                                if (scaled !== original) scaled.recycle()
                                original.recycle()

                                val dir = File(
                                    context.filesDir,
                                    "photos"
                                )
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(
                                    dir,
                                    "$contactId.jpg"
                                )
                                file.writeBytes(output.toByteArray())
                                "${Uri.fromFile(file)}?t=${System.currentTimeMillis()}"
                            }
                    }.getOrNull()
                }
                if (filePath != null) photoUrl = filePath
            }
        }
    }

    val emails = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.emails?.map {
            EditableTypedValue(
                type = it.type ?: "Home",
                value = it.value
            )
        } ?: emptyList()
        if (list.isEmpty()) {
            listOf(EditableTypedValue(type = "Home")).toMutableStateList()
        } else {
            list.toMutableStateList()
        }
    }

    val phones = remember(
        contactWithBook?.contact,
        includeCountryCode
    ) {
        val list = contactWithBook?.contact?.phones?.map {
            val split = PhoneFormatter.split(
                it.value,
                context
            )
            // Use significant-only format for the UI text field to strip trunk '0'
            val displayValue = PhoneFormatter.format(
                phone = it.value,
                includeCountryCode = false,
                context = context,
                region = split.first,
                significantOnly = true
            )
            EditableTypedValue(
                type = it.type ?: "CELL",
                value = displayValue,
                region = it.region ?: split.first
            )
        } ?: emptyList()
        if (list.isEmpty()) {
            listOf(
                EditableTypedValue(
                    type = "CELL",
                    region = Locale.getDefault().country
                )
            ).toMutableStateList()
        } else {
            list.toMutableStateList()
        }
    }

    val websites = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.websites?.map { EditableTypedValue(value = it) }
            ?: emptyList()
        list.toMutableStateList()
    }

    val relationships = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.relationships?.map {
            EditableRelationship(
                type = it.type,
                value = it.value,
                isUid = it.isUid
            )
        } ?: emptyList()
        list.toMutableStateList()
    }

    val hiddenCategories = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.categories ?: emptyList()
        list.filter {
            it.equals(
                "Archived",
                ignoreCase = true
            ) ||
                    it.equals(
                        ContactsRepository.FAVORITE_CATEGORY,
                        ignoreCase = true
                    )
        }
    }

    val categories = remember(contactWithBook?.contact) {
        val list = contactWithBook?.contact?.categories ?: emptyList()
        list
            .filter {
                !it.equals(
                    "Archived",
                    ignoreCase = true
                ) &&
                        !it.equals(
                            ContactsRepository.FAVORITE_CATEGORY,
                            ignoreCase = true
                        )
            }
            .toMutableStateList()
    }

    val structuredAddresses = remember(contactWithBook?.contact) {
        val list =
            contactWithBook?.contact?.structuredAddresses?.map { EditableAddress(value = it) }
                ?: emptyList()
        list.toMutableStateList()
    }

    var displayNameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    // Run after the unsaved-changes dialog resolves.
    var pendingProceed by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showSavingIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) {
        if (isSaving) {
            delay(200.milliseconds)
            showSavingIndicator = true
        } else {
            showSavingIndicator = false
        }
    }

    var pendingFocusId by remember { mutableStateOf<String?>(null) }

    val errorRequiredToSave = stringResource(R.string.edit_error_required_to_save)
    val errorSaveFailed = stringResource(R.string.edit_error_save_failed)

    val isReadOnly = remember(effectiveAddressBookHref) {
        (effectiveAddressBookHref?.contains(
            "server-generated",
            ignoreCase = true
        ) == true) ||
                (effectiveAddressBookHref?.contains(
                    "app-generated",
                    ignoreCase = true
                ) == true) ||
                (effectiveAddressBookHref?.contains(
                    "system",
                    ignoreCase = true
                ) == true) ||
                (effectiveAddressBookHref?.contains(
                    "account",
                    ignoreCase = true
                ) == true)
    }

    val stableColorSeed = remember(contactWithBook?.contact?.id) {
        contactWithBook?.contact?.id ?: UUID
            .randomUUID()
            .toString()
    }

    /**
     * Checks if any changes have been made to the contact.
     */
    fun hasChanges(): Boolean {
        fun hasListChanges(): Boolean {
            val original = contactWithBook?.contact ?: return false

            val originalEmails = (original.emails
                ?.filter { it.value.isNotBlank() }
                ?.map { it.value.trim() to (it.type?.uppercase() ?: "HOME") }
                ?: emptyList()).toSet()
            val currentEmails = emails
                .filter { it.value.isNotBlank() }
                .map { it.value.trim() to it.type.uppercase() }
                .toSet()
            if (originalEmails != currentEmails) return true

            val originalPhones = (original.phones
                ?.filter { it.value.isNotBlank() }
                ?.map { it.value.filter { c -> c.isDigit() } to (it.type?.uppercase() ?: "CELL") }
                ?: emptyList()).toSet()
            val currentPhones = phones
                .filter { it.value.isNotBlank() }
                .map {
                    // Reformat as performSave() would, not the display string.
                    val savedValue =
                        PhoneFormatter.format(it.value, includeCountryCode, context, it.region)
                    savedValue.filter { c -> c.isDigit() } to it.type.uppercase()
                }
                .toSet()
            if (originalPhones != currentPhones) return true

            val originalSocial = (original.socialProfiles
                ?.filter { it.value.isNotBlank() }
                ?.map { it.value.trim() to (it.type?.uppercase() ?: "OTHER") }
                ?: emptyList()).toSet()
            val currentSocial = socialProfiles
                .filter { it.value.isNotBlank() }
                .map { it.value.trim() to it.type.uppercase() }
                .toSet()
            if (originalSocial != currentSocial) return true

            val originalWebsites = (original.websites
                ?.filter { it.isNotBlank() }
                ?.map { it.trim() } ?: emptyList()).toSet()
            val currentWebsites = websites
                .filter { it.value.isNotBlank() }
                .map { it.value.trim() }
                .toSet()
            if (originalWebsites != currentWebsites) return true

            val originalRelationships = (original.relationships
                ?.filter { it.value.isNotBlank() }
                ?.map { it.value.trim() to it.type.uppercase() } ?: emptyList()).toSet()
            val currentRelationships = relationships
                .filter { it.value.isNotBlank() }
                .map { it.value.trim() to it.type.uppercase() }
                .toSet()
            if (originalRelationships != currentRelationships) return true

            val originalAddresses = (original.structuredAddresses ?: emptyList())
                .filter { !it.isBlank() }
                .toSet()
            val currentAddresses = structuredAddresses
                .map { it.value }
                .filter { !it.isBlank() }
                .toSet()
            if (originalAddresses != currentAddresses) return true

            val originalCategories = (original.categories ?: emptyList())
                .filter {
                    !it.equals(
                        "Archived",
                        ignoreCase = true
                    ) &&
                            !it.equals(
                                ContactsRepository.FAVORITE_CATEGORY,
                                ignoreCase = true
                            )
                }
                .toSet()
            val currentCategories = categories.toSet()
            return originalCategories != currentCategories
        }

        val original = contactWithBook?.contact
        if (original == null) {
            return firstName.isNotBlank() || lastName.isNotBlank() || displayName.isNotBlank() ||
                    middleName.isNotBlank() || prefix.isNotBlank() || suffix.isNotBlank() ||
                    nickname.isNotBlank() || company.isNotBlank() || jobTitle.isNotBlank() ||
                    birthday.isNotBlank() || notes.isNotBlank() || photoUrl != null ||
                    emails.any { it.value.isNotBlank() } ||
                    phones.any { it.value.isNotBlank() } ||
                    socialProfiles.any { it.value.isNotBlank() } ||
                    websites.any { it.value.isNotBlank() } ||
                    relationships.any { it.value.isNotBlank() } ||
                    structuredAddresses.any { !it.value.isBlank() } ||
                    categories.isNotEmpty() ||
                    selectedColor != null ||
                    selectedAddressBook != initialAddressBookHref
        } else {
            return firstName.trim() != (original.firstName?.trim() ?: "") ||
                    lastName.trim() != (original.lastName?.trim() ?: "") ||
                    middleName.trim() != (original.middleName?.trim() ?: "") ||
                    prefix.trim() != (original.prefix?.trim() ?: "") ||
                    suffix.trim() != (original.suffix?.trim() ?: "") ||
                    displayName.trim() != original.displayName.trim() ||
                    nickname.trim() != (original.nickname?.trim() ?: "") ||
                    company.trim() != (original.company?.trim() ?: "") ||
                    jobTitle.trim() != (original.jobTitle?.trim() ?: "") ||
                    birthday.trim() != (original.birthday?.trim() ?: "") ||
                    notes.trim() != (original.notes?.trim() ?: "") ||
                    (photoUrl ?: "") != (initialPhotoUrl ?: "") ||
                    selectedColor?.toArgb() != original.colorInt ||
                    selectedAddressBook != original.addressBookHref ||
                    hasListChanges()
        }
    }

    /**
     * Checks if the contact has enough information to be saved.
     */
    fun hasEnoughToSave(): Boolean =
        firstName.isNotBlank() || lastName.isNotBlank() || displayName.isNotBlank()

    /**
     * Validates the contact information and updates error states.
     */
    fun validate(): Boolean {
        val isValid = hasEnoughToSave()
        displayNameError = if (isValid) null else errorRequiredToSave
        return isValid
    }

    /**
     * Performs the save operation, running [onSuccess] once it completes. Defaults to
     * [onSaveComplete] - the plain "Save" button's landing spot. Callers that need to do something
     * else instead (like resuming a navigation attempt that was paused for the unsaved-changes
     * prompt) pass their own [onSuccess], which runs in place of [onSaveComplete] rather than
     * alongside it - [onSave] itself only persists, so there's no second navigation to conflict
     * with either way.
     */
    fun performSave(onSuccess: () -> Unit = { onSaveComplete(contactId) }) {
        val emailsList = emails
            .asSequence()
            .filter { it.value.isNotBlank() }
            .map {
                ModelEmail(
                    value = it.value,
                    type = normalizeType(it.type)
                )
            }
            .toList()
        val phonesList = phones
            .asSequence()
            .filter { it.value.isNotBlank() }
            .map {
                // Ensure the saved value respects the user's country code preference
                val savedValue = PhoneFormatter.format(
                    it.value,
                    includeCountryCode,
                    context,
                    it.region
                )
                Phone(
                    value = savedValue,
                    type = normalizeType(it.type),
                    region = it.region
                )
            }
            .toList()
        val socialList = socialProfiles
            .asSequence()
            .filter { it.value.isNotBlank() }
            .map {
                SocialProfile(
                    value = it.value,
                    type = normalizeType(it.type)
                )
            }
            .toList()
        val websitesList = websites
            .filter { it.value.isNotBlank() }
            .map { it.value }
        val relationshipsList = relationships
            .filter { it.value.isNotBlank() }
            .map {
                Relationship(
                    type = normalizeType(it.type),
                    value = it.value,
                    isUid = it.isUid
                )
            }
        val addrList = structuredAddresses
            .map { it.value }
            .filter { !it.isBlank() }
        val categoriesList = categories + hiddenCategories

        isSaving = true
        scope.launch {
            val success = onSave(
                ContactEntity(
                    id = contactId,
                    displayName = displayName.trim(),
                    firstName = firstName
                        .trim()
                        .ifBlank { null },
                    lastName = lastName
                        .trim()
                        .ifBlank { null },
                    middleName = middleName
                        .trim()
                        .ifBlank { null },
                    prefix = prefix
                        .trim()
                        .ifBlank { null },
                    suffix = suffix
                        .trim()
                        .ifBlank { null },
                    emails = emailsList,
                    phones = phonesList,
                    photoUrl = photoUrl,
                    hasPhoto = photoUrl != null,
                    etag = contactWithBook?.contact?.etag,
                    addressBookHref = effectiveAddressBookHref,
                    contactHref = contactWithBook?.contact?.contactHref,
                    colorInt = selectedColor?.toArgb(),
                    isArchived = contactWithBook?.contact?.isArchived ?: false,
                    categories = categoriesList,
                    company = company
                        .trim()
                        .ifBlank { null },
                    jobTitle = jobTitle
                        .trim()
                        .ifBlank { null },
                    birthday = birthday
                        .trim()
                        .ifBlank { null },
                    nickname = nickname
                        .trim()
                        .ifBlank { null },
                    notes = notes
                        .trim()
                        .ifBlank { null },
                    websites = websitesList,
                    socialProfiles = socialList,
                    relationships = relationshipsList,
                    structuredAddresses = addrList,
                )
            )
            isSaving = false
            if (success) {
                onSuccess()
            } else {
                snackbarHostState.showSnackbar(errorSaveFailed)
            }
        }
    }

    /**
     * Runs [onProceed] immediately if there's nothing worth confirming, otherwise shows the
     * unsaved-changes dialog and runs it once the user chooses Save (and it succeeds) or Discard.
     */
    fun requestNavigation(onProceed: () -> Unit) {
        if (hasChanges() && hasEnoughToSave()) {
            pendingProceed = onProceed
            showUnsavedChangesDialog = true
        } else {
            onProceed()
        }
    }

    BackHandler { requestNavigation(onBack) }

    SideEffect {
        onNavigationGuardReady(NavigationGuard { onProceed -> requestNavigation(onProceed) })
    }

    val phoneController = remember(phones) {
        MultiValueListController(
            items = phones,
            newItem = {
                EditableTypedValue(
                    type = "CELL",
                    region = Locale.getDefault().country
                )
            },
            onItemAdded = { pendingFocusId = it }
        )
    }
    val emailController = remember(emails) {
        MultiValueListController(
            items = emails,
            newItem = { EditableTypedValue(type = "HOME") },
            onItemAdded = { pendingFocusId = it }
        )
    }
    val addressController = remember(structuredAddresses) {
        MultiValueListController(
            items = structuredAddresses,
            newItem = { EditableAddress() },
            onItemAdded = { pendingFocusId = it }
        )
    }
    val socialProfileController = remember(socialProfiles) {
        MultiValueListController(
            items = socialProfiles,
            newItem = { EditableTypedValue(type = "OTHER") },
            onItemAdded = { pendingFocusId = it }
        )
    }
    val websiteController = remember(websites) {
        MultiValueListController(
            items = websites,
            newItem = { EditableTypedValue() },
            onItemAdded = { pendingFocusId = it }
        )
    }
    val relationshipController = remember(relationships) {
        MultiValueListController(
            items = relationships,
            newItem = { EditableRelationship() },
            onItemAdded = { pendingFocusId = it }
        )
    }

    val currentThemeColor = remember(
        selectedColor,
        currentBookColor
    ) {
        selectedColor ?: currentBookColor
    }

    val chromeTitle =
        if (contactWithBook?.contact?.contactHref.isNullOrBlank()) stringResource(R.string.edit_title_new) else stringResource(
            R.string.edit_title_edit
        )
    val chromeNavigationIcon: @Composable () -> Unit = {
        // Use common save-if-changed logic for both back button and gesture.
        BackNavButton { requestNavigation(onBack) }
    }
    val chromeActions: @Composable RowScope.() -> Unit = {
        if (!isReadOnly) {
            if (showSavingIndicator) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp + Dimens.smSpacing * 2)
                        .padding(Dimens.smSpacing),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                CCIconButton(
                    icon = Icons.Rounded.Save,
                    contentDescription = R.string.action_save,
                    onClick = {
                        if (validate()) {
                            performSave()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(errorRequiredToSave)
                            }
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    enabled = !isSaving
                )
            }
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 840.dp

            CCScrollableColumn(
                systemPadding = padding,
                content = {
                    if (isReadOnly) {
                        ReadOnlyWarning()
                    }

                    EditIdentitySection(
                        wideScreen = wideLayout,
                        firstName = firstName,
                        onFirstNameChange = {
                            if (displayName.trim() == combinedName(firstName, lastName)) {
                                displayName = combinedName(it, lastName)
                            }
                            firstName = it
                            if (it.isNotBlank() || lastName.isNotBlank() || displayName.isNotBlank()) displayNameError =
                                null
                        },
                        lastName = lastName,
                        onLastNameChange = {
                            if (displayName.trim() == combinedName(firstName, lastName)) {
                                displayName = combinedName(firstName, it)
                            }
                            lastName = it
                            if (it.isNotBlank() || firstName.isNotBlank() || displayName.isNotBlank()) displayNameError =
                                null
                        },
                        displayName = displayName,
                        onDisplayNameChange = {
                            displayName = it
                            if (it.isNotBlank() || firstName.isNotBlank() || lastName.isNotBlank()) displayNameError =
                                null
                        },
                        prefix = prefix,
                        onPrefixChange = { prefix = it },
                        middleName = middleName,
                        onMiddleNameChange = { middleName = it },
                        suffix = suffix,
                        onSuffixChange = { suffix = it },
                        nickname = nickname,
                        onNicknameChange = { nickname = it },
                        photoUrl = photoUrl,
                        onPhotoUrlChange = { photoUrl = it },
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                        currentBookColor = currentBookColor,
                        stableColorSeed = stableColorSeed,
                        isReadOnly = isReadOnly,
                        addressBooks = addressBooks,
                        onSelectedAddressBookChange = { selectedAddressBook = it },
                        effectiveAddressBookHref = effectiveAddressBookHref,
                        displayNameError = displayNameError,
                        photoPickerLauncher = photoPickerLauncher
                    )

                    if (wideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                            ) {
                                EditContactMethodsSection(
                                    phoneController = phoneController,
                                    emailController = emailController,
                                    addressController = addressController,
                                    geocoderRepository = geocoderRepository,
                                    isReadOnly = isReadOnly,
                                    pendingFocusId = pendingFocusId,
                                    onFocusRequested = { pendingFocusId = it },
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                            ) {
                                EditWorkSection(
                                    company = company,
                                    onCompanyChange = { company = it },
                                    jobTitle = jobTitle,
                                    onJobTitleChange = { jobTitle = it },
                                    isReadOnly = isReadOnly
                                )

                                EditNetworksSection(
                                    socialProfileController = socialProfileController,
                                    websiteController = websiteController,
                                    isReadOnly = isReadOnly,
                                    pendingFocusId = pendingFocusId,
                                    onFocusRequested = { pendingFocusId = it }
                                )

                                EditMiscSection(
                                    birthday = birthday,
                                    onBirthdayChange = { birthday = it },
                                    notes = notes,
                                    onNotesChange = { notes = it },
                                    categories = categories,
                                    onAddCategory = { categories.add(it) },
                                    onRemoveCategory = { categories.remove(it) },
                                    allGroups = allGroups,
                                    relationshipController = relationshipController,
                                    relationshipCandidates = relationshipCandidates,
                                    isReadOnly = isReadOnly,
                                    currentThemeColor = currentThemeColor,
                                    pendingFocusId = pendingFocusId,
                                    onFocusRequested = { pendingFocusId = it }
                                )
                            }
                        }
                    } else {
                        EditWorkSection(
                            company = company,
                            onCompanyChange = { company = it },
                            jobTitle = jobTitle,
                            onJobTitleChange = { jobTitle = it },
                            isReadOnly = isReadOnly
                        )

                        EditContactMethodsSection(
                            phoneController = phoneController,
                            emailController = emailController,
                            addressController = addressController,
                            geocoderRepository = geocoderRepository,
                            isReadOnly = isReadOnly,
                            pendingFocusId = pendingFocusId,
                            onFocusRequested = { pendingFocusId = it },
                        )

                        EditNetworksSection(
                            socialProfileController = socialProfileController,
                            websiteController = websiteController,
                            isReadOnly = isReadOnly,
                            pendingFocusId = pendingFocusId,
                            onFocusRequested = { pendingFocusId = it }
                        )

                        EditMiscSection(
                            birthday = birthday,
                            onBirthdayChange = { birthday = it },
                            notes = notes,
                            onNotesChange = { notes = it },
                            categories = categories,
                            onAddCategory = { categories.add(it) },
                            onRemoveCategory = { categories.remove(it) },
                            allGroups = allGroups,
                            relationshipController = relationshipController,
                            relationshipCandidates = relationshipCandidates,
                            isReadOnly = isReadOnly,
                            currentThemeColor = currentThemeColor,
                            pendingFocusId = pendingFocusId,
                            onFocusRequested = { pendingFocusId = it }
                        )
                    }
                }
            )
        }
    }

    if (showScaffold) {
        CCScaffold(
            title = chromeTitle,
            modifier = modifier,
            navigationIcon = chromeNavigationIcon,
            topBarActions = chromeActions,
            baseColor = currentThemeColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = bodyContent
        )
    } else {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                    actions = chromeActions,
                )
            )
        }
        bodyContent(PaddingValues())
    }

    if (showUnsavedChangesDialog) {
        CCAlertDialog(
            onDismissRequest = {
                showUnsavedChangesDialog = false
                pendingProceed?.invoke()
                pendingProceed = null
            },
            icon = Icons.Outlined.ErrorOutline,
            title = R.string.edit_unsaved_changes_title,
            content = {
                Text(stringResource(R.string.edit_unsaved_changes_message))
            },
            confirmButton = R.string.action_save,
            onConfirm = {
                showUnsavedChangesDialog = false
                if (validate()) {
                    performSave(onSuccess = {
                        pendingProceed?.invoke()
                        pendingProceed = null
                    })
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorRequiredToSave)
                    }
                }
            },
            dismissButton = R.string.action_discard,
            baseColor = currentThemeColor,
        )
    }
}

@Composable
private fun ReadOnlyWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.edit_read_only_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@ThemePreview
@Composable
fun ContactEditScreenPreview() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val geocoderRepository = remember {
        GeocoderRepository(
            context,
            settingsRepository
        )
    }

    val mockContact = ContactEntity(
        id = "1",
        displayName = "John Doe",
        firstName = "John",
        lastName = "Doe",
        emails = listOf(
            ModelEmail(
                "john@example.com",
                "HOME"
            )
        ),
        phones = listOf(
            Phone(
                "+15551234567",
                "CELL",
                "US"
            ),
            Phone(
                "+15551234567",
                "CELL",
                "US"
            )

        ),
        //        address = null,
        photoUrl = null,
        etag = null,
        addressBookHref = "/books/personal/",
        contactHref = "/books/personal/1.vcf",
        colorInt = ContactColors.palette[7].toArgb(),
        isArchived = false,
        categories = listOf("Friends"),
        company = "Example Corp",
        jobTitle = "Software Engineer",
        birthday = "1990-01-01",
        nickname = "Johnny",
        notes = "Met at the conference",
        websites = listOf("https://example.com"),
        relationships = emptyList(),
        structuredAddresses = listOf(
            StructuredAddress(
                type = "HOME",
                street = "123 Main St",
                city = "Anytown",
                state = "CA",
                postalCode = "12345",
                country = "USA"
            )
        )
    )

    val mockBook = AddressBookEntity(
        href = "/books/personal/",
        displayName = "Personal",
        isVisible = true,
        colorInt = ContactColors.palette[3].toArgb(),
    )

    val mockContactWithBook = ContactWithAddressBook(
        contact = mockContact,
        addressBook = mockBook
    )

    val addressBooks = listOf(
        AddressBookEntity(
            href = "/books/personal/",
            displayName = "Personal",
            isVisible = true,
            colorInt = 0xFF328598.toInt()
        ),
        AddressBookEntity(
            href = "/books/work/",
            displayName = "Work",
            isVisible = true,
            colorInt = 0xFF988532.toInt()
        )
    )

    CorvidContactsTheme {
        ContactEditScreen(
            contactWithBook = mockContactWithBook,
            addressBooks = addressBooks,
            allGroups = listOf(
                "Friends",
                "Family",
                "Work"
            ),
            includeCountryCode = true,
            geocoderRepository = geocoderRepository,
            onSave = { true },
            onBack = {}
        )
    }
}
