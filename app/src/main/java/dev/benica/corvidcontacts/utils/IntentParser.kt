// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.utils

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.IntentCompat
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.repository.VCardMapper
import ezvcard.Ezvcard
import java.util.UUID
import dev.benica.corvidcontacts.data.model.Email as ModelEmail

/**
 * Utility for parsing incoming Intents that contain contact information.
 * Handles sharing vCards, "Insert Contact" intents, and plain text shares.
 */
object IntentParser {
    private const val TAG = "IntentParser"

    fun parse(
        intent: Intent?,
        contentResolver: ContentResolver,
        vCardMapper: VCardMapper,
    ): ContactEntity? {
        if (intent == null) return null

        return when (intent.action) {
            Intent.ACTION_INSERT -> parseInsertIntent(intent)
            Intent.ACTION_VIEW, Intent.ACTION_EDIT, Intent.ACTION_SEND -> parseViewOrSendIntent(
                intent,
                contentResolver,
                vCardMapper
            )

            else -> null
        }
    }

    private fun parseInsertIntent(intent: Intent): ContactEntity {
        val name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME) ?: ""
        val phone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE)
        val email = intent.getStringExtra(ContactsContract.Intents.Insert.EMAIL)
        val company = intent.getStringExtra(ContactsContract.Intents.Insert.COMPANY)
        val jobTitle = intent.getStringExtra(ContactsContract.Intents.Insert.JOB_TITLE)

        val firstName = name
            .split(" ")
            .firstOrNull()
        val lastName = if (name.contains(" ")) name
            .split(" ")
            .lastOrNull() else null
        val calculatedFn = listOfNotNull(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return ContactEntity(
            id = UUID
                .randomUUID()
                .toString(),
            displayName = calculatedFn,
            firstName = firstName,
            lastName = lastName,
            emails = email?.let {
                listOf(
                    ModelEmail(
                        it,
                        "HOME"
                    )
                )
            } ?: emptyList(),
            phones = phone?.let {
                listOf(
                    Phone(
                        it,
                        "CELL"
                    )
                )
            } ?: emptyList(),
            photoUrl = null,
            etag = null,
            company = company,
            jobTitle = jobTitle
        )
    }

    private fun parseViewOrSendIntent(
        intent: Intent,
        contentResolver: ContentResolver,
        vCardMapper: VCardMapper,
    ): ContactEntity? {
        val uri = if (intent.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            )
        } else {
            intent.data
        }

        if (uri == null && intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return parsePlainTextShare(intent)
        }

        return uri?.let {
            parseVCardFromUri(
                it,
                contentResolver,
                vCardMapper
            )
        }
    }

    private fun parsePlainTextShare(intent: Intent): ContactEntity? {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        if (sharedText.isBlank()) return null

        var cleanedText = sharedText.trim()
        var detectedUrl: String? = null

        // 1. Extract trailing URL
        val urlRegex = Regex(
            """(https?://\S+)$""",
            RegexOption.IGNORE_CASE
        )
        val urlMatch = urlRegex.find(cleanedText)
        if (urlMatch != null) {
            val textBeforeUrl = cleanedText
                .removeRange(urlMatch.range)
                .trim()
            if (textBeforeUrl.isNotEmpty()) {
                cleanedText = textBeforeUrl
            } else {
                detectedUrl = urlMatch.value
                cleanedText = ""
            }
        }

        // 2. Strip surrounding quotes
        if (cleanedText.startsWith("\"") && cleanedText.endsWith("\"") && cleanedText.length > 2) {
            cleanedText = cleanedText
                .substring(
                    1,
                    cleanedText.length - 1
                )
                .trim()
        }

        val isPhoneNumber = cleanedText.matches(Regex("""^[+]?[0-9\s\-()]{7,15}$"""))
        val isEmail =
            cleanedText.contains("@") && cleanedText.contains(".") && cleanedText.length > 5
        val isAddress = cleanedText.matches(Regex("""^\d+\s+[A-Za-z0-9\s.,#/-]+$"""))

        return ContactEntity(
            id = UUID
                .randomUUID()
                .toString(),
            displayName = if (!isPhoneNumber && !isEmail && !isAddress && cleanedText.length < 40) cleanedText else "",
            firstName = null,
            lastName = null,
            emails = if (isEmail) listOf(
                ModelEmail(
                    cleanedText,
                    "HOME"
                )
            ) else emptyList(),
            phones = if (isPhoneNumber) listOf(
                Phone(
                    cleanedText,
                    "CELL"
                )
            ) else emptyList(),
            photoUrl = null,
            etag = null,
            company = null,
            jobTitle = null,
            notes = if (!isPhoneNumber && !isEmail && !isAddress && cleanedText.length >= 40) cleanedText else null,
            websites = detectedUrl?.let { listOf(it) } ?: emptyList()
        )
    }

    private fun parseVCardFromUri(
        uri: Uri,
        contentResolver: ContentResolver,
        vCardMapper: VCardMapper,
    ): ContactEntity? {
        return try {
            contentResolver
                .openInputStream(uri)
                ?.use { inputStream ->
                    Ezvcard
                        .parse(inputStream)
                        .all()
                        .firstOrNull()
                        ?.let { vcard ->
                            vCardMapper.mapVCardToEntity(
                                vcard = vcard,
                                bookHref = "",
                                contactHref = "",
                                etag = null,
                            )
                        }
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to parse vCard from intent",
                e
            )
            null
        }
    }

}
