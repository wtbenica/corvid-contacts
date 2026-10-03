// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.Context
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemContactEditRoutingTest {

    @Test
    fun `raw contact id is read from a raw contact uri`() {
        assertEquals(42L, SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/raw_contacts/42".toUri()))
    }

    @Test
    fun `other uris are not treated as raw contacts`() {
        assertNull(SystemContactEditRouting.rawContactIdFrom(null))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/contacts/42".toUri()))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/contacts/lookup/abc/42".toUri()))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://other.authority/raw_contacts/42".toUri()))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/raw_contacts/notanumber".toUri()))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/raw_contacts".toUri()))
        assertNull(SystemContactEditRouting.rawContactIdFrom("content://com.android.contacts/raw_contacts/0".toUri()))
    }

    @Test
    fun `edit link opens the contact in edit mode and round trips the id`() {
        val link = SystemContactEditRouting.editLink("a1b2-c3")
        assertEquals("cccontacts", link.scheme)
        assertEquals("contact", link.host)
        assertEquals("a1b2-c3", link.lastPathSegment)
        assertTrue(SystemContactEditRouting.wantsEdit(link))
    }

    @Test
    fun `plain contact deep links do not ask for the editor`() {
        assertFalse(SystemContactEditRouting.wantsEdit("cccontacts://contact/a1b2".toUri()))
        assertFalse(SystemContactEditRouting.wantsEdit(null))
    }

    @Test
    fun `mirror lookup maps a raw contact id back to its contact`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val dao = db.systemContactMirrorDao()
                dao.upsert(
                    listOf(
                        SystemContactMirrorEntity("contact-a", 10, "h1"),
                        SystemContactMirrorEntity("contact-b", 11, "h2"),
                    )
                )
                assertEquals("contact-b", dao.getContactIdForRawContact(11))
                assertNull(dao.getContactIdForRawContact(99))
            }
        } finally {
            db.close()
        }
    }
}
