// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.extensions.container
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * A standard dropdown menu for CC app.
 */
@Composable
fun CCDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = currentThemeColor().container(),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = containerColor,
        content = content
    )
}

/**
 * A dropdown menu specifically styled for the top app bar.
 */
@Composable
fun CCTopBarDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = currentThemeColor().container(),
    content: @Composable ColumnScope.() -> Unit,
) {
    CCDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(280.dp),
        containerColor = containerColor,
        content = content
    )
}

/**
 * A standard dropdown menu item for CC app.
 */
@Composable
fun CCDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val color = if (isDarkTheme()) {
        baseColor.text()
    } else {
        baseColor.text()
    }

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = color
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = color
                )
            }
        },
        enabled = enabled,
    )
}
