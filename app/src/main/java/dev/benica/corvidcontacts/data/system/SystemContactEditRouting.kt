// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.ContentUris
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.net.toUri

/**
 * Helpers for sending the system Contacts app's edit action for a mirrored contact on to Corvid's
 * own edit screen. The Contacts app launches the activity named by `editContactActivity` in
 * `system_contacts_structure.xml` with a raw contact URI; [SystemContactEditActivity] reads the
 * raw contact id from it and looks up which Corvid contact it mirrors.
 */
object SystemContactEditRouting {
    private const val RAW_CONTACTS_SEGMENT = "raw_contacts"

    /** The raw contact id in a `content://com.android.contacts/raw_contacts/<id>` URI, else `null`. */
    fun rawContactIdFrom(uri: Uri?): Long? {
        if (uri == null || uri.authority != ContactsContract.AUTHORITY) return null
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != RAW_CONTACTS_SEGMENT) return null
        return runCatching { ContentUris.parseId(uri) }
            .getOrNull()
            ?.takeIf { it > 0 }
    }

    /** The deep link that opens [contactId] straight in Corvid's edit screen. */
    fun editLink(contactId: String): Uri =
        "cccontacts://contact/${Uri.encode(contactId)}?$EDIT_PARAM=true".toUri()

    /** Whether a `cccontacts://contact/...` deep link asks for the edit screen. */
    fun wantsEdit(link: Uri?): Boolean = link?.getQueryParameter(EDIT_PARAM) == "true"

    private const val EDIT_PARAM = "edit"
}
