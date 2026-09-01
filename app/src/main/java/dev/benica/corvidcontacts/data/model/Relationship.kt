// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class Relationship(
    override val type: String,
    val value: String, // Can be a UID or a Display Name
    val isUid: Boolean = false,
) : TypedValue() {
    override fun itemDisplay(): String = value
}

