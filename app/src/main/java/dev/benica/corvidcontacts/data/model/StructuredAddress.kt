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

    /**
     * Returns a single-line string for simplified searching/legacy support.
     */
    fun toSingleLine(): String {
        return listOfNotNull(
            street,
            city,
            state,
            postalCode,
            country
        ).joinToString(", ")
    }

    override fun itemDisplay(): String {
        val line1 = listOfNotNull(
            poBox,
            extended,
            street
        )
            .joinToString(", ")
            .ifBlank { null }
        val line2 = listOfNotNull(
            city,
            state,
            postalCode
        )
            .joinToString(", ")
            .ifBlank { null }
        val line3 = country?.ifBlank { null }
        return listOfNotNull(
            line1,
            line2,
            line3
        ).joinToString("\n")
    }
}
