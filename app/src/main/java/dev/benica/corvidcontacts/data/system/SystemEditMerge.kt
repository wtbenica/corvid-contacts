// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.SystemContactsLevel

/**
 * The parts of a mirrored contact that another app can change and that are read back. Each is
 * compared and merged as a whole, so two changes to the same part conflict even if they touch
 * different items in it.
 */
enum class MirrorField {
    NAME,
    PHONES,
    EMAILS,
    ADDRESSES,
    LINKS,
    RELATIONS,
    BIRTHDAY,
    ORGANIZATION,
    NICKNAME,
    NOTE,
    PHOTO;

    /** Whether this part is written to the system contacts at [level]. */
    fun isMirroredAt(level: SystemContactsLevel): Boolean = when (this) {
        NAME, PHONES, PHOTO -> true
        NOTE -> level.includesNotes
        else -> level.includesFullContact
    }
}

/** What to take from the system contacts: the parts changed there and left alone in Corvid. */
data class SystemMerge(
    val taken: Set<MirrorField>,
    val base: MirrorContact,
    val theirs: MirrorContact,
) {
    val isEmpty: Boolean get() = taken.isEmpty()
}

/**
 * Three-way merge of edits made to a mirrored contact in another app.
 *
 * `base` is what Corvid last wrote, `theirs` is what the system contacts hold now, and `ours` is
 * what Corvid would write today. A part is taken from the system contacts only when it changed
 * there and has not changed in Corvid since it was written. When both sides changed the same part,
 * Corvid's value wins and the system copy is rewritten from it.
 */
object SystemEditMerge {
    fun merge(base: MirrorContact, theirs: MirrorContact, ours: MirrorContact?): SystemMerge {
        if (ours == null) return SystemMerge(emptySet(), base, theirs)
        val taken = MirrorField.entries
            .filter { it.isMirroredAt(base.level) }
            .filter { field ->
                if (field == MirrorField.PHOTO) return@filter photoTaken(base, theirs, ours)
                val written = value(base, field, base.level)
                value(theirs, field, base.level) != written && value(ours, field, base.level) == written
            }
            .toSet()
        return SystemMerge(taken, base, theirs)
    }

    /**
     * The photo is compared by its stand-in in the system contacts, since the bytes can't be. With
     * no stand-in recorded for a photo that was written, there is nothing to compare, so it is left.
     */
    private fun photoTaken(base: MirrorContact, theirs: MirrorContact, ours: MirrorContact): Boolean {
        val comparable = base.systemPhoto != null || base.photoStamp == null
        return comparable && theirs.systemPhoto != base.systemPhoto && ours.photoStamp == base.photoStamp
    }

    /**
     * Applies the parts of [merge] that were taken to [entity]. [photoUrl] is the file the taken
     * photo was saved to, or `null` if it was removed.
     */
    fun apply(entity: ContactEntity, merge: SystemMerge, photoUrl: String? = null): ContactEntity {
        var result = entity
        val theirs = merge.theirs
        for (field in merge.taken) {
            result = when (field) {
                MirrorField.NAME -> result.withName(merge.base, theirs)
                MirrorField.PHONES -> result.copy(phones = mergePhones(result.phones, theirs.phones))
                MirrorField.EMAILS -> result.copy(emails = mergeEmails(result.emails, theirs.emails))
                MirrorField.ADDRESSES -> result.copy(
                    structuredAddresses = mergeAddresses(result.structuredAddresses, theirs.addresses)
                )

                MirrorField.LINKS -> result.withLinks(theirs)
                MirrorField.RELATIONS -> result.copy(
                    relationships = mergeRelations(result.relationships, theirs.relations)
                )

                MirrorField.BIRTHDAY -> result.copy(birthday = theirs.birthday)
                MirrorField.ORGANIZATION -> result.copy(
                    company = theirs.organization?.company,
                    jobTitle = theirs.organization?.title
                )

                MirrorField.NICKNAME -> result.copy(nickname = theirs.nickname)
                MirrorField.NOTE -> result.copy(notes = theirs.note)
                MirrorField.PHOTO -> result.copy(photoUrl = photoUrl, hasPhoto = photoUrl != null)
            }
        }
        return result
    }

    /** A value for [field] that is equal exactly when the part has not meaningfully changed. */
    private fun value(contact: MirrorContact, field: MirrorField, level: SystemContactsLevel): Any? = when (field) {
        MirrorField.NAME -> buildList {
            add(contact.displayName.trim())
            add(contact.givenName.norm())
            add(contact.familyName.norm())
            if (level.includesFullContact) {
                add(contact.middleName.norm())
                add(contact.prefix.norm())
                add(contact.suffix.norm())
            }
        }

        MirrorField.PHONES -> contact.phones.map { "${phoneKey(it.number)}|${it.type}" }.sorted()
        MirrorField.EMAILS -> contact.emails.map { "${it.address.trim().lowercase()}|${it.type}" }.sorted()
        MirrorField.ADDRESSES -> contact.addresses.map {
            listOf(it.street, it.poBox, it.city, it.region, it.postcode, it.country)
                .joinToString("|") { part -> part.norm().orEmpty() } + "|${it.type}"
        }.sorted()

        MirrorField.LINKS -> (contact.websites + contact.profileLinks)
            .mapNotNull { it.norm() }
            .toSet()

        MirrorField.RELATIONS -> contact.relations.map { "${it.name.trim()}|${it.type}|${it.label.norm()}" }.sorted()
        MirrorField.BIRTHDAY -> contact.birthday.norm()
        MirrorField.ORGANIZATION -> contact.organization?.let { it.company.norm() to it.title.norm() }
        MirrorField.NICKNAME -> contact.nickname.norm()
        MirrorField.NOTE -> contact.note?.replace("\r\n", "\n").norm()
        MirrorField.PHOTO -> contact.systemPhoto
    }

