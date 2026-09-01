// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.KnownRelative
import dev.benica.corvidcontacts.data.model.MiscellaneousValue
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relative
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Contact detail screen: displays header along with cards for different contact information.
 * @param contact The contact to display.
 * @param relatives The relatives of the contact.
 * @param onNavigateToContact Callback for navigating to a contact.
 * @param onShowQr Callback for showing the QR code.
 * @param onShare Callback for sharing the contact.
 * @param onDownloadPhoto Callback for downloading the contact's photo when it's remote-only.
 */
@Composable
fun ContactDetailContent(
    contact: ContactEntity,
    relatives: List<Relative>,
    onNavigateToContact: (String) -> Unit,
    onShowQr: () -> Unit,
    onShare: (ContactEntity) -> Unit,
    onDownloadPhoto: (ContactEntity) -> Unit,
) {
    val context = LocalContext.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideLayout = maxWidth >= 600.dp

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)) {
            ContactDetailHeader(
                contact,
                onShowQr,
                onShare,
                onDownloadPhoto,
                wideLayout,
            )

            if (wideLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
                    ) {
                        ContactMethodsSection(contact, context)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
                    ) {
                        NetworksSection(contact, context)
                        OtherInfoSection(contact, relatives, onNavigateToContact)
                    }
                }
            } else {
                ContactMethodsSection(contact, context)
                NetworksSection(contact, context)
                OtherInfoSection(contact, relatives, onNavigateToContact)
            }
        }
    }
}

/**
 * An [ContactSection] that displays phone numbers and email addresses.
 * @param contact The contact to display.
 * @param context The context.
 */
@Composable
private fun ContactMethodsSection(
    contact: ContactEntity,
    context: Context,
) {
    val emails = contact.emails ?: emptyList()
    val phones = contact.phones ?: emptyList()

    val phoneData = if (phones.isNotEmpty()) {
        ContactFieldData(
            title = R.string.common_phone,
            icon = Icons.Outlined.Phone,
            items = phones,
            primaryAction = ActionItem(
                action = { phone ->
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "smsto:${phone.value}".toUri()
                    }

                    context.startActivity(intent)
                },
                icon = Icons.Outlined.Sms,
                contentDescription = R.string.detail_action_sms,
            ),
            secondaryAction = ActionItem(
                action = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = "tel:${phone.value}".toUri()
                    }

                    context.startActivity(intent)
                },
                icon = Icons.Outlined.Phone,
                contentDescription = R.string.detail_action_call
            ),
        )
    } else null

    val emailData = if (emails.isNotEmpty()) {
        ContactFieldData(
            title = R.string.common_email,
            icon = Icons.Outlined.Email,
            items = emails,
            primaryAction = ActionItem(
                icon = Icons.Outlined.Email,
                contentDescription = R.string.detail_action_email,
                action = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:${it.value}".toUri()
                    }

                    context.startActivity(intent)
                }
            ),
        )
    } else null

    val structuredAddresses = contact.structuredAddresses ?: emptyList()

    val activeStructuredAddresses = structuredAddresses.filter {
        it
            .isBlank()
            .not()
    }

    val addressData = if (activeStructuredAddresses.isNotEmpty()) {
        ContactFieldData(
            title = R.string.common_address,
            icon = Icons.Outlined.Map,
            items = activeStructuredAddresses,
            primaryAction = ActionItem(
                icon = Icons.Outlined.Directions,
                contentDescription = R.string.detail_action_directions,
                action = { addr ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "geo:0,0?q=${Uri.encode(addr.toSingleLine())}".toUri()
                    }

                    context.startActivity(intent)
                }
            ),
            isMultiline = true
        )
    } else null

    val contactData = listOfNotNull(
        phoneData,
        emailData,
        addressData
    )

    if (contactData.isNotEmpty()) {
        ContactFieldsCard(
            title = R.string.detail_section_contact,
            contactData = contactData,
        )
    }
}

/**
 * An [ContactSection] that displays social profiles and websites.
 */
@Composable
private fun NetworksSection(
    contact: ContactEntity,
    context: Context,
) {
    val socialProfiles = contact.socialProfiles ?: emptyList()
    val websites = contact.websites?.map { MiscellaneousValue(value = it) } ?: emptyList()

    val socialData = if (socialProfiles.isNotEmpty()) {
        ContactFieldData(
            title = R.string.common_social,
            icon = Icons.Outlined.People,
            items = socialProfiles,
            primaryAction = ActionItem(
                action = { sp ->
                    val deepLink = sp.getDeepLink()
                    val webFallback = sp.getWebFallback()
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        deepLink.toUri()
                    )
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Fallback to web if app not installed or deep link fails
                        val fallbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            webFallback.toUri()
                        )
                        context.startActivity(fallbackIntent)
                    }
                },
                icon = Icons.Outlined.Language,
                contentDescription = R.string.detail_action_open_link
            ),
            shrinkIcons = true
        )
    } else {
        null
    }

    val webData = if (websites.isNotEmpty()) {
        ContactFieldData(
            title = R.string.common_website,
            icon = Icons.Outlined.Language,
            items = websites,
            primaryAction = ActionItem(
                action = { website ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = website.value.toUri()
                    }
                    context.startActivity(intent)
                },
                icon = Icons.Outlined.Language,
                contentDescription = R.string.common_website,
            ),
            shrinkIcons = true
        )
    } else null

    val networkData = listOfNotNull(
        socialData,
        webData
    )

    if (networkData.isNotEmpty()) {
        ContactFieldsCard(
            title = R.string.detail_section_web,
            contactData = networkData,
        )
    }
}

