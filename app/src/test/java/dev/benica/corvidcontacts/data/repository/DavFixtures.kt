// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

/** Canned CardDAV responses for the repository tests that run against a mock server. */

internal data class AddressBookFixture(
    val href: String,
    val displayName: String,
)

internal fun principalPropfindResponse(principalHref: String): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\">" +
            "<d:response>" +
            "<d:href>/remote.php/dav/</d:href>" +
            "<d:propstat><d:prop>" +
            "<d:current-user-principal><d:href>$principalHref</d:href></d:current-user-principal>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

internal fun homeSetPropfindResponse(
    principalHref: String,
    homeSetHref: String,
): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "<d:response>" +
            "<d:href>$principalHref</d:href>" +
            "<d:propstat><d:prop>" +
            "<card:addressbook-home-set><d:href>$homeSetHref</d:href></card:addressbook-home-set>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

internal fun addressBookListResponse(vararg books: AddressBookFixture): String {
    val responses = books.joinToString("") { book ->
        "<d:response>" +
                "<d:href>${book.href}</d:href>" +
                "<d:propstat><d:prop>" +
                "<d:resourcetype><d:collection/><card:addressbook/></d:resourcetype>" +
                "<d:displayname>${book.displayName}</d:displayname>" +
                "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
                "</d:response>"
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\" " +
            "xmlns:ical=\"http://apple.com/ns/ical/\">" +
            responses +
            "</d:multistatus>"
}

internal fun contactsReportResponse(
    href: String,
    etag: String,
    vcard: String,
): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "<d:response>" +
            "<d:href>$href</d:href>" +
            "<d:propstat><d:prop>" +
            "<d:getetag>$etag</d:getetag>" +
            "<card:address-data>$vcard</card:address-data>" +
            "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>" +
            "</d:response>" +
            "</d:multistatus>"
}

internal fun emptyMultistatusResponse(): String {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">" +
            "</d:multistatus>"
}

// --- In-memory fakes (unlike ContactsRepositoryVCardRoundTripTest's NoOp fakes, these actually
// store data, since these tests need to assert on DAO state after a sync cycle runs) ----------
