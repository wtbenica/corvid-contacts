// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Fax
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Facebook
import compose.icons.simpleicons.Instagram
import compose.icons.simpleicons.Linkedin
import compose.icons.simpleicons.Mastodon
import compose.icons.simpleicons.Signal
import compose.icons.simpleicons.Telegram
import compose.icons.simpleicons.Twitter
import compose.icons.simpleicons.Whatsapp
import dev.benica.corvidcontacts.R
import java.util.Locale

/**
 * Represents classifications for vcard fields.
 * @param id the label as it appears in the vCard
 * @param labelRes the string resource for the label
 * @param icon the icon associated with the label (optional)
 * @param contentDescription the content description for the icon
 */
enum class VCardType(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector?,
    @StringRes val contentDescription: Int,
) {
    // Common Types (Phone, Email, Address)
    HOME(
        id = "HOME",
        labelRes = R.string.common_home,
        icon = Icons.Outlined.Home,
        contentDescription = R.string.common_home
    ),
    WORK(
        id = "WORK",
        labelRes = R.string.common_work,
        icon = Icons.Outlined.Business,
        contentDescription = R.string.common_work
    ),
    SCHOOL(
        id = "SCHOOL",
        labelRes = R.string.vcard_type_school,
        icon = Icons.Outlined.School,
        contentDescription = R.string.vcard_type_school
    ),

    // Phone Types
    CELL(
        id = "CELL",
        labelRes = R.string.vcard_type_cell,
        icon = Icons.Outlined.PhoneAndroid,
        contentDescription = R.string.vcard_type_cell
    ),
    MOBILE(
        id = "MOBILE",
        labelRes = R.string.vcard_type_mobile,
        icon = Icons.Outlined.PhoneAndroid,
        contentDescription = R.string.vcard_type_mobile
    ),
    MAIN(
        id = "MAIN",
        labelRes = R.string.vcard_type_main,
        icon = Icons.Outlined.ContactPhone,
        contentDescription = R.string.vcard_type_main
    ),
    FAX(
        id = "FAX",
        labelRes = R.string.vcard_type_fax,
        icon = Icons.Outlined.Fax,
        contentDescription = R.string.vcard_type_fax
    ),

    // Relationship Types
    PARENT(
        id = "PARENT",
        labelRes = R.string.vcard_type_parent,
        icon = Icons.Outlined.Person,
        contentDescription = R.string.vcard_type_parent
    ),
    GRANDPARENT(
        id = "GRANDPARENT",
        labelRes = R.string.vcard_type_grandparent,
        icon = Icons.Outlined.Person,
        contentDescription = R.string.vcard_type_grandparent
    ),
    CHILD(
        id = "CHILD",
        labelRes = R.string.vcard_type_child,
        icon = Icons.Outlined.Person,
        contentDescription = R.string.vcard_type_child
    ),
    STUDENT(
        id = "STUDENT",
        labelRes = R.string.vcard_type_student,
        icon = Icons.Outlined.School,
        contentDescription = R.string.vcard_type_student
    ),
    SPOUSE(
        id = "SPOUSE",
        labelRes = R.string.vcard_type_spouse,
        icon = Icons.Outlined.Person,
        contentDescription = R.string.vcard_type_spouse
    ),
    FRIEND(
        id = "FRIEND",
        labelRes = R.string.vcard_type_friend,
        icon = Icons.Outlined.Person,
        contentDescription = R.string.vcard_type_friend
    ),
    TEACHER(
        id = "TEACHER",
        labelRes = R.string.vcard_type_teacher,
        icon = Icons.Outlined.School,
        contentDescription = R.string.vcard_type_teacher
    ),
    ASSISTANT(
        id = "ASSISTANT",
        labelRes = R.string.vcard_type_assistant,
        icon = Icons.Outlined.Business,
        contentDescription = R.string.vcard_type_assistant
    ),
    MANAGER(
        id = "MANAGER",
        labelRes = R.string.vcard_type_manager,
        icon = Icons.Outlined.Business,
        contentDescription = R.string.vcard_type_manager
    ),

    // Social Types
    MASTODON(
        id = "MASTODON",
        labelRes = R.string.vcard_type_mastodon,
        icon = SimpleIcons.Mastodon,
        contentDescription = R.string.vcard_type_mastodon
    ),
    SIGNAL(
        id = "SIGNAL",
        R.string.vcard_type_signal,
        icon = SimpleIcons.Signal,
        contentDescription = R.string.vcard_type_signal
    ),
    TWITTER(
        id = "TWITTER",
        labelRes = R.string.vcard_type_twitter,
        icon = SimpleIcons.Twitter,
        contentDescription = R.string.vcard_type_twitter
    ),
    LINKEDIN(
        id = "LINKEDIN",
        labelRes = R.string.vcard_type_linkedin,
        icon = SimpleIcons.Linkedin,
        contentDescription = R.string.vcard_type_linkedin
    ),
    WHATSAPP(
        id = "WHATSAPP",
        labelRes = R.string.vcard_type_whatsapp,
        icon = SimpleIcons.Whatsapp,
        contentDescription = R.string.vcard_type_whatsapp
    ),
    TELEGRAM(
        id = "TELEGRAM",
        labelRes = R.string.vcard_type_telegram,
        icon = SimpleIcons.Telegram,
        contentDescription = R.string.vcard_type_telegram
    ),
    FACEBOOK(
        id = "FACEBOOK",
        labelRes = R.string.vcard_type_facebook,
        icon = SimpleIcons.Facebook,
        contentDescription = R.string.vcard_type_facebook
    ),
    INSTAGRAM(
        id = "INSTAGRAM",
        labelRes = R.string.vcard_type_instagram,
        icon = SimpleIcons.Instagram,
        contentDescription = R.string.vcard_type_instagram
    ),
    WEBSITE(
        id = "WEBSITE",
        labelRes = R.string.vcard_type_websites,
        icon = Icons.Outlined.Language,
        contentDescription = R.string.vcard_type_websites
    ),

    // Other Types
    OTHER(
        id = "OTHER",
        labelRes = R.string.common_other,
        icon = Icons.Outlined.MoreHoriz,
        contentDescription = R.string.common_other
    ),
    PREF(
        id = "PREF",
        labelRes = R.string.vcard_type_pref,
        icon = Icons.Outlined.Star,
        contentDescription = R.string.vcard_type_pref
    ),
    INTERNET(
        id = "INTERNET",
        labelRes = R.string.vcard_type_internet,
        icon = Icons.Outlined.Language,
        contentDescription = R.string.vcard_type_internet
    );

    @Composable
    fun getDisplayName(): String = stringResource(labelRes)

    companion object {
        val commonPhoneTypes = listOf(
            MAIN,
            CELL,
            HOME,
            WORK,
            MOBILE,
            FAX,
            OTHER
        )
        val commonEmailTypes = listOf(
            HOME,
            WORK,
            SCHOOL,
            MOBILE,
            OTHER
        )
        val commonAddressTypes = listOf(
            HOME,
            WORK,
            OTHER
        )
        val commonSocialTypes = listOf(
            WEBSITE,
            MASTODON,
            SIGNAL,
            TWITTER,
            LINKEDIN,
            WHATSAPP,
            TELEGRAM,
            FACEBOOK,
            INSTAGRAM,
            OTHER
        )
        val commonRelationshipTypes = listOf(
            PARENT,
            GRANDPARENT,
            CHILD,
            SPOUSE,
            FRIEND,
            STUDENT,
            TEACHER,
            ASSISTANT,
            MANAGER,
            OTHER
        )

        fun getTypeInfo(id: String?): VCardType? = entries.find {
            it.id.equals(
                id,
                ignoreCase = true
            )
        }

        @Composable
        fun getLabel(id: String?): String =
            getTypeInfo(id)?.labelRes?.let { stringResource(it) } ?: getDisplayName(id)

        /**
         * Formats a string to be used as a label for a vCard type.
         */
        private fun getDisplayName(id: String?): String {
            if (id == null) return ""
            return id
                .lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}

sealed class TypedValue {
    abstract val type: String?

    fun enumType(): VCardType? = VCardType.getTypeInfo(type)

    @Composable
    fun itemType(): String = VCardType.getLabel(type)

    abstract fun itemDisplay(): String
}

data class MiscellaneousValue(
    override val type: String? = null,
    val value: String,
) : TypedValue() {
    override fun itemDisplay(): String = value
}
