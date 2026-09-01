// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * Dismissible chip surfacing an active group filter above the search bar.
 *
 * @param groupName The name of the group to filter by.
 * @param onDismiss Callback when the chip is dismissed.
 * @param modifier Modifier for the root layout.
 * @param baseColor The base color for the chip.
 */
@Composable
fun ActiveGroupFilterChip(
    groupName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = baseColor.surfaceVariant(),
        shadowElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 12.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp
            )
        ) {
            if (groupName.equals(
                    other = ContactsRepository.FAVORITE_CATEGORY,
                    ignoreCase = true
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = groupName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.list_action_clear_group_filter),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
