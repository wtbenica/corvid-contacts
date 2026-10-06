// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.HiddenContact
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.ui.ComposeTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressBookSharingSectionTest : ComposeTestBase() {

    private val shareChanges = mutableListOf<Boolean>()
    private val levelChanges = mutableListOf<SystemContactsLevel>()
    private var hiddenClicks = 0

    private fun showSection(
        isShared: Boolean,
        level: SystemContactsLevel = SystemContactsLevel.CALLER_ID,
        hiddenCount: Int = 0,
    ) = show {
        Column {
            AddressBookSharingSection(
                isShared = isShared,
                level = level,
                hiddenCount = hiddenCount,
                onShareChanged = { shareChanges += it },
                onLevelChanged = { levelChanges += it },
                onHiddenClick = { hiddenClicks++ },
            )
        }
    }

    private val levelRows get() = compose.onAllNodes(isSelectable())

    @Test
    fun `the switch shows whether the book is shared, and reports a change`() {
        showSection(isShared = false)
        compose.onNode(isToggleable()).assertIsOff().performClick()

        assertEquals(listOf(true), shareChanges)
    }

    @Test
    fun `a shared book's switch is on, and turning it off is reported`() {
        showSection(isShared = true)
        compose.onNode(isToggleable()).assertIsOn().performClick()

        assertEquals(listOf(false), shareChanges)
    }

    @Test
    fun `while the book is not shared the levels are still listed but cannot be chosen`() {
        showSection(isShared = false, level = SystemContactsLevel.FULL)

        assertTrue(isShown(R.string.system_contacts_level_caller_id))
        assertTrue(isShown(R.string.system_contacts_level_full))
        assertTrue(isShown(R.string.system_contacts_level_everything))
        SystemContactsLevel.entries.indices.forEach { levelRows[it].assertIsNotEnabled() }

        node(R.string.system_contacts_level_everything).performClick()
        assertTrue(levelChanges.isEmpty())
    }

    @Test
    fun `while the book is shared the current level is selected and another can be chosen`() {
        showSection(isShared = true, level = SystemContactsLevel.FULL)

        SystemContactsLevel.entries.indices.forEach { levelRows[it].assertIsEnabled() }
        levelRows[SystemContactsLevel.entries.indexOf(SystemContactsLevel.FULL)].assertIsSelected()

        node(R.string.system_contacts_level_everything).performClick()
        assertEquals(listOf(SystemContactsLevel.EVERYTHING), levelChanges)
    }

    @Test
    fun `the hidden contacts row appears only when there are some, with their count`() {
        showSection(isShared = true, hiddenCount = 0)
        assertFalse(isShown(R.string.address_book_setting_hidden))
    }

    @Test
    fun `the hidden contacts row shows the count and opens the list`() {
        showSection(isShared = true, hiddenCount = 3)

        assertTrue(isShown("3"))
        node(R.string.address_book_setting_hidden).performClick()
        assertEquals(1, hiddenClicks)
    }
}

class HiddenContactsDialogTest : ComposeTestBase() {

    private val shown = mutableListOf<List<String>>()
    private var dismissed = 0

    private val hidden = listOf(
        HiddenContact("a", "Ada Lovelace", null, null, "/books/a/", noticeDismissed = false),
        HiddenContact("b", "", "Grace", "Hopper", "/books/a/", noticeDismissed = true),
    )

    private fun showDialog() = show {
        HiddenContactsDialog(
            hiddenContacts = hidden,
            onShow = { shown += it },
            onDismiss = { dismissed++ },
        )
    }

    @Test
    fun `each hidden contact is listed by name, using its name parts when it has no display name`() {
        showDialog()

        assertTrue(isShown("Ada Lovelace"))
        assertTrue(isShown("Grace Hopper"))
    }

    @Test
    fun `showing one contact again passes only that contact`() {
        showDialog()

        compose.onAllNodesWithText(text(R.string.detail_hidden_from_system_show))[1].performClick()

        assertEquals(listOf(listOf("b")), shown)
        assertEquals(0, dismissed)
    }

    @Test
    fun `show all passes every contact and closes the dialog`() {
        showDialog()

        compose.onNodeWithText(text(R.string.address_book_hidden_show_all)).performClick()

        assertEquals(listOf(listOf("a", "b")), shown)
        assertEquals(1, dismissed)
    }

    @Test
    fun `done closes the dialog and shows nothing again`() {
        showDialog()

        compose.onNodeWithText(text(R.string.common_done)).performClick()

        assertTrue(shown.isEmpty())
        assertEquals(1, dismissed)
    }
}
