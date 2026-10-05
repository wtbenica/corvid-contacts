// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.OperationApplicationException
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.util.Log
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity

/** Inserts, updates and deletes the mirrored raw contacts, and records what was written in Room. */
internal class MirrorWriter(
    private val dao: SystemContactMirrorDao,
    private val provider: MirrorProvider,
    private val dataRows: MirrorDataRows,
    private val photos: MirrorPhotos,
    private val reader: SystemContactsReader,
) {
    suspend fun applyDeletes(deletes: List<SystemContactMirrorEntity>) {
        deletes.chunked(DELETE_BATCH).forEach { chunk ->
            val results = provider.apply(
                chunk.map { provider.deleteById(RawContacts.CONTENT_URI, RawContacts._ID, it.rawContactId) }
            )
            Log.i(MIRROR_TAG, "deleted raw ids ${chunk.map { it.rawContactId }} counts=${results.map { it.count }}")
            dao.delete(chunk.map { it.contactId })
        }
    }

    /**
     * Rewrites each contact from Corvid's copy. [versions] holds each raw contact's version from
     * before the edits were read: a contact that has changed since is skipped, and the next pass
     * reads that edit instead of overwriting it.
     */
    suspend fun applyUpdates(
        updates: List<Pair<MirrorContact, SystemContactMirrorEntity>>,
        versions: Map<Long, Long>,
    ) {
        val missing = ArrayList<MirrorContact>()
        for ((contact, entry) in updates) {
            val rawId = entry.rawContactId
            val expectedVersion = versions[rawId]
            val ops = ArrayList<ContentProviderOperation>()
            // Touching the raw contact with an expected count of 1 fails the whole batch if the row
            // is gone or has changed, instead of attaching new data rows to a stale id.
            ops += ContentProviderOperation
                .newUpdate(provider.forRow(RawContacts.CONTENT_URI))
                .withSelection(
                    if (expectedVersion != null) "${RawContacts._ID}=? AND ${RawContacts.VERSION}=?" else "${RawContacts._ID}=?",
                    listOfNotNull(rawId.toString(), expectedVersion?.toString()).toTypedArray()
                )
                .withValue(RawContacts.SOURCE_ID, contact.id)
                .withValue(RawContacts.STARRED, if (contact.starred) 1 else 0)
                .withValue(RawContacts.DIRTY, 0)
                .withExpectedCount(1)
                .build()
            ops += dataRows.deleteManaged(rawId)
            ops += dataRows.build(contact) { builder -> builder.withValue(Data.RAW_CONTACT_ID, rawId) }

            try {
                val results = provider.apply(ops)
                // Counts: [0] raw contact touched (expected 1), [1] data rows deleted, then one
                // result per inserted row (no count).
                Log.i(
                    MIRROR_TAG,
                    "update contact=${contact.id} raw=$rawId touched=${results.getOrNull(0)?.count} " +
                        "deletedRows=${results.getOrNull(1)?.count} inserted=${results.size - 2}"
                )
                val photoToken = photos.write(contact, rawId)
                dao.upsert(listOf(entry(contact, rawId, photoToken)))
            } catch (e: OperationApplicationException) {
                val current = reader.version(rawId)
                if (expectedVersion != null && current != null && current != expectedVersion) {
                    Log.i(MIRROR_TAG, "update contact=${contact.id} raw=$rawId changed meanwhile, will read it next")
                } else {
                    Log.w(MIRROR_TAG, "update contact=${contact.id} raw=$rawId failed, will re-insert", e)
                    missing += contact
                }
            }
        }
        if (missing.isNotEmpty()) {
            dao.delete(missing.map { it.id })
            applyInserts(missing)
        }
    }

    suspend fun applyInserts(inserts: List<MirrorContact>) {
        inserts.chunked(INSERT_BATCH).forEach { chunk ->
            val rawIndexes = IntArray(chunk.size)
            val ops = ArrayList<ContentProviderOperation>()
            chunk.forEachIndexed { i, contact ->
                rawIndexes[i] = ops.size
                ops += rawContactInsert(contact)
                val rawIndex = rawIndexes[i]
                ops += dataRows.build(contact) { builder ->
                    builder.withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                }
            }
            val results = provider.apply(ops)

            val entries = chunk.mapIndexed { i, contact ->
                val rawId = ContentUris.parseId(requireNotNull(results[rawIndexes[i]].uri))
                entry(contact, rawId)
            }
            Log.i(MIRROR_TAG, "inserted ${entries.size} contacts, raw ids ${entries.map { it.rawContactId }}")
            dao.upsert(entries)
            chunk.forEachIndexed { i, contact ->
                photos.write(contact, entries[i].rawContactId)?.let { token ->
                    dao.upsert(listOf(entry(contact, entries[i].rawContactId, token)))
                }
            }
        }
    }

    private fun entry(contact: MirrorContact, rawId: Long, photoToken: String? = null) =
        SystemContactMirrorEntity(
            contact.id,
            rawId,
            contact.hash,
            MirrorPlan.snapshotOf(contact.copy(systemPhoto = photoToken))
        )

    private fun rawContactInsert(contact: MirrorContact): ContentProviderOperation =
        ContentProviderOperation
            .newInsert(provider.forAccount(RawContacts.CONTENT_URI))
            .withValue(RawContacts.ACCOUNT_NAME, provider.account.name)
            .withValue(RawContacts.ACCOUNT_TYPE, provider.account.type)
            .withValue(RawContacts.SOURCE_ID, contact.id)
            .withValue(RawContacts.STARRED, if (contact.starred) 1 else 0)
            .build()

    private companion object {
        const val INSERT_BATCH = 50
        const val DELETE_BATCH = 200
    }
}
