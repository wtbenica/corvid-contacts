// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class FarthestHueTest {

    @Test
    fun `first book gets hue 0`() {
        assertEquals(0f, farthestHue(emptyList()), 0f)
    }

    @Test
    fun `successive books spread around the wheel`() {
        val used = mutableListOf<Float>()
        repeat(8) { used.add(farthestHue(used)) }

        assertEquals(listOf(0f, 180f, 90f, 270f, 45f, 135f, 225f, 315f), used)
    }

    @Test
    fun `distance wraps around the top of the wheel`() {
        // 350 is only 10 degrees from 0, so the opposite side of the wheel is 170.
        assertEquals(170f, farthestHue(listOf(350f)), 0f)
    }

    @Test
    fun `picks the middle of the widest gap`() {
        // Gaps are 0-100 (100 wide) and 100-360 (260 wide), so the answer is the middle of the latter.
        assertEquals(230f, farthestHue(listOf(0f, 100f)), 0f)
    }
}
