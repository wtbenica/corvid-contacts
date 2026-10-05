// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemEditMergeTest {

    private fun base(level: SystemContactsLevel = SystemContactsLevel.FULL) = MirrorContact(
        id = "1",
        displayName = "Ada Lovelace",
        givenName = "Ada",
        familyName = "Lovelace",
        phones = listOf(MirrorPhone("+1 555-123-4567", MirrorPlan.TYPE_MOBILE)),
        emails = listOf(MirrorEmail("ada@example.org", MirrorPlan.EMAIL_TYPE_WORK)),
        websites = listOf("https://example.org"),
        profileLinks = listOf("https://mastodon.social/@ada"),
        photoStamp = null,
        bookHref = "/books/main/",
        level = level,
    )

    private fun entity() = ContactEntity(
        id = "1",
        displayName = "",
        firstName = "Ada",
        lastName = "Lovelace",
        phones = listOf(Phone("+1 555-123-4567", "CELL", region = "US")),
        emails = listOf(Email("ada@example.org", "WORK")),
        websites = listOf("https://example.org"),
        socialProfiles = listOf(SocialProfile("@ada", "MASTODON")),
        etag = "etag",
    )

    @Test
    fun `an unchanged contact takes nothing`() {
        val merge = SystemEditMerge.merge(base(), base(), base())

        assertTrue(merge.isEmpty)
    }

    @Test
    fun `a part changed only in the system contacts is taken`() {
        val theirs = base().copy(phones = listOf(MirrorPhone("+1 555-999-0000", MirrorPlan.TYPE_MOBILE)))

        val merge = SystemEditMerge.merge(base(), theirs, base())

        assertEquals(setOf(MirrorField.PHONES), merge.taken)
    }

    @Test
    fun `reformatting a phone number is not an edit`() {
        val theirs = base().copy(phones = listOf(MirrorPhone("+1 (555) 123-4567", MirrorPlan.TYPE_MOBILE)))

        assertTrue(SystemEditMerge.merge(base(), theirs, base()).isEmpty)
    }

    @Test
    fun `changing a phone number's type is an edit`() {
        val theirs = base().copy(phones = listOf(MirrorPhone("+1 555-123-4567", MirrorPlan.TYPE_WORK)))

        assertEquals(setOf(MirrorField.PHONES), SystemEditMerge.merge(base(), theirs, base()).taken)
    }

    @Test
    fun `a part changed on both sides keeps Corvid's value`() {
        val theirs = base().copy(nickname = "Countess")
        val ours = base().copy(nickname = "Ada L.")

        assertTrue(SystemEditMerge.merge(base(), theirs, ours).isEmpty)
    }

    @Test
    fun `different parts changed on each side are both kept`() {
        val theirs = base().copy(nickname = "Countess")
        val ours = base().copy(phones = listOf(MirrorPhone("+1 555-000-1111", MirrorPlan.TYPE_MOBILE)))

        val merge = SystemEditMerge.merge(base(), theirs, ours)

        assertEquals(setOf(MirrorField.NICKNAME), merge.taken)
    }

    @Test
    fun `nothing is taken when Corvid no longer shares the contact`() {
        val theirs = base().copy(nickname = "Countess")

        assertTrue(SystemEditMerge.merge(base(), theirs, null).isEmpty)
    }

    @Test
    fun `parts the level doesn't write are not read back`() {
        val callerId = base(SystemContactsLevel.CALLER_ID)
        val theirs = callerId.copy(
            emails = listOf(MirrorEmail("new@example.org", MirrorPlan.EMAIL_TYPE_HOME)),
            note = "added elsewhere"
        )

        assertTrue(SystemEditMerge.merge(callerId, theirs, callerId).isEmpty)
    }

    @Test
    fun `notes are only read back at the Everything level`() {
        val full = base(SystemContactsLevel.FULL)
        val everything = base(SystemContactsLevel.EVERYTHING)

        assertTrue(SystemEditMerge.merge(full, full.copy(note = "n"), full).isEmpty)
        assertEquals(
            setOf(MirrorField.NOTE),
            SystemEditMerge.merge(everything, everything.copy(note = "n"), everything).taken
        )
    }

    @Test
    fun `an unchanged phone keeps its Corvid details when another is added`() {
        val theirs = base().copy(
            phones = base().phones + MirrorPhone("+1 555-777-8888", MirrorPlan.TYPE_HOME)
        )
        val merge = SystemEditMerge.merge(base(), theirs, base())

        val phones = SystemEditMerge.apply(entity(), merge).phones.orEmpty()

        assertEquals(
            listOf(Phone("+1 555-123-4567", "CELL", region = "US"), Phone("+1 555-777-8888", "HOME")),
            phones
        )
    }

    @Test
    fun `a removed email is removed from the contact`() {
        val theirs = base().copy(emails = emptyList())

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals(emptyList<Email>(), result.emails)
        assertEquals(entity().phones, result.phones)
    }

    @Test
    fun `changing the name parts follows an automatic display name`() {
        val theirs = base().copy(familyName = "King", displayName = "Ada Lovelace")

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals("King", result.lastName)
        assertEquals("", result.displayName)
    }

    @Test
    fun `an explicit display name is left alone when the other app did not change it`() {
        val named = entity().copy(displayName = "The Countess")
        val written = base().copy(displayName = "The Countess")
        val theirs = written.copy(givenName = "Augusta")

        val result = SystemEditMerge.apply(named, SystemEditMerge.merge(written, theirs, written))

        assertEquals("Augusta", result.firstName)
        assertEquals("The Countess", result.displayName)
    }

    @Test
    fun `a changed display name becomes automatic when it matches the parts`() {
        val theirs = base().copy(givenName = "Augusta", displayName = "Augusta Lovelace")

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals("", result.displayName)
        assertEquals("Augusta", result.firstName)
    }

    @Test
    fun `a display name that differs from the parts is kept as an override`() {
        val theirs = base().copy(displayName = "Lady Lovelace")

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals("Lady Lovelace", result.displayName)
    }

    @Test
    fun `links keep the contact's own websites and social profiles that are still there`() {
        val theirs = base().copy(
            websites = listOf("https://example.org", "https://ada.example.net"),
            profileLinks = listOf("https://mastodon.social/@ada")
        )

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals(listOf("https://example.org", "https://ada.example.net"), result.websites)
        assertEquals(listOf(SocialProfile("@ada", "MASTODON")), result.socialProfiles)
    }

    @Test
    fun `removing a profile link removes the social profile`() {
        val theirs = base().copy(profileLinks = emptyList())

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals(emptyList<SocialProfile>(), result.socialProfiles)
        assertEquals(listOf("https://example.org"), result.websites)
    }

    @Test
    fun `a relationship stored as a contact id survives a relationship edit`() {
        val source = entity().copy(
            relationships = listOf(
                Relationship("SPOUSE", "William King"),
                Relationship("FRIEND", "some-uid", isUid = true)
            )
        )
        val written = base().copy(relations = listOf(MirrorRelation("William King", 14, null)))
        val theirs = written.copy(relations = listOf(MirrorRelation("William King", 14, null), MirrorRelation("Byron", 5, null)))

        val result = SystemEditMerge.apply(source, SystemEditMerge.merge(written, theirs, written))

        assertEquals(
            listOf(
                Relationship("FRIEND", "some-uid", isUid = true),
                Relationship("SPOUSE", "William King"),
                Relationship("FATHER", "Byron")
            ),
            result.relationships
        )
    }

    @Test
    fun `an address typed as one line is kept whole as the street`() {
        val theirs = base().copy(
            addresses = listOf(MirrorAddress("1 Main St, London", null, null, null, null, null, null, MirrorPlan.POSTAL_TYPE_HOME))
        )

        val result = SystemEditMerge.apply(entity(), SystemEditMerge.merge(base(), theirs, base()))

        assertEquals("1 Main St, London", result.structuredAddresses.orEmpty().single().street)
        assertEquals("HOME", result.structuredAddresses.orEmpty().single().type)
    }

    @Test
    fun `a cleared birthday and a changed company are applied`() {
        val written = base().copy(
            birthday = "1815-12-10",
            organization = MirrorOrganization("Analytical Engines", "Programmer")
        )
        val theirs = written.copy(birthday = null, organization = MirrorOrganization("Babbage & Co", null))
        val source = entity().copy(birthday = "1815-12-10", company = "Analytical Engines", jobTitle = "Programmer")

        val result = SystemEditMerge.apply(source, SystemEditMerge.merge(written, theirs, written))

        assertNull(result.birthday)
        assertEquals("Babbage & Co", result.company)
        assertNull(result.jobTitle)
    }

    @Test
    fun `a photo changed only in the system contacts is taken, and removing it clears the contact's photo`() {
        val written = base().copy(photoStamp = "1-100", systemPhoto = "aaa")
        val replaced = written.copy(systemPhoto = "bbb")
        val removed = written.copy(systemPhoto = null)
        val withPhoto = entity().copy(photoUrl = "file:///old.jpg", hasPhoto = true)

        val replace = SystemEditMerge.merge(written, replaced, written)
        val remove = SystemEditMerge.merge(written, removed, written)

        assertEquals(setOf(MirrorField.PHOTO), replace.taken)
        assertEquals("file:///new.jpg", SystemEditMerge.apply(withPhoto, replace, "file:///new.jpg").photoUrl)
        assertEquals(setOf(MirrorField.PHOTO), remove.taken)
        val cleared = SystemEditMerge.apply(withPhoto, remove, null)
        assertNull(cleared.photoUrl)
        assertFalse(cleared.hasPhoto)
    }

    @Test
    fun `a photo is read back at every level, including Caller ID`() {
        SystemContactsLevel.entries.forEach { level ->
            val written = base(level)
            val theirs = written.copy(systemPhoto = "bbb")

            assertEquals(
                level.name,
                setOf(MirrorField.PHOTO),
                SystemEditMerge.merge(written, theirs, written).taken
            )
        }
    }

    @Test
    fun `a photo changed in Corvid since it was written is not replaced`() {
        val written = base().copy(photoStamp = "1-100", systemPhoto = "aaa")
        val theirs = written.copy(systemPhoto = "bbb")
        val ours = written.copy(photoStamp = "2-200")

        assertTrue(SystemEditMerge.merge(written, theirs, ours).isEmpty)
    }

    @Test
    fun `a photo that was written without a stand-in is left alone`() {
        val written = base().copy(photoStamp = "1-100", systemPhoto = null)
        val theirs = written.copy(systemPhoto = "bbb")

        assertTrue(SystemEditMerge.merge(written, theirs, written).isEmpty)
    }

    @Test
    fun `a photo added where there was none is taken`() {
        val written = base()
        val theirs = written.copy(systemPhoto = "bbb")

        assertEquals(setOf(MirrorField.PHOTO), SystemEditMerge.merge(written, theirs, written).taken)
    }

    @Test
    fun `a snapshot reads back as the contact that was written`() {
        val contact = base().copy(
            groupIds = listOf(3, 4),
            relations = listOf(MirrorRelation("William King", 14, null)),
            organization = MirrorOrganization("Analytical Engines", null),
        )

        assertEquals(contact, MirrorPlan.readSnapshot(MirrorPlan.snapshotOf(contact)))
    }

    @Test
    fun `an unreadable snapshot is treated as missing`() {
        assertNull(MirrorPlan.readSnapshot(null))
        assertNull(MirrorPlan.readSnapshot("not json"))
        assertNotNull(MirrorPlan.readSnapshot(MirrorPlan.snapshotOf(base())))
    }

    @Test
    fun `the level is part of the hash`() {
        assertTrue(base(SystemContactsLevel.CALLER_ID).hash != base(SystemContactsLevel.FULL).hash)
    }
}
