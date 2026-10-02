// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.util.Log
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Keeps the system contacts mirror up to date for as long as the app process lives, by watching
 * the "System contacts" setting and Room. Watching the database means every way a contact can
 * change - server sync, local edits, imports, archiving, deletes - is covered without hooking each
 * one individually.
 */
class SystemContactsMirrorManager(
    private val settingsRepository: SettingsRepository,
    private val dao: SystemContactMirrorDao,
    private val mirror: SystemContactsMirror,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.systemContactsEnabled
                .distinctUntilChanged()
                .flatMapLatest { enabled ->
                    // null means "feature off": remove the mirror once, then idle. With the feature on
                    // but no address book shared, the mirror is simply empty.
                    if (enabled) {
                        combine(dao.observeSharedBooks(), dao.observeMirrorSources()) { books, sources ->
                            SharedSnapshot(books, sources)
                        }.debounce(DEBOUNCE_MS)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { snapshot ->
                    try {
                        when {
                            snapshot == null -> mirror.removeAll()
                            mirror.hasPermission() -> mirror.reconcile(snapshot.books, snapshot.sources)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "System contacts mirror update failed", e)
                    }
                }
        }
    }

    /** The shared address books and the contacts in them, as of one database state. */
    private class SharedSnapshot(
        val books: List<AddressBookEntity>,
        val sources: List<MirrorSource>,
    )

    private companion object {
        const val TAG = "SystemContactsMirror"
        const val DEBOUNCE_MS = 2_000L
    }
}
