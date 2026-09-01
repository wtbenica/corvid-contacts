// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_merge.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.Icon
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
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.active
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.theme.Dimens

/** Which photo (if any) the merged contact should end up with. */
enum class PhotoChoice { SURVIVOR, ABSORBED, NONE }

/**
 * Photo isn't a typeable value like the other conflict fields, so this is three tappable tiles -
 * survivor's photo, absorbed's photo, or none at all - mirroring the add/choose/remove capability
 * every other field already has. Whichever the user picks is what [copyContactPhoto][
 * dev.benica.corvidcontacts.data.repository.ContactsRepository.copyContactPhoto]s onto the
 * survivor at merge-confirm time (for [PhotoChoice.ABSORBED]) or clears entirely (for
 * [PhotoChoice.NONE]).
 */
@Composable
fun MergeConflictPhotoField(
    survivor: ContactWithAddressBook,
    absorbed: ContactWithAddressBook,
    selected: PhotoChoice,
    onSelect: (PhotoChoice) -> Unit,
    modifier: Modifier = Modifier,
    color: Color,
) {
    ConflictContainer(
        modifier = modifier,
        color = color
    ) {
        Text(
            text = stringResource(R.string.merge_conflict_field_photo),
            style = MaterialTheme.typography.labelMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.medSpacing)) {
            PhotoCandidate(
                contact = absorbed,
                selected = selected == PhotoChoice.ABSORBED,
                onClick = { onSelect(PhotoChoice.ABSORBED) },
                color = color
            )
            PhotoCandidate(
                contact = survivor,
                selected = selected == PhotoChoice.SURVIVOR,
                onClick = { onSelect(PhotoChoice.SURVIVOR) },
                color = color
            )
            NoPhotoCandidate(
                selected = selected == PhotoChoice.NONE,
                onClick = { onSelect(PhotoChoice.NONE) },
                color = color
            )
        }
    }
}

@Composable
private fun PhotoCandidate(
    contact: ContactWithAddressBook,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
) {
    val avatarSize = 56.dp

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        ContactAvatar(
            displayName = contact.contact.getEffectiveDisplayName(),
            modifier = if (selected) {
                Modifier.border(
                    width = 2.dp,
                    color = color,
                    shape = MaterialTheme.shapes.small
                )
            } else {
                Modifier
            },
            baseColor = color.active(),
            hasPhoto = contact.contact.hasPhoto,
            id = contact.contact.id,
            size = avatarSize,
            initials = contact.contact.getInitials()
        )

        SelectionBadge(
            visible = selected,
            color = color
        )
    }
}

@Composable
private fun NoPhotoCandidate(
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
) {
    val avatarSize = 56.dp

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Surface(
            modifier = if (selected) {
                Modifier.border(
                    width = 2.dp,
                    color = color,
                    shape = MaterialTheme.shapes.small
                )
            } else {
                Modifier
            },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.size(avatarSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NoPhotography,
                    contentDescription = stringResource(R.string.merge_conflict_photo_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SelectionBadge(
            visible = selected,
            color = color
        )
    }
}

@Composable
private fun BoxScope.SelectionBadge(
    visible: Boolean,
    color: Color,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(
                x = 4.dp,
                y = 4.dp
            )
            .size(18.dp)
            .background(
                color = color,
                shape = CircleShape
            )
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp)
        )
    }
}
