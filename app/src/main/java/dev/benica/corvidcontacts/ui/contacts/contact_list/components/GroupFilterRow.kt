// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.ui.contacts.ContactColors

/**
 * Horizontal row of filter chips for selecting contact groups (categories).
 *
 * @param groups List of all available group names.
 * @param availableGroups Groups that apply to the current address book selection.
 * @param selectedGroup The currently selected group name, or null if showing all.
 * @param onGroupSelected Callback when a group chip is clicked.
 * @param selectedBook The currently selected address book for coloring.
 */
@Composable
fun GroupFilterRow(
    groups: List<String>,
    availableGroups: Set<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    selectedBook: AddressBookEntity? = null,
) {
    val finalGroups = remember(groups) {
        val hasFavorite = groups.any {
            it.equals(
                ContactsRepository.FAVORITE_CATEGORY,
                ignoreCase = true
            )
        }
        if (hasFavorite) {
            listOf(ContactsRepository.FAVORITE_CATEGORY) + groups.filter {
                !it.equals(
                    ContactsRepository.FAVORITE_CATEGORY,
                    ignoreCase = true
                )
            }
        } else {
            groups
        }
    }

    val baseColor = selectedBook?.let {
        ContactColors.getAddressBookColorWithFallback(
            it.colorInt,
            it.href
        )
    } ?: MaterialTheme.colorScheme.primary

    if (finalGroups.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AddressBookFilterChip(
                    label = { Text(stringResource(R.string.list_group_all)) },
                    icon = Icons.Rounded.PeopleOutline,
                    bookColor = baseColor,
                    book = null,
                    selected = selectedGroup == null,
                    onClick = { onGroupSelected(null) },
                )
            }
            items(finalGroups) { group ->
                val isFavorite = group.equals(
                    ContactsRepository.FAVORITE_CATEGORY,
                    ignoreCase = true
                )
                AddressBookFilterChip(
                    selected = selectedGroup == group,
                    bookColor = baseColor,
                    book = selectedBook,
                    enabled = group in availableGroups,
                    onClick = { onGroupSelected(if (selectedGroup == group) null else group) },
                    label = {
                        if (isFavorite) {
                            Icon(
                                Icons.Rounded.Favorite,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(group)
                        }
                    }
                )
            }
        }
    }
}
