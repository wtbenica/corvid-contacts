// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
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

/** A request for [CONTACTS_PERMISSIONS], and whether Android will still show its prompt after a denial. */
@Stable
class ContactsPermissionRequest internal constructor(
    private val request: () -> Unit,
    canAskAgain: State<Boolean>,
) {
    /**
     * After a second denial, or "don't ask again", Android stops showing the prompt, and the only
     * way to allow it is the app's own settings.
     */
    val canAskAgain: Boolean by canAskAgain

    fun launch() = request()
}

/** Prepares a [ContactsPermissionRequest] that reports whether everything was granted to [onResult]. */
@Composable
fun rememberContactsPermissionRequest(onResult: (granted: Boolean) -> Unit): ContactsPermissionRequest {
    val context = LocalContext.current
    val canAskAgain = remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.grantedAllContacts()
        if (!granted) {
            canAskAgain.value = context.findActivity()?.let { activity ->
                CONTACTS_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            } ?: false
        }
        onResult(granted)
    }
    return remember(launcher) { ContactsPermissionRequest({ launcher.launch(CONTACTS_PERMISSIONS) }, canAskAgain) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
