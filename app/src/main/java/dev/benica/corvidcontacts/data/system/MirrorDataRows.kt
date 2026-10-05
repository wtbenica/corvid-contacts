// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.ContentProviderOperation
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Data

/** Builds the data rows that make up a mirrored contact, and the delete that clears them. */
internal class MirrorDataRows(private val provider: MirrorProvider) {

    /**
     * Deletes the rows of the kinds Corvid writes for [rawId]. Anniversaries, IM and custom rows
     * added by another app are left alone.
     */
    fun deleteManaged(rawId: Long): ContentProviderOperation =
        ContentProviderOperation
            .newDelete(provider.forRow(Data.CONTENT_URI))
            .withSelection(MANAGED_ROWS_SELECTION, arrayOf(rawId.toString()) + MANAGED_ROWS_ARGS)
            .build()

    /**
     * The data rows for [contact] - name, groups, phones and whatever the sharing level adds;
     * [bindRaw] attaches each to its raw contact.
     */
    fun build(
        contact: MirrorContact,
        bindRaw: (ContentProviderOperation.Builder) -> ContentProviderOperation.Builder,
    ): List<ContentProviderOperation> {
        val ops = ArrayList<ContentProviderOperation>()
        fun row(mimeType: String, fill: ContentProviderOperation.Builder.() -> Unit) {
            val builder = bindRaw(ContentProviderOperation.newInsert(provider.forAccount(Data.CONTENT_URI)))
                .withValue(Data.MIMETYPE, mimeType)
            builder.fill()
            ops += builder.build()
        }

        row(StructuredName.CONTENT_ITEM_TYPE) {
            withValue(StructuredName.DISPLAY_NAME, contact.displayName)
            withValue(StructuredName.GIVEN_NAME, contact.givenName)
            withValue(StructuredName.FAMILY_NAME, contact.familyName)
            withValue(StructuredName.MIDDLE_NAME, contact.middleName)
            withValue(StructuredName.PREFIX, contact.prefix)
            withValue(StructuredName.SUFFIX, contact.suffix)
        }
        contact.groupIds.forEach { groupId ->
            row(GroupMembership.CONTENT_ITEM_TYPE) { withValue(GroupMembership.GROUP_ROW_ID, groupId) }
        }
        contact.phones.forEach { phone ->
            row(Phone.CONTENT_ITEM_TYPE) {
                withValue(Phone.NUMBER, phone.number)
                withValue(Phone.TYPE, phone.type)
            }
        }
        contact.emails.forEach { email ->
            row(Email.CONTENT_ITEM_TYPE) {
                withValue(Email.ADDRESS, email.address)
                withValue(Email.TYPE, email.type)
            }
        }
        contact.addresses.forEach { address ->
            row(StructuredPostal.CONTENT_ITEM_TYPE) {
                withValue(StructuredPostal.FORMATTED_ADDRESS, address.formatted)
                withValue(StructuredPostal.STREET, address.street)
                withValue(StructuredPostal.POBOX, address.poBox)
                withValue(StructuredPostal.CITY, address.city)
                withValue(StructuredPostal.REGION, address.region)
                withValue(StructuredPostal.POSTCODE, address.postcode)
                withValue(StructuredPostal.COUNTRY, address.country)
                withValue(StructuredPostal.TYPE, address.type)
            }
        }
        contact.websites.forEach { url ->
            row(Website.CONTENT_ITEM_TYPE) {
                withValue(Website.URL, url)
                withValue(Website.TYPE, Website.TYPE_OTHER)
            }
        }
        contact.profileLinks.forEach { url ->
            row(Website.CONTENT_ITEM_TYPE) {
                withValue(Website.URL, url)
                withValue(Website.TYPE, Website.TYPE_PROFILE)
            }
        }
        contact.relations.forEach { relation ->
            row(Relation.CONTENT_ITEM_TYPE) {
                withValue(Relation.NAME, relation.name)
                withValue(Relation.TYPE, relation.type)
                if (relation.type == Relation.TYPE_CUSTOM) withValue(Relation.LABEL, relation.label)
            }
        }
        contact.birthday?.let { date ->
            row(Event.CONTENT_ITEM_TYPE) {
                withValue(Event.START_DATE, date)
                withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
            }
        }
        contact.organization?.let { organization ->
            row(Organization.CONTENT_ITEM_TYPE) {
                withValue(Organization.COMPANY, organization.company)
                withValue(Organization.TITLE, organization.title)
                withValue(Organization.TYPE, Organization.TYPE_WORK)
            }
        }
        contact.nickname?.let { nickname ->
            row(Nickname.CONTENT_ITEM_TYPE) {
                withValue(Nickname.NAME, nickname)
                withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
            }
        }
        contact.note?.let { note ->
            row(Note.CONTENT_ITEM_TYPE) { withValue(Note.NOTE, note) }
        }
        return ops
    }

    private companion object {
        val MANAGED_MIME_TYPES = arrayOf(
            StructuredName.CONTENT_ITEM_TYPE,
            GroupMembership.CONTENT_ITEM_TYPE,
            Phone.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE,
            StructuredPostal.CONTENT_ITEM_TYPE,
            Website.CONTENT_ITEM_TYPE,
            Relation.CONTENT_ITEM_TYPE,
            Organization.CONTENT_ITEM_TYPE,
            Nickname.CONTENT_ITEM_TYPE,
            Note.CONTENT_ITEM_TYPE,
            Photo.CONTENT_ITEM_TYPE,
        )

        /** Corvid's rows for one raw contact: the kinds it writes, and birthdays among the events. */
        val MANAGED_ROWS_SELECTION =
            "${Data.RAW_CONTACT_ID}=? AND (${Data.MIMETYPE} IN (${MANAGED_MIME_TYPES.joinToString(",") { "?" }}) " +
                "OR (${Data.MIMETYPE}=? AND ${Data.DATA2}=?))"
        val MANAGED_ROWS_ARGS =
            MANAGED_MIME_TYPES + Event.CONTENT_ITEM_TYPE + Event.TYPE_BIRTHDAY.toString()
    }
}
