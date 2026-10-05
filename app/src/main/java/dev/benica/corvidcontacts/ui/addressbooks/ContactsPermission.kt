// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * What the system contacts mirror needs: to write the mirror, and to read back edits and deletes
 * made to it in other apps. Android groups the two, so they are requested together.
 */
val CONTACTS_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS
)

/** Whether every one of [CONTACTS_PERMISSIONS] was granted in a permission result. */
fun Map<String, Boolean>.grantedAllContacts(): Boolean = CONTACTS_PERMISSIONS.all { this[it] == true }

/**
 * Whether the app holds the permissions the system contacts mirror needs. Sharing an address book
 * only takes effect while it does, so screens that show sharing state check it too: revoking a
 * permission in system settings then reads as "not shared" instead of lying.
 */
@Composable
fun rememberHasContactsPermission(): Boolean {
    val context = LocalContext.current
    return CONTACTS_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
