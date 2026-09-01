// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

/**
 * CompositionLocal for the current theme color (baseColor).
 * Defaults to the primary color of the current MaterialTheme.
 */
val LocalThemeColor = compositionLocalOf { Color.Unspecified }

/**
 * CompositionLocal for whether the app is currently in dark mode.
 */
val LocalIsDark = staticCompositionLocalOf { false }

/**
 * Helper to get the current theme color, falling back to MaterialTheme.colorScheme.primary
 * if no color is provided via LocalThemeColor.
 */
@Composable
fun currentThemeColor(): Color {
    val color = LocalThemeColor.current
    return if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
}

/**
 * Helper to check if the theme is currently in dark mode, respecting settings overrides.
 */
@Composable
fun isDarkTheme(): Boolean = LocalIsDark.current

/**
 * Helper to check if the device is tablet-class (matches the threshold used to decide between
 * the phone and wide-screen shell navigation, and wide-layout breakpoints throughout the app).
 */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

