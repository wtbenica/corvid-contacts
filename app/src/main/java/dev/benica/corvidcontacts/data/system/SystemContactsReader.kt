// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
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
import android.provider.ContactsContract.DisplayPhoto
import android.provider.ContactsContract.RawContacts
import java.security.MessageDigest

/**
 * Reads back what other apps have done to the contacts [SystemContactsMirror] wrote, for the
 * Corvid account only. Needs `READ_CONTACTS`.
 *
 * The provider flags a raw contact dirty when a non-sync-adapter app edits it, and flags it deleted
 * (keeping the row, minus its data) when one deletes it. Those two flags are the whole signal:
 * nothing else in the system contacts is read.
 */
class SystemContactsReader(
    private val context: Context,
    private val account: Account,
) {
    sealed interface Change {
        val rawContactId: Long
    }

    /** The raw contact was deleted in another app. */
    data class Deleted(override val rawContactId: Long) : Change

    /**
     * The raw contact was edited in another app. [contact] holds what the provider has now, in the
     * same shape the mirror writes; it has no id, book or level of its own, and only the parts the
     * mirror writes are filled in.
     */
    data class Edited(override val rawContactId: Long, val contact: MirrorContact) : Change

    private val resolver get() = context.contentResolver

    /** Every raw contact in the account that is dirty or deleted. */
    fun readChanges(): List<Change> {
        val deleted = HashSet<Long>()
        val edited = ArrayList<Long>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID, RawContacts.DELETED),
            "${RawContacts.ACCOUNT_NAME}=? AND ${RawContacts.ACCOUNT_TYPE}=? AND " +
                "(${RawContacts.DIRTY}=1 OR ${RawContacts.DELETED}=1)",
            arrayOf(account.name, account.type),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                if (cursor.getInt(1) == 1) deleted += id else edited += id
            }
        }
        if (deleted.isEmpty() && edited.isEmpty()) return emptyList()

        val contacts = edited.chunked(QUERY_CHUNK).flatMap { readContacts(it).entries }
        return deleted.map { Deleted(it) } + contacts.map { Edited(it.key, it.value) }
    }

    /**
     * A stand-in for the photo as the provider holds it, which changes when the photo does. The
     * provider re-encodes what it is given, so the bytes written can't be compared with the bytes
     * read; this is only ever compared with an earlier read of the same thing.
     */
    fun photoToken(rawContactId: Long): String? = readPhotoRow(rawContactId)?.first

    /** The photo for [rawContactId], full size where the provider keeps one, or `null` if it has none. */
    fun photoBytes(rawContactId: Long): ByteArray? {
        val (_, row) = readPhotoRow(rawContactId) ?: return null
        val fileId = row.fileId
        if (fileId != null) {
            runCatching {
                resolver
                    .openAssetFileDescriptor(ContentUris.withAppendedId(DisplayPhoto.CONTENT_URI, fileId), "r")
                    ?.use { it.createInputStream().readBytes() }
            }.getOrNull()?.let { return it }
        }
        return row.thumbnail
    }

    private class PhotoRow(val fileId: Long?, val thumbnail: ByteArray?)

    private fun readPhotoRow(rawContactId: Long): Pair<String, PhotoRow>? {
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Photo.PHOTO_FILE_ID, Photo.PHOTO),
            "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
            arrayOf(rawContactId.toString(), Photo.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val thumbnail = cursor.getBlob(1)
            val fileId = if (cursor.isNull(0)) null else cursor.getLong(0)
            if (thumbnail == null && fileId == null) return null
            return tokenOf(thumbnail, fileId) to PhotoRow(fileId, thumbnail)
        }
        return null
    }

    private fun tokenOf(thumbnail: ByteArray?, fileId: Long?): String {
        // The thumbnail is regenerated from the same source the same way, so it is stable while the
        // photo is; the file id is not, because the provider may rewrite it without any real change.
        if (thumbnail == null) return "file:$fileId"
        return MessageDigest
            .getInstance("SHA-256")
            .digest(thumbnail)
            .joinToString("") { "%02x".format(it) }
            .take(24)
    }

    private fun readContacts(rawIds: List<Long>): Map<Long, MirrorContact> {
        val rows = LinkedHashMap<Long, Rows>()
        rawIds.forEach { rows[it] = Rows() }
        val placeholders = rawIds.joinToString(",") { "?" }
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(
                Data.RAW_CONTACT_ID,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
            ),
            "${Data.RAW_CONTACT_ID} IN ($placeholders)",
            rawIds.map { it.toString() }.toTypedArray(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                rows[cursor.getLong(0)]?.add(cursor)
            }
        }
        return rows.mapValues { (rawId, collected) -> collected.toContact(photoToken(rawId)) }
    }

    private class Rows {
        var name: StructuredNameRow? = null
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

        fun add(cursor: Cursor) {
            fun text(column: Int): String? = cursor.getString(column)?.trim()?.takeIf { it.isNotEmpty() }
            fun int(column: Int): Int = if (cursor.isNull(column)) 0 else cursor.getInt(column)

            // Columns, after the raw id and the mimetype: DATA1 is 2, DATA2 is 3, and so on.
            when (cursor.getString(1)) {
                StructuredName.CONTENT_ITEM_TYPE -> if (name == null) {
                    name = StructuredNameRow(
                        display = text(2),
                        given = text(3),
                        family = text(4),
                        prefix = text(5),
                        middle = text(6),
                        suffix = text(7)
                    )
                }

                Phone.CONTENT_ITEM_TYPE -> text(2)?.let { phones += MirrorPhone(it, int(3)) }
                Email.CONTENT_ITEM_TYPE -> text(2)?.let { emails += MirrorEmail(it, int(3)) }
                StructuredPostal.CONTENT_ITEM_TYPE -> addresses += MirrorAddress(
                    formatted = text(2).orEmpty(),
                    street = text(5),
                    poBox = text(6),
                    city = text(8),
                    region = text(9),
                    postcode = text(10),
                    country = text(11),
                    type = int(3)
                )

                Website.CONTENT_ITEM_TYPE -> text(2)?.let {
                    if (int(3) == Website.TYPE_PROFILE) profileLinks += it else websites += it
                }

                Relation.CONTENT_ITEM_TYPE -> text(2)?.let {
                    relations += MirrorRelation(it, int(3), text(4))
                }

                Event.CONTENT_ITEM_TYPE -> if (int(3) == Event.TYPE_BIRTHDAY && birthday == null) {
                    birthday = MirrorPlan.normalizeBirthday(text(2))
                }

                Organization.CONTENT_ITEM_TYPE -> if (organization == null) {
                    organization = MirrorOrganization(text(2), text(5))
                        .takeIf { it.company != null || it.title != null }
                }

                Nickname.CONTENT_ITEM_TYPE -> if (nickname == null) nickname = text(2)
                Note.CONTENT_ITEM_TYPE -> if (note == null) note = text(2)
            }
        }

        fun toContact(photoToken: String?): MirrorContact {
            val name = name
            return MirrorContact(
                id = "",
                displayName = name?.display.orEmpty(),
                givenName = name?.given,
                familyName = name?.family,
                middleName = name?.middle,
                prefix = name?.prefix,
                suffix = name?.suffix,
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
    }

    private class StructuredNameRow(
        val display: String?,
        val given: String?,
        val family: String?,
        val prefix: String?,
        val middle: String?,
        val suffix: String?,
    )

    private companion object {
        const val QUERY_CHUNK = 200
    }
}
