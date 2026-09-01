// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.util.Base64
import android.util.Log
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.ImageType
import ezvcard.parameter.RelatedType
import ezvcard.parameter.TelephoneType
import ezvcard.property.Address
import ezvcard.property.Birthday
import ezvcard.property.Categories
import ezvcard.property.FormattedName
import ezvcard.property.Nickname
import ezvcard.property.Note
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.RawProperty
import ezvcard.property.Related
import ezvcard.property.StructuredName
import ezvcard.property.Telephone
import ezvcard.property.Title
import ezvcard.property.Uid
import ezvcard.property.Url
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import dev.benica.corvidcontacts.data.model.Email as ModelEmail
import ezvcard.property.Email as VCardEmail

/**
 * Handles bidirectional mapping between [ContactEntity] and [VCard].
 */
class VCardMapper(private val photoManager: PhotoManager) {
    private val TAG = "VCardMapper"
    private val ARCHIVED_CATEGORY = "Archived"
    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    )

    /**
     * Maps a [VCard] to a [ContactEntity].
     */
    fun mapVCardToEntity(
        vcard: VCard,
        bookHref: String,
        contactHref: String,
        etag: String?,
    ): ContactEntity {
        val id = (vcard.uid?.value ?: UUID
            .randomUUID()
            .toString()).removePrefix("urn:uuid:")
        val emails = vcard.emails.map {
            ModelEmail(
                it.value,
                it.types.firstOrNull()?.value?.uppercase()
            )
        }
        val phones = vcard.telephoneNumbers.map {
            Phone(
                it.text,
                it.types.firstOrNull()?.value?.uppercase()
            )
        }
        val categories = vcard
            .getProperties(Categories::class.java)
            .flatMap { it.values ?: emptyList() }
        val websites = vcard.urls.mapNotNull { it.value }

        val socialProfiles = vcard
            .getExtendedProperties("X-SOCIALPROFILE")
            .map { ep ->
                SocialProfile(
                    value = ep.value ?: "",
                    type = ep
                        .getParameter("TYPE")
                        ?.uppercase()
                )
            }

        val relationships = vcard
            .getProperties(Related::class.java)
            .map { rel ->
                val rawValue = rel.uri ?: rel.text ?: ""
                Relationship(
                    type = (rel.types.firstOrNull()?.value ?: "other").uppercase(),
                    value = rawValue.removePrefix("urn:uuid:"),
                    isUid = rel.uri != null || rawValue.startsWith("urn:uuid:")
                )
            }

        val colorInt = vcard.getExtendedProperty("X-NC-CONTACT-COLOR")?.value?.toIntOrNull()

        val structuredAddresses = vcard.addresses.map { adr ->
            StructuredAddress(
                type = (adr.types.firstOrNull()?.value ?: "HOME").uppercase(),
                street = adr.streetAddress,
                city = adr.locality,
                state = adr.region,
                postalCode = adr.postalCode,
                country = adr.country,
                poBox = adr.poBox,
                extended = adr.extendedAddress
            )
        }

        val photoUrl = vcard.photos
            .firstOrNull()
            ?.let { photo ->
                photo.data?.let { data ->
                    photoManager.savePhotoToFile(
                        id,
                        data
                    )
                } ?: photo.url?.trim()
            }
        val hasPhoto = photoUrl != null

        val birthday = vcard.birthday?.let { bday ->
            try {
                val date = bday.date
                when {
                    bday.text != null -> bday.text
                    date != null -> synchronized(dateFormat) { dateFormat.format(date as Date) }
                    bday.partialDate != null -> bday.partialDate.toString()
                    else -> null
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error parsing birthday for contact $id: ${e.message}"
                )
                bday.text
            }
        }

        val n = vcard.structuredName
        val fn = vcard.formattedName?.value ?: ""

        val calculatedFn = listOfNotNull(
            n?.given,
            n?.family
        )
            .joinToString(" ")
            .trim()
        val displayNameOverride = if (fn.isNotBlank() && fn != calculatedFn) fn else calculatedFn

        return ContactEntity(
            id = id,
            displayName = displayNameOverride,
            firstName = n?.given,
            lastName = n?.family,
            middleName = n?.additionalNames?.firstOrNull(),
            prefix = n?.prefixes?.firstOrNull(),
            suffix = n?.suffixes?.firstOrNull(),
            emails = emails,
            phones = phones,
            photoUrl = photoUrl,
            hasPhoto = hasPhoto,
            etag = etag,
            addressBookHref = bookHref,
            contactHref = contactHref,
            colorInt = colorInt,
            isArchived = categories.any {
                it.equals(
                    ARCHIVED_CATEGORY,
                    ignoreCase = true
                )
            },
            categories = categories,
            company = vcard.organization?.values?.firstOrNull(),
            jobTitle = vcard.titles.firstOrNull()?.value,
            birthday = birthday,
            nickname = vcard.nickname?.values?.firstOrNull(),
            notes = vcard.notes.firstOrNull()?.value,
            websites = websites,
            socialProfiles = socialProfiles,
            relationships = relationships,
            structuredAddresses = structuredAddresses
        )
    }

    /**
     * Maps a [ContactEntity] to a [VCard].
     */
    fun mapEntityToVCard(contact: ContactEntity): VCard {
        val vcard = VCard().apply {
            version = VCardVersion.V4_0
            uid = Uid(contact.id)
            structuredName = StructuredName().apply {
                family = contact.lastName
                given = contact.firstName
                contact.middleName?.let { additionalNames.add(it) }
                contact.prefix?.let { prefixes.add(it) }
                contact.suffix?.let { suffixes.add(it) }
            }

            val calculatedFn = listOfNotNull(
                contact.firstName,
                contact.lastName
            )
                .joinToString(" ")
                .trim()
            val finalFn = contact.displayName.ifBlank { calculatedFn }
            addProperty(FormattedName(finalFn.ifBlank { "Unknown" }))
        }

        contact.emails?.forEach {
            val email = VCardEmail(it.value)
            if (it.type != null) email.types.add(EmailType.get(it.type))
            vcard.addEmail(email)
        }

        contact.phones?.forEach {
            val phone = Telephone(it.value)
            if (it.type != null) phone.types.add(TelephoneType.get(it.type))
            vcard.addTelephoneNumber(phone)
        }

        contact.structuredAddresses?.forEach { addr ->
            val adr = Address().apply {
                streetAddress = addr.street
                locality = addr.city
                region = addr.state
                postalCode = addr.postalCode
                country = addr.country
                poBox = addr.poBox
                extendedAddress = addr.extended
            }
            if (addr.type != null) adr.types.add(AddressType.get(addr.type))
            vcard.addAddress(adr)
        }

        val categories = contact.categories?.toMutableList() ?: mutableListOf()
        if (contact.isArchived) {
            categories.add(ARCHIVED_CATEGORY)
        } else {
            categories.remove(ARCHIVED_CATEGORY)
        }
        vcard.setCategories(
            *categories
                .distinct()
                .toTypedArray()
        )

        contact.company?.let { vcard.organization = Organization().apply { values.add(it) } }
        contact.jobTitle?.let { vcard.addTitle(Title(it)) }
        contact.birthday?.let { vcard.birthday = Birthday(it) }
        contact.nickname?.let { vcard.nickname = Nickname().apply { values.add(it) } }
        contact.notes?.let { vcard.addNote(Note(it)) }
        contact.websites?.forEach { vcard.addUrl(Url(it)) }

        contact.colorInt?.let {
            vcard.addExtendedProperty(
                "X-NC-CONTACT-COLOR",
                it.toString()
            )
        }

        contact.socialProfiles?.forEach { sp ->
            val raw = RawProperty(
                "X-SOCIALPROFILE",
                sp.value
            )
            if (sp.type != null) raw.addParameter(
                "TYPE",
                sp.type
            )
            vcard.addProperty(raw)
        }

        contact.relationships?.forEach { rel ->
            vcard.addRelated(Related().apply {
                types.add(RelatedType.get(rel.type.lowercase()))
                if (rel.isUid) uri = "urn:uuid:${rel.value}" else text = rel.value
            })
        }

        contact.photoUrl?.let { url ->
            if (url.startsWith("file://")) {
                photoManager
                    .loadPhotoFromFile(url)
                    ?.let { data ->
                        vcard.addPhoto(
                            Photo(
                                data,
                                ImageType.JPEG
                            )
                        )
                    }
            } else if (url.startsWith("data:")) {
                try {
                    val mediaTypeStr = url
                        .substringBefore(";")
                        .substringAfter(":")
                    val imageType = when {
                        mediaTypeStr.contains(
                            "png",
                            true
                        ) -> ImageType.PNG

                        mediaTypeStr.contains(
                            "gif",
                            true
                        ) -> ImageType.GIF

                        else -> ImageType.JPEG
                    }
                    val base64 = url.substringAfter("base64,")
                    vcard.addPhoto(
                        Photo(
                            Base64.decode(
                                base64,
                                Base64.DEFAULT
                            ),
                            imageType
                        )
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to decode photo for save: ${e.message}"
                    )
                }
            } else if (url.isNotBlank()) {
                vcard.addPhoto(
                    Photo(
                        url,
                        null as ImageType?
                    )
                )
            }
        }
        return vcard
    }
}
