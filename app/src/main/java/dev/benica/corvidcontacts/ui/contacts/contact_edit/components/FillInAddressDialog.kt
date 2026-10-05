// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.repository.AddressSuggestion
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Looks up [query], the address as typed so far, and lists the matches to pick from. The lookup
 * only runs because the user asked for it, and nothing changes unless they pick one.
 */
@Composable
fun FillInAddressDialog(
    query: String,
    findMatches: suspend (String) -> List<AddressSuggestion>,
    onPick: (AddressSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    var matches by remember(query) { mutableStateOf<List<AddressSuggestion>?>(null) }
    LaunchedEffect(query) { matches = findMatches(query) }

    CCAlertDialog(
        onDismissRequest = onDismiss,
        title = R.string.fill_in_address_title,
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
            ) {
                Text(stringResource(R.string.fill_in_address_message, query))
                when (val found = matches) {
                    null -> Text(
                        text = stringResource(R.string.fill_in_address_searching),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    else -> if (found.isEmpty()) {
                        Text(
                            text = stringResource(R.string.fill_in_address_none),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        found.forEach { match ->
                            ListItem(
                                headlineContent = { Text(match.displayTitle) },
                                supportingContent = { Text(match.displaySubtitle) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(match) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = R.string.action_cancel,
        onConfirm = onDismiss,
    )
}
