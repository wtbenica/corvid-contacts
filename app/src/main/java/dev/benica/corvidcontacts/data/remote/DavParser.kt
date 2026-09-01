// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Utility for parsing XML responses from CardDAV servers.
 * It uses [XmlPullParser] to extract principal info, address book lists, and contact data.
 */
object DavParser {
    private val factory = XmlPullParserFactory
        .newInstance()
        .apply {
            isNamespaceAware = true
        }

    /**
     * Parses the current user principal or addressbook home set HREFs from a PROPFIND response.
     */
    fun parsePrincipal(xml: String): DavPrincipal? {
        val parser = createParser(xml)

        var eventType = parser.eventType
        var responseHref: String? = null
        var principalPropertyHref: String? = null
        var homeSetHref: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "response" -> responseHref = null
                    "href" -> if (responseHref == null) responseHref = parser.nextText()
                    "current-user-principal" -> principalPropertyHref = parseHrefInside(parser)
                    "addressbook-home-set" -> homeSetHref = parseHrefInside(parser)
                }
            }
            eventType = parser.next()
        }

        val finalPrincipalHref = principalPropertyHref ?: responseHref
        return if (finalPrincipalHref != null || homeSetHref != null) {
            DavPrincipal(
                finalPrincipalHref,
                homeSetHref
            )
        } else null
    }

    /**
     * Extracts HREFs nested inside property tags like <current-user-principal><href>...</href></current-user-principal>.
     */
    private fun parseHrefInside(parser: XmlPullParser): String? {
        val initialDepth = parser.depth
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT && parser.depth > initialDepth) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "href") {
                return parser.nextText()
            }
            eventType = parser.next()
        }
        return null
    }

    /**
     * Parses the list of address books available on the server.
     */
    fun parseAddressBooks(xml: String): List<DavAddressBook> {
        val addressBooks = mutableListOf<DavAddressBook>()
        val parser = createParser(xml)

        var eventType = parser.eventType
        var currentHref: String? = null
        var currentDisplayName: String? = null
        var currentColor: String? = null
        var isAddressBook = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "response" -> {
                        currentHref = null
                        currentDisplayName = null
                        currentColor = null
                        isAddressBook = false
                    }

                    "href" -> if (currentHref == null) currentHref = parser.nextText()
                    "displayname" -> currentDisplayName = parser.nextText()
                    "addressbook" -> isAddressBook = true
                    "addressbook-color" -> currentColor = parser.nextText()
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "response") {
                if (isAddressBook && currentHref != null) {
                    addressBooks.add(
                        DavAddressBook(
                            currentHref,
                            currentDisplayName,
                            currentColor
                        )
                    )
                }
            }
            eventType = parser.next()
        }
        return addressBooks
    }

    /**
     * Parses raw contact data (vCards) from an addressbook report response.
     * Includes logic to intelligently un-indent vCards that may have been pretty-printed by the server.
     */
    fun parseContacts(xml: String): List<DavContact> {
        val contacts = mutableListOf<DavContact>()
        val parser = createParser(xml)

        var eventType = parser.eventType
        var currentHref: String? = null
        var currentEtag: String? = null
        var currentVCard: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "response" -> {
                        currentHref = null
                        currentEtag = null
                        currentVCard = null
                    }

                    "href" -> if (currentHref == null) currentHref = parser.nextText()
                    "getetag" -> currentEtag = parser.nextText()
                    "address-data" -> currentVCard = cleanRawVCard(parser.nextText())
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "response") {
                if (currentHref != null && currentVCard != null) {
                    contacts.add(
                        DavContact(
                            currentHref,
                            currentEtag,
                            currentVCard
                        )
                    )
                }
            }
            eventType = parser.next()
        }
        return contacts
    }

    private fun createParser(xml: String) = factory
        .newPullParser()
        .apply {
            setInput(StringReader(xml))
        }

    /**
     * vCard 3.0/4.0 uses leading spaces to indicate folded lines. Servers often indent XML data, 
     * which breaks the vCard parser. This method finds the common indentation of the entire block
     * and removes it, while preserving vCard's internal folding spaces.
     */
    private fun cleanRawVCard(rawVCard: String): String {
        val lines = rawVCard.split(Regex("\\r?\\n"))
        val firstLine = lines.find {
            it.contains(
                "BEGIN:VCARD",
                ignoreCase = true
            )
        } ?: return rawVCard

        val indent = firstLine.indexOf(
            "BEGIN:VCARD",
            ignoreCase = true
        )
        if (indent <= 0) return lines.joinToString("\r\n")

        return lines.joinToString("\r\n") {
            if (it.length >= indent) it.substring(indent) else it.trimStart()
        }
    }
}
