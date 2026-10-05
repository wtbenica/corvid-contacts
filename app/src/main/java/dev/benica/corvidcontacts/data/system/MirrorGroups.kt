// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.OperationApplicationException
import android.provider.ContactsContract.Groups
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.local.SystemGroupMirrorEntity

/** Keeps the system groups, one per shared book and one per category in use, in line with Corvid's. */
internal class MirrorGroups(
    private val dao: SystemContactMirrorDao,
    private val provider: MirrorProvider,
) {
    /**
     * Makes the system groups match [wanted] (group key to title) - creating, renaming and
     * removing as needed - and returns each key's group id. A group that has vanished from the
     * provider is detected when renaming it affects nothing, and re-created.
     */
    suspend fun sync(wanted: Map<String, String>): Map<String, Long> {
        val existing = dao.getAllGroups().associateBy { it.groupKey }

        val stale = existing.values.filter { it.groupKey !in wanted }
        if (stale.isNotEmpty()) {
            provider.apply(stale.map { provider.deleteById(Groups.CONTENT_URI, Groups._ID, it.groupId) })
            dao.deleteGroups(stale.map { it.groupKey })
        }

        val result = HashMap<String, Long>()
        for ((key, title) in wanted) {
            val current = existing[key]

            if (current != null && current.title == title) {
                result[key] = current.groupId
                continue
            }
            if (current != null) {
                try {
                    provider.apply(
                        listOf(
                            ContentProviderOperation
                                .newUpdate(provider.forRow(Groups.CONTENT_URI))
                                .withSelection("${Groups._ID}=?", arrayOf(current.groupId.toString()))
                                .withValue(Groups.TITLE, title)
                                .withExpectedCount(1)
                                .build()
                        )
                    )
                    dao.upsertGroups(listOf(current.copy(title = title)))
                    result[key] = current.groupId
                    continue
                } catch (_: OperationApplicationException) {
                    // The group is gone; fall through and create it again.
                }
            }

            val results = provider.apply(
                listOf(
                    ContentProviderOperation
                        .newInsert(provider.forAccount(Groups.CONTENT_URI))
                        .withValue(Groups.ACCOUNT_NAME, provider.account.name)
                        .withValue(Groups.ACCOUNT_TYPE, provider.account.type)
                        .withValue(Groups.TITLE, title)
                        .withValue(Groups.SOURCE_ID, key)
                        .withValue(Groups.GROUP_VISIBLE, 1)
                        .build()
                )
            )
            val groupId = ContentUris.parseId(requireNotNull(results[0].uri))
            dao.upsertGroups(listOf(SystemGroupMirrorEntity(key, groupId, title)))
            result[key] = groupId
        }
        return result
    }
}
