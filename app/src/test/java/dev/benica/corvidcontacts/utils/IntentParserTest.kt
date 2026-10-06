// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.repository.PhotoManager
import dev.benica.corvidcontacts.data.repository.VCardMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/**
 * What the app makes of an intent another app sends it. Runs under Robolectric for the Intent,
 * the content resolver and the vCard mapper's use of Android.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntentParserTest {

    private lateinit var context: Context
    private lateinit var mapper: VCardMapper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mapper = VCardMapper(PhotoManager(context))
    }

    private fun parse(intent: Intent?) = IntentParser.parse(intent, context.contentResolver, mapper)

    private fun shareText(text: String?) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun serveVCard(uri: String, text: String): Uri =
        Uri.parse(uri).also { shadowOf(context.contentResolver).registerInputStream(it, ByteArrayInputStream(text.toByteArray())) }

    // --- Insert Contact -----------------------------------------------------------------------

    @Test
    fun `an insert intent fills in what it carries`() {
        val contact = parse(
            Intent(Intent.ACTION_INSERT).apply {
                putExtra(ContactsContract.Intents.Insert.NAME, "Ada Lovelace")
                putExtra(ContactsContract.Intents.Insert.PHONE, "+1 555-123-4567")
                putExtra(ContactsContract.Intents.Insert.EMAIL, "ada@example.org")
                putExtra(ContactsContract.Intents.Insert.COMPANY, "Analytical Engines")
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, "Programmer")
            }
        )!!

        assertEquals("Ada Lovelace", contact.displayName)
        assertEquals("Ada", contact.firstName)
        assertEquals("Lovelace", contact.lastName)
        assertEquals("+1 555-123-4567", contact.phones?.single()?.value)
        assertEquals("ada@example.org", contact.emails?.single()?.value)
        assertEquals("Analytical Engines", contact.company)
        assertEquals("Programmer", contact.jobTitle)
    }

    @Test
    fun `an insert intent takes the first and last word of a longer name, and a single word is a first name`() {
        val long = parse(Intent(Intent.ACTION_INSERT).apply { putExtra(ContactsContract.Intents.Insert.NAME, "Ada Augusta King") })!!
        val single = parse(Intent(Intent.ACTION_INSERT).apply { putExtra(ContactsContract.Intents.Insert.NAME, "Ada") })!!

        assertEquals("Ada", long.firstName)
        assertEquals("King", long.lastName)
        assertEquals("Ada", single.firstName)
        assertNull(single.lastName)
    }

    @Test
    fun `an insert intent with nothing in it gives an empty new contact, each with its own id`() {
        val first = parse(Intent(Intent.ACTION_INSERT))!!
        val second = parse(Intent(Intent.ACTION_INSERT))!!

        assertEquals("", first.displayName)
        assertEquals(emptyList<Any>(), first.phones)
        assertNotEquals(first.id, second.id)
    }

    // --- Shared text --------------------------------------------------------------------------

    @Test
    fun `a shared phone number becomes a phone`() {
        val contact = parse(shareText("(555) 123-4567"))!!

        assertEquals("(555) 123-4567", contact.phones?.single()?.value)
        assertEquals("", contact.displayName)
    }

    @Test
    fun `a shared email address becomes an email`() {
        val contact = parse(shareText("ada@example.org"))!!

        assertEquals("ada@example.org", contact.emails?.single()?.value)
        assertEquals("", contact.displayName)
    }

    @Test
    fun `a shared street address becomes an address, not an empty contact`() {
        val contact = parse(shareText("123 Main St, Springfield"))!!

        assertEquals("123 Main St, Springfield", contact.structuredAddresses?.single()?.street)
        assertEquals("", contact.displayName)
        assertNull(contact.notes)
    }

    @Test
    fun `short shared text becomes the name, and long text becomes a note`() {
        val name = parse(shareText("Ada Lovelace"))!!
        val long = "Met at the lecture on analytical engines and promised to send the notes."
        val note = parse(shareText(long))!!

        assertEquals("Ada Lovelace", name.displayName)
        assertNull(name.notes)
        assertEquals("", note.displayName)
        assertEquals(long, note.notes)
    }

    @Test
    fun `quotes around shared text are removed`() {
        assertEquals("Ada Lovelace", parse(shareText("\"Ada Lovelace\""))!!.displayName)
    }

    @Test
    fun `a link on its own becomes a website, and a link after other text is dropped`() {
        val alone = parse(shareText("https://example.org/ada"))!!
        val after = parse(shareText("Joe's Pizza https://maps.example/abc"))!!

        assertEquals(listOf("https://example.org/ada"), alone.websites)
        assertEquals("Joe's Pizza", after.displayName)
        assertEquals(emptyList<String>(), after.websites)
    }

    @Test
    fun `shared text that is blank or missing makes no contact`() {
        assertNull(parse(shareText("   ")))
        assertNull(parse(shareText(null)))
    }

    // --- vCards -------------------------------------------------------------------------------

    private val vcard = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada Lovelace\nN:Lovelace;Ada;;;\nTEL;TYPE=cell:+15551234567\nEND:VCARD\n"

    @Test
    fun `a shared vCard is read from its link`() {
        val uri = serveVCard("content://example/ada.vcf", vcard)

        val contact = parse(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uri) })!!

        assertEquals("Ada", contact.firstName)
        assertEquals("+15551234567", contact.phones?.single()?.value)
    }

    @Test
    fun `a vCard opened with view or edit is read from the intent's data`() {
        // Each link is served once, so view and edit get their own.
        assertNotNull(parse(Intent(Intent.ACTION_VIEW, serveVCard("content://example/view.vcf", vcard))))
        assertNotNull(parse(Intent(Intent.ACTION_EDIT, serveVCard("content://example/edit.vcf", vcard))))
    }

    @Test
    fun `only the first contact in a vCard file is used`() {
        val uri = serveVCard("content://example/two.vcf", vcard + "BEGIN:VCARD\nVERSION:4.0\nFN:Charles Babbage\nEND:VCARD\n")

        assertEquals("Ada", parse(Intent(Intent.ACTION_VIEW, uri))!!.firstName)
    }

    @Test
    fun `a file that is not a vCard, or can't be opened, makes no contact`() {
        val garbage = serveVCard("content://example/garbage.vcf", "this is not a vCard")
        val missing = Uri.parse("content://example/missing.vcf")

        assertNull(parse(Intent(Intent.ACTION_VIEW, garbage)))
        assertNull(parse(Intent(Intent.ACTION_VIEW, missing)))
    }

    // --- Everything else ----------------------------------------------------------------------

    @Test
    fun `no intent, or an action it doesn't handle, makes no contact`() {
        assertNull(parse(null))
        assertNull(parse(Intent(Intent.ACTION_MAIN)))
        assertNull(parse(Intent(Intent.ACTION_VIEW)))
    }
}
