// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.merge.ConflictResolution
import dev.benica.corvidcontacts.data.merge.ContactMerger
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableAddress
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableRelationship
import dev.benica.corvidcontacts.ui.contacts.contact_edit.EditableTypedValue
import dev.benica.corvidcontacts.ui.contacts.contact_merge.components.PhotoChoice

class MergeReviewState(
    val survivor: ContactWithAddressBook,
    val absorbed: ContactWithAddressBook,
    val conflicts: List<dev.benica.corvidcontacts.data.merge.ContactMergeConflict>,
    val preview: ContactEntity,
    phonesList: List<EditableTypedValue>,
) {
    var selectedAddressBookHref by mutableStateOf(survivor.contact.addressBookHref)
    var firstName by mutableStateOf(preview.firstName ?: "")
    var lastName by mutableStateOf(preview.lastName ?: "")
    var middleName by mutableStateOf(preview.middleName ?: "")
    var prefix by mutableStateOf(preview.prefix ?: "")
    var suffix by mutableStateOf(preview.suffix ?: "")
    var displayName by mutableStateOf(preview.displayName)
    var nickname by mutableStateOf(preview.nickname ?: "")
    var company by mutableStateOf(preview.company ?: "")
    var jobTitle by mutableStateOf(preview.jobTitle ?: "")
    var birthday by mutableStateOf(preview.birthday ?: "")
    var notes by mutableStateOf(preview.notes ?: "")
    var photoChoice by mutableStateOf(PhotoChoice.SURVIVOR)

    val phones = phonesList.toMutableStateList()

    val emails = (preview.emails ?: emptyList())
        .map {
            EditableTypedValue(
                type = it.type ?: "HOME",
                value = it.value
            )
        }
        .toMutableStateList()

    val socialProfiles = (preview.socialProfiles ?: emptyList())
        .map {
            EditableTypedValue(
                type = it.type ?: "OTHER",
                value = it.value
            )
        }
        .toMutableStateList()

    val websites = (preview.websites ?: emptyList())
        .map { EditableTypedValue(value = it) }
        .toMutableStateList()

    val relationships = (preview.relationships ?: emptyList())
        .map {
            EditableRelationship(
                type = it.type,
                value = it.value,
                isUid = it.isUid
            )
        }
        .toMutableStateList()

    val structuredAddresses = (preview.structuredAddresses ?: emptyList())
        .map { EditableAddress(value = it) }
        .toMutableStateList()

    val hiddenCategories = (preview.categories ?: emptyList()).filter {
        it.equals(
            "Archived",
            ignoreCase = true
        ) || it.equals(
            ContactsRepository.FAVORITE_CATEGORY,
            ignoreCase = true
        )
    }

    val categories = (preview.categories ?: emptyList())
        .filterNot {
            it.equals(
                "Archived",
                ignoreCase = true
            ) || it.equals(
                ContactsRepository.FAVORITE_CATEGORY,
                ignoreCase = true
            )
        }
        .toMutableStateList()

    fun hasChanges(): Boolean {
        fun hasListChanges(): Boolean {
            val previewEmails = (preview.emails ?: emptyList())
                .map { it.value to it.type }
                .toSet()
            val currentEmails = emails
                .filter { it.value.isNotBlank() }
                .map { it.value to it.type }
                .toSet()
            if (previewEmails != currentEmails) return true

            val previewPhones = (preview.phones ?: emptyList())
                .map { it.value to it.type }
                .toSet()
            val currentPhones = phones
                .filter { it.value.isNotBlank() }
                .map { it.value to it.type }
                .toSet()
            if (previewPhones != currentPhones) return true

            val previewSocial = (preview.socialProfiles ?: emptyList())
                .map { it.value to it.type }
                .toSet()
            val currentSocial = socialProfiles
                .filter { it.value.isNotBlank() }
                .map { it.value to it.type }
                .toSet()
            if (previewSocial != currentSocial) return true

            if ((preview.websites ?: emptyList()).toSet() != websites
                    .filter { it.value.isNotBlank() }
                    .map { it.value }
                    .toSet()
            ) return true

            val previewRel = (preview.relationships ?: emptyList())
                .map { it.value to it.type }
                .toSet()
            val currentRel = relationships
                .filter { it.value.isNotBlank() }
                .map { it.value to it.type }
                .toSet()
            if (previewRel != currentRel) return true

            val previewAddr = (preview.structuredAddresses ?: emptyList())
                .filter { !it.isBlank() }
                .toSet()
            val currentAddr = structuredAddresses
                .map { it.value }
                .filter { !it.isBlank() }
                .toSet()
            if (previewAddr != currentAddr) return true

            val previewCat = (preview.categories ?: emptyList())
                .filter {
                    !it.equals(
                        "Archived",
                        ignoreCase = true
                    ) && !it.equals(
                        ContactsRepository.FAVORITE_CATEGORY,
                        ignoreCase = true
                    )
                }
                .toSet()
            return previewCat != categories.toSet()
        }

        return firstName != (preview.firstName ?: "") ||
                lastName != (preview.lastName ?: "") ||
                middleName != (preview.middleName ?: "") ||
                prefix != (preview.prefix ?: "") ||
                suffix != (preview.suffix ?: "") ||
                displayName != preview.displayName ||
                nickname != (preview.nickname ?: "") ||
                company != (preview.company ?: "") ||
                jobTitle != (preview.jobTitle ?: "") ||
                birthday != (preview.birthday ?: "") ||
                notes != (preview.notes ?: "") ||
                photoChoice != PhotoChoice.SURVIVOR ||
                selectedAddressBookHref != survivor.contact.addressBookHref ||
                hasListChanges()
    }

    fun buildMergedEntity(): ContactEntity = survivor.contact.copy(
        addressBookHref = selectedAddressBookHref,
        displayName = displayName.trim(),
        firstName = firstName.ifBlank { null },
        lastName = lastName.ifBlank { null },
        middleName = middleName.ifBlank { null },
        prefix = prefix.ifBlank { null },
        suffix = suffix.ifBlank { null },
        company = company.ifBlank { null },
        jobTitle = jobTitle.ifBlank { null },
        birthday = birthday.ifBlank { null },
        nickname = nickname.ifBlank { null },
        notes = notes.ifBlank { null },
        photoUrl = when (photoChoice) {
            PhotoChoice.ABSORBED -> absorbed.contact.photoUrl
            PhotoChoice.SURVIVOR -> preview.photoUrl
            PhotoChoice.NONE -> null
        },
        hasPhoto = when (photoChoice) {
            PhotoChoice.ABSORBED -> absorbed.contact.hasPhoto
            PhotoChoice.SURVIVOR -> preview.hasPhoto
            PhotoChoice.NONE -> false
        },
        emails = emails
            .filter { it.value.isNotBlank() }
            .map {
                Email(
                    it.value,
                    it.type.uppercase()
                )
            },
        phones = phones
            .filter { it.value.isNotBlank() }
            .map {
                Phone(
                    it.value,
                    it.type.uppercase(),
                    it.region
                )
            },
        socialProfiles = socialProfiles
            .filter { it.value.isNotBlank() }
            .map {
                SocialProfile(
                    it.value,
                    it.type.uppercase()
                )
            },
        websites = websites
            .filter { it.value.isNotBlank() }
            .map { it.value },
        relationships = relationships
            .filter { it.value.isNotBlank() }
            .map {
                Relationship(
                    it.type.uppercase(),
                    it.value,
                    it.isUid
                )
            },
        structuredAddresses = structuredAddresses
            .map { it.value }
            .filter { !it.isBlank() },
        categories = categories.toList() + hiddenCategories,
    )
}

@Composable
fun rememberMergeReviewState(
    survivor: ContactWithAddressBook,
    absorbed: ContactWithAddressBook,
): MergeReviewState {
    val conflicts = remember(
        survivor,
        absorbed
    ) {
        ContactMerger.detectConflicts(
            survivor.contact,
            absorbed.contact
        )
    }
    val preview = remember(
        survivor,
        absorbed,
        conflicts
    ) {
        ContactMerger.buildMergedContact(
            survivor.contact,
            absorbed.contact,
            conflicts.associate { it.field to ConflictResolution.USE_SURVIVOR }
        )
    }

    val context = LocalContext.current
    val phonesList = remember(preview.phones) {
        (preview.phones ?: emptyList()).map {
            val split = PhoneFormatter.split(it.value, context)
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
        }
    }

    return remember(
        survivor,
        absorbed,
        conflicts,
        preview,
        phonesList
    ) {
        MergeReviewState(
            survivor,
            absorbed,
            conflicts,
            preview,
            phonesList
        )
    }
}
