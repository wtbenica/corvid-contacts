// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Logout
import androidx.compose.material.icons.twotone.Archive
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material.icons.twotone.Feedback
import androidx.compose.material.icons.twotone.Menu
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.QrCode
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material.icons.twotone.SwitchAccount
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.benica.corvidcontacts.BuildConfig
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.container
import dev.benica.corvidcontacts.extensions.onBackground
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCDropdownMenuItem
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopAppBar
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTopBarDropdownMenu
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme

/**
 * Standard top app bar for browsing contacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopBar(
    showArchived: Boolean,
    selfContact: ContactWithAddressBook?,
    onToggleProfileMenu: (Boolean) -> Unit,
    profileMenuExpanded: Boolean,
    onToggleArchived: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    isLocalOnlyMode: Boolean,
    onSetUpSync: () -> Unit,
    onLogout: () -> Unit,
    onStartPickingSelf: () -> Unit,
    onShareSelf: (ContactWithAddressBook) -> Unit,
    onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
    detailActions: @Composable (RowScope.() -> Unit)? = null,
) {
    val title = if (showArchived) {
        stringResource(R.string.list_title_archived)
    } else {
        stringResource(R.string.list_title_contacts)
    }

    CCTopAppBar(
        title = title,
        icon = if (showArchived) Icons.TwoTone.Archive else null,
        actions = {
            if (detailActions != null) {
                detailActions()
                Spacer(Modifier.width(8.dp))
                VerticalDivider(
                    modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.width(8.dp))
            }

            IconButton(
                onClick = { onToggleProfileMenu(true) }
            ) {
                val effectiveDisplayName = selfContact?.contact?.getEffectiveDisplayName()

                if (effectiveDisplayName != null) {
                    ContactAvatar(
                        displayName = effectiveDisplayName,
                        hasPhoto = selfContact.contact.hasPhoto,
                        id = selfContact.contact.id,
                        shape = CircleShape,
                        initials = selfContact.contact.getInitials()
                    )
                } else {
                    Icon(
                        imageVector = Icons.TwoTone.Menu,
                        contentDescription = stringResource(R.string.list_menu_show_menu)
                    )
                }
            }

            ProfileMenu(
                expanded = profileMenuExpanded,
                onDismissRequest = { onToggleProfileMenu(false) },
                selfContact = selfContact,
                onToggleArchived = onToggleArchived,
                showArchived = showArchived,
                onRefresh = onRefresh,
                onSettingsClick = onSettingsClick,
                isLocalOnlyMode = isLocalOnlyMode,
                onSetUpSync = onSetUpSync,
                onLogout = onLogout,
                onStartPickingSelf = onStartPickingSelf,
                onShareSelf = onShareSelf,
                onShareSelfViaQr = onShareSelfViaQr
            )
        },
    )
}

@Composable
fun profileMenuContainerColor(baseColor: Color) = if (isDarkTheme()) {
    baseColor.surface()
} else {
    baseColor.container()
}

/**
 * Dropdown menu for profile-related actions.
 */
@Composable
private fun ProfileMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selfContact: ContactWithAddressBook?,
    onToggleArchived: () -> Unit,
    showArchived: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    isLocalOnlyMode: Boolean,
    onSetUpSync: () -> Unit,
    onLogout: () -> Unit,
    onStartPickingSelf: () -> Unit,
    onShareSelf: (ContactWithAddressBook) -> Unit,
    onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
    baseColor: Color = currentThemeColor(),
) {
    val context = LocalContext.current

    CCTopBarDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = profileMenuContainerColor(baseColor)
    ) {
        ProfileMenuContent(
            onDismissRequest = onDismissRequest,
            selfContact = selfContact,
            onToggleArchived = onToggleArchived,
            showArchived = showArchived,
            onRefresh = onRefresh,
            onSettingsClick = onSettingsClick,
            isLocalOnlyMode = isLocalOnlyMode,
            onSetUpSync = onSetUpSync,
            onLogout = onLogout,
            onStartPickingSelf = onStartPickingSelf,
            onShareSelf = onShareSelf,
            onShareSelfViaQr = onShareSelfViaQr,
            baseColor = baseColor,
            context = context
        )
    }
}

