// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.oklch

/**
 * OKLCH lightness/chroma for [HueSlider]. Verified to keep hues in sRGB gamut without clipping.
 */
object HueSliderDefaults {
    const val LIGHTNESS = 0.65f
    const val CHROMA = 0.125f
}

/**
 * A horizontal slider over hue (0-360°) at a fixed lightness and chroma.
 *
 * @param hue Current hue in degrees.
 * @param onHueChange Called with the new hue as the user drags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    lightness: Float = HueSliderDefaults.LIGHTNESS,
    chroma: Float = HueSliderDefaults.CHROMA,
) {
    val trackHeight = 32.dp
    val gradientColors = remember(
        lightness,
        chroma
    ) {
        (0..12).map { step ->
            oklch(
                lightness,
                chroma,
                step * 30f
            )
        }
    }
    val thumbColor = remember(
        hue,
        lightness,
        chroma
    ) {
        oklch(
            lightness,
            chroma,
            hue
        )
    }
    val huePickerLabel = stringResource(R.string.settings_address_book_hue_slider_label)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(Brush.horizontalGradient(gradientColors))
    ) {
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            modifier = modifier
                .semantics { contentDescription = huePickerLabel },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .background(Brush.horizontalGradient(gradientColors))
                )
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(thumbColor)
                        .border(
                            3.dp,
                            Color.White,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            Color.Black.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
            }
        )
    }
}

@Preview
@Composable
fun HueSliderPreview() {
    HueSlider(
        360f,
        onHueChange = {},
    )
}