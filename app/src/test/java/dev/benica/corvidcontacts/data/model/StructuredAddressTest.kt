// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import org.junit.Assert.assertFalse
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
}
