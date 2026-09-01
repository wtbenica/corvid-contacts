// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.model.VCardType
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCCardBordered
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

enum class ShareFieldType {
    FIRST_NAME,
    LAST_NAME,
    OTHER_NAME,
    DISPLAY_NAME,
    NICKNAME,
    PHOTO,
    COMPANY,
    JOB_TITLE,
    EMAIL,
    PHONE,
    ADDRESS,
    BIRTHDAY,
    NOTES,
    WEB_SITE,
    RELATION,
    SOCIAL,
}

data class ShareField(
    val type: ShareFieldType,
    val number: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSelectionScreen(
    contactWithBook: ContactWithAddressBook,
    isQr: Boolean,
    onBack: () -> Unit,
    onComplete: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val contact = contactWithBook.contact

    var selectedOptions: Set<ShareField> by remember {
        val initial = mutableSetOf<ShareField>()

        if (!contact.firstName.isNullOrBlank()) initial.add(ShareField(ShareFieldType.FIRST_NAME))
        if (!contact.lastName.isNullOrBlank()) initial.add(ShareField(ShareFieldType.LAST_NAME))
        if (!contact.middleName.isNullOrBlank() || !contact.prefix.isNullOrBlank() || !contact.suffix.isNullOrBlank()) {
            initial.add(ShareField(ShareFieldType.OTHER_NAME))
        }
        if (!contact.displayName.isBlank()) initial.add(ShareField(ShareFieldType.DISPLAY_NAME))
        if (!contact.nickname.isNullOrBlank()) initial.add(ShareField(ShareFieldType.NICKNAME))
        if (contact.hasPhoto) initial.add(ShareField(ShareFieldType.PHOTO))

        contact.emails?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.EMAIL,
                    index
                )
            )
        }
        contact.phones?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.PHONE,
                    index
                )
            )
        }
        contact.structuredAddresses?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.ADDRESS,
                    index
                )
            )
        }
        if (!contact.company.isNullOrBlank()) initial.add(
            ShareField(ShareFieldType.COMPANY)
        )
        if (!contact.jobTitle.isNullOrBlank()) initial.add(
            ShareField(ShareFieldType.JOB_TITLE)
        )
        if (!contact.birthday.isNullOrBlank()) initial.add(
            ShareField(ShareFieldType.BIRTHDAY)
        )
        if (!contact.notes.isNullOrBlank()) initial.add(
            ShareField(ShareFieldType.NOTES)
        )
        contact.websites?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.WEB_SITE,
                    index
                )
            )
        }
        contact.relationships?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.RELATION,
                    index
                )
            )
        }
        contact.socialProfiles?.forEachIndexed { index, _ ->
            initial.add(
                ShareField(
                    ShareFieldType.SOCIAL,
                    index
                )
            )
        }
        mutableStateOf(initial.toSet())
    }

    fun toggleOption(option: ShareField) {
        selectedOptions = if (selectedOptions.contains(option)) {
            selectedOptions - option
        } else {
            selectedOptions + option
        }
    }

    val baseColor = ContactColors.resolveContactColor(contactWithBook)

    val chromeTitle =
        if (isQr) stringResource(R.string.detail_share_selection_title_qr) else stringResource(
            R.string.detail_share_selection_title_vcard
        )
    val chromeNavigationIcon: @Composable () -> Unit = { BackNavButton(onBack) }
    val chromeOnFabClick: () -> Unit = {
        onComplete(
            filterContact(
                contact,
                selectedOptions
            )
        )
    }
    val chromeFabContent: @Composable () -> Unit = {
        Icon(
            if (isQr) Icons.Rounded.QrCode else Icons.Rounded.Share,
            contentDescription = null
        )
    }

    if (!showScaffold) {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                    fabContent = chromeFabContent,
                    onFabClick = chromeOnFabClick,
                )
            )
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.smSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing)
        ) {
            CCCardBordered {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.innerSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ContactAvatar(
                        displayName = contact.getEffectiveDisplayName(),
                        photoUrl = contact.photoUrl,
                        hasPhoto = contact.hasPhoto,
                        id = contact.id,
                        size = 64.dp,
                        initials = contact.getInitials()
                    )
                    Column {
                        Text(
                            text = contact.getEffectiveDisplayName(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.detail_share_selection_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            CCCardBordered {
                Column {
                    // 1. Name and Identity
                    val otherNameValue = listOfNotNull(
                        contact.prefix,
                        contact.middleName,
                        contact.suffix
                    )
                        .joinToString(" ")
                        .trim()

                    mapOf(
                        ShareFieldType.FIRST_NAME to (contact.firstName to R.string.detail_share_selection_first_name),
                        ShareFieldType.LAST_NAME to (contact.lastName to R.string.detail_share_selection_last_name),
                        ShareFieldType.OTHER_NAME to (otherNameValue.ifBlank { null } to R.string.detail_share_selection_other_name),
                        ShareFieldType.DISPLAY_NAME to (contact.displayName.ifBlank { null } to R.string.detail_share_selection_display_name),
                        ShareFieldType.NICKNAME to (contact.nickname to R.string.detail_share_selection_nickname),
                    ).forEach { (type, data) ->
                        val (value, labelRes) = data
                        if (!value.isNullOrBlank()) {
                            ShareOptionItem(
                                label = stringResource(labelRes),
                                value = value,
                                selected = selectedOptions.contains(ShareField(type)),
                                onToggle = { toggleOption(ShareField(type)) },
                                baseColor = baseColor
                            )
                        }
                    }

                    if (contact.hasPhoto) {
                        ShareOptionItem(
                            label = stringResource(R.string.detail_share_selection_photo),
                            value = stringResource(R.string.detail_share_selection_photo),
                            selected = selectedOptions.contains(ShareField(ShareFieldType.PHOTO)),
                            onToggle = { toggleOption(ShareField(ShareFieldType.PHOTO)) },
                            baseColor = baseColor
                        )
                    }

                    // 2. Multi-value fields (Phones, Emails, etc.)
                    contact.phones?.forEachIndexed { index, phone ->
                        ShareOptionItem(
                            label = stringResource(
                                R.string.detail_share_selection_phone,
                                VCardType.getLabel(phone.type ?: "OTHER")
                            ),
                            value = phone.value,
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.PHONE,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.PHONE,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    contact.emails?.forEachIndexed { index, email ->
                        ShareOptionItem(
                            label = stringResource(
                                R.string.detail_share_selection_email,
                                VCardType.getLabel(email.type ?: "OTHER")
                            ),
                            value = email.value,
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.EMAIL,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.EMAIL,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    contact.structuredAddresses?.forEachIndexed { index, address ->
                        ShareOptionItem(
                            label = stringResource(
                                R.string.detail_share_selection_address,
                                VCardType.getLabel(address.type ?: "OTHER")
                            ),
                            value = address.toSingleLine(),
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.ADDRESS,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.ADDRESS,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    contact.socialProfiles?.forEachIndexed { index, profile ->
                        ShareOptionItem(
                            label = stringResource(
                                R.string.detail_share_selection_social,
                                VCardType.getLabel(profile.type ?: "OTHER")
                            ),
                            value = profile.value,
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.SOCIAL,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.SOCIAL,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    contact.websites?.forEachIndexed { index, url ->
                        ShareOptionItem(
                            label = stringResource(R.string.detail_share_selection_website),
                            value = url,
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.WEB_SITE,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.WEB_SITE,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    contact.relationships?.forEachIndexed { index, rel ->
                        ShareOptionItem(
                            label = stringResource(
                                R.string.detail_share_selection_relationship,
                                VCardType.getLabel(rel.type)
                            ),
                            value = rel.value,
                            selected = selectedOptions.contains(
                                ShareField(
                                    type = ShareFieldType.RELATION,
                                    number = index
                                )
                            ),
                            onToggle = {
                                toggleOption(
                                    ShareField(
                                        type = ShareFieldType.RELATION,
                                        number = index
                                    )
                                )
                            },
                            baseColor = baseColor
                        )
                    }

                    // 3. Professional and Misc simple fields
                    mapOf(
                        ShareFieldType.COMPANY to (contact.company to R.string.detail_share_selection_company),
                        ShareFieldType.JOB_TITLE to (contact.jobTitle to R.string.detail_share_selection_job_title),
                        ShareFieldType.BIRTHDAY to (contact.birthday to R.string.detail_share_selection_birthday),
                        ShareFieldType.NOTES to (contact.notes to R.string.detail_share_selection_notes),
                    ).forEach { (type, data) ->
                        val (value, labelRes) = data
                        if (!value.isNullOrBlank()) {
                            ShareOptionItem(
                                label = stringResource(labelRes),
                                value = value,
                                selected = selectedOptions.contains(ShareField(type)),
                                onToggle = { toggleOption(ShareField(type)) },
                                baseColor = baseColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            title = chromeTitle,
            navigationIcon = chromeNavigationIcon,
            baseColor = baseColor,
            onFabClick = chromeOnFabClick,
            fabContent = chromeFabContent,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}

@Composable
private fun ShareOptionItem(
    label: String,
    value: String,
    selected: Boolean,
    onToggle: () -> Unit,
    baseColor: Color = Color.Unspecified,
) {
    val actualColor = if (baseColor == Color.Unspecified) currentThemeColor() else baseColor
    ListItem(
        headlineContent = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        supportingContent = { Text(value) },
        trailingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = actualColor,
                    uncheckedColor = actualColor
                )
            )
        }
    )
}

private fun filterContact(
    contact: ContactEntity,
    options: Set<ShareField>,
): ContactEntity {
    return ContactEntity(
        id = contact.id,
        displayName = if (options.contains(ShareField(ShareFieldType.DISPLAY_NAME))) contact.displayName else "",
        firstName = if (options.contains(ShareField(ShareFieldType.FIRST_NAME))) contact.firstName else null,
        lastName = if (options.contains(ShareField(ShareFieldType.LAST_NAME))) contact.lastName else null,
        middleName = if (options.contains(ShareField(ShareFieldType.OTHER_NAME))) contact.middleName else null,
        prefix = if (options.contains(ShareField(ShareFieldType.OTHER_NAME))) contact.prefix else null,
        suffix = if (options.contains(ShareField(ShareFieldType.OTHER_NAME))) contact.suffix else null,
        emails = contact.emails?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.EMAIL,
                    index
                )
            )
        },
        phones = contact.phones?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.PHONE,
                    index
                )
            )
        },
        photoUrl = if (options.contains(ShareField(ShareFieldType.PHOTO))) contact.photoUrl else null,
        hasPhoto = if (options.contains(ShareField(ShareFieldType.PHOTO))) contact.hasPhoto else false,
        etag = null,
        addressBookHref = null,
        contactHref = null,
        colorInt = null,
        isArchived = false,
        categories = emptyList(),
        company = if (options.contains(ShareField(ShareFieldType.COMPANY))) contact.company else null,
        jobTitle = if (options.contains(ShareField(ShareFieldType.JOB_TITLE))) contact.jobTitle else null,
        birthday = if (options.contains(ShareField(ShareFieldType.BIRTHDAY))) contact.birthday else null,
        nickname = if (options.contains(ShareField(ShareFieldType.NICKNAME))) contact.nickname else null,
        notes = if (options.contains(ShareField(ShareFieldType.NOTES))) contact.notes else null,
        websites = contact.websites?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.WEB_SITE,
                    index
                )
            )
        },
        socialProfiles = contact.socialProfiles?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.SOCIAL,
                    index
                )
            )
        },
        relationships = contact.relationships?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.RELATION,
                    index
                )
            )
        },
        structuredAddresses = contact.structuredAddresses?.filterIndexed { index, _ ->
            options.contains(
                ShareField(
                    ShareFieldType.ADDRESS,
                    index
                )
            )
        }
    )
}

@ThemePreview
@Composable
private fun ShareSelectionScreenPreview() {
    val contact = ContactEntity(
        id = "1",
        firstName = "John",
        lastName = "Doe",
        displayName = "Broccoli",
        photoUrl = null,
        hasPhoto = false,
        emails = listOf(
            Email(
                "john.doe@example.com",
                "home"
            )
        ),
        phones = listOf(
            Phone(
                "1234567890",
                "mobile"
            )
        ),
        structuredAddresses = listOf(
            StructuredAddress(
                type = "home",
                street = "123 Main St",
                city = "Anytown",
                state = "CA",
                postalCode = "12345",
                country = "USA"
            )
        ),
        socialProfiles = listOf(
            SocialProfile(
                type = "twitter",
                value = "@johndoe"
            )
        ),
        etag = "BOB",
    )

    CorvidContactsTheme {
        ShareSelectionScreen(
            contactWithBook = ContactWithAddressBook(
                contact,
                null
            ),
            isQr = false,
            onBack = {},
            onComplete = {},
        )
    }
}
