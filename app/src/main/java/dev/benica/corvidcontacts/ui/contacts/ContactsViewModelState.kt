// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.repository.SyncRepairStats

/** Snapshot of every filter dimension combined to (re)compute [ContactsUiState] whenever any of them changes. */
internal data class ContactFilter(
    val archived: Boolean,
    val query: String,
    val group: String?,
    val bookHrefs: Set<String>,
    val pickType: PickContent?,
    val excludedContactId: String? = null,
)

/** UI state for the contact list screen. */
sealed class ContactsUiState {
    /** Initial state before the first contact list emission has arrived. */
    object Loading : ContactsUiState()

    /** The current filtered/sorted list of contacts to display. */
    data class Success(
        val contacts: List<ContactWithAddressBook>,
    ) : ContactsUiState()

    /** A full-screen error occurred (e.g. sync failed with no cached contacts to fall back on). */
    data class Error(
        val resId: Int,
        val formatArgs: Any? = null,
    ) : ContactsUiState()
}

/** One-shot events emitted by [ContactsViewModel] for the list screen to display (snackbars, dialogs, etc.). */
sealed class ContactsEvent {
    /** A transient error occurred (shown as a snackbar rather than replacing the whole screen). */
    data class Error(
        val resId: Int,
        val formatArgs: Any? = null,
    ) : ContactsEvent()

    /** A sync self-healed one or more data issues; [stats] describes what was repaired. */
    data class RepairPerformed(val stats: SyncRepairStats) : ContactsEvent()

    /** A short confirmation [message] to show after a successful action. */
    data class Message(val message: ContactsMessage) : ContactsEvent()

    /** Specific confirmation messages for [ContactsEvent.Message]. */
    sealed class ContactsMessage {
        /** [count] contacts were archived. */
        data class ContactsArchived(val count: Int) : ContactsMessage()

        /** [count] contacts were unarchived. */
        data class ContactsUnarchived(val count: Int) : ContactsMessage()

        /** [count] contacts were deleted. */
        data class ContactsDeleted(val count: Int) : ContactsMessage()

        /** [count] contacts were moved to a different address book. */
        data class ContactsMoved(val count: Int) : ContactsMessage()

        /** [count] contacts were added to group [groupName]. */
        data class ContactsAddedToGroup(
            val count: Int,
            val groupName: String,
        ) : ContactsMessage()

        /** [count] contacts were removed from group [groupName]. */
        data class ContactsRemovedFromGroup(
            val count: Int,
            val groupName: String,
        ) :
            ContactsMessage()

        /** A single contact was saved (created or updated) successfully. */
        object ContactSaved : ContactsMessage()

        /** Two contacts were successfully merged into one. */
        object ContactsMerged : ContactsMessage()
    }
}
