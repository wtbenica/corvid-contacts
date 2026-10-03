// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Whether the app holds the permission the system contacts mirror needs to write. Sharing an
 * address book only takes effect while it does, so screens that show sharing state check it too:
 * revoking the permission in system settings then reads as "not shared" instead of lying.
 */
@Composable
fun rememberHasContactsWritePermission(): Boolean {
    val context = LocalContext.current
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}
