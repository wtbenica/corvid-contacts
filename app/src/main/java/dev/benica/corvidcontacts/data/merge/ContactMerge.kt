// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.merge

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.merge.ContactMerger.buildMergedContact
import dev.benica.corvidcontacts.data.merge.ContactMerger.detectConflicts
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.StructuredAddress

/** A single-value field where [survivor] and [absorbed] disagree and merging requires a choice. */
enum class ConflictField {
    DISPLAY_NAME, FIRST_NAME, LAST_NAME, MIDDLE_NAME, PREFIX, SUFFIX,
    COMPANY, JOB_TITLE, BIRTHDAY, NICKNAME, PHOTO_URL, NOTES
}

/** A detected conflict: both contacts have a non-blank value for [field], and the values differ. */
data class ContactMergeConflict(
    val field: ConflictField,
    val survivorValue: String,
    val absorbedValue: String,
)

/**
 * How to resolve a [ContactMergeConflict]. [KEEP_BOTH] is only meaningful for
 * [ConflictField.NOTES] - [ContactMerger.buildMergedContact] falls back to [USE_SURVIVOR] if it's
 * supplied for any other field.
 */
enum class ConflictResolution { USE_SURVIVOR, USE_ABSORBED, KEEP_BOTH }

/**
 * Computes the result of merging two contacts: [absorbed] is folded into [survivor], which keeps
 * its identity ([ContactEntity.id], sync/location fields, color, and archive state) throughout.
 *
 * Multi-value fields (phones, emails, addresses, categories, websites, social profiles,
 * relationships) are always unioned, with light de-duplication so combining two overlapping
 * contacts doesn't produce visible duplicate entries. Single-value fields auto-fill from
 * whichever side is non-blank; a choice is only needed when both sides disagree (see
 * [detectConflicts]).
 */
object ContactMerger {

    /**
     * Returns every single-value field where both [survivor] and [absorbed] have a non-blank
     * value and those values differ. Fields where only one side (or neither) has a value aren't
     * conflicts - [buildMergedContact] auto-fills those without asking.
     */
    fun detectConflicts(
        survivor: ContactEntity,
        absorbed: ContactEntity,
    ): List<ContactMergeConflict> {
        val candidates = listOf(
            ConflictField.DISPLAY_NAME to (survivor.displayName to absorbed.displayName),
            ConflictField.FIRST_NAME to (survivor.firstName to absorbed.firstName),
            ConflictField.LAST_NAME to (survivor.lastName to absorbed.lastName),
            ConflictField.MIDDLE_NAME to (survivor.middleName to absorbed.middleName),
            ConflictField.PREFIX to (survivor.prefix to absorbed.prefix),
            ConflictField.SUFFIX to (survivor.suffix to absorbed.suffix),
            ConflictField.COMPANY to (survivor.company to absorbed.company),
            ConflictField.JOB_TITLE to (survivor.jobTitle to absorbed.jobTitle),
            ConflictField.BIRTHDAY to (survivor.birthday to absorbed.birthday),
            ConflictField.NICKNAME to (survivor.nickname to absorbed.nickname),
            ConflictField.NOTES to (survivor.notes to absorbed.notes),
        )

        val textConflicts = candidates.mapNotNull { (field, values) ->
            val (survivorValue, absorbedValue) = values
            if (survivorValue
                    .isNullOrBlank()
                    .not() &&
                absorbedValue
                    .isNullOrBlank()
                    .not() &&
                survivorValue.trim() != absorbedValue.trim()
            ) {
                ContactMergeConflict(
                    field,
                    survivorValue,
                    absorbedValue
                )
            } else {
                null
            }
        }

        // Treat any overlap as a conflict since photo URLs are often null or ID-keyed
        // even for identical images.
        val photoConflict = if (survivor.hasPhoto && absorbed.hasPhoto) {
            ContactMergeConflict(
                ConflictField.PHOTO_URL,
                survivor.photoUrl ?: "",
                absorbed.photoUrl ?: ""
            )
        } else null

        return textConflicts + listOfNotNull(photoConflict)
    }

