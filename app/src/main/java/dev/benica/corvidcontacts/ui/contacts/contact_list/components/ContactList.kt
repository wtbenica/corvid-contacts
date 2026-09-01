// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isWideScreen

/**
 * Scrollable list of contacts, grouped alphabetically by their display name.
 *
 * Uses sticky headers for each letter of the alphabet.
 *
 * @param contacts List of contacts to display.
 * @param selectedIds Set of IDs for currently selected contacts.
 * @param isSelectionMode Whether the list is in selection mode.
 * @param onToggleSelection Callback to toggle a contact's selection state.
 * @param onContactClick Callback when a contact row is clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactList(
    contacts: List<ContactWithAddressBook>,
    selectedIds: Set<String>,
    isSelectionMode: Boolean,
    onToggleSelection: (String) -> Unit,
    onContactClick: (ContactWithAddressBook) -> Unit,
    baseColor: Color = currentThemeColor(),
    showSimpleItems: Boolean = false,
    activeContactId: String? = null,
    showNewContactPlaceholder: Boolean = false,
) {
    // Group contacts by their first initial for sticky headers
    val grouped =
        contacts.groupBy {
            it.contact
                .getEffectiveDisplayName()
                .firstOrNull()
                ?.uppercaseChar() ?: '#'
        }
    val sortedKeys = grouped.keys.sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showNewContactPlaceholder) {
            item(key = "new_contact_placeholder") {
                NewContactPlaceholderItem(bookColor = baseColor)
            }
        }

        sortedKeys.forEach { initial ->
            // Sticky alphabetical header
            stickyHeader {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = baseColor.surfaceVariant(),
                    shape = if (isWideScreen()) {
                        RoundedCornerShape(
                            topEnd = Dimens.medSpacing,
                            bottomEnd = Dimens.medSpacing
                        )
                    } else {
                        RectangleShape
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            ),
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            // Individual contact items for this initial
            items(
                grouped[initial] ?: emptyList(),
                key = { it.contact.id }) { contactWithBook ->
                val contact = contactWithBook.contact
                val bookColor = ContactColors.resolveContactColor(contactWithBook)

                if (showSimpleItems) {
                    SimpleContactItem(
                        contact = contact,
                        selected = selectedIds.contains(contact.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) onToggleSelection(contact.id)
                            else onContactClick(contactWithBook)
                        },
                        onLongClick = { onToggleSelection(contact.id) },
                        bookColor = bookColor,
                        isActive = activeContactId != null && contact.id == activeContactId,
                    )
                } else {
                    ContactItem(
                        contact = contact,
                        selected = selectedIds.contains(contact.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) onToggleSelection(contact.id)
                            else onContactClick(contactWithBook)
                        },
                        onLongClick = { onToggleSelection(contact.id) },
                        bookColor = bookColor
                    )
                }
            }
        }
    }
}
