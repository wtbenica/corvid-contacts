// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** The contacts permissions the mirror needs: to write the copy, and to read back edits made to it. */
internal class MirrorPermissions(private val context: Context) {

    fun canRead(): Boolean = held(Manifest.permission.READ_CONTACTS)

    /** Whether both are held. */
    fun hasAll(): Boolean = canRead() && held(Manifest.permission.WRITE_CONTACTS)

    private fun held(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
