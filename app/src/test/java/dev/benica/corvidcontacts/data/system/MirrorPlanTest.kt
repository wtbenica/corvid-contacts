// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorPlanTest {

    private fun source(
        id: String = "1",
        displayName: String = "Ada Lovelace",
        firstName: String? = "Ada",
        lastName: String? = "Lovelace",
        phones: List<Phone>? = listOf(Phone("+15551234567", "CELL")),
        hasPhoto: Boolean = false,
        addressBookHref: String = "/books/main/",
        emails: List<Email>? = listOf(Email("ada@example.org", "WORK")),
        addresses: List<StructuredAddress>? = listOf(
            StructuredAddress(type = "HOME", street = "1 Main St", city = "London")
        ),
        birthday: String? = "1815-12-10",
        notes: String? = "Door code 1234",
        categories: List<String>? = listOf("Family", "Archived"),
        websites: List<String> = listOf("https://example.org"),
        socialProfiles: List<SocialProfile> = listOf(SocialProfile("@ada", "MASTODON")),
    ) = MirrorSource(
        id = id,
        displayName = displayName,
        firstName = firstName,
        lastName = lastName,
        middleName = "Augusta",
        prefix = "Countess",
        suffix = null,
        phones = phones,
        emails = emails,
        structuredAddresses = addresses,
        websites = websites,
        socialProfiles = socialProfiles,
        relationships = listOf(
            Relationship("SPOUSE", "William King"),
            Relationship("FRIEND", "some-uid", isUid = true)
        ),
        birthday = birthday,
        company = "Analytical Engines",
        jobTitle = "Programmer",
        nickname = "Ada",
        notes = notes,
        categories = categories,
        hasPhoto = hasPhoto,
        addressBookHref = addressBookHref,
    )

    private fun mirrored(
        source: MirrorSource,
        photoStamp: String? = null,
        level: SystemContactsLevel = SystemContactsLevel.CALLER_ID,
    ) = MirrorPlan.toMirrorContact(source, photoStamp, level)!!

    @Test
    fun `contact with name and phone is mirrored with mapped phone type`() {
        val contact = mirrored(source(phones = listOf(Phone("555", "HOME,VOICE"), Phone("556", null))))
        assertEquals("Ada Lovelace", contact.displayName)
        assertEquals(
            listOf(
                MirrorPhone("555", MirrorPlan.TYPE_HOME),
                MirrorPhone("556", MirrorPlan.TYPE_OTHER)
            ),
            contact.phones
        )
    }

    @Test
    fun `name falls back to first and last name when display name is blank`() {
        assertEquals("Ada Lovelace", mirrored(source(displayName = "")).displayName)
    }

    @Test
    fun `contacts with no phone numbers or no name are not mirrored`() {
        assertNull(MirrorPlan.toMirrorContact(source(phones = emptyList()), null))
        assertNull(MirrorPlan.toMirrorContact(source(phones = listOf(Phone("  ", "CELL"))), null))
        assertNull(MirrorPlan.toMirrorContact(source(displayName = "", firstName = null, lastName = null), null))
    }

    @Test
    fun `photo stamp only counts when the contact has a photo`() {
        assertNull(MirrorPlan.toMirrorContact(source(hasPhoto = false), "stamp")!!.photoStamp)
        assertEquals("stamp", MirrorPlan.toMirrorContact(source(hasPhoto = true), "stamp")!!.photoStamp)
    }

    @Test
    fun `hash changes when any written field changes`() {
        val base = mirrored(source())
        assertEquals(base.hash, mirrored(source()).hash)
        assertNotEquals(base.hash, mirrored(source(displayName = "Ada King")).hash)
        assertNotEquals(base.hash, mirrored(source(phones = listOf(Phone("+1999", "CELL")))).hash)
        assertNotEquals(base.hash, mirrored(source(phones = listOf(Phone("+15551234567", "WORK")))).hash)
        assertNotEquals(base.hash, mirrored(source(hasPhoto = true), photoStamp = "a").hash)
    }

    @Test
    fun `hash changes when the contact moves book or its group changes`() {
        val base = mirrored(source())
        assertEquals("/books/main/", base.bookHref)
        assertNotEquals(base.hash, mirrored(source(addressBookHref = "/books/other/")).hash)
        assertNotEquals(base.hash, base.copy(groupIds = listOf(7)).hash)
        assertNotEquals(base.copy(groupIds = listOf(7)).hash, base.copy(groupIds = listOf(8)).hash)
        assertEquals(base.copy(groupIds = listOf(7)).hash, base.copy(groupIds = listOf(7)).hash)
    }

    @Test
    fun `diff updates contacts whose group was recreated`() {
        val before = mirrored(source()).copy(groupIds = listOf(7))
        val after = before.copy(groupIds = listOf(9))
        val plan = MirrorPlan.diff(
            listOf(after),
            listOf(SystemContactMirrorEntity("1", 5, before.hash))
        )
        assertEquals(1, plan.updates.size)
        assertTrue(plan.inserts.isEmpty() && plan.deletes.isEmpty())
    }

    @Test
    fun `diff inserts new, updates changed, deletes removed, leaves unchanged`() {
        val unchanged = mirrored(source(id = "same"))
        val changed = mirrored(source(id = "changed", displayName = "New Name"))
        val added = mirrored(source(id = "added"))
        val mapped = listOf(
            SystemContactMirrorEntity("same", 10, unchanged.hash),
            SystemContactMirrorEntity("changed", 11, "old-hash"),
            SystemContactMirrorEntity("gone", 12, "whatever"),
        )

        val plan = MirrorPlan.diff(listOf(unchanged, changed, added), mapped)

        assertEquals(listOf(added), plan.inserts)
        assertEquals(listOf(changed to mapped[1]), plan.updates)
        assertEquals(listOf(mapped[2]), plan.deletes)
    }

    @Test
    fun `diff of an in-sync mirror is empty`() {
        val contact = mirrored(source())
        val plan = MirrorPlan.diff(listOf(contact), listOf(SystemContactMirrorEntity("1", 5, contact.hash)))
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `contact that stops qualifying is deleted from the mirror`() {
        val mapped = listOf(SystemContactMirrorEntity("1", 5, "h"))
        val plan = MirrorPlan.diff(emptyList(), mapped)
        assertEquals(mapped, plan.deletes)
    }

    @Test
    fun `caller id level writes only name phones and photo`() {
        val contact = mirrored(source(hasPhoto = true), photoStamp = "p")
        assertTrue(contact.emails.isEmpty())
        assertTrue(contact.addresses.isEmpty())
        assertTrue(contact.websites.isEmpty() && contact.profileLinks.isEmpty() && contact.relations.isEmpty())
        assertNull(contact.birthday)
        assertNull(contact.organization)
        assertNull(contact.nickname)
        assertNull(contact.note)
        assertNull(contact.middleName)
        assertTrue(contact.categories.isEmpty())
        assertEquals("p", contact.photoStamp)
    }

    @Test
    fun `full contact level adds everything except notes`() {
        val contact = mirrored(source(), level = SystemContactsLevel.FULL)
        assertEquals(listOf(MirrorEmail("ada@example.org", MirrorPlan.EMAIL_TYPE_WORK)), contact.emails)
        assertEquals("1 Main St", contact.addresses.single().street)
        assertEquals(MirrorPlan.POSTAL_TYPE_HOME, contact.addresses.single().type)
        assertEquals(listOf("https://example.org"), contact.websites)
        assertEquals(listOf("https://mastodon.social/@ada"), contact.profileLinks)
        assertEquals("1815-12-10", contact.birthday)
        assertEquals(MirrorOrganization("Analytical Engines", "Programmer"), contact.organization)
        assertEquals("Ada", contact.nickname)
        assertEquals("Augusta", contact.middleName)
        assertEquals("Countess", contact.prefix)
        assertNull(contact.note)
    }

    @Test
    fun `email and address types use their own numbering`() {
        assertEquals(2, MirrorPlan.emailType("WORK"))
        assertEquals(3, MirrorPlan.emailType(null))
        assertEquals(4, MirrorPlan.emailType("MOBILE"))
        assertEquals(2, MirrorPlan.addressType("WORK"))
        assertEquals(3, MirrorPlan.addressType("SCHOOL"))
        assertEquals(3, MirrorPlan.phoneType("WORK"))
        assertEquals(7, MirrorPlan.phoneType("SCHOOL"))
    }

    @Test
    fun `everything level adds notes`() {
        assertEquals("Door code 1234", mirrored(source(), level = SystemContactsLevel.EVERYTHING).note)
    }

    @Test
    fun `relationships stored as contact ids are left out and known types are mapped`() {
        val relations = mirrored(source(), level = SystemContactsLevel.FULL).relations
        assertEquals(listOf(MirrorRelation("William King", 14, null)), relations)
        assertEquals(
            MirrorRelation("Grace", MirrorPlan.RELATION_TYPE_CUSTOM, "Teacher"),
            MirrorPlan.relation("TEACHER", "Grace")
        )
    }

    @Test
    fun `categories become groups except the archived bookkeeping one`() {
        assertEquals(listOf("Family"), mirrored(source(), level = SystemContactsLevel.FULL).categories)
    }

    @Test
    fun `favorites and archived are never mirrored as groups but other categories are`() {
        val contact = mirrored(
            source(categories = listOf("Family", "Favorites", "favorites", "ARCHIVED", "Work")),
            level = SystemContactsLevel.FULL
        )
        assertEquals(listOf("Family", "Work"), contact.categories)
        // The favorite is still carried, as the starred flag.
        assertTrue(contact.starred)
    }

    @Test
    fun `birthdays are kept only in provider-readable forms`() {
        assertEquals("--03-12", MirrorPlan.normalizeBirthday("--03-12"))
        assertEquals("--03-12", MirrorPlan.normalizeBirthday("--0312"))
        assertEquals("1815-12-10", MirrorPlan.normalizeBirthday("1815-12-10"))
        assertNull(MirrorPlan.normalizeBirthday("December tenth"))
        assertNull(MirrorPlan.normalizeBirthday(null))
    }

    @Test
    fun `changing the level changes the hash so contacts are rewritten`() {
        val caller = mirrored(source())
        val full = mirrored(source(), level = SystemContactsLevel.FULL)
        val everything = mirrored(source(), level = SystemContactsLevel.EVERYTHING)
        assertNotEquals(caller.hash, full.hash)
        assertNotEquals(full.hash, everything.hash)
    }

    @Test
    fun `favorites are starred at every level regardless of case`() {
        val favorite = source(categories = listOf("favorites"))
        SystemContactsLevel.entries.forEach { level ->
            assertTrue(mirrored(favorite, level = level).starred)
            assertFalse(mirrored(source(), level = level).starred)
        }
    }

    @Test
    fun `toggling a favorite changes the hash so the mirror updates`() {
        val plain = mirrored(source())
        val starred = mirrored(source(categories = listOf("Favorites")))
        assertNotEquals(plain.hash, starred.hash)
    }

    @Test
    fun `group keys distinguish books from categories`() {
        assertNotEquals(MirrorPlan.bookGroupKey("Family"), MirrorPlan.categoryGroupKey("Family"))
    }

    @Test
    fun `each contact is mirrored at the level of its own address book`() {
        val work = source(addressBookHref = "/books/work/")
        val family = source(addressBookHref = "/books/family/").copy(id = "other")

        val contacts = MirrorPlan.toMirrorContacts(
            sources = listOf(work, family),
            levels = mapOf(
                "/books/work/" to SystemContactsLevel.CALLER_ID,
                "/books/family/" to SystemContactsLevel.EVERYTHING,
            ),
            photoStamp = { null },
        ).associateBy { it.id }

        val workContact = contacts.getValue(work.id)
        assertTrue(workContact.emails.isEmpty())
        assertNull(workContact.note)

        val familyContact = contacts.getValue("other")
        assertTrue(familyContact.emails.isNotEmpty())
        assertEquals("Door code 1234", familyContact.note)
    }

    @Test
    fun `a book with no recorded level is mirrored at the most private one`() {
        val contacts = MirrorPlan.toMirrorContacts(
            sources = listOf(source(addressBookHref = "/books/unknown/")),
            levels = emptyMap(),
            photoStamp = { null },
        )

        assertTrue(contacts.single().emails.isEmpty())
        assertNull(contacts.single().note)
    }

    @Test
    fun `hidden contacts are left out, and the rest are kept`() {
        val kept = source()
        val hidden = source().copy(id = "hidden")

        val contacts = MirrorPlan.toMirrorContacts(
            sources = listOf(kept, hidden),
            levels = emptyMap(),
            photoStamp = { null },
            hidden = setOf("hidden"),
        )

        assertEquals(listOf(kept.id), contacts.map { it.id })
    }

    @Test
    fun `hiding an already mirrored contact deletes it from the system contacts`() {
        val contact = mirrored(source())
        val mapped = listOf(SystemContactMirrorEntity(contact.id, 5, contact.hash))

        val plan = MirrorPlan.diff(emptyList(), mapped)

        assertEquals(mapped, plan.deletes)
    }

    @Test
    fun `a plan can leave out the contacts that are waiting to be read back`() {
        val one = mirrored(source(id = "one"))
        val two = mirrored(source(id = "two"))
        val three = mirrored(source(id = "three"))
        val mapped = listOf(
            SystemContactMirrorEntity("one", 1, "old"),
            SystemContactMirrorEntity("two", 2, "old"),
            SystemContactMirrorEntity("gone", 3, "old"),
        )

        val plan = MirrorPlan.diff(listOf(one, two, three), mapped).excluding(setOf("two", "gone"))

        assertEquals(listOf("three"), plan.inserts.map { it.id })
        assertEquals(listOf("one"), plan.updates.map { it.first.id })
        assertTrue(plan.deletes.isEmpty())
    }

    @Test
    fun `social profiles become profile links for known services and pass real urls through`() {
        val contact = mirrored(
            source(
                socialProfiles = listOf(
                    SocialProfile("ada", "TWITTER"),
                    SocialProfile("ada_l", "INSTAGRAM"),
                    SocialProfile("https://example.com/ada", "OTHER"),
                    SocialProfile("  ", "TWITTER"),
                )
            ),
            level = SystemContactsLevel.FULL
        )

        assertEquals(
            listOf(
                "https://twitter.com/ada",
                "https://instagram.com/ada_l",
                "https://example.com/ada",
            ),
            contact.profileLinks
        )
    }

    @Test
    fun `a profile link that is already one of the websites is not written twice`() {
        val contact = mirrored(
            source(
                websites = listOf("https://twitter.com/ada"),
                socialProfiles = listOf(SocialProfile("ada", "TWITTER"), SocialProfile("ada", "TWITTER"))
            ),
            level = SystemContactsLevel.FULL
        )

        assertEquals(listOf("https://twitter.com/ada"), contact.websites)
        assertTrue(contact.profileLinks.isEmpty())
    }
}

