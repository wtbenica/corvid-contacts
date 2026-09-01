// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCCardBordered
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * Identity section of the contact detail screen: avatar, name/nickname/job info, and QR/share
 * actions.
 * @param wideLayout Lays the avatar and name info out side by side instead of stacked, for use
 * when [ContactDetailContent] has split into two columns.
 */
@Composable
fun ContactDetailHeader(
    contact: ContactEntity,
    onShowQr: () -> Unit,
    onShare: (ContactEntity) -> Unit,
    onDownloadPhoto: (ContactEntity) -> Unit,
    wideLayout: Boolean = false,
) {
    CCCardBordered {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
        ) {
            if (wideLayout) {
                Row(verticalAlignment = Alignment.Bottom) {
                    ContactAvatar(
                        displayName = contact.getEffectiveDisplayName(),
                        photoUrl = contact.photoUrl,
                        hasPhoto = contact.hasPhoto,
                        id = contact.id,
                        size = 120.dp,
                        shape = MaterialTheme.shapes.medium,
                        borderWidth = 4.dp,
                        initials = contact.getInitials()
                    )

                    Spacer(modifier = Modifier.width(Dimens.lgSpacing))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = Dimens.lgSpacing),
                        verticalArrangement = Arrangement.spacedBy(
                            Dimens.xsSpacing, Alignment.Bottom
                        )
                    ) {
                        ContactNameInfo(contact)
                    }

                    Column {
                        CCIconButton(
                            icon = Icons.Outlined.QrCode,
                            contentDescription = R.string.detail_share_qr_code,
                            onClick = onShowQr,
                        )

                        CCIconButton(
                            icon = Icons.Outlined.Share,
                            contentDescription = R.string.common_share,
                            onClick = { onShare(contact) },
                        )
                    }
                }

                if (contact.photoUrl?.startsWith("http") == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
                    ) {
                        Text(
                            text = stringResource(R.string.detail_photo_not_downloaded),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        CCTextButton(
                            text = R.string.detail_action_download_photo,
                            onClick = { onDownloadPhoto(contact) })
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactAvatar(
                        displayName = contact.getEffectiveDisplayName(),
                        photoUrl = contact.photoUrl,
                        hasPhoto = contact.hasPhoto,
                        id = contact.id,
                        size = 120.dp,
                        shape = MaterialTheme.shapes.medium,
                        borderWidth = 4.dp,
                        initials = contact.getInitials()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column {
                        CCIconButton(
                            icon = Icons.Outlined.QrCode,
                            contentDescription = R.string.detail_share_qr_code,
                            onClick = onShowQr,
                        )


                        CCIconButton(
                            icon = Icons.Outlined.Share,
                            contentDescription = R.string.common_share,
                            onClick = { onShare(contact) },
                        )
                    }
                }

                if (contact.photoUrl?.startsWith("http") == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
                    ) {
                        Text(
                            text = stringResource(R.string.detail_photo_not_downloaded),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        CCTextButton(
                            text = R.string.detail_action_download_photo,
                            onClick = { onDownloadPhoto(contact) })
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.smSpacing))

                ContactNameInfo(contact)
            }
        }
    }
}

/**
 * Name, full name (if different), nickname, and job/company line - the text block shown either
 * below the avatar (portrait) or beside it (wide layout).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactNameInfo(contact: ContactEntity) {
    val effectiveDisplayName = contact.getEffectiveDisplayName()

    Text(
        text = effectiveDisplayName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    val fullName = contact.getFullName()

    if (effectiveDisplayName != fullName && fullName.isNullOrBlank().not()) Text(
        text = fullName,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    contact.nickname?.let {
        Text(
            text = "\"$it\"",
            style = MaterialTheme.typography.titleSmall,
            color = currentThemeColor()
        )
    }

    if (!contact.jobTitle.isNullOrBlank() || !contact.company.isNullOrBlank()) {
        val jobLine = if (!contact.jobTitle.isNullOrBlank() && !contact.company.isNullOrBlank()) {
            stringResource(
                R.string.detail_job_at, contact.jobTitle, contact.company
            )
        } else {
            contact.jobTitle ?: contact.company ?: ""
        }
        Text(
            text = jobLine,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // "Favorites" already has its own always-visible heart icon in the top bar - a chip for it
    // here would just be a second indicator for the same thing.
    val groups = contact.categories?.filterNot {
        it.equals("Archived", ignoreCase = true) || it.equals(
            ContactsRepository.FAVORITE_CATEGORY,
            ignoreCase = true
        )
    } ?: emptyList()
    if (contact.isArchived || groups.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(top = Dimens.xsSpacing),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
        ) {
            if (contact.isArchived) {
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(stringResource(R.string.list_title_archived)) },
                )
            }
            groups.forEach { group ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(group) },
                )
            }
        }
    }
}

val contact = ContactEntity(
    id = "A",
    displayName = "Aunty Em",
    firstName = "Emily",
    middleName = "Potato",
    lastName = "Giviono",
    jobTitle = "Gardener",
    company = "Emerald City Gardens",
    nickname = "Em",
    photoUrl = null,
    emails = listOf(
        Email(
            "emily@example.com", "home"
        )
    ),
    phones = listOf(
        Phone(
            "+1234567890", "mobile"
        )
    ),
    //        address = "123 Main St, Anytown, State, 12345, Country",
    etag = "etag",
)

@Preview(
    showBackground = true, backgroundColor = 0xFF88CCFF, uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
internal fun ContactDetailHeaderPreviewDark() {

    CorvidContactsTheme {
        ContactDetailHeader(contact = contact, onShowQr = { }, onShare = {}, onDownloadPhoto = {})
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF88CCFF,
)
@Composable
internal fun ContactDetailHeaderPreview() {

    CorvidContactsTheme {
        ContactDetailHeader(contact = contact, onShowQr = { }, onShare = {}, onDownloadPhoto = {})
    }
}

