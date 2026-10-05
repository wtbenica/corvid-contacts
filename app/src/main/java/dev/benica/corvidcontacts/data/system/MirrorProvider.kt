// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

internal const val MIRROR_TAG = "SystemContactsMirror"

/** Talks to `ContactsContract` as the sync adapter for Corvid's own [account]. */
internal class MirrorProvider(private val context: Context, val account: Account) {

    fun apply(ops: List<ContentProviderOperation>): Array<ContentProviderResult> =
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(ops))

    /**
     * Marks the call as coming from the account's sync adapter, so changes aren't flagged dirty and
     * deletes remove the row instead of just marking it deleted. Updates and deletes address rows by
     * id and must not carry the account as query parameters: the provider turns those into an
     * `account_name` filter on the raw contacts table, which has no such column, and rejects the
     * whole batch with "Invalid token account_name".
     */
    fun forRow(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build()

    /** As [forRow], plus the account, for inserts that name no account in their values. */
    fun forAccount(uri: Uri): Uri = forRow(uri).buildUpon()
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .build()

    /** An operation that deletes the row of [uri] whose [idColumn] is [id]. */
    fun deleteById(uri: Uri, idColumn: String, id: Long): ContentProviderOperation =
        ContentProviderOperation
            .newDelete(forRow(uri))
            .withSelection("$idColumn=?", arrayOf(id.toString()))
            .build()
}
