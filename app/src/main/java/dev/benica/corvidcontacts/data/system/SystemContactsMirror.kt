// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import android.util.Log
import androidx.core.content.ContextCompat
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.MirrorSource
import dev.benica.corvidcontacts.data.local.SystemContactMirrorDao
import dev.benica.corvidcontacts.data.local.SystemContactMirrorEntity
import dev.benica.corvidcontacts.data.local.SystemGroupMirrorEntity
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.PhotoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Writes a one-way mirror of Corvid's contacts into Android's `ContactsContract` provider, under
 * Corvid's own account type, so other apps (Messages, the dialer) can show a name and photo for a
 * phone number.
 *
 * Room stays the source of truth; nothing is ever read back from the system contacts. This class
 * only holds [Manifest.permission.WRITE_CONTACTS]: the raw contact ids returned when inserting
 * are kept in Room (see [SystemContactMirrorEntity]) and used for later updates and deletes. A
 * mirrored row that has disappeared is detected when an update affects nothing, and re-inserted.
 *
 * Only address books the user has chosen to share are mirrored, each as one system group so the
 * books stay distinguishable in the Contacts app. How much of each contact is written depends on
 * the [SystemContactsLevel] chosen for its book; at the Full contact level and up, contact
 * categories become groups too.
 */
class SystemContactsMirror(
    private val context: Context,
    private val dao: SystemContactMirrorDao,
    private val photoManager: PhotoManager,
) {
    private val mutex = Mutex()
    private val accountManager = AccountManager.get(context)
    private val account = Account(
        context.getString(R.string.app_name),
        context.getString(R.string.system_contacts_account_type)
    )

    /** Whether the raw-contact read-only flag is accepted by this device's provider. */
    @Volatile
    private var readOnlyFlagSupported = true

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

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
            ContactsContract.Settings.CONTENT_URI.asSyncAdapter(),
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

            val contacts = MirrorPlan.toMirrorContacts(
                sources,
                books.associate { it.href to it.systemContactsLevel },
                ::photoStamp
            )

            // Every group the contacts need: one per shared book, plus one per category in use.
            val groups = LinkedHashMap<String, String>()
            books.forEach { groups[MirrorPlan.bookGroupKey(it.href)] = bookTitle(it) }
            contacts.forEach { contact ->
                contact.categories.forEach { groups.putIfAbsent(MirrorPlan.categoryGroupKey(it), it) }
            }
            val groupIds = syncGroups(groups)

            val desired = contacts.map { contact ->
                val keys = listOf(MirrorPlan.bookGroupKey(contact.bookHref)) +
                    contact.categories.map { MirrorPlan.categoryGroupKey(it) }
                contact.copy(groupIds = keys.mapNotNull { groupIds[it] })
            }
            val plan = MirrorPlan.diff(desired, dao.getAll())
            if (plan.isEmpty) return@withLock

            applyDeletes(plan.deletes)
            applyUpdates(plan.updates)
            applyInserts(plan.inserts)
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

    /**
     * Makes the system groups match [wanted] (group key to title) - creating, renaming and
     * removing as needed - and returns each key's group id. A group that has vanished from the
     * provider is detected when renaming it affects nothing, and re-created.
     */
    private suspend fun syncGroups(wanted: Map<String, String>): Map<String, Long> {
        val existing = dao.getAllGroups().associateBy { it.groupKey }

        val stale = existing.values.filter { it.groupKey !in wanted }
        if (stale.isNotEmpty()) {
            apply(
                stale.map {
                    ContentProviderOperation
                        .newDelete(Groups.CONTENT_URI.asSyncAdapter())
                        .withSelection("${Groups._ID}=?", arrayOf(it.groupId.toString()))
                        .build()
                }
            )
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
                    apply(
                        listOf(
                            ContentProviderOperation
                                .newUpdate(Groups.CONTENT_URI.asSyncAdapter())
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

            val results = apply(
                listOf(
                    ContentProviderOperation
                        .newInsert(Groups.CONTENT_URI.asSyncAdapter())
                        .withValue(Groups.ACCOUNT_NAME, account.name)
                        .withValue(Groups.ACCOUNT_TYPE, account.type)
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

    private suspend fun applyDeletes(deletes: List<SystemContactMirrorEntity>) {
        deletes.chunked(DELETE_BATCH).forEach { chunk ->
            val ops = chunk.map {
                ContentProviderOperation
                    .newDelete(RawContacts.CONTENT_URI.asSyncAdapter())
                    .withSelection("${RawContacts._ID}=?", arrayOf(it.rawContactId.toString()))
                    .build()
            }
            apply(ops)
            dao.delete(chunk.map { it.contactId })
        }
    }

    private suspend fun applyUpdates(updates: List<Pair<MirrorContact, SystemContactMirrorEntity>>) {
        val missing = ArrayList<MirrorContact>()
        for ((contact, entry) in updates) {
            val rawId = entry.rawContactId
            val ops = ArrayList<ContentProviderOperation>()
            // Touching the raw contact with an expected count of 1 fails the whole batch if the row
            // is gone, instead of attaching new data rows to a stale id.
            ops += ContentProviderOperation
                .newUpdate(RawContacts.CONTENT_URI.asSyncAdapter())
                .withSelection("${RawContacts._ID}=?", arrayOf(rawId.toString()))
                .withValue(RawContacts.SOURCE_ID, contact.id)
                .withExpectedCount(1)
                .build()
            ops += ContentProviderOperation
                .newDelete(Data.CONTENT_URI.asSyncAdapter())
                .withSelection("${Data.RAW_CONTACT_ID}=?", arrayOf(rawId.toString()))
                .build()
            ops += dataOps(contact) { builder -> builder.withValue(Data.RAW_CONTACT_ID, rawId) }

            try {
                apply(ops)
                writePhoto(contact, rawId)
                dao.upsert(listOf(SystemContactMirrorEntity(contact.id, rawId, contact.hash)))
            } catch (_: OperationApplicationException) {
                missing += contact
            }
        }
        if (missing.isNotEmpty()) {
            dao.delete(missing.map { it.id })
            applyInserts(missing)
        }
    }

    private suspend fun applyInserts(inserts: List<MirrorContact>) {
        inserts.chunked(INSERT_BATCH).forEach { chunk ->
            val rawIndexes = IntArray(chunk.size)
            fun buildOps(): List<ContentProviderOperation> {
                val ops = ArrayList<ContentProviderOperation>()
                chunk.forEachIndexed { i, contact ->
                    rawIndexes[i] = ops.size
                    ops += rawContactInsert(contact)
                    val rawIndex = rawIndexes[i]
                    ops += dataOps(contact) { builder ->
                        builder.withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                    }
                }
                return ops
            }

            val results = try {
                apply(buildOps())
            } catch (e: IllegalArgumentException) {
                // A provider that rejects the read-only flag still gets a working (if editable)
                // mirror: retry once without it.
                if (!readOnlyFlagSupported) throw e
                Log.w(TAG, "Provider rejected the read-only flag; continuing without it", e)
                readOnlyFlagSupported = false
                apply(buildOps())
            }

            val entries = chunk.mapIndexed { i, contact ->
                val rawId = ContentUris.parseId(requireNotNull(results[rawIndexes[i]].uri))
                SystemContactMirrorEntity(contact.id, rawId, contact.hash)
            }
            dao.upsert(entries)
            chunk.forEachIndexed { i, contact -> writePhoto(contact, entries[i].rawContactId) }
        }
    }

    private fun rawContactInsert(contact: MirrorContact): ContentProviderOperation {
        val builder = ContentProviderOperation
            .newInsert(RawContacts.CONTENT_URI.asSyncAdapter())
            .withValue(RawContacts.ACCOUNT_NAME, account.name)
            .withValue(RawContacts.ACCOUNT_TYPE, account.type)
            .withValue(RawContacts.SOURCE_ID, contact.id)
        if (readOnlyFlagSupported) {
            // Asks the Contacts app not to offer editing: the mirror is overwritten from Corvid.
            builder.withValue(RawContacts.RAW_CONTACT_IS_READ_ONLY, 1)
        }
        return builder.build()
    }

    /**
     * The data rows for [contact] - name, groups, phones and whatever the sharing level adds;
     * [bindRaw] attaches each to its raw contact.
     */
    private fun dataOps(
        contact: MirrorContact,
        bindRaw: (ContentProviderOperation.Builder) -> ContentProviderOperation.Builder,
    ): List<ContentProviderOperation> {
        val ops = ArrayList<ContentProviderOperation>()
        fun row(mimeType: String, fill: ContentProviderOperation.Builder.() -> Unit) {
            val builder = bindRaw(ContentProviderOperation.newInsert(Data.CONTENT_URI.asSyncAdapter()))
                .withValue(Data.MIMETYPE, mimeType)
            builder.fill()
            ops += builder.build()
        }

        row(StructuredName.CONTENT_ITEM_TYPE) {
            withValue(StructuredName.DISPLAY_NAME, contact.displayName)
            withValue(StructuredName.GIVEN_NAME, contact.givenName)
            withValue(StructuredName.FAMILY_NAME, contact.familyName)
            withValue(StructuredName.MIDDLE_NAME, contact.middleName)
            withValue(StructuredName.PREFIX, contact.prefix)
            withValue(StructuredName.SUFFIX, contact.suffix)
        }
        contact.groupIds.forEach { groupId ->
            row(GroupMembership.CONTENT_ITEM_TYPE) { withValue(GroupMembership.GROUP_ROW_ID, groupId) }
        }
        contact.phones.forEach { phone ->
            row(Phone.CONTENT_ITEM_TYPE) {
                withValue(Phone.NUMBER, phone.number)
                withValue(Phone.TYPE, phone.type)
            }
        }
        contact.emails.forEach { email ->
            row(Email.CONTENT_ITEM_TYPE) {
                withValue(Email.ADDRESS, email.address)
                withValue(Email.TYPE, email.type)
            }
        }
        contact.addresses.forEach { address ->
            row(StructuredPostal.CONTENT_ITEM_TYPE) {
                withValue(StructuredPostal.FORMATTED_ADDRESS, address.formatted)
                withValue(StructuredPostal.STREET, address.street)
                withValue(StructuredPostal.POBOX, address.poBox)
                withValue(StructuredPostal.CITY, address.city)
                withValue(StructuredPostal.REGION, address.region)
                withValue(StructuredPostal.POSTCODE, address.postcode)
                withValue(StructuredPostal.COUNTRY, address.country)
                withValue(StructuredPostal.TYPE, address.type)
            }
        }
        contact.websites.forEach { url ->
            row(Website.CONTENT_ITEM_TYPE) {
                withValue(Website.URL, url)
                withValue(Website.TYPE, Website.TYPE_OTHER)
            }
        }
        contact.socials.forEach { social ->
            row(Im.CONTENT_ITEM_TYPE) {
                withValue(Im.DATA, social.handle)
                withValue(Im.PROTOCOL, Im.PROTOCOL_CUSTOM)
                withValue(Im.CUSTOM_PROTOCOL, social.network)
                withValue(Im.TYPE, Im.TYPE_OTHER)
            }
        }
        contact.relations.forEach { relation ->
            row(Relation.CONTENT_ITEM_TYPE) {
                withValue(Relation.NAME, relation.name)
                withValue(Relation.TYPE, relation.type)
                if (relation.type == Relation.TYPE_CUSTOM) withValue(Relation.LABEL, relation.label)
            }
        }
        contact.birthday?.let { date ->
            row(Event.CONTENT_ITEM_TYPE) {
                withValue(Event.START_DATE, date)
                withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
            }
        }
        contact.organization?.let { organization ->
            row(Organization.CONTENT_ITEM_TYPE) {
                withValue(Organization.COMPANY, organization.company)
                withValue(Organization.TITLE, organization.title)
                withValue(Organization.TYPE, Organization.TYPE_WORK)
            }
        }
        contact.nickname?.let { nickname ->
            row(Nickname.CONTENT_ITEM_TYPE) {
                withValue(Nickname.NAME, nickname)
                withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
            }
        }
        contact.note?.let { note ->
            row(Note.CONTENT_ITEM_TYPE) { withValue(Note.NOTE, note) }
        }
        return ops
    }

    /**
     * Writes [contact]'s photo as its own small batch, since photo bytes can approach the binder
     * transaction limit. A failure here leaves the contact without a photo rather than failing the
     * whole reconcile.
     */
    private fun writePhoto(contact: MirrorContact, rawId: Long) {
        if (contact.photoStamp == null) return
        val bytes = photoBytes(contact.id) ?: return
        try {
            apply(
                listOf(
                    ContentProviderOperation
                        .newInsert(Data.CONTENT_URI.asSyncAdapter())
                        .withValue(Data.RAW_CONTACT_ID, rawId)
                        .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                        .withValue(Photo.PHOTO, bytes)
                        .build()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't mirror photo for contact ${contact.id}", e)
        }
    }

    private fun apply(ops: List<ContentProviderOperation>): Array<ContentProviderResult> =
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(ops))

    private fun photoStamp(source: MirrorSource): String? {
        if (!source.hasPhoto) return null
        val file = photoManager.getPhotoFile(source.id)
        return if (file.exists()) "${file.lastModified()}-${file.length()}" else null
    }

    /** The contact's photo, downscaled if large so it fits comfortably in one provider call. */
    private fun photoBytes(contactId: String): ByteArray? {
        val file = photoManager.getPhotoFile(contactId)
        if (!file.exists()) return null
        val raw = file.readBytes()
        if (raw.size <= PHOTO_PASS_THROUGH_BYTES) return raw

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_DIMENSION) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(
            raw,
            0,
            raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun Uri.asSyncAdapter(): Uri = buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
        .build()

    private companion object {
        const val TAG = "SystemContactsMirror"
        const val INSERT_BATCH = 50
        const val DELETE_BATCH = 200
        const val PHOTO_PASS_THROUGH_BYTES = 256 * 1024
        const val PHOTO_MAX_DIMENSION = 720
    }
}
