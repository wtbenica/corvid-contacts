// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Row shown in place of the search bar while selection mode is active, on both phone (bottom bar)
 * and the wide-screen shell (list pane's search slot). Sized to the same 56dp content height as
 * [SearchBar]'s own text field/filter button, so callers that wrap [SearchBar] in background/inset
 * modifiers for one slot get the identical footprint when they swap in this row instead.
 */
@Composable
fun SelectAllRow(
    allSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = Dimens.lgSpacing, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.innerSpacing, Alignment.End),
    ) {
        Text(
            text = stringResource(
                if (allSelected) R.string.list_action_deselect_all
                else R.string.list_action_select_all
            ),
        )
        Checkbox(
            checked = allSelected,
            onCheckedChange = { onToggle() },
        )
    }
}
