// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Manages photo file operations and downloads for contacts.
 */
class PhotoManager(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val TAG = "PhotoManager"

    /** Deletes every locally cached contact photo. */
    fun deleteAllPhotos() {
        File(context.filesDir, "photos").deleteRecursively()
    }

    /** Returns the on-disk location for a contact's photo. */
    fun getPhotoFile(contactId: String): File {
        val dir = File(
            context.filesDir,
            "photos"
        )
        if (!dir.exists()) dir.mkdirs()
        return File(
            dir,
            "$contactId.jpg"
        )
    }

    /**
     * Copies a photo from one contact to another.
     * @return The file URI of the new photo, or null if source doesn't exist.
     */
    suspend fun copyContactPhoto(
        fromContactId: String,
        toContactId: String,
    ): String? =
        withContext(Dispatchers.IO) {
            val source = getPhotoFile(fromContactId)
            if (!source.exists()) return@withContext null
            savePhotoToFile(
                toContactId,
                source.readBytes()
            )
        }

    /**
     * Saves photo data to disk.
     * @return The file URI with a cache-busting timestamp, or null on failure.
     */
    fun savePhotoToFile(
        contactId: String,
        data: ByteArray,
    ): String? {
        return try {
            val file = getPhotoFile(contactId)
            file.writeBytes(data)
            "${Uri.fromFile(file)}?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to save photo for $contactId",
                e
            )
            null
        }
    }

    /**
     * Loads photo data from a file path or URI.
     */
    fun loadPhotoFromFile(path: String): ByteArray? {
        return try {
            val uri = path.toUri()
            val cleanPath = uri.path ?: path
                .substringBefore("?")
                .removePrefix("file://")
            val file = File(cleanPath)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to load photo from $path",
                e
            )
            null
        }
    }

    /**
     * Downloads a photo from a URL and saves it locally.
     */
    suspend fun fetchPhotoAndSave(
        contactId: String,
        url: String,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request
                    .Builder()
                    .url(url)
                    .build()
                httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body

                        if (body.contentLength() > 5 * 1024 * 1024) {
                            Log.w(
                                TAG,
                                "Photo at $url is too large (${body.contentLength()} bytes)"
                            )
                            return@withContext null
                        }

                        savePhotoToFile(
                            contactId,
                            body.bytes()
                        )
                    }
            } catch (_: Exception) {
                null
            }
        }
}
