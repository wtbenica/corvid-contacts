// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import android.content.Context
import android.util.Log
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

object PhoneFormatter {

    /**
     * Standardizes a number for display using the country's national standard.
     * If [includeCountryCode] is true, it returns the international format (+XX ...).
     * If [significantOnly] is true, it returns the number without trunk prefixes (like the UK '0').
     */
    fun format(
        phone: String,
        includeCountryCode: Boolean,
        context: Context? = null,
        region: String? = null,
        significantOnly: Boolean = false,
    ): String {
        if (context == null || phone.isBlank()) return phone

        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val defaultRegion = region?.uppercase() ?: Locale.getDefault().country.ifBlank { "US" }

        try {
            val numberProto = phoneUtil.parse(
                phone,
                defaultRegion
            )

            if (significantOnly) {
                // To get it formatted (with spaces/dashes), we format as INTERNATIONAL 
                // and then strip the country code prefix.
                val international =
                    phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                val countryCode = "+${numberProto.countryCode}"
                val formattedSignificant = international.removePrefix(countryCode).trim()

                val numberRegion = phoneUtil.getRegionCodeForNumber(numberProto)
                return if (numberRegion == "US" || numberRegion == "CA") {
                    formattedSignificant
                        .replace("(", "")
                        .replace(") ", "-")
                        .replace(")", "-")
                } else {
                    formattedSignificant
                }
            }

            val formatted = if (includeCountryCode) {
                phoneUtil.format(
                    numberProto,
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                )
            } else {
                phoneUtil.format(
                    numberProto,
                    PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                )
            }

            val numberRegion = phoneUtil.getRegionCodeForNumber(numberProto)
            if (numberRegion == "US" || numberRegion == "CA" || (includeCountryCode && formatted.startsWith(
                    "+1"
                ))
            ) {
                return formatted
                    .replace(
                        "(",
                        ""
                    )
                    .replace(
                        ") ",
                        "-"
                    )
                    .replace(
                        ")",
                        "-"
                    )
            }
            return formatted
        } catch (_: Exception) {
            // Not a complete valid number yet, fall through to AsYouTypeFormatter
        }

        // Fallback for partial input
        val ayf = phoneUtil.getAsYouTypeFormatter(defaultRegion)
        val digits = phone.filter { it.isDigit() || it == '+' }
        var ayfResult = ""
        digits.forEach { char ->
            ayfResult = ayf.inputDigit(char)
        }

        if (ayfResult.isBlank()) ayfResult = phone

        val isUsOrCa =
            defaultRegion == "US" || defaultRegion == "CA" || ayfResult.startsWith("+1")
        return if (isUsOrCa) {
            ayfResult
                .replace(
                    "(",
                    ""
                )
                .replace(
                    ") ",
                    "-"
                )
                .replace(
                    ")",
                    "-"
                )
        } else {
            ayfResult
        }
    }

    /**
     * Splits a raw phone number string into a region code (ISO) and the national number.
     * Falls back to the system locale if the number has no country code.
     */
    fun split(
        phone: String,
        context: Context,
    ): Pair<String, String> {
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val defaultRegion = Locale.getDefault().country.ifBlank { "US" }

        return try {
            val numberProto = phoneUtil.parse(
                phone,
                defaultRegion
            )
            val region = phoneUtil.getRegionCodeForNumber(numberProto) ?: defaultRegion
            val national = phoneUtil.format(
                numberProto,
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL
            )

            // Standardize US/CA national part to use dashes
            val standardizedNational = if (region == "US" || region == "CA") {
                national
                    .replace(
                        "(",
                        ""
                    )
                    .replace(
                        ") ",
                        "-"
                    )
                    .replace(
                        ")",
                        "-"
                    )
            } else {
                national
            }

            region to standardizedNational
        } catch (_: Exception) {
            try {
                // Second attempt: clean non-numeric characters and try again
                val digits = phone.filter { it.isDigit() || (it == '+') }
                val numberProto = phoneUtil.parse(
                    digits,
                    defaultRegion
                )
                val region = phoneUtil.getRegionCodeForNumber(numberProto) ?: defaultRegion
                val national =
                    phoneUtil.format(
                        numberProto,
                        PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                    )

                val standardizedNational = if (region == "US" || region == "CA") {
                    national
                        .replace(
                            "(",
                            ""
                        )
                        .replace(
                            ") ",
                            "-"
                        )
                        .replace(
                            ")",
                            "-"
                        )
                } else {
                    national
                }

                region to standardizedNational
            } catch (_: Exception) {
                defaultRegion to phone
            }
        }
    }

    data class CountryInfo(
        val code: String, // ISO 3166-1 alpha-2 (e.g. US, MX, CA)
        val dialCode: String, // e.g. +1, +52
        val name: String,
        val flag: String? = null,
    )

    fun getCountries(context: Context): List<CountryInfo> {
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val supportedRegions = phoneUtil.supportedRegions
        return supportedRegions
            .asSequence()
            .map { isoCode ->
                val locale = Locale
                    .Builder()
                    .setRegion(isoCode)
                    .build()
                CountryInfo(
                    code = isoCode,
                    dialCode = "+${phoneUtil.getCountryCodeForRegion(isoCode)}",
                    name = locale.getDisplayCountry(Locale.getDefault()),
                    flag = try {
                        val firstChar = Character.toChars(isoCode[0].code - 'A'.code + 0x1F1E6)
                        val secondChar = Character.toChars(isoCode[1].code - 'A'.code + 0x1F1E6)
                        String(firstChar) + String(secondChar)
                    } catch (_: Exception) {
                        Log.d(
                            "PhoneFormatter",
                            "Error generating flag for country: $isoCode"
                        )
                        null
                    }
                )
            }
            .sortedBy { it.name }
            .toList()
    }
}
