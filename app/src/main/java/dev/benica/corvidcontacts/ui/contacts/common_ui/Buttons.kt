// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.annotation.StringRes
import dev.benica.corvidcontacts.ui.theme.isDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.extensions.active
import dev.benica.corvidcontacts.extensions.borderFocused
import dev.benica.corvidcontacts.extensions.complementary
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * A floating action button with theme-aware coloring.
 */
@Composable
fun CCFloatingActionButton(
    onClick: () -> Unit,
    baseColor: Color = currentThemeColor(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = (baseColor.complementary()).let {
            if (isDarkTheme()) it.borderFocused() else it.surfaceVariant()
        },
        contentColor = (baseColor
            .complementary()).let {
                if (isDarkTheme()) it.surface() else it.active()
            },
        elevation = FloatingActionButtonDefaults.elevation(Dimens.xsSpacing),
        content = content
    )
}

/**
 * Base implementation for CC buttons.
 */
@Composable
private fun BaseCCButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    text: String? = null,
    baseColor: Color = currentThemeColor(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = baseColor.text()
        ),
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription?.let { stringResource(it) },
            )
        }

        if (icon != null && text != null) {
            Spacer(Modifier.width(Dimens.xsSpacing))
        }

        if (text != null) {
            Text(text)
        }
    }
}

/**
 * A text button with theme-aware coloring.
 */
@Composable
fun CCButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = currentThemeColor(),
    content: @Composable RowScope.() -> Unit = {},
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = baseColor.text()),
        content = content
    )
}

/**
 * A text button with a string resource label.
 */
@Composable
fun CCButton(
    @StringRes text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = currentThemeColor(),
) {
    CCButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        baseColor = baseColor
    ) {
        Text(text = stringResource(text))
    }
}

/**
 * A text button that can include an icon.
 */
@Composable
fun CCTextButton(
    @StringRes text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    enabled: Boolean = true,
    baseColor: Color = currentThemeColor(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
) {
    BaseCCButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        iconDescription = iconDescription,
        text = stringResource(text),
        baseColor = baseColor,
        contentPadding = contentPadding
    )
}

/**
 * A text button that can include an icon, for labels that aren't a plain string resource
 * (e.g. "Add %s" formatted with a field name).
 */
@Composable
fun CCTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    enabled: Boolean = true,
    baseColor: Color = currentThemeColor(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
) {
    BaseCCButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        iconDescription = iconDescription,
        text = text,
        baseColor = baseColor,
        contentPadding = contentPadding
    )
}

/**
 * An icon button with theme-aware coloring.
 */
@Composable
fun CCIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    @StringRes contentDescription: Int?,
    formatArgs: Array<Any> = arrayOf(),
    size: Dp = 48.dp,
    color: Color = currentThemeColor(),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = color.text(),
            disabledContentColor = color
                .text()
                .copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription?.let {
                stringResource(
                    it,
                    *formatArgs
                )
            }
        )
    }
}
