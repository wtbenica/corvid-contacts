// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LooksTwo
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Propane
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.TypedValue
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCListItem
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * Data class for holding a list of data and display info for a particular contact field (e.g. phone numbers, email
 * addresses, birthday, etc.)
 * @param icon the icon to display in the leading position for each item
 * @param items the list of items to display
 * @param primaryAction the action to perform when the primary action is clicked
 * @param secondaryAction the action to perform when the secondary action is clicked
 */
data class ContactFieldData<T : TypedValue>(
    @StringRes val title: Int,
    val icon: ImageVector,
    val items: List<T>,
    val primaryAction: ActionItem<T>?,
    val secondaryAction: ActionItem<T>? = null,
    val isMultiline: Boolean = false,
    val shrinkIcons: Boolean = false,
)

/**
 * Data class for an action to perform on a contact field value
 * @param action the function to perform when the action is clicked
 * @param icon the icon to display in the action button
 * @param contentDescription the content description for the action button
 * @param showAction the function to determine if the action should be displayed for the given item
 */
data class ActionItem<T>(
    val action: ((T) -> Unit),
    @StringRes val contentDescription: Int,
    val icon: ImageVector,
    val showAction: (T) -> Boolean = { true },
)

/**
 * A titled [ContactSection] card grouping one or more [ContactFieldData] value lists (e.g. phone
 * numbers and email addresses together under "Contact"). Used by the contact-detail sections -
 * [ContactMethodsSection], [NetworksSection], [OtherInfoSection] - as their shared rendering
 * primitive.
 */
@Composable
fun ContactFieldsCard(
    @StringRes title: Int,
    contactData: List<ContactFieldData<*>>,
) {
    ContactSection(
        title = stringResource(title),
        content = {
            contactData.forEach { data ->
                ContactValuesList(
                    data = data,
                    icon = data.icon,
                    contentDescription = data.title,
                    isMultiline = data.isMultiline,
                )
            }
        },
    )
}

/**
 * Displays a list of items (type, value, and actions) along with a header (icon and label)
 * for a specific field (e.g. Phone, Email, Birthday)
 * @param data the data for the contact field
 * @param baseColor the color for the contact field - action button icons
 */
@Composable
fun <T : TypedValue> ContactValuesList(
    data: ContactFieldData<T>,
    baseColor: Color = currentThemeColor(),
    icon: ImageVector? = null,
    @StringRes contentDescription: Int? = null,
    isMultiline: Boolean = false,
) {
    Section(
        title = stringResource(data.title),
        modifier = Modifier.padding(vertical = Dimens.medSpacing),
        baseColor = baseColor,
        icon = icon,
        contentDescription = contentDescription,
        itemSpacing = 0.dp
    ) {
        data.items.forEach { item ->
            ContactValuesListItem(
                item = item,
                primaryAction = data.primaryAction,
                secondaryAction = data.secondaryAction,
                baseColor = baseColor,
                isMultiline = isMultiline
            )
        }
    }
}

/**
 * A single item in a [ContactValuesList]
 * @param item the item to display
 * @param baseColor the color for the trailing and overline content
 * @param primaryAction info for a primary action button
 * @param secondaryAction info for a secondary action button
 * @param isMultiline whether the value should be displayed on multiple lines
 */
@Composable
fun <T : TypedValue> ContactValuesListItem(
    item: T,
    baseColor: Color = currentThemeColor(),
    primaryAction: ActionItem<T>?,
    secondaryAction: ActionItem<T>? = null,
    isMultiline: Boolean = false,
) {
    CCListItem(
        headlineText = item.itemDisplay(),
        headlineIsMultiline = isMultiline,
        icon = item.enumType()?.icon,
        iconDescription = item.enumType()?.contentDescription,
        baseColor = baseColor,
        overlineText = item.itemType(),
        trailingContent = {
            val showPrimary = primaryAction != null && primaryAction.showAction(item)
            val showSecondary = secondaryAction != null && secondaryAction.showAction(item)

            if (showSecondary) {
                CCIconButton(
                    icon = secondaryAction.icon,
                    contentDescription = secondaryAction.contentDescription,
                    onClick = { secondaryAction.action(item) },
                    color = baseColor.text()
                )
            }

            if (showPrimary && showSecondary)
                Spacer(Modifier.width(8.dp))

            if (showPrimary)
                CCIconButton(
                    icon = primaryAction.icon,
                    contentDescription = primaryAction.contentDescription,
                    onClick = { primaryAction.action(item) },
                    color = baseColor
                )

        }
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFC8F8F8,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ContactFieldsCardPreview() {
    val phoneData = ContactFieldData(
        title = R.string.snackbar_contact_moved,
        icon = Icons.Outlined.LooksTwo,
        items = listOf(
            Phone(
                value = "+123-456-7890",
                type = "HOME",
                region = "US"
            ),
            Phone(
                value = "+13987654321",
                type = "WORK",
                region = "US"
            )
        ),
        primaryAction = ActionItem(
            icon = Icons.Outlined.Call,
            contentDescription = R.string.detail_action_call,
            action = { _ -> /* Call action */ },
            showAction = { _ -> true }
        ),
    )

    val emailData = ContactFieldData(
        title = R.string.common_qr_code,
        icon = Icons.Outlined.Propane,
        items = listOf(
            Email(
                value = "john@example.com",
                type = "HOME"
            ),
            Email(
                value = "jane@example.com",
                type = "WORK"
            )
        ),
        primaryAction = ActionItem(
            icon = Icons.Outlined.Email,
            contentDescription = R.string.detail_action_email,
            action = { _ -> /* Call action */ },
            showAction = { _ -> true }
        ),
    )

    CorvidContactsTheme {
        ContactFieldsCard(
            title = R.string.detail_section_contact,
            contactData = listOf(
                phoneData,
                emailData
            )
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFC8F8F8
)
@Composable
private fun ContactValuesListPreview() {
    val data = ContactFieldData(
        title = R.string.snackbar_contact_moved,
        icon = Icons.Outlined.LooksTwo,
        items = listOf(
            Phone(
                value = "+123-456-7890",
                type = "HOME",
                region = "US"
            ),
            Phone(
                value = "+13987654321",
                type = "WORK",
                region = "US"
            )
        ),
        primaryAction = ActionItem(
            icon = Icons.Outlined.Call,
            contentDescription = R.string.detail_action_call,
            action = { _ -> /* Call action */ },
            showAction = { _ -> true }
        ),
    )

    CorvidContactsTheme {
        ContactValuesList(
            data = data,
            baseColor = ContactColors.palette[7],
            icon = Icons.Outlined.Phone,
        )
    }
}

@Preview
@Composable
fun ContactValuesListItemPreview() {
    CorvidContactsTheme {
        val color: Color = ContactColors.palette[0]

        val item = Phone(
            type = "HOME",
            value = "+17735125343",
        )

        val primaryAction = ActionItem<Phone>(
            action = {},
            contentDescription = R.string.common_website,
            icon = Icons.Rounded.Sms,
        )

        val secondaryAction = ActionItem<Phone>(
            action = {},
            contentDescription = R.string.list_group_all,
            icon = Icons.Rounded.Phone,
        )

        ContactValuesListItem(
            item = item,
            baseColor = color,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
        )
    }
}
