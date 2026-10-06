// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.ui.ComposeTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactDetailTopBarActionsTest : ComposeTestBase() {

    private val calls = mutableListOf<String>()

    private val editable = ContactEntity(id = "c1", displayName = "Ada", addressBookHref = "/books/a/")
    private val serverManaged = editable.copy(addressBookHref = "/books/server-generated/")

    private fun showActions(
        contact: ContactEntity = editable,
        isFavorite: Boolean = false,
        systemVisibility: SystemVisibility? = null,
    ) = show {
        Row {
            ContactDetailTopBarActions(
                contact = contact,
                isFavorite = isFavorite,
                onToggleFavorite = { calls += "favorite" },
                onEdit = { calls += "edit" },
                onArchive = { calls += "archive" },
                onMerge = { calls += "merge" },
                onDelete = { calls += "delete" },
                systemVisibility = systemVisibility,
                onToggleSystemVisibility = { calls += "toggle visibility" },
            )
        }
    }

    private fun openMenu() {
        compose.onNodeWithContentDescription(text(R.string.detail_action_more)).performClick()
    }

    @Test
    fun `favorite and edit are buttons in the bar`() {
        showActions()

        compose.onNodeWithContentDescription(text(R.string.detail_menu_favorite)).performClick()
        compose.onNodeWithContentDescription(text(R.string.detail_menu_edit)).performClick()

        assertEquals(listOf("favorite", "edit"), calls)
    }

    @Test
    fun `the menu offers archive, merge and delete for a contact that can be edited`() {
        showActions()
        openMenu()

        assertTrue(isShown(R.string.detail_menu_archive))
        assertTrue(isShown(R.string.detail_menu_merge))
        assertTrue(isShown(R.string.detail_menu_delete))
    }

    @Test
    fun `an archived contact is offered unarchive instead of archive`() {
        showActions(contact = editable.copy(isArchived = true))
        openMenu()

        assertTrue(isShown(R.string.detail_menu_unarchive))
        assertFalse(isShown(R.string.detail_menu_archive))
    }

    @Test
    fun `a contact the server manages cannot be edited or changed from the menu`() {
        showActions(contact = serverManaged, systemVisibility = SystemVisibility(isHidden = false, noticeDismissed = false))
        openMenu()

        assertEquals(0, compose.onAllNodesWithContentDescriptionCount(text(R.string.detail_menu_edit)))
        assertFalse(isShown(R.string.detail_menu_archive))
        assertFalse(isShown(R.string.detail_menu_delete))
        assertFalse(isShown(R.string.detail_menu_hide_from_system))
    }

    @Test
    fun `with nothing to do with the phone contacts the menu has no hide or show item`() {
        showActions(systemVisibility = null)
        openMenu()

        assertFalse(isShown(R.string.detail_menu_hide_from_system))
        assertFalse(isShown(R.string.detail_menu_show_in_system))
    }

    @Test
    fun `a contact in a shared book can be hidden from the menu`() {
        showActions(systemVisibility = SystemVisibility(isHidden = false, noticeDismissed = false))
        openMenu()

        assertFalse(isShown(R.string.detail_menu_show_in_system))
        node(R.string.detail_menu_hide_from_system).performClick()

        assertEquals(listOf("toggle visibility"), calls)
        assertFalse("the menu closes", isShown(R.string.detail_menu_hide_from_system))
    }

    @Test
    fun `a hidden contact can be shown again from the menu`() {
        showActions(systemVisibility = SystemVisibility(isHidden = true, noticeDismissed = true))
        openMenu()

        assertFalse(isShown(R.string.detail_menu_hide_from_system))
        node(R.string.detail_menu_show_in_system).performClick()

        assertEquals(listOf("toggle visibility"), calls)
    }

    @Test
    fun `each menu item calls only its own handler and closes the menu`() {
        showActions()

        openMenu(); node(R.string.detail_menu_archive).performClick()
        openMenu(); node(R.string.detail_menu_merge).performClick()
        openMenu(); node(R.string.detail_menu_delete).performClick()

        assertEquals(listOf("archive", "merge", "delete"), calls)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithContentDescriptionCount(description: String): Int =
        onAllNodes(androidx.compose.ui.test.hasContentDescription(description)).fetchSemanticsNodes().size
}
