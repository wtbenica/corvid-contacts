// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.local.HiddenContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemVisibilityTest {

    private fun contact(shared: Boolean) = ContactWithAddressBook(
        contact = ContactEntity(id = "c1", displayName = "Ada", addressBookHref = "/book/"),
        addressBook = AddressBookEntity("/book/", "Book", colorInt = 0, shareWithSystem = shared),
    )

    private fun hidden(dismissed: Boolean) =
        HiddenContact("c1", "Ada", null, null, "/book/", noticeDismissed = dismissed)

    @Test
    fun `a contact in a book that isn't shared has no system state`() {
        assertNull(SystemVisibility.of(contact(shared = false), emptyList()))
        assertNull(SystemVisibility.of(null, emptyList()))
    }

    @Test
    fun `a shared contact that isn't hidden can be hidden, without a notice`() {
        val state = SystemVisibility.of(contact(shared = true), emptyList())

        assertEquals(SystemVisibility(isHidden = false, noticeDismissed = false), state)
        assertFalse(state!!.showsNotice)
    }

    @Test
    fun `a hidden contact shows its notice until it is dismissed`() {
        assertTrue(SystemVisibility.of(contact(shared = true), listOf(hidden(dismissed = false)))!!.showsNotice)
        assertFalse(SystemVisibility.of(contact(shared = true), listOf(hidden(dismissed = true)))!!.showsNotice)
    }

    @Test
    fun `a hidden contact can be shown again even when its book is no longer shared`() {
        val state = SystemVisibility.of(contact(shared = false), listOf(hidden(dismissed = true)))

        assertTrue(state!!.isHidden)
    }
}
