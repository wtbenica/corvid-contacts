// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import dev.benica.corvidcontacts.data.local.ContactEntity

/**
 * UI model representing a relative of a contact.
 * Decouples the relationship display from whether the relative is a known contact in the app.
 */
sealed class Relative(override val type: String) : TypedValue()

class KnownRelative(
    type: String,
    val contact: ContactEntity,
) : Relative(type) {
    override fun itemDisplay(): String = contact.getEffectiveDisplayName()
}

class UnknownRelative(
    type: String,
    val displayName: String,
) : Relative(type) {
    override fun itemDisplay(): String = displayName
}
