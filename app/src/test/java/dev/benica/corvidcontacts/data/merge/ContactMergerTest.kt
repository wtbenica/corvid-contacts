// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.merge

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMergerTest {

    private fun contact(id: String, displayName: String = "", init: ContactEntity.() -> ContactEntity = { this }) =
        ContactEntity(id = id, displayName = displayName).init()

    private val survivor = contact("survivor", "Ada Lovelace")
    private val absorbed = contact("absorbed", "Ada L.")

    private fun merge(
        survivor: ContactEntity,
        absorbed: ContactEntity,
        resolutions: Map<ConflictField, ConflictResolution> = emptyMap(),
    ) = ContactMerger.buildMergedContact(survivor, absorbed, resolutions)

    // --- Finding conflicts --------------------------------------------------------------------

    @Test
    fun `a field only one side has is not a conflict`() {
        val conflicts = ContactMerger.detectConflicts(
            survivor.copy(company = "Analytical Engines"),
            absorbed.copy(jobTitle = "Programmer", displayName = "")
        )

        assertEquals(emptyList<ContactMergeConflict>(), conflicts)
    }

    @Test
    fun `equal values are not a conflict, even with different spacing`() {
        val conflicts = ContactMerger.detectConflicts(
            survivor.copy(company = "Analytical Engines "),
            absorbed.copy(company = " Analytical Engines", displayName = "Ada Lovelace")
        )

        assertEquals(emptyList<ContactMergeConflict>(), conflicts)
    }

    @Test
    fun `every single value field that differs on both sides is reported`() {
        val a = ContactEntity(
            id = "a", displayName = "A", firstName = "Ada", lastName = "Lovelace", middleName = "Augusta",
            prefix = "Dr.", suffix = "Jr.", company = "Engines", jobTitle = "Programmer",
            birthday = "1815-12-10", nickname = "Ada", notes = "one"
        )
        val b = ContactEntity(
            id = "b", displayName = "B", firstName = "Augusta", lastName = "King", middleName = "Ada",
            prefix = "Lady", suffix = "III", company = "Babbage & Co", jobTitle = "Analyst",
            birthday = "1815-12-11", nickname = "Countess", notes = "two"
        )

        val fields = ContactMerger.detectConflicts(a, b).map { it.field }

        assertEquals(
            listOf(
                ConflictField.DISPLAY_NAME, ConflictField.FIRST_NAME, ConflictField.LAST_NAME,
                ConflictField.MIDDLE_NAME, ConflictField.PREFIX, ConflictField.SUFFIX, ConflictField.COMPANY,
                ConflictField.JOB_TITLE, ConflictField.BIRTHDAY, ConflictField.NICKNAME, ConflictField.NOTES
            ),
            fields
        )
    }

    @Test
    fun `a conflict carries both values`() {
        val conflict = ContactMerger
            .detectConflicts(survivor.copy(company = "Engines"), absorbed.copy(company = "Babbage & Co"))
            .single { it.field == ConflictField.COMPANY }

        assertEquals("Engines", conflict.survivorValue)
        assertEquals("Babbage & Co", conflict.absorbedValue)
    }

    @Test
    fun `two photos are a conflict, and one is not`() {
        val both = ContactMerger.detectConflicts(
            survivor.copy(hasPhoto = true, photoUrl = "file://a.jpg"),
            absorbed.copy(displayName = survivor.displayName, hasPhoto = true, photoUrl = null)
        )
        val one = ContactMerger.detectConflicts(survivor.copy(hasPhoto = true), absorbed)

        assertEquals(listOf(ConflictField.PHOTO_URL), both.map { it.field })
        assertEquals("", both.single().absorbedValue)
        assertTrue(one.none { it.field == ConflictField.PHOTO_URL })
    }

    // --- Merging single value fields ----------------------------------------------------------

    @Test
    fun `the survivor keeps its identity`() {
        val merged = merge(
            survivor.copy(
                addressBookHref = "/books/a/", contactHref = "/books/a/s.vcf", etag = "etag-s",
                colorInt = 123, isArchived = true
            ),
            absorbed.copy(addressBookHref = "/books/b/", contactHref = "/books/b/a.vcf", etag = "etag-a", colorInt = 456)
        )

        assertEquals("survivor", merged.id)
        assertEquals("/books/a/", merged.addressBookHref)
        assertEquals("/books/a/s.vcf", merged.contactHref)
        assertEquals("etag-s", merged.etag)
        assertEquals(123, merged.colorInt)
        assertTrue(merged.isArchived)
    }

    @Test
    fun `a field only the absorbed contact has is filled in, and one only the survivor has is kept`() {
        val merged = merge(
            survivor.copy(company = "Engines", firstName = null),
            absorbed.copy(jobTitle = "Programmer", firstName = "Ada")
        )

        assertEquals("Engines", merged.company)
        assertEquals("Programmer", merged.jobTitle)
        assertEquals("Ada", merged.firstName)
    }

    @Test
    fun `an unresolved conflict keeps the survivor's value`() {
        val merged = merge(survivor.copy(company = "Engines"), absorbed.copy(company = "Babbage & Co"))

        assertEquals("Engines", merged.company)
    }

    @Test
    fun `each conflict follows its own resolution`() {
        val merged = merge(
            survivor.copy(company = "Engines", jobTitle = "Programmer"),
            absorbed.copy(company = "Babbage & Co", jobTitle = "Analyst"),
            mapOf(
                ConflictField.COMPANY to ConflictResolution.USE_ABSORBED,
                ConflictField.JOB_TITLE to ConflictResolution.USE_SURVIVOR,
            )
        )

        assertEquals("Babbage & Co", merged.company)
        assertEquals("Programmer", merged.jobTitle)
    }

    @Test
    fun `keeping both notes joins them with a blank line, and keeping both of anything else keeps the survivor`() {
        val merged = merge(
            survivor.copy(notes = " First note ", nickname = "Ada"),
            absorbed.copy(notes = "Second note", nickname = "Countess"),
            mapOf(
                ConflictField.NOTES to ConflictResolution.KEEP_BOTH,
                ConflictField.NICKNAME to ConflictResolution.KEEP_BOTH,
            )
        )

        assertEquals("First note\n\nSecond note", merged.notes)
        assertEquals("Ada", merged.nickname)
    }

    @Test
    fun `identical notes are not repeated`() {
        val merged = merge(survivor.copy(notes = "Same"), absorbed.copy(notes = "Same "))

        assertEquals("Same", merged.notes?.trim())
    }

    @Test
    fun `merging two blank fields leaves null, and a blank display name stays blank`() {
        val merged = merge(contact("s"), contact("a"))

        assertNull(merged.company)
        assertEquals("", merged.displayName)
    }

    // --- Photos -------------------------------------------------------------------------------

    @Test
    fun `with two photos the survivor's wins unless the absorbed one is chosen`() {
        val s = survivor.copy(hasPhoto = true, photoUrl = "file://s.jpg")
        val a = absorbed.copy(hasPhoto = true, photoUrl = "file://a.jpg")

        assertEquals("file://s.jpg", merge(s, a).photoUrl)
        assertEquals(
            "file://a.jpg",
            merge(s, a, mapOf(ConflictField.PHOTO_URL to ConflictResolution.USE_ABSORBED)).photoUrl
        )
    }

    @Test
    fun `a photo on one side only is kept, and no photo stays no photo`() {
        assertEquals("file://a.jpg", merge(survivor, absorbed.copy(hasPhoto = true, photoUrl = "file://a.jpg")).photoUrl)
        assertTrue(merge(survivor, absorbed.copy(hasPhoto = true, photoUrl = "file://a.jpg")).hasPhoto)
        assertEquals("file://s.jpg", merge(survivor.copy(hasPhoto = true, photoUrl = "file://s.jpg"), absorbed).photoUrl)

        val none = merge(survivor, absorbed)
        assertNull(none.photoUrl)
        assertFalse(none.hasPhoto)
    }

    // --- Lists --------------------------------------------------------------------------------

    @Test
    fun `phones are joined, and the same number written two ways is kept once, survivor first`() {
        val merged = merge(
            survivor.copy(phones = listOf(Phone("+1 555-123-4567", "CELL"))),
            absorbed.copy(phones = listOf(Phone("(+1) 5551234567", "HOME"), Phone("+1 555-987-6543", "WORK")))
        )

        assertEquals(
            listOf(Phone("+1 555-123-4567", "CELL"), Phone("+1 555-987-6543", "WORK")),
            merged.phones
        )
    }

    @Test
    fun `emails are joined ignoring case and spacing`() {
        val merged = merge(
            survivor.copy(emails = listOf(Email("Ada@Example.org", "HOME"))),
            absorbed.copy(emails = listOf(Email(" ada@example.org ", "WORK"), Email("ada@work.example", "WORK")))
        )

        assertEquals(listOf("Ada@Example.org", "ada@work.example"), merged.emails?.map { it.value })
    }

    @Test
    fun `categories and websites are joined ignoring case`() {
        val merged = merge(
            survivor.copy(categories = listOf("Family"), websites = listOf("https://Example.org")),
            absorbed.copy(categories = listOf("family", "Work"), websites = listOf("https://example.org ", "https://b.example"))
        )

        assertEquals(listOf("Family", "Work"), merged.categories)
        assertEquals(listOf("https://Example.org", "https://b.example"), merged.websites)
    }

    @Test
    fun `social profiles match by service and name, ignoring a leading at sign and case`() {
        val merged = merge(
            survivor.copy(socialProfiles = listOf(SocialProfile("@Ada", "MASTODON"))),
            absorbed.copy(
                socialProfiles = listOf(
                    SocialProfile("ada", "mastodon"),
                    SocialProfile("ada", "TWITTER"),
                )
            )
        )

        assertEquals(listOf("MASTODON", "TWITTER"), merged.socialProfiles?.map { it.type })
    }

    @Test
    fun `addresses are joined, blank ones dropped, and the same address in different case is kept once`() {
        val merged = merge(
            survivor.copy(structuredAddresses = listOf(StructuredAddress(street = "1 Main St", city = "Springfield"), StructuredAddress())),
            absorbed.copy(
                structuredAddresses = listOf(
                    StructuredAddress(street = "1 MAIN ST ", city = "springfield"),
                    StructuredAddress(street = "2 Side St"),
                )
            )
        )

        assertEquals(listOf("1 Main St", "2 Side St"), merged.structuredAddresses?.map { it.street })
    }

    @Test
    fun `merging contacts with no lists gives empty lists`() {
        val merged = merge(
            survivor.copy(phones = null, emails = null, categories = null, websites = null),
            absorbed.copy(phones = null, emails = null, categories = null, websites = null)
        )

        assertEquals(emptyList<Phone>(), merged.phones)
        assertEquals(emptyList<Email>(), merged.emails)
        assertEquals(emptyList<String>(), merged.categories)
    }

    // --- Relationships ------------------------------------------------------------------------

    @Test
    fun `relationships are joined, a name matches ignoring case, and a link matches by contact`() {
        val merged = merge(
            survivor.copy(relationships = listOf(Relationship("SPOUSE", "William King"), Relationship("FRIEND", "noah", isUid = true))),
            absorbed.copy(
                relationships = listOf(
                    Relationship("spouse", "william king"),
                    Relationship("FRIEND", "noah", isUid = true),
                    Relationship("FRIEND", "Noah", isUid = false),
                    Relationship("PARENT", "Byron"),
                )
            )
        )

        assertEquals(
            listOf(
                Relationship("SPOUSE", "William King"),
                Relationship("FRIEND", "noah", isUid = true),
                Relationship("FRIEND", "Noah", isUid = false),
                Relationship("PARENT", "Byron"),
            ),
            merged.relationships
        )
    }

    @Test
    fun `a link between the two contacts doesn't become a link to itself`() {
        val merged = merge(
            survivor.copy(relationships = listOf(Relationship("FRIEND", "absorbed", isUid = true))),
            absorbed.copy(relationships = listOf(Relationship("FRIEND", "survivor", isUid = true), Relationship("FRIEND", "other", isUid = true)))
        )

        assertEquals(listOf(Relationship("FRIEND", "other", isUid = true)), merged.relationships)
    }
}
