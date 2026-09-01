// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit

import dev.benica.corvidcontacts.data.model.StructuredAddress
import java.util.UUID

/**
 * Identifies one row in a multi-value field list (phones, emails, addresses, etc.) - lets
 * [dev.benica.corvidcontacts.ui.contacts.contact_edit.components.MultiValueFieldList] render any
 * of them generically, and lets pending-focus tracking follow a specific row across reorders.
 */
interface HasId {
    val id: String
}

/**
 * Shared mutable row shape for phones, emails, websites, and social profiles while being edited -
 * the same four fields are reused across all of them even though not every field applies to
 * every kind: [region] only matters for phones, where [dev.benica.corvidcontacts.utils.PhoneFormatter]
 * needs it to parse/format the number; websites leave [type]/[region] unused.
 */
data class EditableTypedValue(
    override val id: String = UUID
        .randomUUID()
        .toString(),
    var type: String = "",
    var value: String = "",
    var region: String? = null,
) : HasId

/**
 * A relationship row being edited. [value] holds a free-typed name, unless [isUid] is true, in
 * which case it holds another contact's id instead - one field standing in for either an
 * unstructured name or a reference to a known in-app contact.
 */
data class EditableRelationship(
    override val id: String = UUID
        .randomUUID()
        .toString(),
    var type: String = "",
    var value: String = "",
    var isUid: Boolean = false,
) : HasId

/** Wraps a [StructuredAddress] with the stable [id] the other editable rows use for focus/reorder tracking. */
data class EditableAddress(
    override val id: String = UUID
        .randomUUID()
        .toString(),
    var value: StructuredAddress = StructuredAddress(),
) : HasId

/** Uppercases and trims a user-entered type string (e.g. "home ") so it matches vCard's constant type labels. */
fun normalizeType(type: String): String {
    return type
        .uppercase()
        .trim()
}
