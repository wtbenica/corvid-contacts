// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.model.Phone
import org.junit.Assert.assertEquals
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
    ) = MirrorSource(id, displayName, firstName, lastName, phones, hasPhoto)

    private fun mirrored(source: MirrorSource, photoStamp: String? = null) =
        MirrorPlan.toMirrorContact(source, photoStamp)!!

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
}
