// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.extensions

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OKLab/OKLCH color conversions (Björn Ottosson's perceptual color space:
 * https://bottosson.github.io/posts/oklab/). Unlike HSL, a fixed OKLCH lightness looks equally
 * bright to the eye across every hue - HSL doesn't have that property (a yellow and a blue at the
 * same HSL lightness look noticeably different in brightness), which is what let a naive
 * RGB-channel-based color adjustment produce a muddy-looking green next to a vivid-looking pink
 * for the same nominal "darken" amount. OKLCH is used here so a hue slider with a fixed
 * lightness/chroma produces colors that all read as the same visual "weight" - see [oklch] and
 * [HueSlider][dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider].
 */

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

private fun linearToSrgb(c: Float): Float {
    val clamped = c.coerceIn(
        0f,
        1f
    )
    return if (clamped <= 0.0031308f) clamped * 12.92f else 1.055f * clamped.pow(1f / 2.4f) - 0.055f
}

/** A color in OKLab space: [L] perceptual lightness (0=black, 1=white), [a]/[b] chroma axes. */
data class Oklab(
    val L: Float,
    val a: Float,
    val b: Float,
)

/** A color in OKLCH space (polar form of [Oklab]): [L] lightness, [C] chroma, [h] hue in degrees. */
data class Oklch(
    val L: Float,
    val C: Float,
    val h: Float,
)

fun Color.toOklab(): Oklab {
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)

    val l = 0.41222146f * r + 0.53633255f * g + 0.051445995f * b
    val m = 0.2119035f * r + 0.6806995f * g + 0.10739696f * b
    val s = 0.08830246f * r + 0.28171885f * g + 0.6299787f * b

    val l_ = cbrt(l.toDouble())
        .toFloat()
    val m_ = cbrt(m.toDouble())
        .toFloat()
    val s_ = cbrt(s.toDouble())
        .toFloat()

    return Oklab(
        L = 0.21045426f * l_ + 0.7936178f * m_ - 0.004072047f * s_,
        a = 1.9779985f * l_ - 2.4285922f * m_ + 0.4505937f * s_,
        b = 0.025904037f * l_ + 0.78277177f * m_ - 0.80867577f * s_
    )
}

fun Oklab.toColor(alpha: Float = 1f): Color {
    val l_ = L + 0.39633778f * a + 0.21580376f * b
    val m_ = L - 0.105561346f * a - 0.06385417f * b
    val s_ = L - 0.08948418f * a - 1.2914855f * b

    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_

    val r = 4.0767417f * l - 3.3077116f * m + 0.23096994f * s
    val g = -1.268438f * l + 2.6097574f * m - 0.34131938f * s
    val bl = -0.0041960864f * l - 0.7034186f * m + 1.7076147f * s

    return Color(
        red = linearToSrgb(r),
        green = linearToSrgb(g),
        blue = linearToSrgb(bl),
        alpha = alpha
    )
}

fun Color.toOklch(): Oklch {
    val lab = toOklab()
    val c = sqrt(lab.a * lab.a + lab.b * lab.b)
    val hueRad = atan2(
        lab.b,
        lab.a
    )
    val hueDeg = Math
        .toDegrees(hueRad.toDouble())
        .toFloat()
        .let { if (it < 0) it + 360f else it }
    return Oklch(
        lab.L,
        c,
        hueDeg
    )
}

fun Oklch.toColor(alpha: Float = 1f): Color {
    val hueRad = Math.toRadians(h.toDouble())
    val a = (C * cos(hueRad)).toFloat()
    val b = (C * sin(hueRad)).toFloat()
    return Oklab(
        L,
        a,
        b
    ).toColor(alpha)
}

/**
 * Builds a [Color] from fixed OKLCH [lightness]/[chroma] and a varying [hueDegrees] (0-360).
 * [dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider] uses this with
 * [dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults]'s lightness/chroma so every
 * hue it can produce stays inside the sRGB gamut (verified numerically for all 360 hues) and
 * reads as the same visual weight - there's no "user picked a color that's too light/dark/
 * washed-out to work as a theme color" failure mode to guard against, because the picker can't
 * produce one.
 */
fun oklch(
    lightness: Float,
    chroma: Float,
    hueDegrees: Float,
    alpha: Float = 1f,
): Color = Oklch(
    lightness,
    chroma,
    hueDegrees
).toColor(alpha)
