// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Multi-select in the contact list: what is checked, and whether selection mode is on. */
class ContactSelectionManagerTest {

    private val manager = ContactSelectionManager()

    @Test
    fun `starts with nothing selected and selection mode off`() {
        assertTrue(manager.selectedContactIds.value.isEmpty())
        assertFalse(manager.isSelectionMode.value)
    }

    @Test
    fun `toggling checks a contact and turns selection mode on`() {
        manager.toggleSelection("a")

        assertEquals(setOf("a"), manager.selectedContactIds.value)
        assertTrue(manager.isSelectionMode.value)
    }

    @Test
    fun `toggling again unchecks it`() {
        manager.toggleSelection("a")
        manager.toggleSelection("b")
        manager.toggleSelection("a")

        assertEquals(setOf("b"), manager.selectedContactIds.value)
    }

    @Test
    fun `unchecking the last contact does not leave selection mode`() {
        manager.toggleSelection("a")
        manager.toggleSelection("a")

        assertTrue(manager.selectedContactIds.value.isEmpty())
        assertTrue(manager.isSelectionMode.value)
    }

    @Test
    fun `deselecting all keeps selection mode on`() {
        manager.selectAll(listOf("a", "b"))

        manager.deselectAll()

        assertTrue(manager.selectedContactIds.value.isEmpty())
        assertTrue(manager.isSelectionMode.value)
    }

    @Test
    fun `clearing the selection leaves selection mode`() {
        manager.selectAll(listOf("a", "b"))

        manager.clearSelection()

        assertTrue(manager.selectedContactIds.value.isEmpty())
        assertFalse(manager.isSelectionMode.value)
    }

    @Test
    fun `selecting all checks every given contact and turns selection mode on`() {
        manager.selectAll(listOf("a", "b", "b", "c"))

        assertEquals(setOf("a", "b", "c"), manager.selectedContactIds.value)
        assertTrue(manager.isSelectionMode.value)
    }

    @Test
    fun `selecting all replaces an earlier selection`() {
        manager.toggleSelection("old")

        manager.selectAll(listOf("a"))

        assertEquals(setOf("a"), manager.selectedContactIds.value)
    }
}
