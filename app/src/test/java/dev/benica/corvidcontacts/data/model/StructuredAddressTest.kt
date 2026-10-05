// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredAddressTest {

    @Test
    fun `an address with only a street is street only`() {
        assertTrue(StructuredAddress(street = "335 W 35th St, Chicago, IL 60616").isStreetOnly())
    }

    @Test
    fun `an address with any other part is not street only`() {
        assertFalse(StructuredAddress(street = "335 W 35th St", city = "Chicago").isStreetOnly())
        assertFalse(StructuredAddress(street = "335 W 35th St", postalCode = "60616").isStreetOnly())
        assertFalse(StructuredAddress(street = "335 W 35th St", extended = "Apt 2").isStreetOnly())
    }

    @Test
    fun `a blank address is not street only`() {
        assertFalse(StructuredAddress().isStreetOnly())
        assertFalse(StructuredAddress(street = "  ").isStreetOnly())
    }

    @Test
    fun `cleared parts add no commas to the displayed address`() {
        val address = StructuredAddress(
            street = "335 West 35th Street",
            city = "",
            state = " ",
            postalCode = "",
            country = "",
            poBox = "",
            extended = "",
        )

        assertEquals("335 West 35th Street", address.itemDisplay())
        assertEquals("335 West 35th Street", address.toSingleLine())
    }

    @Test
    fun `a full address shows street, then city state and postal code, then country`() {
        val address = StructuredAddress(
            street = "335 W 35th St",
            city = "Chicago",
            state = "IL",
            postalCode = "60616",
            country = "USA",
        )

        assertEquals("335 W 35th St\nChicago, IL, 60616\nUSA", address.itemDisplay())
        assertEquals("335 W 35th St, Chicago, IL, 60616, USA", address.toSingleLine())
    }

    @Test
    fun `a missing middle part leaves no gap`() {
        val address = StructuredAddress(street = "335 W 35th St", city = "Chicago", postalCode = "60616")

        assertEquals("335 W 35th St\nChicago, 60616", address.itemDisplay())
    }

    @Test
    fun `cleaning turns blank parts into null and trims the rest`() {
        val cleaned = StructuredAddress(street = " 1 Main St ", city = "", state = "  ", country = "USA").cleaned()

        assertEquals("1 Main St", cleaned.street)
        assertNull(cleaned.city)
        assertNull(cleaned.state)
        assertEquals("USA", cleaned.country)
    }
}
