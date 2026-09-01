// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.onBackground
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * A theme-aware alert dialog for CC app.
 */
@Composable
fun CCAlertDialog(
    onDismissRequest: () -> Unit,
    @StringRes confirmButton: Int,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    @StringRes dismissButton: Int? = null,
    icon: ImageVector? = null,
    @StringRes title: Int? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    baseColor: Color = currentThemeColor(),
    dismissEnabled: Boolean = true,
    content: @Composable (BoxScope.() -> Unit),
) {
    CCAlertDialog(
        onDismissRequest = onDismissRequest,
        content = content,
        confirmButton = confirmButton,
        onConfirm = onConfirm,
        modifier = modifier,
        confirmEnabled = confirmEnabled,
        dismissButton = dismissButton,
        title = {
            title?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = icon,
                            contentDescription = null
                        )
                    }

                    Text(stringResource(it))
                }
            }
        },
        shape = shape,
        baseColor = baseColor,
        dismissEnabled = dismissEnabled
    )
}

/**
 * A theme-aware alert dialog with a Composable title.
 */
@Composable
fun CCAlertDialog(
    onDismissRequest: () -> Unit,
    @StringRes confirmButton: Int,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    @StringRes dismissButton: Int? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    baseColor: Color = currentThemeColor(),
    dismissEnabled: Boolean = true,
    content: @Composable (BoxScope.() -> Unit),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            CCTextButton(
                confirmButton,
                onClick = onConfirm,
                enabled = confirmEnabled,
                baseColor = baseColor
            )
        },
        modifier = modifier,
        dismissButton = dismissButton?.let {
            {
                CCTextButton(
                    dismissButton,
                    onClick = onDismissRequest,
                    enabled = dismissEnabled,
                    baseColor = baseColor
                )
            }
        },
        icon = icon,
        title = title,
        text = {
            CCCardBordered(
                modifier = Modifier.fillMaxWidth(),
                baseColor = baseColor,
                content = content
            )
        },
        shape = shape,
        containerColor = baseColor.surface(),
        iconContentColor = baseColor.onBackground(),
    )
}


@ThemePreview
@Composable
fun CCAlertDialogPreview() {
    CorvidContactsTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CCAlertDialog(
                onDismissRequest = {},
                confirmButton = R.string.widget_confirm_title,
                onConfirm = {},
                icon = Icons.TwoTone.Transform,
                title = R.string.merge_conflict_label,
            ) {
                Text("Content")
            }
        }
    }
}

