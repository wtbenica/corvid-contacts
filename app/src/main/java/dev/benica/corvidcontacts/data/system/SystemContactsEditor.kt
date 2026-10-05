// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId

/**
 * How [SystemContactsMirror] applies an edit made in another app to the Corvid contact: the same
 * save the contact screen uses, so the change reaches the server too.
 */
interface SystemContactsEditor {
    suspend fun get(id: ContactId): ContactEntity?

    suspend fun save(contact: ContactEntity): Result<Unit>
}
