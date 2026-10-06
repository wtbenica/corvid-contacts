// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.PhotoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps a copy of Corvid's contacts in Android's `ContactsContract` provider, under Corvid's own
 * account type, so other apps (Messages, the dialer) can show a name and photo for a phone number.
 *
 * Room stays the source of truth. The raw contact ids from inserts are kept in Room (see
 * [SystemContactMirrorEntity]) for later updates and deletes, and a row that has disappeared is
 * re-inserted.
 *
 * Edits made to the copy in other apps are read back first (see [SystemEditAbsorber]): an edit is
 * merged into the Corvid contact, and a delete hides the contact from the copy instead of deleting
 * it anywhere else. Only then is anything written, so an edit is never overwritten unread.
 *
 * Only address books the user has chosen to share are mirrored, each as one system group so the
 * books stay distinguishable in the Contacts app. How much of each contact is written depends on
 * the [SystemContactsLevel] chosen for its book; at the Full contact level and up, contact
 * categories become groups too.
 *
 * This class owns the account and the order of a reconcile; the pieces it uses do the rest.
 */
class SystemContactsMirror(
    private val context: Context,
    private val dao: SystemContactMirrorDao,
    photoManager: PhotoManager,
    editor: SystemContactsEditor,
    private val account: Account = Account(
        context.getString(R.string.app_name),
        context.getString(R.string.system_contacts_account_type)
    ),
) {
    private val mutex = Mutex()
    private val accountManager = AccountManager.get(context)

    private val permissions = MirrorPermissions(context)
    private val provider = MirrorProvider(context, account)
    private val reader = SystemContactsReader(context, account)
    private val photos = MirrorPhotos(photoManager, provider, reader, permissions::canRead)
    private val writer = MirrorWriter(dao, provider, MirrorDataRows(provider), photos, reader)
    private val groups = MirrorGroups(dao, provider)
    private val absorber = SystemEditAbsorber(dao, reader, editor, photos, writer)

    /** Whether both permissions are held: writing the copy, and reading back edits made to it. */
    fun hasPermission(): Boolean = permissions.hasAll()

    /** Whether another app has edited or deleted a mirrored contact since the last reconcile. */
    fun hasSystemChanges(): Boolean = permissions.canRead() && reader.hasChanges()

    private fun accountExists(): Boolean =
        accountManager.getAccountsByType(account.type).any { it.name == account.name }

    /**
     * Creates the owning account if it's missing. A missing account means no mirrored rows can
     * exist (removing an account deletes its contacts), so any stored mapping is stale - for
     * example after restoring a backup onto a new device - and is cleared.
     */
    private suspend fun ensureAccount() {
        if (accountExists()) return
        dao.deleteAll()
        dao.deleteAllGroups()
        accountManager.addAccountExplicitly(account, null, null)
        // Contacts with no group are hidden from the Contacts list unless the account says so.
        val settings = ContentValues().apply {
            put(ContactsContract.Settings.UNGROUPED_VISIBLE, 1)
        }
        context.contentResolver.insert(
            provider.forAccount(ContactsContract.Settings.CONTENT_URI),
            settings
        )
    }

    /**
     * Brings the system contacts in line with [sources], the contacts in the shared address
     * [books], each written at its own book's level. Requires [hasPermission].
     */
    suspend fun reconcile(
        books: List<AddressBookEntity>,
        sources: List<MirrorSource>,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureAccount()

            // Read before the edits, so a contact changed after this point fails its guarded rewrite.
            val versions = reader.versions()
            val absorbed = absorber.absorb(sources)
            val current = if (absorbed.roomChanged) dao.getMirrorSources() else sources

            dao.pruneHidden()
            val contacts = MirrorPlan.toMirrorContacts(
                current,
                books.associate { it.href to it.systemContactsLevel },
                photos::stamp,
                dao.getHiddenIds().toSet(),
                dao.linkedNames(current)
            )

            // Every group the contacts need: one per shared book, plus one per category in use.
            val wantedGroups = LinkedHashMap<String, String>()
            books.forEach { wantedGroups[MirrorPlan.bookGroupKey(it.href)] = bookTitle(it) }
            contacts.forEach { contact ->
                contact.categories.forEach { wantedGroups.putIfAbsent(MirrorPlan.categoryGroupKey(it), it) }
            }
            val groupIds = groups.sync(wantedGroups)

            val desired = contacts.map { contact ->
                val keys = listOf(MirrorPlan.bookGroupKey(contact.bookHref)) +
                    contact.categories.map { MirrorPlan.categoryGroupKey(it) }
                contact.copy(groupIds = keys.mapNotNull { groupIds[it] })
            }
            val mapped = dao.getAll()
            val plan = MirrorPlan.diff(desired, mapped).excluding(absorbed.blocked)
            Log.i(
                MIRROR_TAG,
                "reconcile: desired=${desired.size} mapped=${mapped.size} inserts=${plan.inserts.size} " +
                    "updates=${plan.updates.size} deletes=${plan.deletes.size}"
            )
            if (plan.isEmpty) return@withLock

            writer.applyDeletes(plan.deletes)
            writer.applyUpdates(plan.updates, versions)
            writer.applyInserts(plan.inserts)
        }
    }

    private fun bookTitle(book: AddressBookEntity): String =
        book.displayName?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.settings_address_book_unnamed)

    /** Removes the account, which deletes every mirrored contact, and forgets the mapping. */
    suspend fun removeAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (accountExists()) {
                accountManager.removeAccountExplicitly(account)
            }
            dao.deleteAll()
            dao.deleteAllGroups()
        }
    }
}
