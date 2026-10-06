// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs under Robolectric because libphonenumber-android reads its metadata from the app's assets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PhoneFormatterTest {

    @Test
    fun `a raw number is reformatted, not passed through`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val formatted = PhoneFormatter.format(
            "5551234567",
            includeCountryCode = true,
            context = context,
            region = "US"
        )

        assertNotEquals("5551234567", formatted)
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `with the country code on, a number without one gets its own country's`() {
        assertEquals("+1 202-555-0143", PhoneFormatter.format("2025550143", true, context, "US"))
        assertEquals("+44 20 7946 0958", PhoneFormatter.format("020 7946 0958", true, context, "GB"))
    }

    @Test
    fun `with the country code on, a number that has one keeps it, whatever the user's region`() {
        assertEquals("+44 20 7946 0958", PhoneFormatter.format("+44 20 7946 0958", true, context, "US"))
        assertEquals("+33 1 23 45 67 89", PhoneFormatter.format("+33 1 23 45 67 89", true, context, "US"))
        assertEquals("+52 55 1234 5678", PhoneFormatter.format("+52 55 1234 5678", true, context, "US"))
    }

    @Test
    fun `formatting a number that is already formatted changes nothing`() {
        val once = PhoneFormatter.format("2025550143", true, context, "US")

        assertEquals(once, PhoneFormatter.format(once, true, context, "US"))
    }

    @Test
    fun `with the country code off, a number that has one keeps it, foreign or not`() {
        assertEquals("+44 20 7946 0958", PhoneFormatter.format("+44 20 7946 0958", false, context, "US"))
        assertEquals("+33 1 23 45 67 89", PhoneFormatter.format("+33 1 23 45 67 89", false, context, "US"))
        assertEquals("+1 202-555-0143", PhoneFormatter.format("+1 202 555 0143", false, context, "US"))
    }

    @Test
    fun `with the country code off, a number dialed with an international prefix keeps its country code`() {
        assertEquals("+33 1 23 45 67 89", PhoneFormatter.format("00 33 1 23 45 67 89", false, context, "GB"))
    }

    @Test
    fun `with the country code off, a number without one gets none, and keeps its trunk digit`() {
        assertEquals("202-555-0143", PhoneFormatter.format("2025550143", false, context, "US"))
        assertEquals("020 7946 0958", PhoneFormatter.format("020 7946 0958", false, context, "GB"))
    }

    @Test
    fun `with the country code off, formatting twice changes nothing`() {
        listOf("+44 20 7946 0958" to "US", "2025550143" to "US", "020 7946 0958" to "GB").forEach { (number, region) ->
            val once = PhoneFormatter.format(number, false, context, region)
            assertEquals(once, PhoneFormatter.format(once, false, context, region))
        }
    }

    @Test
    fun `turning the setting off never removes a code the setting on would have kept`() {
        listOf("+44 20 7946 0958", "+33 1 23 45 67 89", "+52 55 1234 5678", "+49 151 23456789").forEach { number ->
            assertEquals(
                PhoneFormatter.format(number, true, context, "US"),
                PhoneFormatter.format(number, false, context, "US")
            )
        }
    }
}
