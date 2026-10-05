// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.security.MessageDigest

// The `type` values below are those of android.provider.ContactsContract.CommonDataKinds.*,
// duplicated as constants (see MirrorPlan) so the plan stays free of Android dependencies and
// unit-testable.

/** A phone number as written to the system contacts. [type] is a `Phone.TYPE_*` value. */
@JsonClass(generateAdapter = true)
data class MirrorPhone(val number: String, val type: Int)

/** An email address. [type] is an `Email.TYPE_*` value. */
@JsonClass(generateAdapter = true)
data class MirrorEmail(val address: String, val type: Int)

/** A postal address. [type] is a `StructuredPostal.TYPE_*` value. */
@JsonClass(generateAdapter = true)
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

/** A relationship. [type] is a `Relation.TYPE_*` value; [label] is used when it is custom. */
@JsonClass(generateAdapter = true)
data class MirrorRelation(val name: String, val type: Int, val label: String?)

@JsonClass(generateAdapter = true)
data class MirrorOrganization(val company: String?, val title: String?)

/**
 * What the chosen [SystemContactsLevel] writes to the system contacts for one contact.
 * [photoStamp] identifies the photo file's current contents (or `null` for none), so a changed
 * photo changes [hash]. [bookHref] is the shared address book it belongs to and [categories] its
 * groups (only at the Full contact level and up). [groupIds] are the system groups for the book and
 * categories, which are only known once the groups have been synced, so they are filled in
 * afterwards.
 */
