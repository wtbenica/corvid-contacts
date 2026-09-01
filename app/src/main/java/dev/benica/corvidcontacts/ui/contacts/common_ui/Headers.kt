// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Vrpano
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.borderFocused
import dev.benica.corvidcontacts.extensions.headline
import dev.benica.corvidcontacts.extensions.onBackground
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * Primary header for contact sections.
 */
@Composable
fun PrimaryHeader(
    title: String,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    onAdd: (() -> Unit)? = null,
    @StringRes onAddLabel: Int? = null,
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    padding: Dp = Dimens.medSpacing,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing),
        modifier = modifier.padding(vertical = padding)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = iconDescription?.let { desc -> stringResource(desc) },
                tint = baseColor.headline(),
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = baseColor.headline(),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        onAdd?.let {
            CCIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = onAddLabel,
                onClick = it,
                modifier = Modifier.size(24.dp),
                color = baseColor
            )
        }
    }
}

/**
 * Secondary header for smaller sections.
 */
@Composable
fun SecondaryHeader(
    title: String,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    onAdd: (() -> Unit)? = null,
    @StringRes onAddLabel: Int? = null,
) {
    val borderColor = if (isDarkTheme()) {
        baseColor.copy(alpha = 0.2f)
    } else {
        baseColor.borderFocused()
    }
    val borderWidth = 2.dp

    val containerColor = if (isDarkTheme()) {
        baseColor.copy(alpha = 0.1f)
    } else {
        baseColor.surface()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Box {
                HorizontalDivider(
                    modifier = Modifier.width(Dimens.xlSpacing),
                    thickness = borderWidth,
                    color = containerColor,
                )

                HorizontalDivider(
                    modifier = Modifier.width(Dimens.xlSpacing),
                    thickness = borderWidth,
                    color = borderColor,
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = Dimens.lgSpacing,
                    topEnd = Dimens.lgSpacing
                ),
                color = containerColor,
                border = BorderStroke(
                    width = borderWidth,
                    color = borderColor
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        Dimens.smSpacing,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .padding(
                            vertical = Dimens.smSpacing,
                            horizontal = Dimens.lgSpacing
                        )
                        .widthIn(min = Dimens.xxlSpacing)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = iconDescription?.let { desc -> stringResource(desc) },
                            tint = baseColor.onBackground(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = textStyle,
                        color = baseColor.onBackground(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = borderWidth,
                    color = containerColor,
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = borderWidth,
                    color = borderColor,
                )
            }

            onAdd?.let {
                CCIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = onAddLabel,
                    onClick = it,
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}


// Valid: 0-7
private const val colorNumber = 7
private val baseColor = ContactColors.palette[colorNumber]

@ThemePreview
@Composable
fun HeaderPreview() {
    CorvidContactsTheme {
        CCCardBordered(baseColor = baseColor) {
            Column(
                modifier = Modifier.padding(Dimens.medSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)
            ) {
                HorizontalDivider()
                Text("Primary Header")
                PrimaryHeader(
                    title = "Contacts",
                    baseColor = baseColor,
                    icon = Icons.Rounded.LocalCafe,
                    onAdd = {},
                    onAddLabel = R.string.common_notes,
                )

                HorizontalDivider()
                Text("Secondary Header")
                SecondaryHeader(
                    title = "Contacts",
                    baseColor = baseColor,
                    icon = Icons.Rounded.Vrpano,
                    onAdd = {},
                    onAddLabel = R.string.feedback_email_subject
                )
            }
        }
    }
}
