// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.content_providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import dev.benica.corvidcontacts.CorvidContactsApplication
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

class ContactFileProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String {
        return "text/vcard"
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val context = context ?: throw IllegalStateException("Context is null")
        val app = context.applicationContext as CorvidContactsApplication
        val repository = app.container.contactsRepository

        val contactId = uri.lastPathSegment?.substringBefore(".vcf") ?: throw FileNotFoundException(
            "No contact ID"
        )

        val contactWithBook = runBlocking {
            repository.getContactByIdSync(contactId)
        } ?: throw FileNotFoundException("Contact not found: $contactId")

        val contact = contactWithBook.contact
        val displayName = contact.getEffectiveDisplayName()

        val columnNames = projection ?: arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
            "data1", // Email / Phone
            "data2", // Type
            "data3", // Label
            "display_name",
            "mimetype"
        )

        val cursor = MatrixCursor(columnNames)
        val row = arrayOfNulls<Any>(columnNames.size)

        for (i in columnNames.indices) {
            val col = columnNames[i]
            when (col) {
                OpenableColumns.DISPLAY_NAME, "display_name", "display_name_primary" -> {
                    row[i] = displayName
                }

                OpenableColumns.SIZE -> {
                    val vcard = repository.getVCardString(contact)
                    row[i] = vcard.toByteArray().size.toLong()
                }

                "data1" -> {
                    // Return first email or first phone
                    val email = contact.emails?.firstOrNull()?.value
                    val phone = contact.phones?.firstOrNull()?.value
                    row[i] = email ?: phone
                }

                "data2" -> row[i] = 3 // OTHER / MOBILE
                "mimetype" -> {
                    val hasEmail = (contact.emails ?: emptyList()).isNotEmpty()
                    row[i] =
                        if (hasEmail) "vnd.android.cursor.item/email_v2" else "vnd.android.cursor.item/phone_v2"
                }

                "_id" -> {
                    row[i] = contact.id
                        .hashCode()
                        .toLong()
                }
            }
        }
        cursor.addRow(row)
        return cursor
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor? {
        val context = context ?: return null
        val app = context.applicationContext as CorvidContactsApplication
        val repository = app.container.contactsRepository

        val contactId = uri.lastPathSegment?.substringBefore(".vcf") ?: throw FileNotFoundException(
            "No contact ID"
        )

        val vcardString = runBlocking {
            val contactWithBook = repository.getContactByIdSync(contactId)
                ?: throw FileNotFoundException("Contact not found: $contactId")
            repository.getVCardString(contactWithBook.contact)
        }

        val cacheFile = File(
            context.cacheDir,
            "contact_$contactId.vcf"
        )
        cacheFile.writeText(vcardString)

        return ParcelFileDescriptor.open(
            cacheFile,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
    }

    // Stub out remaining CRUD methods
    override fun insert(
        u: Uri,
        v: ContentValues?,
    ): Uri? = null

    override fun update(
        u: Uri,
        v: ContentValues?,
        s: String?,
        sa: Array<out String>?,
    ): Int = 0

    override fun delete(
        u: Uri,
        s: String?,
        sa: Array<out String>?,
    ): Int = 0
}
