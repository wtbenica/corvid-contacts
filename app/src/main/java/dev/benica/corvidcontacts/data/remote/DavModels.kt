// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

data class DavAddressBook(
    val href: String,
    val displayName: String?,
    val color: String? = null,
)

data class DavPrincipal(
    val href: String?,
    val addressbookHomeSetHref: String?,
)

data class DavContact(
    val href: String,
    val etag: String?,
    val vcardData: String?,
)
