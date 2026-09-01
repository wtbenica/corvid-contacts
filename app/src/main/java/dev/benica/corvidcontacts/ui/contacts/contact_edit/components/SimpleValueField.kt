// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * A single untyped value (e.g. a website URL) with an optional delete action - the untyped
 * counterpart to [TypedValueField], for fields that don't have a type selector.
 */
@Composable
fun SimpleValueField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDelete: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    requestInitialFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CCOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        if (enabled) {
            if (onMoveUp != null) {
                CCIconButton(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (onMoveDown != null) {
                CCIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (onDelete != null) {
            CCIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = R.string.detail_menu_delete,
                onClick = onDelete,
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                enabled = enabled
            )
        }
    }
}
