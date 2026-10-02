// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder

/**
 * Registers a sync adapter for the mirror's account type, which is what lets the account own
 * contacts in `ContactsContract`. It deliberately does nothing: the mirror is written one-way
 * from Corvid's own database by [SystemContactsMirrorManager], never pulled by the system.
 */
class SystemContactsSyncService : Service() {
    private lateinit var syncAdapter: NoOpSyncAdapter

    override fun onCreate() {
        syncAdapter = NoOpSyncAdapter(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = syncAdapter.syncAdapterBinder

    private class NoOpSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true) {
        override fun onPerformSync(
            account: Account?,
            extras: Bundle?,
            authority: String?,
            provider: ContentProviderClient?,
            syncResult: SyncResult?,
        ) = Unit
    }
}
