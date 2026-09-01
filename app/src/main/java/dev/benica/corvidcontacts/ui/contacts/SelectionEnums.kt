// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

/**
 * Defines the type of content being requested during a contact picking flow.
 */
enum class PickContent {
    /** Any contact can be picked. */
    ALL,

    /** Only contacts with at least one email address. */
    EMAIL,

    /** Only contacts with at least one phone number. */
    PHONE,

    /** Only contacts with at least one physical address. */
    ADDRESS
}
