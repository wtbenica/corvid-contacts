// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

/**
 * How much of each shared contact is mirrored to the system contacts. Each level includes
 * everything in the one before it. [CALLER_ID] is the default and the most private.
 */
enum class SystemContactsLevel {
    /** Name, phone numbers and photo - enough for Messages and the dialer to show who it is. */
    CALLER_ID,

    /**
     * Also email addresses, postal addresses, social profiles, websites, birthday, groups,
     * company, job title, nickname and relationships.
     */
    FULL,

    /** Also notes, which are free text and the field most likely to hold sensitive details. */
    EVERYTHING;

    val includesFullContact: Boolean get() = this != CALLER_ID
    val includesNotes: Boolean get() = this == EVERYTHING
}
