// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.navigation

import androidx.navigation3.runtime.NavKey
import dev.benica.corvidcontacts.data.local.ContactEntity
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Resolving : Destination

    @Serializable
    data object Welcome : Destination

    @Serializable
    data class Login(val prefilledUrl: String? = null) : Destination

    @Serializable
    data object ContactList : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    data class ContactDetail(val contactId: String) : Destination

    @Serializable
    data class ShareSelection(
        val contactId: String,
        val isQr: Boolean = false,
    ) : Destination

    @Serializable
    data class QrDisplay(
        val vcard: String,
        val contactId: String,
    ) : Destination

    @Serializable
    data class ContactEdit(
        val contactId: String? = null,
        val initialContact: ContactEntity? = null,
        val initialAddressBookHref: String? = null,
        val markAsSelfOnSave: Boolean = false,
    ) : Destination

    @Serializable
    data class MergeReview(
        val survivorContactId: String,
        val absorbedContactId: String,
    ) : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object About : Destination

    @Serializable
    data object Licenses : Destination
}
