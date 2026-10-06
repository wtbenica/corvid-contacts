// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.StructuredAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which contacts the list shows for a search, group, address books, pick type and exclusion. */
class ContactFilterManagerTest {

    private val manager = ContactFilterManager()
    private val books = setOf("/a/", "/b/")

    private fun contact(
        id: String,
        name: String,
        book: String? = "/a/",
        company: String? = null,
        jobTitle: String? = null,
        notes: String? = null,
        categories: List<String>? = emptyList(),
        emails: List<Email>? = emptyList(),
        phones: List<Phone>? = emptyList(),
        addresses: List<StructuredAddress>? = emptyList(),
    ) = ContactWithAddressBook(
        ContactEntity(
            id = id,
            displayName = name,
            addressBookHref = book,
            company = company,
            jobTitle = jobTitle,
            notes = notes,
            categories = categories,
            emails = emails,
            phones = phones,
            structuredAddresses = addresses,
        ),
        null
    )

    private fun filter(
        contacts: List<ContactWithAddressBook>,
        query: String = "",
        group: String? = null,
        bookHrefs: Set<String> = books,
        pickType: PickContent? = null,
        excluded: String? = null,
    ) = manager.filterContacts(contacts, query, group, bookHrefs, pickType, excluded).map { it.contact.id }

    // --- Search -------------------------------------------------------------------------------

    @Test
    fun `a blank search shows everything in the chosen books`() {
        val all = listOf(contact("1", "Ada"), contact("2", "Charles"))

        assertEquals(listOf("1", "2"), filter(all, query = "  "))
    }

    @Test
    fun `search matches the name without regard to case`() {
        val all = listOf(contact("1", "Ada Lovelace"), contact("2", "Charles Babbage"))

        assertEquals(listOf("1"), filter(all, query = "LOVE"))
    }

    @Test
    fun `search also matches company, job title, notes and group`() {
        val all = listOf(
            contact("company", "A", company = "Analytical Engines"),
            contact("title", "B", jobTitle = "Programmer"),
            contact("notes", "C", notes = "met at the exhibition"),
            contact("group", "D", categories = listOf("Mathematicians")),
            contact("none", "E"),
        )

        assertEquals(listOf("company"), filter(all, query = "engines"))
        assertEquals(listOf("title"), filter(all, query = "programmer"))
        assertEquals(listOf("notes"), filter(all, query = "exhibition"))
        assertEquals(listOf("group"), filter(all, query = "mathemat"))
    }

    @Test
    fun `a contact with no name still matches on the name its parts make up`() {
        val nameless = ContactWithAddressBook(
            ContactEntity(id = "1", displayName = "", firstName = "Ada", lastName = "Lovelace", addressBookHref = "/a/"),
            null
        )

        assertEquals(listOf("1"), filter(listOf(nameless), query = "ada lovelace"))
    }

    // --- Group and books ----------------------------------------------------------------------

    @Test
    fun `the group filter keeps only members, ignoring case`() {
        val all = listOf(
            contact("1", "A", categories = listOf("Family")),
            contact("2", "B", categories = listOf("Work")),
            contact("3", "C", categories = null),
        )

        assertEquals(listOf("1"), filter(all, group = "family"))
    }

    @Test
    fun `only contacts in the chosen books are shown`() {
        val all = listOf(contact("1", "A", book = "/a/"), contact("2", "B", book = "/b/"), contact("3", "C", book = "/c/"))

        assertEquals(listOf("1"), filter(all, bookHrefs = setOf("/a/")))
        assertEquals(emptyList<String>(), filter(all, bookHrefs = emptySet()))
    }

    @Test
    fun `a contact with no book is never shown`() {
        assertEquals(emptyList<String>(), filter(listOf(contact("1", "A", book = null))))
    }

    // --- Picking ------------------------------------------------------------------------------

    @Test
    fun `picking an email shows only contacts that have one`() {
        val all = listOf(contact("1", "A", emails = listOf(Email("a@example.org", null))), contact("2", "B"))

        assertEquals(listOf("1"), filter(all, pickType = PickContent.EMAIL))
    }

    @Test
    fun `picking a phone number shows only contacts that have one`() {
        val all = listOf(contact("1", "A", phones = listOf(Phone("555", null))), contact("2", "B"))

        assertEquals(listOf("1"), filter(all, pickType = PickContent.PHONE))
    }

    @Test
    fun `picking an address ignores blank addresses`() {
        val all = listOf(
            contact("real", "A", addresses = listOf(StructuredAddress(street = "1 Main St"))),
            contact("blank", "B", addresses = listOf(StructuredAddress())),
            contact("none", "C"),
        )

        assertEquals(listOf("real"), filter(all, pickType = PickContent.ADDRESS))
    }

    @Test
    fun `picking any contact, or no type, shows everyone`() {
        val all = listOf(contact("1", "A"), contact("2", "B"))

        assertEquals(listOf("1", "2"), filter(all, pickType = PickContent.ALL))
        assertEquals(listOf("1", "2"), filter(all, pickType = null))
    }

    // --- Merge target exclusion ---------------------------------------------------------------

    @Test
    fun `while picking a merge target the source is left out`() {
        val all = listOf(contact("source", "A"), contact("other", "B"))

        assertEquals(listOf("other"), filter(all, excluded = "source"))
    }

    @Test
    fun `while picking a merge target contacts in server managed books are left out`() {
        val all = listOf(
            contact("normal", "A", book = "/a/"),
            contact("system", "B", book = "/system/"),
        )

        assertEquals(listOf("normal"), filter(all, bookHrefs = setOf("/a/", "/system/"), excluded = "someone else"))
    }

    @Test
    fun `the filters combine`() {
        val all = listOf(
            contact("match", "Ada", categories = listOf("Family"), phones = listOf(Phone("555", null))),
            contact("wrong group", "Ada", categories = listOf("Work"), phones = listOf(Phone("555", null))),
            contact("no phone", "Ada", categories = listOf("Family")),
            contact("wrong name", "Zed", categories = listOf("Family"), phones = listOf(Phone("555", null))),
        )

        assertEquals(listOf("match"), filter(all, query = "ada", group = "Family", pickType = PickContent.PHONE))
    }

    // --- State --------------------------------------------------------------------------------

    @Test
    fun `selecting one address book narrows to it and null returns to all`() {
        assertNull(manager.selectedAddressBookHrefsState.value)

        manager.selectAddressBook("/a/")
        assertEquals(setOf("/a/"), manager.selectedAddressBookHrefsState.value)

        manager.selectAddressBook(null)
        assertNull(manager.selectedAddressBookHrefsState.value)
    }

    @Test
    fun `switching between active and archived contacts clears the group filter`() {
        manager.updateSelectedGroup("Family")
        assertFalse(manager.showArchived.value)

        manager.toggleShowArchived()

        assertTrue(manager.showArchived.value)
        assertNull(manager.selectedGroup.value)
    }

    @Test
    fun `search, pick type and exclusion are remembered until cleared`() {
        manager.updateSearchQuery("ada")
        manager.setRequiredPickType(PickContent.EMAIL)
        manager.setExcludedContactId("source")

        assertEquals("ada", manager.searchQuery.value)
        assertEquals(PickContent.EMAIL, manager.requiredPickType.value)
        assertEquals("source", manager.excludedContactId.value)

        manager.setRequiredPickType(null)
        manager.setExcludedContactId(null)

        assertNull(manager.requiredPickType.value)
        assertNull(manager.excludedContactId.value)
    }
}
