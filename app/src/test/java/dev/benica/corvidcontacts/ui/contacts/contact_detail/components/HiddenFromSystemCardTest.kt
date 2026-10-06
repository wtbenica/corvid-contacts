// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.ComposeTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenFromSystemCardTest : ComposeTestBase() {

    private val calls = mutableListOf<String>()

    private fun showCard() = show {
        HiddenFromSystemCard(
            onShowAgain = { calls += "show" },
            onDelete = { calls += "delete" },
            onDismiss = { calls += "dismiss" },
        )
    }

    @Test
    fun `the card says the contact is hidden and why`() {
        showCard()

        node(R.string.detail_hidden_from_system_title).assertIsDisplayed()
        node(R.string.detail_hidden_from_system_message).assertIsDisplayed()
    }

    @Test
    fun `each action calls only its own handler`() {
        showCard()

        node(R.string.detail_hidden_from_system_show).performClick()
        node(R.string.detail_hidden_from_system_delete).performClick()
        compose.onNodeWithContentDescription(text(R.string.detail_hidden_from_system_dismiss)).performClick()

        assertEquals(listOf("show", "delete", "dismiss"), calls)
    }
}
