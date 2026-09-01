// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class Phone(
    val value: String,
    override val type: String?,
    val region: String? = null,
) : TypedValue() {
    override fun itemDisplay(): String = value
}
