// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import java.security.MessageDigest

/** A phone number as written to the system contacts. [type] is a `Phone.TYPE_*` constant value. */
data class MirrorPhone(val number: String, val type: Int)

/**
 * What the "Caller ID" sharing level writes to the system contacts for one contact: name, phone
 * numbers and photo. [photoStamp] identifies the photo file's current contents (or `null` for
 * none), so a changed photo changes [hash].
 */
data class MirrorContact(
    val id: ContactId,
    val displayName: String,
    val givenName: String?,
    val familyName: String?,
    val phones: List<MirrorPhone>,
    val photoStamp: String?,
) {
    /** Stable fingerprint of everything that gets written; unchanged contacts aren't touched. */
    val hash: String by lazy {
        val canonical = buildString {
            append(displayName).append('\u0000')
            append(givenName.orEmpty()).append('\u0000')
            append(familyName.orEmpty()).append('\u0000')
            phones.forEach { append(it.number).append('\u0001').append(it.type).append('\u0000') }
            append(photoStamp.orEmpty())
        }
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
        /**
         * Builds the caller-ID-level [MirrorContact] for [source], or `null` if it shouldn't be
         * mirrored: a contact with no name or no phone number is no use for caller ID, so it
         * isn't shared at all.
         */
        fun toMirrorContact(source: MirrorSource, photoStamp: String?): MirrorContact? {
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

            return MirrorContact(
                id = source.id,
                displayName = name,
                givenName = source.firstName?.takeIf { it.isNotBlank() },
                familyName = source.lastName?.takeIf { it.isNotBlank() },
                phones = phones,
                photoStamp = if (source.hasPhoto) photoStamp else null,
            )
        }

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

        // Values of android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_*, duplicated here
        // so the plan stays free of Android dependencies and unit-testable.
        const val TYPE_HOME = 1
        const val TYPE_MOBILE = 2
        const val TYPE_WORK = 3
        const val TYPE_OTHER = 7

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
