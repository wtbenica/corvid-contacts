// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * Provides a background color that is close to "neutral": black or white
 * Light: Lighten 0.95
 * Dark: Darken 0.9
 */
@Composable
fun Color.background() = if (isDarkTheme()) {
    darken(0.75f)
} else {
    lighten(0.9f)
}

@Composable
fun Color.container() = if (isDarkTheme()) {
    darken(0.5f)
} else {
    lighten(0.85f)
}

/**
 * Provides a content color that contrasts with background
 * Light: Darken 0.5
 * Dark: Lighten 0.8
 */
@Composable
fun Color.onBackground() = if (isDarkTheme()) {
    lighten(0.7f)
} else {
    darken(0.5f)
}

/**
 * Provides a surface color that offers a slight contrast with background
 * Light: Lighten 0.9
 * Dark: Darken 0.7
 */
@Composable
fun Color.surface() = if (isDarkTheme()) {
    darken(0.7f)
} else {
    lighten(0.85f)
}

/**
 * Provides a surface color that offers a slight contrast with background
 * Light: Darken 0.9
 * Dark: Lighten 0.7
 */
@Composable
fun Color.onSurface() = if (isDarkTheme()) {
    lighten(0.2f)
} else {
    darken(0.1f)
}

/**
 * Provides a surface variant color to indicate active or selected state
 * Light: Lighten 0.7
 * Dark: Darken 0.5
 */
@Composable
fun Color.surfaceVariant() = if (isDarkTheme()) {
    darken(0.6f)
} else {
    lighten(0.75f)
}

/**
 * Provides a focused border color that contrasts with background and surface/surface variant
 * Light: Lighten 0.6
 * Dark: lighten 0.1
 */
@Composable
fun Color.borderFocused() = if (isDarkTheme()) {
    lighten(0.1f)
} else {
    lighten(0.6f)
}

/**
 * Provides a border color that contrasts with background and surface/surface variant
 * Light: Lighten 0.2
 * Dark: Lighten 0.4
 */
@Composable
fun Color.border() = if (isDarkTheme()) {
    lighten(0.4f)
} else {
    lighten(0.2f)
}

/**
 * A variant of the base color to indicate active/selected state, meant to read clearly against
 * a [surfaceVariant] background of the same base color. Same in both themes: Darken 0.3.
 */
fun Color.active() = darken(0.3f)

@Composable
fun Color.text() = if (isDarkTheme()) {
    lighten(0.1f)
} else {
    darken(0.1f)
}

@Composable
fun Color.headline() = if (isDarkTheme()) {
    darken(0.1f)
} else {
    darken(0.3f)
}

/**
 * Provides a complementary color to the base color
 */
@Composable
fun Color.complementary(): Color = addAngle(280f)

@Composable
fun Color.addAngle(angle: Float): Color {
    // Array to hold HSV components: [0] = Hue (0-360), [1] = Saturation (0-1), [2] = Value (0-1)
    val hsv = FloatArray(3)

    // Android's built-in utility to convert ARGB Int to HSV
    android.graphics.Color.colorToHSV(
        this.toArgb(),
        hsv
    )

    // Shift the Hue by [angle] degrees and wrap it around 360
    hsv[0] = (hsv[0] + angle) % 360f

    // Reconstruct the color, retaining the original Alpha
    val newArgb = android.graphics.Color.HSVToColor(
        (this.alpha * 255).toInt(),
        hsv
    )

    return Color(newArgb)
}

/**
 * Darkens toward black by blending this color's OKLab coordinates toward black (L=0, a=0, b=0)
 * by [factor], not just reducing lightness - chroma has to come down too, or the result clips
 * out of gamut and looks oversaturated at high factors. OKLab keeps hues perceptually uniform,
 * unlike scaling RGB channels directly, which darkens some hues (e.g. green) faster than others.
 */
fun Color.darken(factor: Float = 0.3f): Color {
    val lab = toOklab()
    val f = factor.coerceIn(
        0f,
        1f
    )
    return Oklab(
        L = lab.L * (1f - f),
        a = lab.a * (1f - f),
        b = lab.b * (1f - f)
    ).toColor(alpha)
}

/** Lightens toward white by linearly blending this color's OKLab coordinates toward white
 * (L=1, a=0, b=0) by [factor]. See [darken] for why chroma has to fade too, not just lightness. */
fun Color.lighten(factor: Float = 0.3f): Color {
    val lab = toOklab()
    val f = factor.coerceIn(
        0f,
        1f
    )
    return Oklab(
        L = lab.L + (1f - lab.L) * f,
        a = lab.a * (1f - f),
        b = lab.b * (1f - f)
    ).toColor(alpha)
}