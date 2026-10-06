// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website

/**
 * One row of the provider's data table: its kind and its generic columns. [columns] holds DATA1
 * through DATA10, so what each one means depends on [mimeType].
 */
internal class DataRow(val mimeType: String?, private val columns: List<String?>) {
    /** DATA[n] (1 to 10), trimmed, or `null` if it is missing or blank. */
    fun text(n: Int): String? = columns.getOrNull(n - 1)?.trim()?.takeIf { it.isNotEmpty() }

    /** DATA[n] as a number, or 0 if it is missing. */
    fun int(n: Int): Int = columns.getOrNull(n - 1)?.trim()?.toIntOrNull() ?: 0
}

/**
 * What the provider holds for one raw contact, in the shape the mirror writes. It has no id, book or
 * level of its own, and only the kinds of row the mirror writes are read; the first name, birthday,
 * organization, nickname and note are taken, and a second one is ignored. A birthday is kept as the
 * provider has it, since [SystemEditMerge] decides whether it makes sense.
 */
internal fun List<DataRow>.toMirrorContact(photoToken: String?): MirrorContact {
    var name: DataRow? = null
    val phones = ArrayList<MirrorPhone>()
    val emails = ArrayList<MirrorEmail>()
    val addresses = ArrayList<MirrorAddress>()
    val websites = ArrayList<String>()
    val profileLinks = ArrayList<String>()
    val relations = ArrayList<MirrorRelation>()
    var birthday: String? = null
    var organization: MirrorOrganization? = null
    var nickname: String? = null
    var note: String? = null

    for (row in this) {
        when (row.mimeType) {
            StructuredName.CONTENT_ITEM_TYPE -> if (name == null) name = row
            Phone.CONTENT_ITEM_TYPE -> row.text(1)?.let { phones += MirrorPhone(it, row.int(2)) }
            Email.CONTENT_ITEM_TYPE -> row.text(1)?.let { emails += MirrorEmail(it, row.int(2)) }
            StructuredPostal.CONTENT_ITEM_TYPE -> addresses += MirrorAddress(
                formatted = row.text(1).orEmpty(),
                street = row.text(4),
                poBox = row.text(5),
                city = row.text(7),
                region = row.text(8),
                postcode = row.text(9),
                country = row.text(10),
                type = row.int(2)
            )

            Website.CONTENT_ITEM_TYPE -> row.text(1)?.let {
                if (row.int(2) == Website.TYPE_PROFILE) profileLinks += it else websites += it
            }

            Relation.CONTENT_ITEM_TYPE -> row.text(1)?.let {
                relations += MirrorRelation(it, row.int(2), row.text(3))
            }

            Event.CONTENT_ITEM_TYPE -> if (row.int(2) == Event.TYPE_BIRTHDAY && birthday == null) {
                birthday = row.text(1)
            }

            Organization.CONTENT_ITEM_TYPE -> if (organization == null) {
                organization = MirrorOrganization(row.text(1), row.text(4))
                    .takeIf { it.company != null || it.title != null }
            }

            Nickname.CONTENT_ITEM_TYPE -> if (nickname == null) nickname = row.text(1)
            Note.CONTENT_ITEM_TYPE -> if (note == null) note = row.text(1)
        }
    }

    return MirrorContact(
        id = "",
        displayName = name?.text(1).orEmpty(),
        givenName = name?.text(2),
        familyName = name?.text(3),
        middleName = name?.text(5),
        prefix = name?.text(4),
        suffix = name?.text(6),
        phones = phones,
        emails = emails,
        addresses = addresses,
        websites = websites,
        profileLinks = profileLinks,
        relations = relations,
        birthday = birthday,
        organization = organization,
        nickname = nickname,
        note = note,
        photoStamp = null,
        bookHref = "",
        systemPhoto = photoToken,
    )
}
