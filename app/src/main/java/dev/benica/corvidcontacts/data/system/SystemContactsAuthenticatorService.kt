// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import dev.benica.corvidcontacts.R

/**
 * Registers the account type that owns the mirrored system contacts. The account has no
 * credentials and no sign-in: it's created and removed by [SystemContactsMirror] when the user
 * toggles the feature in Corvid's settings, so adding one from Android's account settings is
 * refused.
 */
class SystemContactsAuthenticatorService : Service() {
    private lateinit var authenticator: Authenticator

    override fun onCreate() {
        authenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? = authenticator.iBinder

    private class Authenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
        override fun addAccount(
            response: AccountAuthenticatorResponse?,
            accountType: String?,
            authTokenType: String?,
            requiredFeatures: Array<out String>?,
            options: Bundle?,
        ): Bundle = Bundle().apply {
            putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
            putString(
                AccountManager.KEY_ERROR_MESSAGE,
                context.getString(R.string.system_contacts_add_account_unsupported)
            )
        }

        override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle =
            unsupported()

        override fun confirmCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            options: Bundle?,
        ): Bundle = unsupported()

        override fun getAuthToken(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?,
        ): Bundle = unsupported()

        override fun getAuthTokenLabel(authTokenType: String?): String? = null

        override fun updateCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?,
        ): Bundle = unsupported()

        override fun hasFeatures(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            features: Array<out String>?,
        ): Bundle = Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }

        private fun unsupported() = Bundle().apply {
            putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
            putString(AccountManager.KEY_ERROR_MESSAGE, "Unsupported")
        }
    }
}
