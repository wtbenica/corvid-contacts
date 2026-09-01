// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * Individual contact row in the list, displaying the avatar, name, and primary contact info.
 *
 * @param contact The contact entity to display.
 * @param selected Whether this contact is currently selected.
 * @param isSelectionMode Whether the UI is in bulk selection mode.
 * @param onClick Callback when the item is clicked.
 * @param onLongClick Callback when the item is long-pressed.
 * @param bookColor Color associated with the contact's address book.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: ContactEntity,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    bookColor: Color = currentThemeColor(),
) {
    val contactColor = if (contact.colorInt != null) Color(contact.colorInt) else bookColor
    val firstEmail: String? = contact.emails?.firstOrNull()?.value
    val firstPhone: String? = contact.phones?.firstOrNull()?.value

    ListItem(
        headlineContent = {
            Text(
                text = buildAnnotatedString {
                    append(contact.getEffectiveDisplayName())

                    contact.nickname?.let {
                        if (it.isNotBlank()) withStyle(
                            style = SpanStyle(
                                color = bookColor.text(),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        ) {
                            append(" \"$it\"")
                        }
                    }
                })
        }, supportingContent = {
            Column {
                if (contact.company.isNullOrBlank().not() || contact.jobTitle.isNullOrBlank()
                        .not()
                ) Text(
                    text = buildAnnotatedString {
                        contact.company?.let { companyName ->
                            if (companyName.isNotBlank()) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                                    )
                                ) {
                                    append(companyName)
                                }
                            }
                        }

                        if (contact.jobTitle.isNullOrBlank()
                                .not() && contact.company.isNullOrBlank()
                                .not()
                        ) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                            ) {
                                append(" • ")
                            }
                        }

                        contact.jobTitle?.let { title ->
                            if (title.isNotBlank()) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                                    )
                                ) {
                                    append(title)
                                }
                            }
                        }
                    },
                )

                Text(
                    text = buildAnnotatedString {
                        if (!firstPhone.isNullOrBlank()) {
                            append(firstPhone)
                        }
                        if (!firstEmail.isNullOrBlank()) {
                            if (length > 0) append("  •  ")
                            append(firstEmail)
                        }
                    }, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }, trailingContent = {
            // Same 48dp footprint, but only mounted while selecting - disabling still consumes touch.
            if (isSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = contactColor,
                        checkmarkColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            } else {
                Box(Modifier.size(48.dp))
            }
        }, leadingContent = {
            ContactAvatar(
                displayName = contact.getEffectiveDisplayName(),
                baseColor = contactColor,
                hasPhoto = contact.hasPhoto,
                selected = selected,
                id = contact.id,
                initials = contact.getInitials()
            )
        }, modifier = Modifier
            .combinedClickable(
                onClick = onClick, onLongClick = onLongClick
            )
            .then(
                if (selected) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                contactColor.surfaceVariant(),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                } else {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                }
            ), colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * A simplified version of [ContactItem] for narrow layouts (like tablet side panes).
 * Shows only the avatar and the name.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleContactItem(
    contact: ContactEntity,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    bookColor: Color = currentThemeColor(),
    isActive: Boolean = false,
) {
    val contactColor = if (contact.colorInt != null) Color(contact.colorInt) else bookColor

    ListItem(
        headlineContent = {
            Text(
                text = contact.getEffectiveDisplayName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, modifier = Modifier
            .combinedClickable(
                onClick = onClick, onLongClick = onLongClick
            )
            .then(
                if (selected || isActive) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                contactColor.surfaceVariant(),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                } else {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                }
            ),
        trailingContent = {
            // Same 48dp footprint, but only mounted while selecting - disabling still consumes touch.
            if (isSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = contactColor,
                        checkmarkColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            } else {
                Box(Modifier.size(48.dp))
            }
        }, leadingContent = {
            ContactAvatar(
                displayName = contact.getEffectiveDisplayName(),
                baseColor = contactColor,
                hasPhoto = contact.hasPhoto,
                selected = selected,
                id = contact.id,
                initials = contact.getInitials()
            )
        }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}


/**
 * Pinned placeholder shown at the top of the list pane while a new contact is being created,
 * so the list doesn't jump around as the in-progress name is typed.
 */
@Composable
fun NewContactPlaceholderItem(bookColor: Color = currentThemeColor()) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.edit_title_new),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = bookColor.background(),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = bookColor
                    )
                }
            }
        }, modifier = Modifier.border(
            BorderStroke(2.dp, bookColor), MaterialTheme.shapes.small
        ), colors = ListItemDefaults.colors(
            containerColor = bookColor.background()
        )
    )
}

private val mockPreviewContact = ContactEntity(
    id = "1",
    displayName = "John Doe",
    firstName = "John",
    lastName = "Doe",
    nickname = "Jadoey",
    emails = listOf(
        Email(
            value = "jdoe@acmecorp.example.com", type = "home"
        )
    ),
    phones = listOf(
        Phone(
            value = "+1 234-567-8903", type = "mobile"
        )
    ),
    company = "Acme Corporation",
    photoUrl = null,
    hasPhoto = false,
    addressBookHref = "",
    categories = emptyList(),
    websites = emptyList(),
    relationships = emptyList(),
    isArchived = false,
    contactHref = null,
    etag = null,
    colorInt = null,
    jobTitle = "Software Engineer",
    birthday = null,
    notes = null,
)

@Preview(showBackground = true)
@Composable
private fun ContactItemPreview() {
    CorvidContactsTheme {
        ContactItem(
            contact = mockPreviewContact,
            selected = false,
            isSelectionMode = false,
            onClick = {},
            onLongClick = {})
    }
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContactItemDarkPreview() {
    CorvidContactsTheme {
        ContactItem(
            contact = mockPreviewContact,
            selected = false,
            isSelectionMode = false,
            onClick = {},
            onLongClick = {})
    }
}