@Composable
private fun ProfileMenuContent(
    onDismissRequest: () -> Unit,
    selfContact: ContactWithAddressBook?,
    onToggleArchived: () -> Unit,
    showArchived: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    isLocalOnlyMode: Boolean,
    onSetUpSync: () -> Unit,
    onLogout: () -> Unit,
    onStartPickingSelf: () -> Unit,
    onShareSelf: (ContactWithAddressBook) -> Unit,
    onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
    baseColor: Color = currentThemeColor(),
    context: Context? = null
) {
    val feedbackSubject = stringResource(R.string.feedback_email_subject)

    ProfileMenuHeader(
        selfContact,
        baseColor,
        onStartPickingSelf,
        onShareSelf,
        onShareSelfViaQr,
        onDismissRequest
    )

    HorizontalDivider(
        thickness = 1.dp, color = baseColor
    )

    ProfileMenuActions(
        showArchived,
        onToggleArchived,
        onDismissRequest,
        baseColor,
        isLocalOnlyMode,
        onSetUpSync,
        onRefresh,
        onSettingsClick,
        feedbackSubject,
        context,
        onLogout
    )

    HorizontalDivider(
        thickness = 1.dp, color = baseColor
    )

    Spacer(modifier = Modifier.height(Dimens.lgSpacing))
}


@Composable
private fun ProfileMenuActions(
    showArchived: Boolean,
    onToggleArchived: () -> Unit,
    onDismissRequest: () -> Unit,
    baseColor: Color,
    isLocalOnlyMode: Boolean,
    onSetUpSync: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    feedbackSubject: String,
    context: Context? = null,
    onLogout: () -> Unit
) {
    Surface(
        color = if (isDarkTheme()) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(Dimens.outerSpacing)
        ) {
            CCDropdownMenuItem(
                text = if (showArchived) stringResource(R.string.list_menu_show_active) else stringResource(
                    R.string.list_menu_show_archived
                ),
                onClick = {
                    onToggleArchived()
                    onDismissRequest()
                },
                icon = if (showArchived) Icons.TwoTone.Unarchive else Icons.TwoTone.Archive,
                baseColor = baseColor
            )

            CCDropdownMenuItem(
                text = stringResource(if (isLocalOnlyMode) R.string.list_menu_set_up_sync else R.string.list_menu_sync),
                onClick = {
                    if (isLocalOnlyMode) onSetUpSync() else onRefresh()
                    onDismissRequest()
                },
                icon = Icons.TwoTone.Refresh,
                baseColor = baseColor
            )

            CCDropdownMenuItem(
                text = stringResource(R.string.common_settings),
                onClick = {
                    onSettingsClick()
                    onDismissRequest()
                },
                icon = Icons.TwoTone.Settings,
                baseColor = baseColor
            )

            CCDropdownMenuItem(
                text = stringResource(R.string.list_menu_send_feedback),
                onClick = {
                    val body = "\n\n---\n" +
                            "App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                            "Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n" +
                            "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
                    val mailtoUri = "mailto:feedback@benica.dev" +
                            "?subject=${Uri.encode(feedbackSubject)}" +
                            "&body=${Uri.encode(body)}"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = mailtoUri.toUri()
                    }

                    context?.startActivity(intent)
                    onDismissRequest()
                },
                icon = Icons.TwoTone.Feedback,
                baseColor = baseColor
            )

            CCDropdownMenuItem(
                text = stringResource(if (isLocalOnlyMode) R.string.list_menu_delete_local_data else R.string.list_menu_logout),
                onClick = {
                    onLogout()
                    onDismissRequest()
                },
                icon = if (isLocalOnlyMode) Icons.TwoTone.DeleteForever else Icons.AutoMirrored.TwoTone.Logout,
                baseColor = baseColor
            )
        }
    }
}