/**
 * An [ContactSection] that displays other information (birthday, notes, etc.)
 * @param contact The contact being displayed.
 * @param relatives The relatives of the contact.
 * @param onNavigateToContact Callback for when a relative is clicked.
 */
@Composable
private fun OtherInfoSection(
    contact: ContactEntity,
    relatives: List<Relative>,
    onNavigateToContact: (String) -> Unit,
) {
    val relationshipsData = if (relatives.isNotEmpty()) {
        ContactFieldData(
            title = R.string.detail_section_relationships,
            icon = Icons.Outlined.People,
            items = relatives,
            primaryAction = ActionItem(
                icon = Icons.Outlined.ChevronRight,
                contentDescription = R.string.detail_action_view_contact,
                action = { rel ->
                    rel as KnownRelative
                    onNavigateToContact(rel.contact.id)
                },
                showAction = { rel ->
                    rel is KnownRelative
                }
            ),
        )
    } else null

    val birthdayData = contact.birthday?.let { bday ->
        val displayBirthday = remember(bday) {
            try {
                val date = LocalDate.parse(bday)
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            } catch (_: Exception) {
                bday
            }
        }

        ContactFieldData(
            title = R.string.common_birthday,
            icon = Icons.Outlined.Cake,
            items = listOf(
                MiscellaneousValue(
                    type = null,
                    value = displayBirthday
                )
            ),
            primaryAction = null,
        )
    }

    val notes = contact.notes

    val notesData = if (notes
            .isNullOrBlank()
            .not()
    ) {
        ContactFieldData(
            title = R.string.common_notes,
            icon = Icons.AutoMirrored.Outlined.Notes,
            items = listOf(MiscellaneousValue(value = notes)),
            primaryAction = null,
            isMultiline = true
        )
    } else null

    val photoUrlData = contact.photoUrl
        ?.takeIf { it.startsWith("http") }
        ?.let { url ->
            ContactFieldData(
                title = R.string.detail_photo_url_label,
                icon = Icons.Outlined.Link,
                items = listOf(MiscellaneousValue(value = url)),
                primaryAction = null,
                isMultiline = true
            )
        }

    val otherData = listOfNotNull(
        relationshipsData,
        birthdayData,
        notesData,
        photoUrlData
    )

    if (otherData.isNotEmpty()) {
        ContactFieldsCard(
            title = R.string.detail_section_other,
            contactData = otherData,
        )
    }
}

@ThemePreview
@Composable
fun ContactDetailContentPreview() {
    val contact = ContactEntity(
        id = "1",
        displayName = "John Doe",
        firstName = "John",
        lastName = "Doe",
        emails = listOf(
            Email(
                "john@example.com",
                "HOME"
            )
        ),
        phones = listOf(
            Phone(
                "+15551234567",
                "CELL",
                "US"
            ),
            Phone(
                "+15551234567",
                "CELL",
                "US"
            )

        ),
        //        address = null,
        photoUrl = null,
        etag = null,
        addressBookHref = "/books/personal/",
        contactHref = "/books/personal/1.vcf",
        colorInt = ContactColors.palette[7].toArgb(),
        isArchived = false,
        categories = listOf("Friends"),
        company = "Example Corp",
        jobTitle = "Software Engineer",
        birthday = "1990-01-01",
        nickname = "Johnny",
        notes = "Met at the conference",
        websites = listOf("https://example.com"),
        socialProfiles = listOf(
            SocialProfile(
                value = "@johndoe",
                type = "TWITTER"
            ),
            SocialProfile(
                value = "johndoe420",
                type = "FACEBOOK"
            )
        ),
        relationships = emptyList(),
        structuredAddresses = listOf(
            StructuredAddress(
                type = "HOME",
                street = "123 Main St",
                city = "Anytown",
                state = "CA",
                postalCode = "12345",
                country = "USA"
            )
        )
    )

    CorvidContactsTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            ContactDetailContent(
                contact = contact,
                relatives = emptyList(),
                onNavigateToContact = {},
                onShowQr = {},
                onShare = {},
                onDownloadPhoto = {}
            )
        }
    }
}
