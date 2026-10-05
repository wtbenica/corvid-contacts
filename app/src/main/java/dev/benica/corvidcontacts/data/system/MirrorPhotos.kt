// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.ContentProviderOperation
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.Data
import android.util.Log
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.repository.PhotoManager
import java.io.ByteArrayOutputStream

/** Moves photos between Corvid's files and the mirrored contacts, in both directions. */
internal class MirrorPhotos(
    private val photoManager: PhotoManager,
    private val provider: MirrorProvider,
    private val reader: SystemContactsReader,
    private val canRead: () -> Boolean,
) {
    /** Identifies the photo file's current contents, so a changed photo changes the contact's hash. */
    fun stamp(source: MirrorSource): String? {
        if (!source.hasPhoto) return null
        val file = photoManager.getPhotoFile(source.id)
        return if (file.exists()) "${file.lastModified()}-${file.length()}" else null
    }

    /**
     * Writes [contact]'s photo as its own small batch, since photo bytes can approach the binder
     * transaction limit. A failure here leaves the contact without a photo rather than failing the
     * whole reconcile. Returns how the provider now holds the photo (see
     * [SystemContactsReader.photoToken]), or `null` if none was written or it can't be read back.
     */
    fun write(contact: MirrorContact, rawId: Long): String? {
        if (contact.photoStamp == null) return null
        val bytes = bytes(contact.id) ?: return null
        return try {
            provider.apply(
                listOf(
                    ContentProviderOperation
                        .newInsert(provider.forAccount(Data.CONTENT_URI))
                        .withValue(Data.RAW_CONTACT_ID, rawId)
                        .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                        .withValue(Photo.PHOTO, bytes)
                        .build()
                )
            )
            if (canRead()) reader.photoToken(rawId) else null
        } catch (e: Exception) {
            Log.w(MIRROR_TAG, "Couldn't mirror photo for contact ${contact.id}", e)
            null
        }
    }

    /** Stores the photo that another app set on [rawId], or removes the contact's photo if there is none. */
    fun saveFromSystem(contactId: ContactId, rawId: Long): String? {
        val bytes = reader.photoBytes(rawId)
        if (bytes == null) {
            photoManager.getPhotoFile(contactId).delete()
            return null
        }
        return photoManager.savePhotoToFile(contactId, bytes)
    }

    /** The contact's photo, downscaled if large so it fits comfortably in one provider call. */
    private fun bytes(contactId: ContactId): ByteArray? {
        val file = photoManager.getPhotoFile(contactId)
        if (!file.exists()) return null
        val raw = file.readBytes()
        if (raw.size <= PASS_THROUGH_BYTES) return raw

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(
            raw,
            0,
            raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private companion object {
        const val PASS_THROUGH_BYTES = 256 * 1024
        const val MAX_DIMENSION = 720
    }
}
