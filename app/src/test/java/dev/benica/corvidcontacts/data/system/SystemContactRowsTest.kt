// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Checks that each kind of provider row is read from the columns the provider documents for it.
 * The columns are generic (DATA1 to DATA10), so a wrong number reads a different field without any
 * error. These rows are laid out as the provider holds them.
 */
class SystemContactRowsTest {

    /** A row of [mimeType] with [columns] as DATA1, DATA2, and so on; the rest are null. */
    private fun row(mimeType: String, vararg columns: String?) =
        DataRow(mimeType, List(10) { columns.getOrNull(it) })

    private fun read(vararg rows: DataRow, photoToken: String? = null) =
        rows.toList().toMirrorContact(photoToken)

    @Test
    fun `a name row gives the display name and each part`() {
        val contact = read(
            row(StructuredName.CONTENT_ITEM_TYPE, "Dr. Ada Augusta Lovelace Jr.", "Ada", "Lovelace", "Dr.", "Augusta", "Jr.")
        )

        assertEquals("Dr. Ada Augusta Lovelace Jr.", contact.displayName)
        assertEquals("Ada", contact.givenName)
        assertEquals("Lovelace", contact.familyName)
        assertEquals("Dr.", contact.prefix)
        assertEquals("Augusta", contact.middleName)
        assertEquals("Jr.", contact.suffix)
    }

    @Test
    fun `only the first name row is read`() {
        val contact = read(
            row(StructuredName.CONTENT_ITEM_TYPE, "First", "First"),
            row(StructuredName.CONTENT_ITEM_TYPE, "Second", "Second"),
        )

        assertEquals("First", contact.displayName)
    }

    @Test
    fun `phones and emails keep their number or address and type`() {
        val contact = read(
            row(Phone.CONTENT_ITEM_TYPE, "+1 555-123-4567", "2"),
            row(Phone.CONTENT_ITEM_TYPE, "+1 555-987-6543", "1"),
            row(Email.CONTENT_ITEM_TYPE, "ada@example.org", "2"),
        )

        assertEquals(listOf(MirrorPhone("+1 555-123-4567", 2), MirrorPhone("+1 555-987-6543", 1)), contact.phones)
        assertEquals(listOf(MirrorEmail("ada@example.org", 2)), contact.emails)
    }

    @Test
    fun `a phone with no number is skipped, and a missing or odd type reads as zero`() {
        val contact = read(
            row(Phone.CONTENT_ITEM_TYPE, "  ", "2"),
            row(Phone.CONTENT_ITEM_TYPE, "+1 555-123-4567", null),
            row(Phone.CONTENT_ITEM_TYPE, "+1 555-000-0000", "mobile"),
        )

        assertEquals(listOf(MirrorPhone("+1 555-123-4567", 0), MirrorPhone("+1 555-000-0000", 0)), contact.phones)
    }

    @Test
    fun `an address is read from its street, box, city, region, postcode and country columns`() {
        val contact = read(
            row(
                StructuredPostal.CONTENT_ITEM_TYPE,
                "1 Main St, Springfield, IL 62704, USA", // DATA1 formatted
                "1", // DATA2 type
                null, // DATA3 label
                "1 Main St", // DATA4 street
                "PO 12", // DATA5 PO box
                "Downtown", // DATA6 neighborhood, not read
                "Springfield", // DATA7 city
                "IL", // DATA8 region
                "62704", // DATA9 postcode
                "USA", // DATA10 country
            )
        )

        assertEquals(
            MirrorAddress(
                formatted = "1 Main St, Springfield, IL 62704, USA",
                street = "1 Main St",
                poBox = "PO 12",
                city = "Springfield",
                region = "IL",
                postcode = "62704",
                country = "USA",
                type = 1,
            ),
            contact.addresses.single()
        )
    }

    @Test
    fun `profile links are kept apart from other websites`() {
        val contact = read(
            row(Website.CONTENT_ITEM_TYPE, "https://example.org", Website.TYPE_OTHER.toString()),
            row(Website.CONTENT_ITEM_TYPE, "https://mastodon.social/@ada", Website.TYPE_PROFILE.toString()),
            row(Website.CONTENT_ITEM_TYPE, "https://ada.example.net", Website.TYPE_HOMEPAGE.toString()),
        )

        assertEquals(listOf("https://example.org", "https://ada.example.net"), contact.websites)
        assertEquals(listOf("https://mastodon.social/@ada"), contact.profileLinks)
    }

    @Test
    fun `a relation keeps its name, type and custom label`() {
        val contact = read(
            row(Relation.CONTENT_ITEM_TYPE, "William King", Relation.TYPE_SPOUSE.toString()),
            row(Relation.CONTENT_ITEM_TYPE, "Charles", Relation.TYPE_CUSTOM.toString(), "Mentor"),
        )

        assertEquals(
            listOf(
                MirrorRelation("William King", Relation.TYPE_SPOUSE, null),
                MirrorRelation("Charles", Relation.TYPE_CUSTOM, "Mentor"),
            ),
            contact.relations
        )
    }

    @Test
    fun `only a birthday event is read, as the provider holds it, and the first one wins`() {
        val contact = read(
            row(Event.CONTENT_ITEM_TYPE, "2001-02-03", Event.TYPE_ANNIVERSARY.toString()),
            row(Event.CONTENT_ITEM_TYPE, "--01-15", Event.TYPE_BIRTHDAY.toString()),
            row(Event.CONTENT_ITEM_TYPE, "1990-01-15", Event.TYPE_BIRTHDAY.toString()),
        )

        assertEquals("--01-15", contact.birthday)
    }

    @Test
    fun `an organization reads its company and its title, which is DATA4`() {
        val contact = read(row(Organization.CONTENT_ITEM_TYPE, "Analytical Engines", "1", null, "Programmer"))

        assertEquals(MirrorOrganization("Analytical Engines", "Programmer"), contact.organization)
    }

    @Test
    fun `an empty organization row is no organization`() {
        assertNull(read(row(Organization.CONTENT_ITEM_TYPE, " ", "1", null, " ")).organization)
    }

    @Test
    fun `the first nickname and note are read, and unknown row kinds are ignored`() {
        val contact = read(
            row(Nickname.CONTENT_ITEM_TYPE, "Ada", "1"),
            row(Nickname.CONTENT_ITEM_TYPE, "Countess", "1"),
            row(Note.CONTENT_ITEM_TYPE, "Met at the lecture."),
            row(Note.CONTENT_ITEM_TYPE, "A second note."),
            row("vnd.android.cursor.item/im", "ada@chat.example", "1"),
            row("vnd.com.example.custom", "anything"),
        )

        assertEquals("Ada", contact.nickname)
        assertEquals("Met at the lecture.", contact.note)
    }

    @Test
    fun `no rows read as an empty contact, and the photo token is passed through`() {
        val contact = read(photoToken = "abc123")

        assertEquals("", contact.displayName)
        assertEquals(emptyList<MirrorPhone>(), contact.phones)
        assertNull(contact.birthday)
        assertEquals("abc123", contact.systemPhoto)
    }
}
