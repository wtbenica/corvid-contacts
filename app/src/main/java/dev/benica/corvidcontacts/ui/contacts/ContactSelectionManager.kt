// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the multi-selection state for contacts.
 */
class ContactSelectionManager {
    private val _selectedContactIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedContactIds: StateFlow<Set<String>> = _selectedContactIds.asStateFlow()

    // Independent of _selectedContactIds.isNotEmpty() - only clearSelection() exits selection mode.
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleSelection(contactId: String) {
        val current = _selectedContactIds.value
        _selectedContactIds.value = if (current.contains(contactId)) {
            current - contactId
        } else {
            current + contactId
        }
        _isSelectionMode.value = true
    }

    /** Empties the selection without leaving selection mode. */
    fun deselectAll() {
        _selectedContactIds.value = emptySet()
    }

    /** Empties the selection and exits selection mode entirely. */
    fun clearSelection() {
        _selectedContactIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun selectAll(allContactIds: List<String>) {
        _selectedContactIds.value = allContactIds.toSet()
        _isSelectionMode.value = true
    }
}
