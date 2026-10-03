// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.benica.corvidcontacts.data.system.SystemContactEditRouting
import kotlinx.coroutines.launch

/**
 * Receives the system Contacts app's edit and create actions for the mirrored account (declared as
 * `editContactActivity` and `createContactActivity` in `system_contacts_structure.xml`) and hands
 * them on to Corvid's own screens, since the mirror is read-only and Corvid is where contacts are
 * edited. Has no UI of its own: it forwards to [MainActivity] and finishes.
 */
class SystemContactEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            Intent.ACTION_EDIT -> routeEdit()
            Intent.ACTION_INSERT -> routeInsert()
            else -> finish()
        }
    }

    private fun routeEdit() {
        val rawContactId = SystemContactEditRouting.rawContactIdFrom(intent.data)
        val dao = (application as CorvidContactsApplication).container.systemContactMirrorDao

        lifecycleScope.launch {
            val contactId = rawContactId?.let { dao.getContactIdForRawContact(it) }
            if (contactId != null) {
                startActivity(
                    Intent(this@SystemContactEditActivity, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(SystemContactEditRouting.editLink(contactId))
                )
            } else {
                // The mapping has no such row (for example after the mirror was reset), so open
                // Corvid itself and say why the edit screen didn't appear.
                Toast
                    .makeText(
                        this@SystemContactEditActivity,
                        R.string.system_contact_edit_not_found,
                        Toast.LENGTH_LONG
                    )
                    .show()
                startActivity(Intent(this@SystemContactEditActivity, MainActivity::class.java))
            }
            finish()
        }
    }

    /** New contacts are created in Corvid, which already handles the standard insert intent. */
    private fun routeInsert() {
        val forward = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_INSERT)
            .setType(ContactsContract.Contacts.CONTENT_TYPE)
        intent.extras?.let { forward.putExtras(it) }
        startActivity(forward)
        finish()
    }
}
