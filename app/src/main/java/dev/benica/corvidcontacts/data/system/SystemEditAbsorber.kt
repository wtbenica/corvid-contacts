// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.util.Log
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactHiddenEntity
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity

/** What absorbing the edits made in other apps changed, and which contacts must not be written yet. */
internal class Absorbed(val roomChanged: Boolean, val blocked: Set<ContactId>) {
    companion object {
        val NONE = Absorbed(false, emptySet())
    }
}

/**
 * Reads what other apps did to the mirrored contacts and applies it: an edit is merged into the
 * Corvid contact, and a delete hides the contact from the copy without deleting it anywhere else.
 */
internal class SystemEditAbsorber(
    private val dao: SystemContactMirrorDao,
    private val reader: SystemContactsReader,
    private val editor: SystemContactsEditor,
    private val photos: MirrorPhotos,
    private val writer: MirrorWriter,
) {
    private enum class EditOutcome { APPLIED, REWRITE, RETRY }

    /**
     * A contact whose edit could not be saved is [Absorbed.blocked], so the write that follows
     * leaves it dirty to be tried again instead of overwriting the edit. Needs `READ_CONTACTS`.
     */
    suspend fun absorb(sources: List<MirrorSource>): Absorbed {
        val changes = reader.readChanges()
        if (changes.isEmpty()) return Absorbed.NONE

        val byRaw = dao.getAll().associateBy { it.rawContactId }
        val sourcesById = sources.associateBy { it.id }
        var roomChanged = false
        val blocked = HashSet<ContactId>()
        val rewrite = ArrayList<ContactId>()
        for (change in changes) {
            val entry = byRaw[change.rawContactId] ?: continue
            when (change) {
                is SystemContactsReader.Deleted -> {
                    Log.i(MIRROR_TAG, "contact=${entry.contactId} raw=${entry.rawContactId} deleted elsewhere, hiding")
                    dao.hide(listOf(SystemContactHiddenEntity(entry.contactId)))
                    writer.applyDeletes(listOf(entry))
                }

                is SystemContactsReader.Edited -> when (absorbEdit(entry, change, sourcesById[entry.contactId])) {
                    EditOutcome.APPLIED -> {
                        roomChanged = true
                        rewrite += entry.contactId
                    }

                    EditOutcome.REWRITE -> rewrite += entry.contactId
                    EditOutcome.RETRY -> blocked += entry.contactId
                }
            }
        }
        dao.markStale(rewrite)
        return Absorbed(roomChanged, blocked)
    }

    private suspend fun absorbEdit(
        entry: SystemContactMirrorEntity,
        change: SystemContactsReader.Edited,
        source: MirrorSource?,
    ): EditOutcome {
        val base = MirrorPlan.readSnapshot(entry.snapshot)
        if (base == null || source == null) return EditOutcome.REWRITE

        val linkedNames = dao.linkedNames(listOf(source))
        val ours = MirrorPlan.toMirrorContact(source, photos.stamp(source), base.level, linkedNames)
        val merge = SystemEditMerge.merge(base, change.contact, ours)
        if (merge.isEmpty) return EditOutcome.REWRITE

        val entity = editor.get(entry.contactId)
        if (entity == null || !entity.isEditable()) return EditOutcome.REWRITE

        val photoUrl = if (MirrorField.PHOTO in merge.taken) {
            photos.saveFromSystem(entity.id, change.rawContactId)
        } else {
            null
        }
        return editor.save(SystemEditMerge.apply(entity, merge, photoUrl, linkedNames)).fold(
            onSuccess = {
                Log.i(MIRROR_TAG, "contact=${entry.contactId} took ${merge.taken} from the system contacts")
                EditOutcome.APPLIED
            },
            onFailure = {
                Log.w(MIRROR_TAG, "contact=${entry.contactId} edit not saved, will retry", it)
                EditOutcome.RETRY
            }
        )
    }
}
