// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactColorsTest {

    @Test
    fun `resolveColor returns contact override when present`() {
        val contactColor = Color(0xFFFF0000).toArgb() // Red
        val bookColor = Color(0xFF00FF00).toArgb() // Green

        val contact = ContactEntity(
            id = "1",
            displayName = "Test",
            firstName = "First",
            lastName = "Last",
            emails = emptyList(),
            phones = emptyList(),
            photoUrl = null,
            etag = null,
            addressBookHref = "/book/",
            colorInt = contactColor
        )
        val book = AddressBookEntity(
            href = "/book/",
            displayName = "Book",
            colorInt = bookColor
        )

        val resolved = ContactColors.resolveContactColor(
            ContactWithAddressBook(
                contact,
                book
            )
        )
        assertEquals(
            Color(contactColor),
            resolved
        )
    }

    @Test
    fun `resolveColor returns book override when contact override is missing`() {
        val bookColor = Color(0xFF00FF00).toArgb() // Green

        val contact = ContactEntity(
            id = "1",
            displayName = "Test",
            firstName = "First",
            lastName = "Last",
            emails = emptyList(),
            phones = emptyList(),
            photoUrl = null,
            etag = null,
            addressBookHref = "/book/",
            colorInt = null
        )
        val book = AddressBookEntity(
            href = "/book/",
            displayName = "Book",
            colorInt = bookColor
        )

        val resolved = ContactColors.resolveContactColor(
            ContactWithAddressBook(
                contact,
                book
            )
        )
        assertEquals(
            Color(bookColor),
            resolved
        )
    }

    @Test
    fun `resolveColor returns default book color when no book missing`() {
        val contact = ContactEntity(
            id = "1",
            displayName = "Test",
            firstName = "First",
            lastName = "Last",
            emails = emptyList(),
            phones = emptyList(),
            photoUrl = null,
            etag = null,
            addressBookHref = "/book/",
            colorInt = null
        )

        val expected = ContactColors.getAddressBookColorWithFallback(
            customColor = null,
            href = "/book/"
        )

        val resolved = ContactColors.resolveContactColor(
            ContactWithAddressBook(
                contact = contact,
                addressBook = null
            )
        )
        assertEquals(
            expected,
            resolved
        )
    }

    @Test
    fun `resolveColor returns first palette color when contactWithBook is null`() {
        val resolved = ContactColors.resolveContactColor(null)
        assertEquals(
            ContactColors.palette.first(),
            resolved
        )
    }
}
