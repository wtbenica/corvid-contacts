// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Says that a contact was deleted from the phone's contacts in another app and is kept out of them,
 * and offers to put it back or to delete it from Corvid as well.
 */
@Composable
fun HiddenFromSystemCard(
    onShowAgain: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.medSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
        ) {
            Text(
                text = stringResource(R.string.detail_hidden_from_system_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.detail_hidden_from_system_message),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onShowAgain) {
                    Text(stringResource(R.string.detail_hidden_from_system_show))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.detail_hidden_from_system_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
