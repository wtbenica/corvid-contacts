// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * A card container with an inner shadow border.
 */
@Composable
fun CCCardBordered(
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    padding: PaddingValues = PaddingValues(Dimens.innerSpacing),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(6.dp)
            .then(
                if (isDarkTheme())
                    Modifier
                        .dropShadow(
                            shape = MaterialTheme.shapes.extraSmall,
                            shadow = Shadow(
                                radius = 6.dp,
                                color = Color.White,
                                spread = (-2).dp,
                                alpha = 0.1f
                            ),
                        )
                        .dropShadow(
                            shape = MaterialTheme.shapes.large,
                            shadow = Shadow(
                                radius = 3.dp,
                                color = Color.White,
                                alpha = 0.3f
                            ),
                        )
                else Modifier
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = modifier
                .then(
                    if (isDarkTheme()) {
                        Modifier
                    } else {
                        Modifier
                            .innerShadow(
                                shape = MaterialTheme.shapes.large,
                                shadow = Shadow(
                                    radius = 6.dp,
                                    color = Color.Black,
                                    spread = 2.dp,
                                    alpha = 0.1f
                                )
                            )
                            .innerShadow(
                                shape = MaterialTheme.shapes.large,
                                shadow = Shadow(
                                    radius = 3.dp,
                                    color = Color.Black,
                                    alpha = 0.3f
                                )
                            )
                    }
                )
                .padding(padding),
            content = content
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun CCCardBorderedPreview() {
    CorvidContactsTheme {
        CCCardBordered(
            baseColor = ContactColors.palette[5]
        ) {
            Text("This is good content")
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun CCCardBorderedDarkPreview() {
    CorvidContactsTheme {
        CCCardBordered(
            baseColor = ContactColors.palette[5],
        ) {
            Text("This is good content")
        }
    }
}