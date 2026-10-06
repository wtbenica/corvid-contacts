// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.repository.RepositoryTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** A relationship that links to another contact is shared by that contact's name, but only from a shared book. */
class LinkedNamesTest : RepositoryTestBase() {

    private val shared = "/books/shared/"
    private val private = "/books/private/"

    private fun sourceLinkingTo(vararg ids: String) = MirrorSource(
        id = "me", displayName = "Me", firstName = null, lastName = null, middleName = null,
        prefix = null, suffix = null, phones = null, emails = null, structuredAddresses = null,
        websites = null, socialProfiles = null,
        relationships = ids.map { Relationship("FRIEND", it, isUid = true) } + Relationship("SPOUSE", "A Name"),
        birthday = null, company = null, jobTitle = null, nickname = null, notes = null,
        categories = null, hasPhoto = false, addressBookHref = shared,
    )

    private fun names(vararg ids: String) =
        runBlocking { database.systemContactMirrorDao().linkedNames(listOf(sourceLinkingTo(*ids))) }

    @Test
    fun `a contact in a shared book is named`() {
        seedBooks(book(shared).copy(shareWithSystem = true))
        seedContacts(contact("friend", shared, "Grace Hopper"))

        assertEquals(mapOf("friend" to "Grace Hopper"), names("friend"))
    }

    @Test
    fun `a contact in a book that is not shared is not named`() {
        seedBooks(book(shared).copy(shareWithSystem = true), book(private))
        seedContacts(contact("friend", private, "Grace Hopper"))

        assertEquals(emptyMap<String, String>(), names("friend"))
    }

    @Test
    fun `of two links only the one in a shared book is named, and a link to nobody is left out`() {
        seedBooks(book(shared).copy(shareWithSystem = true), book(private))
        seedContacts(contact("a", shared, "Ada"), contact("b", private, "Bob"))

        assertEquals(mapOf("a" to "Ada"), names("a", "b", "missing"))
    }

    @Test
    fun `a name typed in is not a link, so it needs no lookup`() {
        seedBooks(book(shared).copy(shareWithSystem = true))

        assertEquals(emptyMap<String, String>(), names())
    }
}
