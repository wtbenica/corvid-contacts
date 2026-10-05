// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.HiddenContact
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import kotlinx.coroutines.flow.Flow

/**
 * The contacts kept out of the system contacts on this device because they were deleted there from
 * another app. Showing one again lets [SystemContactsMirror] write it back on its next reconcile.
 */
class SystemContactVisibility(private val dao: SystemContactMirrorDao) {
    val hidden: Flow<List<HiddenContact>> = dao.observeHiddenContacts()

    suspend fun showAgain(ids: List<ContactId>) = ids.forEach { dao.unhide(it) }
}
