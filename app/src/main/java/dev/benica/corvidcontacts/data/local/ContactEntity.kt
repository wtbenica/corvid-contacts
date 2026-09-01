// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import kotlinx.serialization.Serializable

typealias ContactId = String

/**
 * Represents a contact stored in the local database.
 *
 * @property id The unique identifier for the contact.
 * @property displayName The manually set or primary display name for the contact.
 * @property firstName The contact's first name.
 * @property lastName The contact's last name.
 * @property middleName The contact's middle name.
 * @property prefix The name prefix, such as "Mr." or "Dr."
 * @property suffix The name suffix, such as "Jr." or "PhD"
 * @property emails A list of email objects.
 * @property phones A list of phone number objects.
 * @property photoUrl The URL for the contact's profile photo.
 * @property hasPhoto Indicates if a photo is available for the contact (used to avoid loading photoUrl in lists).
 * @property etag The synchronization entity tag for cache validation.
 * @property addressBookHref The server-side path to the address book containing this contact.
 * @property contactHref The server-side path to this specific contact resource.
 * @property colorInt An integer representing the color assigned to the contact.
 * @property isArchived Indicates whether the contact is currently archived.
 * @property categories A list of categories or tags.
 * @property company The organization or company associated with the contact.
 * @property jobTitle The contact's job title within their company.
 * @property birthday The contact's date of birth.
 * @property nickname An informal name for the contact.
 * @property notes General notes or comments regarding the contact.
 * @property websites A list of website URLs.
 * @property socialProfiles A list of social media profiles.
 * @property relationships A list of various relationships.
 * @property structuredAddresses A list of structured addresses.
 */
@Serializable
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: ContactId,
    val displayName: String,

    // Name components
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,

    // Contact communication details
    val emails: List<Email>? = emptyList(),
    val phones: List<Phone>? = emptyList(),
    val photoUrl: String? = null,
    val hasPhoto: Boolean = false,

    // Synchronization and state
    val etag: String? = null,
    val addressBookHref: String? = null,
    val contactHref: String? = null,
    val colorInt: Int? = null,
    val isArchived: Boolean = false,

    // Categorization and professional info
    val categories: List<String>? = emptyList(),
    val company: String? = null,
    val jobTitle: String? = null,
    val birthday: String? = null,
    val nickname: String? = null,
    val notes: String? = null,

    // Extended information
    val websites: List<String>? = emptyList(),
    val socialProfiles: List<SocialProfile>? = emptyList(),
    val relationships: List<Relationship>? = emptyList(),
    val structuredAddresses: List<StructuredAddress>? = emptyList(),
) {
    /**
     * Returns the effective display name for this contact.
     * Uses the manual [displayName] override if present, otherwise constructs it from components.
     */
    fun getEffectiveDisplayName(): String {
        if (displayName.isNotBlank()) return displayName

        return getSimpleName() ?: "Unknown"
    }

    /**
     * Simple name for UI display: firstName + lastName only (no prefix, middle, or suffix).
     * This is what auto-generates when displayName is left blank.
     */
    private fun getSimpleName(): String? {
        val components = listOfNotNull(
            firstName, lastName
        ).filter { it.isNotBlank() }

        return if (components.isNotEmpty()) {
            components.joinToString(" ")
        } else null
    }

    /**
     * Complete formal name including middle name and suffix (but not prefix).
     * Used for vCard export, sharing, and formal display contexts.
     */
    fun getFullName(): String? {
        val components = listOfNotNull(
            prefix, firstName, middleName, lastName, suffix
        ).filter { it.isNotBlank() }

        return if (components.isNotEmpty()) {
            components.joinToString(" ")
        } else null
    }

    /**
     * Get initials for avatar display, using firstName and lastName only (not prefix/middle/suffix).
     * Returns 1-2 uppercase letters.
     */
    fun getInitials(): String {
        val firstInitial = firstName?.firstOrNull()?.uppercase() ?: ""
        val lastInitial = lastName?.firstOrNull()?.uppercase() ?: ""

        return when {
            firstInitial.isNotEmpty() && lastInitial.isNotEmpty() -> "$firstInitial$lastInitial"
            firstInitial.isNotEmpty() -> firstInitial
            lastInitial.isNotEmpty() -> lastInitial
            displayName.isNotBlank() -> displayName.trim().firstOrNull()?.uppercase() ?: ""

            else -> ""
        }
    }

    /**
     * Whether this contact can be mutated (archived, deleted, merged) by the user - `false` for
     * contacts living in server-generated/app-generated/system/account address books, which the
     * server manages and the app shouldn't touch.
     */
    fun isEditable(): Boolean {
        val href = addressBookHref ?: ""
        return !href.contains(
            "server-generated", ignoreCase = true
        ) && !href.contains(
            "app-generated", ignoreCase = true
        ) && !href.contains(
            "system", ignoreCase = true
        ) && !href.contains(
            "account", ignoreCase = true
        )
    }
}
