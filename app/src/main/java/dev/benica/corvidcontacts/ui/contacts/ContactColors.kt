// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.extensions.oklch
import dev.benica.corvidcontacts.ui.contacts.ContactColors.guessIconNameForAddressBook
import dev.benica.corvidcontacts.ui.contacts.ContactColors.iconPalette
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults
import kotlin.math.abs

object ContactColors {

    // Vibrant coordinated palette: Green, Orange, Yellow, Cyanish-Blue
    val palette = listOf(
        Color(0xFFD81B60),
        Color(0xFFAB47BC),
        Color(0xFF5C6BC0),
        Color(0xFF00ACC1),
        Color(0xFF7CB342),
        Color(0xFFAFB42B),
        Color(0xFFFB8C00),
        Color(0xFFE53935),
    )

    val iconPalette: Map<String, ImageVector> = mapOf(
        "Person" to Icons.Rounded.Person,
        "Work" to Icons.Rounded.Work,
        "Group" to Icons.Rounded.Group,
        "School" to Icons.Rounded.School,
        "Book" to Icons.Rounded.Book
    )

    /**
     * Maps the persisted icon identifiers used in [iconPalette] to translatable string resources,
     * for use as content descriptions/labels wherever the icon is presented to the user.
     */
    val iconPaletteLabels: Map<String, Int> = mapOf(
        "Person" to R.string.contact_icon_person,
        "Work" to R.string.contact_icon_work,
        "Group" to R.string.contact_icon_group,
        "School" to R.string.contact_icon_school,
        "Book" to R.string.contact_icon_book
    )

    /**
     * Returns contact's custom color, if set, otherwise returns the address book's color
     * If the book is missing for some reason, returns a fallback color based on the contact's href.
     */
    fun resolveContactColor(
        contactWithAddressBook: ContactWithAddressBook?,
    ): Color {
        if (contactWithAddressBook == null) return palette.first()

        val contact = contactWithAddressBook.contact
        contact.colorInt?.let { return Color(it) }

        val book = contactWithAddressBook.addressBook
        book?.colorInt?.let { return Color(it) }

        return getFallbackColorForAddressBook(contact.addressBookHref ?: "")
    }

    /**
     * Returns a color for the given address book: if [customColor] is provided, that is used,
     * otherwise falls back to a deterministic color based on the address book's href.
     * @param customColor A custom color to use instead of the default.
     * @param href The href of the address book.
     */
    fun getAddressBookColorWithFallback(
        customColor: Int?,
        href: String,
    ): Color {
        return if (customColor != null) {
            Color(customColor)
        } else {
            getFallbackColorForAddressBook(href)
        }
    }

    /**
     * Looks up the address book with [href] in [addressBooks] and returns its color (or a
     * deterministic fallback if it has none or isn't found). Shared by [dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen] (for
     * the current book's swatch/fallback while editing) and the wide-screen shell (to tint the
     * edit pane before [dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen] itself has mounted and reported a color).
     */
    fun resolveAddressBookColor(
        addressBooks: List<AddressBookEntity>,
        href: String?,
    ): Color {
        val book = addressBooks.find { it.href == href }
        return getAddressBookColorWithFallback(customColor = book?.colorInt, href = href ?: "")
    }

    /**
     * The default color [dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen] opens with, before any in-session color pick: the
     * contact's own explicit color if set, otherwise the color of whichever address book the edit
     * targets - the contact's own book if editing an existing contact, otherwise
     * [initialAddressBookHref] (the book a new contact will be saved into), falling back to the
     * first available book. Used to tint the wide-screen shell's edit pane synchronously, since
     * [dev.benica.corvidcontacts.ui.contacts.contact_edit.ContactEditScreen]'s own computed color only becomes available a frame after it mounts.
     */
    fun resolveEditDefaultColor(
        contactWithBook: ContactWithAddressBook?,
        addressBooks: List<AddressBookEntity>,
        initialAddressBookHref: String?,
    ): Color {
        contactWithBook?.contact?.colorInt?.let { return Color(it) }

        val effectiveHref = contactWithBook?.contact?.addressBookHref
            ?: initialAddressBookHref
            ?: addressBooks.firstOrNull()?.href

        return resolveAddressBookColor(addressBooks, effectiveHref)
    }

    /**
     * Returns a deterministic fallback color for an address book based on its [href], used when no
     * explicit color has been assigned. This ensures that contacts from the same (but unknown)
     * address book are visually grouped with the same color, and that the color is consistent with
     * the app's OKLCH-based design system.
     *
     * @param href The href of the address book.
     */
    private fun getFallbackColorForAddressBook(
        href: String,
    ): Color {
        val hue = abs(href.hashCode() % 360).toFloat()
        return oklch(
            lightness = HueSliderDefaults.LIGHTNESS,
            chroma = HueSliderDefaults.CHROMA,
            hueDegrees = hue
        )
    }

    /**
     * For common address book names, guesses a matching icon key from [iconPalette]:
     *      personal, private  -> "Person"
     *      work, job, office -> "Work"
     *      family, home, shared -> "Group"
     *      school, class, college -> "School"
     *      anything else -> "Book"
     * @param displayName The name of the address book.
     */
    fun guessIconNameForAddressBook(displayName: String?): String {
        val name = displayName?.lowercase() ?: ""
        return when {
            name.contains("personal") || name.contains("private") -> "Person"
            name.contains("work") || name.contains("job") || name.contains("office") -> "Work"
            name.contains("family") || name.contains("home") || name.contains("shared") -> "Group"
            name.contains("school") || name.contains("class") || name.contains("college") -> "School"
            else -> "Book"
        }
    }

    /**
     * If iconName is provided, returns the matching icon; otherwise falls back to a
     * guess based on displayName (see [guessIconNameForAddressBook]).
     * @param displayName The name of the address book.
     * @param iconName An optional icon name to use instead of the guess.
     */
    fun getIconForAddressBook(
        displayName: String?,
        iconName: String? = null,
    ): ImageVector {
        val name = iconName ?: guessIconNameForAddressBook(displayName)
        return iconPalette[name] ?: Icons.Rounded.Book
    }
}

