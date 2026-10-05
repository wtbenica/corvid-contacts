// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Keeps the system contacts mirror up to date for as long as the app process lives, by watching
 * Room. Watching the database means every way a contact can change - server sync, local edits,
 * imports, archiving, deletes - and every sharing choice (which books are shared, and at what
 * level) is covered without hooking each one individually.
 *
 * There is no separate on/off setting: the mirror exists exactly while at least one address book
 * is shared and the contacts permission is held. Unsharing the last book, or losing the
 * permission, removes it.
 */
class SystemContactsMirrorManager(
    private val context: Context,
    private val dao: SystemContactMirrorDao,
    private val mirror: SystemContactsMirror,
) {
    /** Bumped whenever the system contacts change, so edits made in other apps are looked for. */
    private val systemChanges = MutableStateFlow(0)

    private var observing = false

    /** Looks for edits made in other apps; the phone can drop change notices while the app is away. */
    fun rescan() {
        systemChanges.update { it + 1 }
    }

    /**
     * Starts watching the system contacts for edits made in other apps. Registering needs a contacts
     * permission, so this waits until one is held; the app must still start without any.
     */
    private fun observeSystemContacts() {
        if (observing) return
        try {
            // Also fires for the mirror's own writes; the check that follows finds nothing to do.
            context.contentResolver.registerContentObserver(
                ContactsContract.RawContacts.CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        systemChanges.update { it + 1 }
                    }
                }
            )
            observing = true
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't watch the system contacts without the contacts permission", e)
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch { reconcileOnRoomChanges() }
        scope.launch { reconcileOnSystemChanges() }
    }

    /** Keeps the mirror in line with Corvid's own data; also the first pass after the app starts. */
    @OptIn(FlowPreview::class)
    private suspend fun reconcileOnRoomChanges() {
        combine(
            dao.observeSharedBooks(),
            dao.observeMirrorSources(),
            dao.observeHiddenIds()
        ) { books, sources, _ ->
            SharedSnapshot(books, sources)
        }
            .debounce(DEBOUNCE_MS.milliseconds)
            .collect { snapshot ->
                Log.i(
                    TAG,
                    "snapshot: sharedBooks=${snapshot.books.size} sources=${snapshot.sources.size} " +
                            "permission=${mirror.hasPermission()}"
                )
                update(snapshot)
            }
    }

    /**
     * Picks up edits made in other apps. The phone reports changes to every contact on it, so this
     * only does the full pass when a mirrored contact is actually edited or deleted.
     */
    @OptIn(FlowPreview::class)
    private suspend fun reconcileOnSystemChanges() {
        systemChanges
            .drop(1)
            .debounce(DEBOUNCE_MS.milliseconds)
            .collect {
                if (mirror.hasSystemChanges()) {
                    update(SharedSnapshot(dao.getSharedBooks(), dao.getMirrorSources()))
                }
            }
    }

    private suspend fun update(snapshot: SharedSnapshot) {
        try {
            when {
                // Removing the account needs no contacts permission and takes every mirrored
                // contact with it, so a revoked permission doesn't leave the copy behind. The
                // sharing flags are kept, so granting the permission again brings the mirror back
                // on the next reconcile.
                snapshot.books.isEmpty() || !mirror.hasPermission() -> mirror.removeAll()
                else -> {
                    observeSystemContacts()
                    mirror.reconcile(snapshot.books, snapshot.sources)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "System contacts mirror update failed", e)
        }
    }

    /** The shared address books, with their levels, and the contacts in them, as of one state. */
    private class SharedSnapshot(
        val books: List<AddressBookEntity>,
        val sources: List<MirrorSource>,
    )

    private companion object {
        const val TAG = "SystemContactsMirror"
        const val DEBOUNCE_MS = 2_000L
    }
}