    private fun String?.norm(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** A phone number with the formatting removed, so reformatting a number isn't an edit. */
    internal fun phoneKey(number: String): String {
        val trimmed = number.trim()
        return (if (trimmed.startsWith("+")) "+" else "") + trimmed.filter { it.isDigit() }
    }

    private fun ContactEntity.withName(base: MirrorContact, theirs: MirrorContact): ContactEntity {
        var result = copy(firstName = theirs.givenName.norm(), lastName = theirs.familyName.norm())
        if (base.level.includesFullContact) {
            result = result.copy(
                middleName = theirs.middleName.norm(),
                prefix = theirs.prefix.norm(),
                suffix = theirs.suffix.norm()
            )
        }
        // A display name the other app left untouched is not a choice: an automatic name in Corvid
        // follows the name parts, and an override stays.
        val display = theirs.displayName.trim()
        if (display != base.displayName.trim()) {
            val automatic = listOfNotNull(result.firstName, result.lastName).joinToString(" ")
            result = result.copy(
                displayName = if (display == automatic || display == result.getFullName()) "" else display
            )
        }
        return result
    }

    private fun mergePhones(current: List<Phone>?, theirs: List<MirrorPhone>): List<Phone> {
        val remaining = current.orEmpty().toMutableList()
        return theirs.map { phone ->
            val i = remaining.indexOfFirst {
                phoneKey(it.value) == phoneKey(phone.number) && MirrorPlan.phoneType(it.type) == phone.type
            }
            if (i >= 0) remaining.removeAt(i) else Phone(phone.number.trim(), MirrorPlan.phoneTypeName(phone.type))
        }
    }

    private fun mergeEmails(current: List<Email>?, theirs: List<MirrorEmail>): List<Email> {
        val remaining = current.orEmpty().toMutableList()
        return theirs.map { email ->
            val i = remaining.indexOfFirst {
                it.value.trim().equals(email.address.trim(), ignoreCase = true) &&
                        MirrorPlan.emailType(it.type) == email.type
            }
            if (i >= 0) remaining.removeAt(i) else Email(email.address.trim(), MirrorPlan.emailTypeName(email.type))
        }
    }

    private fun mergeAddresses(
        current: List<StructuredAddress>?,
        theirs: List<MirrorAddress>,
    ): List<StructuredAddress> {
        val remaining = current.orEmpty().toMutableList()
        return theirs.map { address ->
            val i = remaining.indexOfFirst {
                listOf(it.street, it.poBox, it.city, it.state, it.postalCode, it.country).map { part -> part.norm() } ==
                        listOf(address.street, address.poBox, address.city, address.region, address.postcode, address.country)
                            .map { part -> part.norm() } &&
                        MirrorPlan.addressType(it.type) == address.type
            }
            if (i >= 0) {
                remaining.removeAt(i)
            } else {
                val structured = listOf(
                    address.street,
                    address.poBox,
                    address.city,
                    address.region,
                    address.postcode,
                    address.country
                ).any { it.norm() != null }
                StructuredAddress(
                    type = MirrorPlan.addressTypeName(address.type),
                    // An address typed as one line has no parts, so it is kept whole as the street.
                    street = if (structured) address.street.norm() else address.formatted.norm(),
                    city = address.city.norm(),
                    state = address.region.norm(),
                    postalCode = address.postcode.norm(),
                    country = address.country.norm(),
                    poBox = address.poBox.norm()
                )
            }
        }
    }

    private fun ContactEntity.withLinks(theirs: MirrorContact): ContactEntity {
        val urls = (theirs.websites + theirs.profileLinks).mapNotNull { it.norm() }.distinct()
        val keptWebsites = websites.orEmpty().filter { it.norm() in urls }
        val keptProfiles = socialProfiles.orEmpty().filter {
            it.value.isNotBlank() && it.getWebFallback() in urls
        }
        val covered = keptWebsites.mapNotNull { it.norm() } + keptProfiles.map { it.getWebFallback() }
        return copy(
            websites = keptWebsites + urls.filter { it !in covered },
            socialProfiles = keptProfiles
        )
    }

    private fun mergeRelations(current: List<Relationship>?, theirs: List<MirrorRelation>): List<Relationship> {
        // A relationship stored as a contact UID is never written, so it can't have been edited.
        val unmirrored = current.orEmpty().filter { it.isUid || it.value.isBlank() }
        val remaining = current.orEmpty().filterNot { it.isUid || it.value.isBlank() }.toMutableList()
        return unmirrored + theirs.map { relation ->
            val i = remaining.indexOfFirst { MirrorPlan.relation(it.type, it.value.trim()) == relation }
            if (i >= 0) remaining.removeAt(i) else Relationship(MirrorPlan.relationTypeName(relation), relation.name)
        }
    }
}