@Composable
private fun ProfileMenuHeader(
    selfContact: ContactWithAddressBook?,
    baseColor: Color,
    onStartPickingSelf: () -> Unit,
    onShareSelf: (ContactWithAddressBook) -> Unit,
    onShareSelfViaQr: (ContactWithAddressBook) -> Unit,
    onDismissRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.medSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAvatar(
            displayName = selfContact?.contact?.getEffectiveDisplayName(),
            modifier = Modifier.size(64.dp),
            baseColor = baseColor,
            hasPhoto = selfContact?.contact?.hasPhoto ?: false,
            id = selfContact?.contact?.id,
            initials = selfContact?.contact?.getInitials()
        )

        val buttonColor = baseColor.onBackground()
        val nameColor = baseColor.text()

        if (selfContact?.contact != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.width(48.dp))

                Text(
                    text = selfContact.contact.getEffectiveDisplayName(),
                    color = nameColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                CCIconButton(
                    icon = Icons.TwoTone.SwitchAccount,
                    contentDescription = R.string.list_menu_set_self,
                    onClick = {
                        onStartPickingSelf()
                        onDismissRequest()
                    },
                    color = buttonColor,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)) {
                CCTextButton(
                    text = R.string.common_share,
                    icon = Icons.TwoTone.Share,
                    onClick = { onShareSelf(selfContact) },
                    baseColor = buttonColor
                )

                CCTextButton(
                    text = R.string.detail_share_selection_title_qr,
                    icon = Icons.TwoTone.QrCode,
                    onClick = { onShareSelfViaQr(selfContact) },
                    baseColor = buttonColor
                )
            }
        } else {
            CCTextButton(
                text = R.string.list_menu_set_self,
                icon = Icons.TwoTone.Person,
                onClick = {
                    onStartPickingSelf()
                    onDismissRequest()
                },
                baseColor = buttonColor
            )
        }
    }
}

@Preview
@Composable
fun NormalTopBarPreview() {
    CorvidContactsTheme {
        NormalTopBar(
            showArchived = false,
            selfContact = null,
            onToggleProfileMenu = {},
            profileMenuExpanded = false,
            onToggleArchived = {},
            onRefresh = {},
            onSettingsClick = {},
            isLocalOnlyMode = false,
            onSetUpSync = {},
            onLogout = {},
            onStartPickingSelf = {},
            onShareSelf = {},
            onShareSelfViaQr = {}
        )
    }
}

@ThemePreview
@Composable
fun ProfileMenuPreview() {
    CorvidContactsTheme {
        val baseColor = ContactColors.palette[7]
        Surface(
            modifier = Modifier.padding(Dimens.xxlSpacing),
            color = profileMenuContainerColor(baseColor),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp),
            ) {
                ProfileMenuContent(
                    onDismissRequest = {},
                    selfContact = null,
                    onStartPickingSelf = {},
                    onShareSelf = {},
                    onShareSelfViaQr = {},
                    baseColor = baseColor,
                    onToggleArchived = {},
                    showArchived = false,
                    onRefresh = {},
                    onSettingsClick = {},
                    isLocalOnlyMode = false,
                    onSetUpSync = {},
                    onLogout = {},
                )
            }
        }
    }
}

@ThemePreview
@Composable
fun ProfileMenuWithSelfPreview() {
    val mockContact = ContactWithAddressBook(
        contact = ContactEntity(
            id = "1",
            displayName = "Marky Mark",
            firstName = null,
            lastName = null,
            emails = emptyList(),
            phones = emptyList(),
            photoUrl = null,
            etag = "pop",
            addressBookHref = "",
            categories = emptyList(),
            websites = emptyList(),
            relationships = emptyList(),
            isArchived = false,
            contactHref = null,
            colorInt = null,
            jobTitle = null,
            birthday = null,
            notes = null,
            middleName = null,
            prefix = null,
            suffix = null,
            hasPhoto = false,
            socialProfiles = emptyList(),
            structuredAddresses = emptyList()
        ),
        addressBook = AddressBookEntity(
            href = "1",
            displayName = "Work",
            colorInt = 0xFF983459.toInt(),
        )
    )

    CorvidContactsTheme {
        val baseColor = ContactColors.palette[7]
        Surface(
            modifier = Modifier.padding(Dimens.xxlSpacing),
            color = profileMenuContainerColor(baseColor),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp),
            ) {
                ProfileMenuContent(
                    onDismissRequest = {},
                    selfContact = mockContact,
                    onStartPickingSelf = {},
                    onShareSelf = {},
                    onShareSelfViaQr = {},
                    baseColor = baseColor,
                    onToggleArchived = {},
                    showArchived = false,
                    onRefresh = {},
                    onSettingsClick = {},
                    isLocalOnlyMode = false,
                    onSetUpSync = {},
                    onLogout = {},
                )
            }
        }
    }
}
