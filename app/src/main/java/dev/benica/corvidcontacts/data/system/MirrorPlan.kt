// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import java.security.MessageDigest

// The `type` values below are those of android.provider.ContactsContract.CommonDataKinds.*,
// duplicated as constants (see MirrorPlan) so the plan stays free of Android dependencies and
// unit-testable.

/** A phone number as written to the system contacts. [type] is a `Phone.TYPE_*` value. */
data class MirrorPhone(val number: String, val type: Int)

/** An email address. [type] is an `Email.TYPE_*` value. */
data class MirrorEmail(val address: String, val type: Int)

/** A postal address. [type] is a `StructuredPostal.TYPE_*` value. */
data class MirrorAddress(
    val formatted: String,
    val street: String?,
    val poBox: String?,
    val city: String?,
    val region: String?,
    val postcode: String?,
    val country: String?,
    val type: Int,
)

/** A social profile, written as a custom-protocol instant messaging row. */
data class MirrorSocial(val handle: String, val network: String?)

/** A relationship. [type] is a `Relation.TYPE_*` value; [label] is used when it is custom. */
data class MirrorRelation(val name: String, val type: Int, val label: String?)

data class MirrorOrganization(val company: String?, val title: String?)

/**
 * What the chosen [SystemContactsLevel] writes to the system contacts for one contact.
 * [photoStamp] identifies the photo file's current contents (or `null` for none), so a changed
 * photo changes [hash]. [bookHref] is the shared address book it belongs to and [categories] its
 * groups (only at the Full contact level and up). [groupIds] are the system groups for the book and
 * categories, which are only known once the groups have been synced, so they are filled in
 * afterwards.
 */
