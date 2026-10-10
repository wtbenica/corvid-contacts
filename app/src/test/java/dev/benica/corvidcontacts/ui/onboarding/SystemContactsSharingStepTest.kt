// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.onboarding

import android.Manifest
import android.app.Application
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.ComposeTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows.shadowOf

/** A permission prompt that answers at once, and remembers that it was asked. */
private class FakePermissionPrompt(private val granted: Boolean) : ActivityResultRegistry() {
    var asked = 0

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        asked++
        @Suppress("UNCHECKED_CAST")
        val results = (input as Array<String>).associateWith { granted } as O
        dispatchResult(requestCode, results)
    }
}

class SystemContactsSharingStepTest : ComposeTestBase() {

    private val saved = mutableListOf<Set<String>>()

    private val family = AddressBookEntity(href = "/books/family/", displayName = "Family", colorInt = 0)
    private val work = AddressBookEntity(href = "/books/work/", displayName = "Work", colorInt = 0)
    private val local = AddressBookEntity(href = "local://contacts", displayName = "On this phone", colorInt = 0)

    private fun showStep(books: List<AddressBookEntity>, prompt: FakePermissionPrompt = FakePermissionPrompt(granted = true)) {
        show {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides object : ActivityResultRegistryOwner {
                    override val activityResultRegistry = prompt
                }
            ) {
                SystemContactsSharingStep(addressBooks = books, onSave = { saved += it })
            }
        }
    }

    private fun grantContactsPermission() {
        shadowOf(context as Application).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    }

    private fun switchOf(index: Int) = compose.onAllNodes(isToggleable())[index]

    /** Taps a switch the way a user does: scrolling to it first, since the page is taller than the screen. */
    private fun toggle(index: Int) = switchOf(index).performScrollTo().performClick()

    private fun switchStates(count: Int) = (0 until count).map {
        switchOf(it).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On
    }

    @Test
    fun `books start off, and continuing with none chosen saves nothing without asking for permission`() {
        val prompt = FakePermissionPrompt(granted = true)
        showStep(listOf(family, work), prompt)

        assertEquals(listOf(false, false), switchStates(2))
        node(R.string.onboarding_action_continue).performClick()

        assertEquals(listOf(emptySet<String>()), saved)
        assertEquals(0, prompt.asked)
    }

    @Test
    fun `choosing a book and continuing asks for permission, then saves the choice`() {
        val prompt = FakePermissionPrompt(granted = true)
        showStep(listOf(family, work), prompt)

        toggle(1)
        node(R.string.onboarding_action_continue).performClick()

        assertEquals(1, prompt.asked)
        assertEquals(listOf(setOf(work.href)), saved)
    }

    @Test
    fun `tapping a book's row chooses it too`() {
        showStep(listOf(family, work))

        node("Family").performClick()

        assertEquals(listOf(true, false), switchStates(2))
    }

    @Test
    fun `when permission is denied nothing is saved, the books go back off and the reason is shown`() {
        val prompt = FakePermissionPrompt(granted = false)
        showStep(listOf(family, work), prompt)

        toggle(0)
        toggle(1)
        node(R.string.onboarding_action_continue).performClick()

        assertEquals(1, prompt.asked)
        assertTrue("nothing was saved", saved.isEmpty())
        assertTrue(isShown(R.string.onboarding_sharing_permission_denied))
        assertEquals(listOf(false, false), switchStates(2))
    }

    @Test
    fun `after a denial that cannot be asked again the app's settings are offered`() {
        showStep(listOf(family), FakePermissionPrompt(granted = false))

        toggle(0)
        node(R.string.onboarding_action_continue).performClick()

        assertTrue(isShown(R.string.onboarding_sharing_open_settings))
    }

    @Test
    fun `after a denial, continuing with the books off saves nothing and moves on`() {
        val prompt = FakePermissionPrompt(granted = false)
        showStep(listOf(family), prompt)
        toggle(0)
        node(R.string.onboarding_action_continue).performClick()

        node(R.string.onboarding_action_continue).performClick()

        assertEquals(listOf(emptySet<String>()), saved)
        assertEquals("the second Continue does not ask again", 1, prompt.asked)
    }

    @Test
    fun `changing a switch after a denial clears the message`() {
        showStep(listOf(family), FakePermissionPrompt(granted = false))
        toggle(0)
        node(R.string.onboarding_action_continue).performClick()
        assertTrue(isShown(R.string.onboarding_sharing_permission_denied))

        toggle(0)

        assertFalse(isShown(R.string.onboarding_sharing_permission_denied))
    }

    @Test
    fun `a book already shared starts on while permission is held, and continuing does not ask again`() {
        grantContactsPermission()
        val prompt = FakePermissionPrompt(granted = true)
        showStep(listOf(family.copy(shareWithSystem = true), work), prompt)

        assertEquals(listOf(true, false), switchStates(2))
        node(R.string.onboarding_action_continue).performClick()

        assertEquals(listOf(setOf(family.href)), saved)
        assertEquals(0, prompt.asked)
    }

    @Test
    fun `a book marked shared starts off when the permission has been taken away`() {
        showStep(listOf(family.copy(shareWithSystem = true)))

        assertEquals(listOf(false), switchStates(1))
    }

    @Test
    fun `a book on this phone says so`() {
        showStep(listOf(local))

        assertTrue(isShown(R.string.settings_address_book_local_badge))
    }

    @Test
    fun `the setup page gives the same explanation as the book settings dialog, after its own lead line`() {
        showStep(listOf(family))

        assertTrue(isShown(R.string.onboarding_sharing_title))
        val explanation = text(R.string.system_contacts_description)
        assertTrue(compose.onAllNodes(hasText(explanation, substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText(text(R.string.onboarding_sharing_lead), substring = true)).fetchSemanticsNodes().isNotEmpty())
    }
}
