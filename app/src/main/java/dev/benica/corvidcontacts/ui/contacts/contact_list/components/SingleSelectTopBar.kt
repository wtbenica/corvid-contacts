// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopAppBar
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * Top app bar for picking a single contact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSelectTopBar(
    onCancelPickingSelf: () -> Unit,
    title: String = stringResource(R.string.list_title_select_contact),
    onClearSelf: (() -> Unit)? = null,
) {
    CCTopAppBar(
        title = title,
        navigationIcon = {
            CCIconButton(
                icon = Icons.TwoTone.Close,
                contentDescription = R.string.action_cancel,
                onClick = onCancelPickingSelf,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            if (onClearSelf != null) {
                CCTextButton(
                    text = R.string.list_action_clear_self,
                    onClick = onClearSelf,
                    baseColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

@Preview
@Composable
fun SingleSelectTopBarPreview() {
    CorvidContactsTheme {
        SingleSelectTopBar(
            onCancelPickingSelf = {}
        )
    }
}
