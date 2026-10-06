// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao

private const val NAME_QUERY_BATCH = 500

/**
 * The names of the contacts that [sources] link to in their relationships, by contact id. Only contacts in
 * shared books have one; a link to any other contact is left out of the phone's contacts.
 */
internal suspend fun SystemContactMirrorDao.linkedNames(sources: List<MirrorSource>): Map<ContactId, String> {
    val ids = sources
        .flatMap { it.relationships.orEmpty() }
        .filter { it.isUid }
        .map { it.value }
        .distinct()
    return ids.chunked(NAME_QUERY_BATCH).flatMap { getSharedContactNames(it) }.associate { it.id to it.name }
}
