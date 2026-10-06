// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Photo
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

    /** Whether any raw contact in the account is dirty or deleted. Cheap enough to run on every change. */
    fun hasChanges(): Boolean = queryChanged(arrayOf(RawContacts._ID)) { it.count > 0 } ?: false

    /**
     * The version of every raw contact in the account. A rewrite that names the version it read only
     * applies if nothing has changed the contact since.
     */
    fun versions(): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID, RawContacts.VERSION),
            "${RawContacts.ACCOUNT_NAME}=? AND ${RawContacts.ACCOUNT_TYPE}=?",
            arrayOf(account.name, account.type),
            null
        )?.use { while (it.moveToNext()) result[it.getLong(0)] = it.getLong(1) }
        return result
    }

    /** The current version of [rawContactId], or `null` if it no longer exists. */
    fun version(rawContactId: Long): Long? = resolver.query(
        RawContacts.CONTENT_URI,
        arrayOf(RawContacts.VERSION),
        "${RawContacts._ID}=?",
        arrayOf(rawContactId.toString()),
        null
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun <R> queryChanged(projection: Array<String>, read: (Cursor) -> R): R? = resolver.query(
        RawContacts.CONTENT_URI,
        projection,
        "${RawContacts.ACCOUNT_NAME}=? AND ${RawContacts.ACCOUNT_TYPE}=? AND " +
            "(${RawContacts.DIRTY}=1 OR ${RawContacts.DELETED}=1)",
        arrayOf(account.name, account.type),
        null
    )?.use(read)

    /** Every raw contact in the account that is dirty or deleted. */
    fun readChanges(): List<Change> {
        val deleted = HashSet<Long>()
        val edited = ArrayList<Long>()
        queryChanged(arrayOf(RawContacts._ID, RawContacts.DELETED)) { cursor ->
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
        val rows = LinkedHashMap<Long, MutableList<DataRow>>()
        rawIds.forEach { rows[it] = ArrayList() }
        val placeholders = rawIds.joinToString(",") { "?" }
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.RAW_CONTACT_ID, Data.MIMETYPE) + DATA_COLUMNS,
            "${Data.RAW_CONTACT_ID} IN ($placeholders)",
            rawIds.map { it.toString() }.toTypedArray(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                // After the raw contact id and the mimetype come DATA1 to DATA10.
                rows[cursor.getLong(0)]?.add(
                    DataRow(cursor.getString(1), DATA_COLUMNS.indices.map { cursor.getString(it + 2) })
                )
            }
        }
        return rows.mapValues { (rawId, dataRows) -> dataRows.toMirrorContact(photoToken(rawId)) }
    }

    private companion object {
        const val QUERY_CHUNK = 200
        val DATA_COLUMNS = arrayOf(
            Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5,
            Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10,
        )
    }
}
