// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * A shortcut for a standard phone preview with system UI and background.
 */
@Preview(
    name = "Light Mode",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp"
)
annotation class PhonePreview

/**
 * A shortcut for a dark mode phone preview with system UI and background.
 */
@Preview(
    name = "Dark Mode",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
annotation class DarkPhonePreview

/**
 * A shortcut for a standard tablet preview.
 */
@Preview(
    name = "Tablet Light",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=1280dp,height=800dp,orientation=landscape"
)
annotation class TabletPreview

/**
 * A shortcut for a dark mode tablet preview.
 */
@Preview(
    name = "Tablet Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1280dp,height=800dp,orientation=landscape"
)
annotation class DarkTabletPreview

/**
 * A shortcut that shows both Light and Dark mode phone previews.
 */
@PhonePreview
@DarkPhonePreview
@TabletPreview
@DarkTabletPreview
annotation class ThemePreview