data class MirrorContact(
    val id: ContactId,
    val displayName: String,
    val givenName: String?,
    val familyName: String?,
    val middleName: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    val phones: List<MirrorPhone>,
    val emails: List<MirrorEmail> = emptyList(),
    val addresses: List<MirrorAddress> = emptyList(),
    val websites: List<String> = emptyList(),
    val socials: List<MirrorSocial> = emptyList(),
    val relations: List<MirrorRelation> = emptyList(),
    val birthday: String? = null,
    val organization: MirrorOrganization? = null,
    val nickname: String? = null,
    val note: String? = null,
    val categories: List<String> = emptyList(),
    val photoStamp: String?,
    val bookHref: String,
    val groupIds: List<Long> = emptyList(),
) {
    /**
     * Stable fingerprint of everything that gets written; unchanged contacts aren't touched.
     * Changing the sharing level changes it, which is what makes a level change rewrite contacts.
     */
    val hash: String by lazy {
        val canonical = listOf(
            displayName,
            givenName,
            familyName,
            middleName,
            prefix,
            suffix,
            phones,
            emails,
            addresses,
            websites,
            socials,
            relations,
            birthday,
            organization,
            nickname,
            note,
            categories,
            photoStamp,
            bookHref,
            groupIds,
        ).joinToString("\u0000") { it.toString() }
        MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

/** The inserts, updates and deletes needed to bring the system contacts in line with Room. */
data class MirrorPlan(
    val inserts: List<MirrorContact>,
    /** Contacts whose hash changed, paired with the raw contact to update. */
    val updates: List<Pair<MirrorContact, SystemContactMirrorEntity>>,
    val deletes: List<SystemContactMirrorEntity>,
) {
    val isEmpty: Boolean get() = inserts.isEmpty() && updates.isEmpty() && deletes.isEmpty()

    companion object {
        /** Key identifying the system group that mirrors the address book [href]. */
        fun bookGroupKey(href: String) = "book:$href"

        /** Key identifying the system group that mirrors the contact category [name]. */
        fun categoryGroupKey(name: String) = "category:$name"

        /** Categories that are bookkeeping, not groups the user made, and so aren't mirrored. */
        private val HIDDEN_CATEGORIES = setOf("Archived")

        /**
         * Builds the [MirrorContact] for [source] at [level], or `null` if it shouldn't be
         * mirrored: a contact with no name or no phone number is no use for caller ID, so it
         * isn't shared at all.
         */
        fun toMirrorContact(
            source: MirrorSource,
            photoStamp: String?,
            level: SystemContactsLevel = SystemContactsLevel.CALLER_ID,
        ): MirrorContact? {
            val name = source.displayName.ifBlank {
                listOfNotNull(source.firstName, source.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }
            if (name.isBlank()) return null

            val phones = source.phones
                .orEmpty()
                .filter { it.value.isNotBlank() }
                .map { MirrorPhone(it.value.trim(), phoneType(it.type)) }
            if (phones.isEmpty()) return null

            val base = MirrorContact(
                id = source.id,
                displayName = name,
                givenName = source.firstName.clean(),
                familyName = source.lastName.clean(),
                phones = phones,
                photoStamp = if (source.hasPhoto) photoStamp else null,
                bookHref = source.addressBookHref,
            )
            if (!level.includesFullContact) return base

            val full = base.copy(
                middleName = source.middleName.clean(),
                prefix = source.prefix.clean(),
                suffix = source.suffix.clean(),
                emails = source.emails
                    .orEmpty()
                    .filter { it.value.isNotBlank() }
                    .map { MirrorEmail(it.value.trim(), emailType(it.type)) },
                addresses = source.structuredAddresses
                    .orEmpty()
                    .filterNot { it.isBlank() }
                    .map {
                        MirrorAddress(
                            formatted = it.itemDisplay(),
                            street = it.street.clean(),
                            poBox = it.poBox.clean(),
                            city = it.city.clean(),
                            region = it.state.clean(),
                            postcode = it.postalCode.clean(),
                            country = it.country.clean(),
                            type = addressType(it.type),
                        )
                    },
                websites = source.websites.orEmpty().mapNotNull { it.clean() },
                socials = source.socialProfiles
                    .orEmpty()
                    .filter { it.value.isNotBlank() }
                    .map { MirrorSocial(it.value.trim(), it.type.clean()) },
                // A relationship stored as a contact UID has no name to show, so it is left out.
                relations = source.relationships
                    .orEmpty()
                    .filter { !it.isUid && it.value.isNotBlank() }
                    .map { relation(it.type, it.value.trim()) },
                birthday = normalizeBirthday(source.birthday),
                organization = MirrorOrganization(source.company.clean(), source.jobTitle.clean())
                    .takeIf { it.company != null || it.title != null },
                nickname = source.nickname.clean(),
                categories = source.categories
                    .orEmpty()
                    .mapNotNull { it.clean() }
                    .filter { it !in HIDDEN_CATEGORIES }
                    .distinct(),
            )
            return if (level.includesNotes) full.copy(note = source.notes.clean()) else full
        }

        private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

        /** Keeps only birthdays in a form the provider understands: `yyyy-MM-dd` or `--MM-dd`. */
        internal fun normalizeBirthday(raw: String?): String? {
            val value = raw?.trim() ?: return null
            return when {
                FULL_DATE.matches(value) -> value
                PARTIAL_DATE.matches(value) -> value
                PARTIAL_DATE_COMPACT.matches(value) -> "--${value.substring(2, 4)}-${value.substring(4, 6)}"
                else -> null
            }
        }

        private val FULL_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        private val PARTIAL_DATE = Regex("""--\d{2}-\d{2}""")
        private val PARTIAL_DATE_COMPACT = Regex("""--\d{4}""")

        /** Maps a vCard TEL type string (e.g. `"CELL"`, `"HOME,VOICE"`) to a `Phone.TYPE_*` value. */
        internal fun phoneType(type: String?): Int {
            val t = type?.uppercase().orEmpty()
            return when {
                "CELL" in t || "MOBILE" in t -> TYPE_MOBILE
                "HOME" in t -> TYPE_HOME
                "WORK" in t -> TYPE_WORK
                else -> TYPE_OTHER
            }
        }

        /** Maps a vCard EMAIL type string to an `Email.TYPE_*` value. */
        internal fun emailType(type: String?): Int {
            val t = type?.uppercase().orEmpty()
            return when {
                "HOME" in t -> EMAIL_TYPE_HOME
                "WORK" in t -> EMAIL_TYPE_WORK
                "MOBILE" in t || "CELL" in t -> EMAIL_TYPE_MOBILE
                else -> EMAIL_TYPE_OTHER
            }
        }

        /** Maps a vCard ADR type string to a `StructuredPostal.TYPE_*` value. */
        internal fun addressType(type: String?): Int {
            val t = type?.uppercase().orEmpty()
            return when {
                "HOME" in t -> POSTAL_TYPE_HOME
                "WORK" in t -> POSTAL_TYPE_WORK
                else -> POSTAL_TYPE_OTHER
            }
        }

        /** Maps a relationship type to a `Relation.TYPE_*` value, keeping unknown ones as a label. */
        internal fun relation(type: String, name: String): MirrorRelation {
            val mapped = when (type.uppercase()) {
                "ASSISTANT" -> 1
                "CHILD" -> 3
                "FRIEND" -> 6
                "MANAGER" -> 7
                "PARENT" -> 9
                "SPOUSE" -> 14
                else -> null
            }
            return if (mapped != null) {
                MirrorRelation(name, mapped, null)
            } else {
                val label = type
                    .lowercase()
                    .replaceFirstChar { it.titlecase() }
                    .takeIf { it.isNotBlank() }
                MirrorRelation(name, RELATION_TYPE_CUSTOM, label)
            }
        }

        const val TYPE_HOME = 1
        const val TYPE_MOBILE = 2
        const val TYPE_WORK = 3
        const val TYPE_OTHER = 7

        // Email and StructuredPostal number their types differently from Phone (home 1, work 2,
        // other 3, and email also has mobile 4), so they get their own constants.
        const val EMAIL_TYPE_HOME = 1
        const val EMAIL_TYPE_WORK = 2
        const val EMAIL_TYPE_OTHER = 3
        const val EMAIL_TYPE_MOBILE = 4
        const val POSTAL_TYPE_HOME = 1
        const val POSTAL_TYPE_WORK = 2
        const val POSTAL_TYPE_OTHER = 3
        const val RELATION_TYPE_CUSTOM = 0

        /**
         * Diffs [desired] against what [mapped] says is currently written. A contact that is
         * mapped but no longer desired (deleted, archived, lost its phone numbers) is deleted
         * from the system contacts.
         */
        fun diff(desired: List<MirrorContact>, mapped: List<SystemContactMirrorEntity>): MirrorPlan {
            val mappedById = mapped.associateBy { it.contactId }
            val desiredIds = desired.mapTo(HashSet()) { it.id }

            val inserts = ArrayList<MirrorContact>()
            val updates = ArrayList<Pair<MirrorContact, SystemContactMirrorEntity>>()
            for (contact in desired) {
                val existing = mappedById[contact.id]
                when {
                    existing == null -> inserts += contact
                    existing.hash != contact.hash -> updates += contact to existing
                }
            }
            val deletes = mapped.filter { it.contactId !in desiredIds }
            return MirrorPlan(inserts, updates, deletes)
        }
    }
}
