// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

sealed interface Address

/**
 * Represents an unstructured address.
 * @param type The type of the address.
 * @param value The value of the address.
 */
@Serializable
@JsonClass(generateAdapter = true)
data class LegacyAddress(
    override val type: String? = null,
    val value: String,
) : Address, TypedValue() {
    override fun itemDisplay(): String = value
}

/**
 * Represents a structured address.
 * @param type The type of the address.
 * @param street The street address.
 * @param city The city.
 * @param state The state.
 * @param postalCode The postal code.
 * @param country The country.
 * @param poBox The PO Box.
 * @param extended The extended address.
 */
@Serializable
@JsonClass(generateAdapter = true)
data class StructuredAddress(
    override val type: String? = "HOME",
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val poBox: String? = null,
    val extended: String? = null,
) : Address, TypedValue() {
    /**
     * Returns true if all address fields (except type) are null or blank.
     */
    fun isBlank(): Boolean {
        return street.isNullOrBlank() &&
                city.isNullOrBlank() &&
                state.isNullOrBlank() &&
                postalCode.isNullOrBlank() &&
                country.isNullOrBlank() &&
                poBox.isNullOrBlank() &&
                extended.isNullOrBlank()
    }

    /** Whether the whole address is in the street field, as when another app saved it as one line. */
    fun isStreetOnly(): Boolean = !street.isNullOrBlank() &&
            city.isNullOrBlank() &&
            state.isNullOrBlank() &&
            postalCode.isNullOrBlank() &&
            country.isNullOrBlank() &&
            poBox.isNullOrBlank() &&
            extended.isNullOrBlank()

    /** This address with blank parts as `null`, so a cleared field doesn't leave an empty one behind. */
    fun cleaned(): StructuredAddress = copy(
        street = street.cleaned(),
        city = city.cleaned(),
        state = state.cleaned(),
        postalCode = postalCode.cleaned(),
        country = country.cleaned(),
        poBox = poBox.cleaned(),
        extended = extended.cleaned(),
    )

    /**
     * Returns a single-line string for simplified searching/legacy support.
     */
    fun toSingleLine(): String = joinParts(", ", street, city, state, postalCode, country).orEmpty()

    override fun itemDisplay(): String = listOfNotNull(
        joinParts(", ", poBox, extended, street),
        joinParts(", ", city, state, postalCode),
        country?.trim()?.ifBlank { null },
    ).joinToString("\n")

    /** The parts that aren't blank, joined, or `null` if there are none; blank ones add no separator. */
    private fun joinParts(separator: String, vararg parts: String?): String? =
        parts.filter { !it.isNullOrBlank() }.joinToString(separator) { it!!.trim() }.ifBlank { null }

    private fun String?.cleaned(): String? = this?.trim()?.ifBlank { null }
}
