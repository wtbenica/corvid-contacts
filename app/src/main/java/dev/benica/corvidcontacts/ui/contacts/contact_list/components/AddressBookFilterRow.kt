// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import android.content.res.Configuration
import dev.benica.corvidcontacts.ui.theme.isDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.extensions.active
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.extensions.border
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * Horizontal row of filter chips for selecting address books.
 */
@Composable
fun AddressBookFilterRow(
    selectedAddressBookHrefs: Set<String>,
    availableAddressBooks: List<AddressBookEntity>,
    onSelectAddressBook: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (availableAddressBooks.size > 1) {
            item {
                val isShowingAll =
                    selectedAddressBookHrefs.size > 1 || selectedAddressBookHrefs.isEmpty()
                AddressBookFilterChip(
                    label = stringResource(R.string.common_all_books),
                    baseColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.PeopleOutline,
                    book = null,
                    selected = isShowingAll,
                    onClick = onSelectAddressBook
                )
            }
        }
        items(availableAddressBooks) { book ->
            val isSelected =
                selectedAddressBookHrefs.size == 1 && selectedAddressBookHrefs.contains(book.href)
            AddressBookFilterChip(
                label = book.displayName ?: "Unknown",
                icon = ContactColors.getIconForAddressBook(
                    book.displayName,
                    book.iconName
                ),
                baseColor = ContactColors.getAddressBookColorWithFallback(
                    customColor = book.colorInt,
                    href = book.href
                ),
                book = book,
                selected = isSelected
            ) { onSelectAddressBook(book.href) }
        }
    }
}

@Composable
fun AddressBookFilterChip(
    label: String,
    baseColor: Color,
    book: AddressBookEntity?,
    selected: Boolean,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (String?) -> Unit,
) {
    AddressBookFilterChip(
        label = { Text(label) },
        bookColor = baseColor,
        book = book,
        selected = selected,
        icon = icon,
        enabled = enabled,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBookFilterChip(
    label: @Composable () -> Unit,
    bookColor: Color,
    book: AddressBookEntity?,
    selected: Boolean,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (String?) -> Unit,
) {
    val isLocal = book != null && book.isLocal

    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = { onClick(book?.href) },
        label = label,
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        trailingIcon = if (isLocal) {
            {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = stringResource(R.string.settings_address_book_local_badge),
                    modifier = Modifier.size(14.dp),
                )
            }
        } else null,
        colors = getBookFilterChipColors(bookColor = bookColor),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = if (isDarkTheme()) bookColor.active() else bookColor.border(),
            selectedBorderColor = if (isDarkTheme()) bookColor else bookColor.active(),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@Composable
private fun getBookFilterChipColors(
    bookColor: Color,
): SelectableChipColors = if (isDarkTheme()) {
    FilterChipDefaults.filterChipColors(
        containerColor = bookColor.background(),
        labelColor = bookColor.active(),
        iconColor = bookColor.active(),
        selectedContainerColor = bookColor.surface(),
        selectedLabelColor = bookColor,
        selectedLeadingIconColor = bookColor,
        selectedTrailingIconColor = bookColor
    )
} else {
    FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = bookColor.border(),
        iconColor = bookColor.border(),
        selectedContainerColor = bookColor.surface(),
        selectedLabelColor = bookColor.active(),
        selectedLeadingIconColor = bookColor.active(),
        selectedTrailingIconColor = bookColor.active()
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun AddressBookFilterChipPreview() {
    CorvidContactsTheme {
        Column {
            AddressBookFilterChip(
                label = "All Books",
                baseColor = Color(0xFFFF3388),
                icon = Icons.Rounded.PeopleOutline,
                book = null,
                selected = false,
                onClick = {})

            val book1 = AddressBookEntity(
                href = "0",
                displayName = "Selected",
                isVisible = true,
                colorInt = ContactColors.palette[3].toArgb()
            )
            AddressBookFilterChip(
                label = "Selected",
                icon = ContactColors.getIconForAddressBook(book1.displayName),
                baseColor = Color(0xFF323384),
                book = book1,
                selected = true,
                onClick = {})

            val book2 = AddressBookEntity(
                href = "1",
                displayName = "Unselected",
                isVisible = true,
                colorInt = ContactColors.palette[7].toArgb()
            )
            AddressBookFilterChip(
                label = "Unselected",
                icon = ContactColors.getIconForAddressBook(book2.displayName),
                baseColor = Color(0xFF832384),
                book = book2,
                selected = false,
                onClick = {}
            )
        }
    }
}
