// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class SocialProfile(
    val value: String, // URL or Username
    override val type: String?,  // Service name (e.g., "Twitter", "Mastodon")
) : TypedValue() {
    override fun itemDisplay(): String {
        return when (type?.uppercase()) {
            "TWITTER", "X", "MASTODON" -> {
                if (value.startsWith("@") || value.startsWith("http")) value
                else "@$value"
            }

            else -> value
        }
    }

    fun getDeepLink(): String {
        val cleanValue = value
            .removePrefix("@")
            .substringAfterLast("/")
        return when (type?.uppercase()) {
            "TWITTER", "X" -> "twitter://user?screen_name=$cleanValue"
            "MASTODON" -> {
                if (value.startsWith("http")) value
                else {
                    // Mastodon values are often user@instance.com
                    val parts = value
                        .removePrefix("@")
                        .split("@")
                    if (parts.size == 2) "https://${parts[1]}/@${parts[0]}"
                    else "https://mastodon.social/@$cleanValue" // Default instance
                }
            }

            "FACEBOOK" -> "fb://profile/$cleanValue"
            "INSTAGRAM" -> "instagram://user?username=$cleanValue"
            "LINKEDIN" -> "linkedin://profile/$cleanValue"
            "SIGNAL" -> "https://signal.me/#p/$value"
            "WHATSAPP" -> "whatsapp://send?phone=$value"
            "TELEGRAM" -> "tg://resolve?domain=$cleanValue"
            else -> {
                if (value.startsWith("http")) value
                else "https://$value"
            }
        }
    }

    fun getWebFallback(): String {
        val cleanValue = value
            .removePrefix("@")
            .substringAfterLast("/")
        return when (type?.uppercase()) {
            "TWITTER", "X" -> "https://twitter.com/$cleanValue"
            "MASTODON" -> {
                if (value.startsWith("http")) value
                else {
                    val parts = value
                        .removePrefix("@")
                        .split("@")
                    if (parts.size == 2) "https://${parts[1]}/@${parts[0]}"
                    else "https://mastodon.social/@$cleanValue"
                }
            }

            "FACEBOOK" -> "https://facebook.com/$cleanValue"
            "INSTAGRAM" -> "https://instagram.com/$cleanValue"
            "LINKEDIN" -> "https://linkedin.com/in/$cleanValue"
            "SIGNAL" -> "https://signal.me/#p/$value"
            "WHATSAPP" -> "https://wa.me/$value"
            "TELEGRAM" -> "https://t.me/$cleanValue"
            else -> {
                if (value.startsWith("http")) value
                else "https://$value"
            }
        }
    }
}
