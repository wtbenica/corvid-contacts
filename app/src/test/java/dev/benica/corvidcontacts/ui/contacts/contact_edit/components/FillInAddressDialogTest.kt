// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.ui.test.performClick
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.repository.AddressSuggestion
import dev.benica.corvidcontacts.data.repository.SuggestionSource
import dev.benica.corvidcontacts.ui.ComposeTestBase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillInAddressDialogTest : ComposeTestBase() {

    private val queries = mutableListOf<String>()
    private val picked = mutableListOf<AddressSuggestion>()
    private var dismissed = 0

    private val oak = AddressSuggestion("1", "12 Oak St", "Springfield, IL", SuggestionSource.PHOTON)
    private val elm = AddressSuggestion("2", "12 Elm St", "Portland, OR", SuggestionSource.PHOTON)

    private val lookup = CompletableDeferred<List<AddressSuggestion>>()

    private fun showDialog(query: String = "12 Main St") = show {
        FillInAddressDialog(
            query = query,
            findMatches = { queries += it; lookup.await() },
            onPick = { picked += it },
            onDismiss = { dismissed++ },
        )
    }

    @Test
    fun `it says what is being looked up and that it is searching`() {
        showDialog("12 Main St")

        assertTrue(isShown(R.string.fill_in_address_message, "12 Main St"))
        assertTrue(isShown(R.string.fill_in_address_searching))
        assertEquals(listOf("12 Main St"), queries)
    }

    @Test
    fun `the matches are listed once found, and the searching message goes`() {
        showDialog()
        lookup.complete(listOf(oak, elm))
        compose.waitForIdle()

        assertFalse(isShown(R.string.fill_in_address_searching))
        assertTrue(isShown("12 Oak St"))
        assertTrue(isShown("Portland, OR"))
    }

    @Test
    fun `choosing a match passes it on and nothing else happens`() {
        showDialog()
        lookup.complete(listOf(oak, elm))
        compose.waitForIdle()

        node("12 Elm St").performClick()

        assertEquals(listOf(elm), picked)
        assertEquals(0, dismissed)
    }

    @Test
    fun `no matches says the address stays as it is`() {
        showDialog()
        lookup.complete(emptyList())
        compose.waitForIdle()

        assertTrue(isShown(R.string.fill_in_address_none))
        assertFalse(isShown(R.string.fill_in_address_searching))
    }

    @Test
    fun `cancel closes it without picking`() {
        showDialog()

        node(R.string.action_cancel).performClick()

        assertEquals(1, dismissed)
        assertTrue(picked.isEmpty())
    }

    @Test
    fun `the lookup runs once for a query, not again as the dialog redraws`() {
        showDialog()
        lookup.complete(listOf(oak))
        compose.waitForIdle()
        compose.waitForIdle()

        assertEquals(1, queries.size)
    }
}
