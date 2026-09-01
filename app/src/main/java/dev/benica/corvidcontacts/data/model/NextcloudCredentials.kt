// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.model

data class NextcloudCredentials(
    val serverUrl: String,
    val username: String,
    val appPassword: String,
) {
    /** Stable identifier for the account these credentials belong to, excluding the password
     *  (which can rotate without it being a different account). Used to tell whether onboarding
     *  has already run for this specific account. */
    val accountKey: String get() = "$serverUrl|$username"
}