@JsonClass(generateAdapter = true)
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
    val profileLinks: List<String> = emptyList(),
    val relations: List<MirrorRelation> = emptyList(),
    val birthday: String? = null,
    val organization: MirrorOrganization? = null,
    val nickname: String? = null,
    val note: String? = null,
    val categories: List<String> = emptyList(),
    /** Whether the contact is a favorite, written as the provider's starred flag at every level. */
    val starred: Boolean = false,
    val photoStamp: String?,
    val bookHref: String,
    val groupIds: List<Long> = emptyList(),
    /** The sharing level this was written at, so a read-back knows which fields were mirrored. */
    val level: SystemContactsLevel = SystemContactsLevel.CALLER_ID,
    /**
     * A stand-in for the photo as the provider stored it right after it was written, or as it is
     * now when read back. Not part of [hash], since it is only known once the write is done.
     */
    val systemPhoto: String? = null,
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
            profileLinks,
            relations,
            birthday,
            organization,
            nickname,
            note,
            categories,
            starred,
            photoStamp,
            bookHref,
            groupIds,
            level,
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

    /** This plan without anything for the contacts in [ids]. */
    fun excluding(ids: Set<ContactId>): MirrorPlan = if (ids.isEmpty()) {
        this
    } else {
        MirrorPlan(
            inserts.filter { it.id !in ids },
            updates.filter { (contact, _) -> contact.id !in ids },
            deletes.filter { it.contactId !in ids }
        )
    }

    companion object {
        /** Key identifying the system group that mirrors the address book [href]. */
        fun bookGroupKey(href: String) = "book:$href"

        /** Key identifying the system group that mirrors the contact category [name]. */
        fun categoryGroupKey(name: String) = "category:$name"

        /**
         * Categories that aren't mirrored as groups. `Archived` is bookkeeping, not a group the
         * user made. `Favorites` is a group in Corvid and on CardDAV, but the system contacts have
         * a dedicated starred flag for it, which is written instead (see [MirrorContact.starred]).
         * Compared ignoring case, like the rest of the app treats favorites.
         */
        private val HIDDEN_CATEGORIES = setOf("archived", ContactsRepository.FAVORITE_CATEGORY.lowercase())

        /**
         * Builds the [MirrorContact] for every one of [sources] that can be mirrored, each at the
         * level of the address book it is in ([levels], by book href). A book missing from
         * [levels] falls back to the most private level. Contacts in [hidden] are left out.
         */
        fun toMirrorContacts(
            sources: List<MirrorSource>,
            levels: Map<String, SystemContactsLevel>,
            photoStamp: (MirrorSource) -> String?,
            hidden: Set<ContactId> = emptySet(),
        ): List<MirrorContact> = sources.filter { it.id !in hidden }.mapNotNull { source ->
            toMirrorContact(
                source,
                photoStamp(source),
                levels[source.addressBookHref] ?: SystemContactsLevel.CALLER_ID
            )
        }

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
                starred = source.categories.orEmpty().any {
                    it.trim().equals(ContactsRepository.FAVORITE_CATEGORY, ignoreCase = true)
                },
                bookHref = source.addressBookHref,
                level = level,
            )
            if (!level.includesFullContact) return base

            val websites = source.websites.orEmpty().mapNotNull { it.clean() }
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
                websites = websites,
                profileLinks = source.socialProfiles
                    .orEmpty()
                    .filter { it.value.isNotBlank() }
                    .map { it.copy(value = it.value.trim()).getWebFallback() }
                    .distinct()
                    .filter { it !in websites },
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
                    .filter { it.lowercase() !in HIDDEN_CATEGORIES }
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

        /** The vCard TEL type for a `Phone.TYPE_*` value, or `null` when there is no matching one. */
        internal fun phoneTypeName(type: Int): String? = when (type) {
            TYPE_MOBILE -> "CELL"
            TYPE_HOME -> "HOME"
            TYPE_WORK -> "WORK"
            PHONE_TYPE_FAX_WORK, PHONE_TYPE_FAX_HOME, PHONE_TYPE_OTHER_FAX -> "FAX"
            PHONE_TYPE_MAIN -> "MAIN"
            else -> null
        }

        /** The vCard EMAIL type for an `Email.TYPE_*` value, or `null` for other. */
        internal fun emailTypeName(type: Int): String? = when (type) {
            EMAIL_TYPE_HOME -> "HOME"
            EMAIL_TYPE_WORK -> "WORK"
            EMAIL_TYPE_MOBILE -> "CELL"
            else -> null
        }

        /** The vCard ADR type for a `StructuredPostal.TYPE_*` value, or `null` for other. */
        internal fun addressTypeName(type: Int): String? = when (type) {
            POSTAL_TYPE_HOME -> "HOME"
            POSTAL_TYPE_WORK -> "WORK"
            else -> null
        }

        /** The relationship type for a [MirrorRelation], the reverse of [relation]. */
        internal fun relationTypeName(relation: MirrorRelation): String = when (relation.type) {
            1 -> "ASSISTANT"
            2 -> "BROTHER"
            3 -> "CHILD"
            4 -> "DOMESTIC_PARTNER"
            5 -> "FATHER"
            6 -> "FRIEND"
            7 -> "MANAGER"
            8 -> "MOTHER"
            9 -> "PARENT"
            10 -> "PARTNER"
            11 -> "REFERRED_BY"
            12 -> "RELATIVE"
            13 -> "SISTER"
            14 -> "SPOUSE"
            else -> relation.label?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: "OTHER"
        }

        private val snapshotAdapter by lazy { Moshi.Builder().build().adapter(MirrorContact::class.java) }

        /** What [contact] looked like when it was written, for [SystemEditMerge] to compare against. */
        fun snapshotOf(contact: MirrorContact): String = snapshotAdapter.toJson(contact)

        /** The contact in [snapshot], or `null` if there is none or it can't be read. */
        fun readSnapshot(snapshot: String?): MirrorContact? =
            snapshot?.let { runCatching { snapshotAdapter.fromJson(it) }.getOrNull() }

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
        const val PHONE_TYPE_FAX_WORK = 4
        const val PHONE_TYPE_FAX_HOME = 5
        const val PHONE_TYPE_MAIN = 12
        const val PHONE_TYPE_OTHER_FAX = 13
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