    /**
     * Builds the merged contact. [resolutions] should cover every [ContactMergeConflict] returned
     * by [detectConflicts] for the same pair - any conflict missing a resolution defaults to
     * [ConflictResolution.USE_SURVIVOR].
     */
    fun buildMergedContact(
        survivor: ContactEntity,
        absorbed: ContactEntity,
        resolutions: Map<ConflictField, ConflictResolution>,
    ): ContactEntity {
        fun resolve(
            field: ConflictField,
            survivorValue: String?,
            absorbedValue: String?,
        ): String? {
            val hasSurvivor = survivorValue
                .isNullOrBlank()
                .not()
            val hasAbsorbed = absorbedValue
                .isNullOrBlank()
                .not()
            return when {
                hasSurvivor && hasAbsorbed && survivorValue.trim() != absorbedValue.trim() ->
                    when (resolutions[field] ?: ConflictResolution.USE_SURVIVOR) {
                        ConflictResolution.USE_SURVIVOR -> survivorValue
                        ConflictResolution.USE_ABSORBED -> absorbedValue
                        ConflictResolution.KEEP_BOTH ->
                            if (field == ConflictField.NOTES) {
                                "${survivorValue.trim()}\n\n${absorbedValue.trim()}"
                            } else {
                                survivorValue
                            }
                    }

                hasSurvivor -> survivorValue
                hasAbsorbed -> absorbedValue
                else -> null
            }
        }

        val mergedRelationships = unionRelationships(
            survivor.relationships,
            absorbed.relationships
        )
            // Drop self-relationships resulting from the merge.
            .filterNot { it.isUid && (it.value == absorbed.id || it.value == survivor.id) }

        return survivor.copy(
            displayName = resolve(
                ConflictField.DISPLAY_NAME,
                survivor.displayName,
                absorbed.displayName
            ) ?: survivor.displayName,
            firstName = resolve(
                ConflictField.FIRST_NAME,
                survivor.firstName,
                absorbed.firstName
            ),
            lastName = resolve(
                ConflictField.LAST_NAME,
                survivor.lastName,
                absorbed.lastName
            ),
            middleName = resolve(
                ConflictField.MIDDLE_NAME,
                survivor.middleName,
                absorbed.middleName
            ),
            prefix = resolve(
                ConflictField.PREFIX,
                survivor.prefix,
                absorbed.prefix
            ),
            suffix = resolve(
                ConflictField.SUFFIX,
                survivor.suffix,
                absorbed.suffix
            ),
            company = resolve(
                ConflictField.COMPANY,
                survivor.company,
                absorbed.company
            ),
            jobTitle = resolve(
                ConflictField.JOB_TITLE,
                survivor.jobTitle,
                absorbed.jobTitle
            ),
            birthday = resolve(
                ConflictField.BIRTHDAY,
                survivor.birthday,
                absorbed.birthday
            ),
            nickname = resolve(
                ConflictField.NICKNAME,
                survivor.nickname,
                absorbed.nickname
            ),
            photoUrl = when {
                survivor.hasPhoto && absorbed.hasPhoto ->
                    if (resolutions[ConflictField.PHOTO_URL] == ConflictResolution.USE_ABSORBED) {
                        absorbed.photoUrl
                    } else {
                        survivor.photoUrl
                    }

                survivor.hasPhoto -> survivor.photoUrl
                absorbed.hasPhoto -> absorbed.photoUrl
                else -> null
            },
            hasPhoto = survivor.hasPhoto || absorbed.hasPhoto,
            notes = resolve(
                ConflictField.NOTES,
                survivor.notes,
                absorbed.notes
            ),
            emails = unionByKey(
                survivor.emails,
                absorbed.emails
            ) {
                it.value
                    .trim()
                    .lowercase()
            },
            phones = unionByKey(
                survivor.phones,
                absorbed.phones
            ) { phone ->
                phone.value
                    .filter { it.isDigit() }
                    .ifEmpty { phone.value.trim() }
            },
            categories = unionByKey(
                survivor.categories,
                absorbed.categories
            ) {
                it
                    .trim()
                    .lowercase()
            },
            websites = unionByKey(
                survivor.websites,
                absorbed.websites
            ) {
                it
                    .trim()
                    .lowercase()
            },
            socialProfiles = unionByKey(
                survivor.socialProfiles,
                absorbed.socialProfiles
            ) { profile ->
                (profile.type
                    ?.trim()
                    ?.lowercase() ?: "") to profile.value
                    .trim()
                    .removePrefix("@")
                    .lowercase()
            },
            structuredAddresses = unionByKey(
                survivor.structuredAddresses
                    ?.filterNot { it.isBlank() },
                absorbed.structuredAddresses
                    ?.filterNot { it.isBlank() }
            ) { addressDedupKey(it) },
            relationships = mergedRelationships
        )
    }

    private fun addressDedupKey(address: StructuredAddress): List<String> = listOf(
        address.street,
        address.city,
        address.state,
        address.postalCode,
        address.country,
        address.poBox,
        address.extended
    ).map {
        it
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }

    private fun unionRelationships(
        survivorRelationships: List<Relationship>?,
        absorbedRelationships: List<Relationship>?,
    ): List<Relationship> = unionByKey(
        survivorRelationships,
        absorbedRelationships
    ) { relationship ->
        Triple(
            relationship.type
                .trim()
                .lowercase(),
            if (relationship.isUid) relationship.value else relationship.value
                .trim()
                .lowercase(),
            relationship.isUid
        )
    }

    /** Unions [survivorList] and [absorbedList], keeping survivor's entries first and dropping
     * any entry from [absorbedList] whose [key] already appeared in [survivorList]. */
    private fun <T, K> unionByKey(
        survivorList: List<T>?,
        absorbedList: List<T>?,
        key: (T) -> K,
    ): List<T> {
        val survivorItems = survivorList ?: emptyList()
        val absorbedItems = absorbedList ?: emptyList()
        val seenKeys = survivorItems
            .map(key)
            .toMutableSet()
        val newFromAbsorbed = absorbedItems.filter { seenKeys.add(key(it)) }
        return survivorItems + newFromAbsorbed
    }
}
