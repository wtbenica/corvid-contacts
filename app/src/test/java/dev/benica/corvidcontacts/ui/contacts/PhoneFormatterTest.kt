// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs under Robolectric because libphonenumber-android reads its metadata from the app's assets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
}
