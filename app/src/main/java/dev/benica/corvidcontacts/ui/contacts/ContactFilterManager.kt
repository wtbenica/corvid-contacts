// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the filtering state and logic for contacts.
 */
class ContactFilterManager {
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _selectedAddressBookHrefs = MutableStateFlow<Set<String>?>(null)
    val selectedAddressBookHrefsState: StateFlow<Set<String>?> =
        _selectedAddressBookHrefs.asStateFlow()

    private val _requiredPickType = MutableStateFlow<PickContent?>(null)
    val requiredPickType: StateFlow<PickContent?> = _requiredPickType.asStateFlow()

    private val _excludedContactId = MutableStateFlow<String?>(null)
    val excludedContactId: StateFlow<String?> = _excludedContactId.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun selectAddressBook(href: String?) {
        _selectedAddressBookHrefs.value = if (href == null) null else setOf(href)
    }

    fun setRequiredPickType(type: PickContent?) {
        _requiredPickType.value = type
    }

    /**
     * Sets [id] as excluded from the filtered list - used while picking a merge target, so the
     * contact being merged FROM can't be picked as its own merge target. Non-null also implies
     * "editable contacts only" (see [dev.benica.corvidcontacts.data.local.ContactEntity.isEditable]),
     * since the picked contact is the one that gets deleted once the merge completes; pass `null`
     * to clear both restrictions.
     */
    fun setExcludedContactId(id: String?) {
        _excludedContactId.value = id
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
        _selectedGroup.value = null // Clear group filter when switching views
    }

    /**
     * Filters the given contacts based on current search and filter settings.
     */
    fun filterContacts(
        contacts: List<ContactWithAddressBook>,
        query: String,
        group: String?,
        bookHrefs: Set<String>,
        pickType: PickContent?,
        excludedContactId: String? = null,
    ): List<ContactWithAddressBook> {
        return contacts.filter { contactWithBook ->
            val contact = contactWithBook.contact
            val matchesQuery = query.isBlank() ||
                    contact
                        .getEffectiveDisplayName()
                        .contains(
                            query,
                            ignoreCase = true
                        ) ||
                    contact.company?.contains(
                        query,
                        ignoreCase = true
                    ) == true ||
                    contact.jobTitle?.contains(
                        query,
                        ignoreCase = true
                    ) == true ||
                    contact.notes?.contains(
                        query,
                        ignoreCase = true
                    ) == true ||
                    (contact.categories ?: emptyList()).any {
                        it.contains(
                            query,
                            ignoreCase = true
                        )
                    }

            val matchesGroup = group == null ||
                    (contact.categories ?: emptyList()).any {
                        it.equals(
                            group,
                            ignoreCase = true
                        )
                    }

            val matchesAddressBook = bookHrefs.contains(contact.addressBookHref)

            val matchesPickType = when (pickType) {
                PickContent.EMAIL -> (contact.emails ?: emptyList()).isNotEmpty()
                PickContent.PHONE -> (contact.phones ?: emptyList()).isNotEmpty()
                PickContent.ADDRESS -> (contact.structuredAddresses
                    ?: emptyList()).any { !it.isBlank() }

                PickContent.ALL, null -> true
            }

            // While picking a merge target, the contact being merged FROM can't be picked as its
            // own target, and only editable contacts qualify (the picked contact gets deleted).
            val matchesExclusion = excludedContactId == null ||
                    (contact.id != excludedContactId && contact.isEditable())

            matchesQuery && matchesGroup && matchesAddressBook && matchesPickType && matchesExclusion
        }
    }
}
