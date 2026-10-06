// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.system

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.model.Email as ModelEmail
import dev.benica.corvidcontacts.data.model.Phone as ModelPhone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SystemContactsLevel
import dev.benica.corvidcontacts.data.repository.PhotoManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Runs the mirror against the real contacts provider, with an in-memory Room database and an account
 * of its own ("Corvid Test"), so it never touches the app's real copy. The "other app" is a plain
 * [android.content.ContentResolver] call, which the provider marks dirty or deleted just as it does
 * for Google Contacts. Run it on an emulator: `ANDROID_SERIAL=emulator-5554 ./gradlew
 * connectedDebugAndroidTest`.
 */
class SystemContactsMirrorInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val resolver get() = context.contentResolver
    private val account = Account("Corvid Test", context.getString(R.string.system_contacts_account_type))
    private val bookHref = "/books/main/"

    private lateinit var database: AppDatabase
    private lateinit var photoManager: PhotoManager
    private lateinit var editor: DatabaseEditor
    private lateinit var mirror: SystemContactsMirror
    private lateinit var reader: SystemContactsReader

    private val dao get() = database.systemContactMirrorDao()

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS).forEach {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it)
        }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        photoManager = PhotoManager(context)
        editor = DatabaseEditor()
        mirror = SystemContactsMirror(context, dao, photoManager, editor, account)
        reader = SystemContactsReader(context, account)
        runBlocking { mirror.removeAll() }
    }

    @After
    fun tearDown() {
        runBlocking { mirror.removeAll() }
        database.close()
    }

    // --- The copy is written, at the level the book is shared at -----------------------------

    @Test
    fun sharingWritesTheDataTheLevelCovers() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567", email = "ada@example.org")

        reconcile()

        val raw = rawIdOf(ada.id)
        assertEquals(listOf("+1 555-123-4567"), data1(raw, Phone.CONTENT_ITEM_TYPE))
        assertEquals(listOf("ada@example.org"), data1(raw, Email.CONTENT_ITEM_TYPE))
        assertFalse(isDirty(raw))
    }

    @Test
    fun callerIdLevelWritesNoEmail() {
        shareBook(SystemContactsLevel.CALLER_ID)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567", email = "ada@example.org")

        reconcile()

        val raw = rawIdOf(ada.id)
        assertEquals(listOf("+1 555-123-4567"), data1(raw, Phone.CONTENT_ITEM_TYPE))
        assertEquals(emptyList<String>(), data1(raw, Email.CONTENT_ITEM_TYPE))
    }

    @Test
    fun aRelationshipThatLinksToAnotherContactIsWrittenAsItsName() {
        shareBook(SystemContactsLevel.FULL)
        val noah = addContact("noah", "Noah", "Kaplan", phone = "+1 555-000-0001")
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
            .withRelationships(Relationship("FRIEND", noah.id, isUid = true))

        reconcile()

        assertEquals(listOf("Noah Kaplan"), data1(rawIdOf(ada.id), Relation.CONTENT_ITEM_TYPE))
    }

    // --- Edits made in another app are read back ---------------------------------------------

    @Test
    fun anEditMadeInAnotherAppIsSavedToTheContact() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)

        otherAppSetsData1(raw, Phone.CONTENT_ITEM_TYPE, "+1 555-999-0000")
        assertTrue(isDirty(raw))
        reconcile()

        assertEquals(listOf("+1 555-999-0000"), contactInRoom(ada.id).phones.orEmpty().map { it.value })
        assertFalse(isDirty(raw))
        assertEquals(1, editor.saved.size)
    }

    @Test
    fun whenBothSidesChangedTheSamePartCorvidWins() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)

        otherAppSetsData1(raw, Phone.CONTENT_ITEM_TYPE, "+1 555-999-0000")
        saveInRoom(contactInRoom(ada.id).copy(phones = listOf(ModelPhone("+1 555-111-2222", "CELL"))))
        reconcile()

        assertEquals(listOf("+1 555-111-2222"), contactInRoom(ada.id).phones.orEmpty().map { it.value })
        assertEquals(listOf("+1 555-111-2222"), data1(raw, Phone.CONTENT_ITEM_TYPE))
        assertEquals(0, editor.saved.size)
    }

    @Test
    fun aBirthdayInAFormWeDontUnderstandIsLeftAlone() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567").copy(birthday = "1990-01-05")
        saveInRoom(ada)
        reconcile()
        val raw = rawIdOf(ada.id)

        otherAppSetsData1(raw, Event.CONTENT_ITEM_TYPE, "5 January 1990")
        otherAppAddsNickname(raw, "Countess")
        reconcile()

        val saved = contactInRoom(ada.id)
        assertEquals("1990-01-05", saved.birthday)
        assertEquals("Countess", saved.nickname)
    }

    // --- Deleting in another app only hides the contact ---------------------------------------

    @Test
    fun deletingInAnotherAppHidesTheContactAndKeepsIt() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)

        resolver.delete(ContentValuesUri.rawContact(raw), null, null)
        reconcile()

        assertEquals(listOf(ada.id), runBlocking { dao.getHiddenIds() })
        assertNotNull(contactInRoomOrNull(ada.id))
        assertFalse(rawExists(raw))

        reconcile()
        assertNull(runBlocking { dao.getAll().firstOrNull { it.contactId == ada.id } })
    }

    @Test
    fun aHiddenContactIsNotWrittenUntilItIsShownAgain() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        runBlocking { dao.hide(listOf(dev.benica.corvidcontacts.data.local.SystemContactHiddenEntity(ada.id))) }

        reconcile()
        assertNull(runBlocking { dao.getAll().firstOrNull { it.contactId == ada.id } })

        runBlocking { dao.unhide(ada.id) }
        reconcile()
        assertNotNull(runBlocking { dao.getAll().firstOrNull { it.contactId == ada.id } })
    }

    // --- A rewrite doesn't throw away what it doesn't own, or an edit it hasn't read -----------

    @Test
    fun rowsOfKindsCorvidDoesntWriteSurviveARewrite() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)

        resolver.insert(
            Data.CONTENT_URI,
            ContentValues().apply {
                put(Data.RAW_CONTACT_ID, raw)
                put(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                put(Event.START_DATE, "2001-02-03")
                put(Event.TYPE, Event.TYPE_ANNIVERSARY)
            }
        )
        resolver.insert(
            Data.CONTENT_URI,
            ContentValues().apply {
                put(Data.RAW_CONTACT_ID, raw)
                put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                put(Im.DATA, "ada@chat.example")
                put(Im.PROTOCOL, Im.PROTOCOL_JABBER)
            }
        )
        saveInRoom(contactInRoom(ada.id).copy(firstName = "Augusta"))
        reconcile()

        assertEquals(listOf("2001-02-03"), data1(raw, Event.CONTENT_ITEM_TYPE))
        assertEquals(listOf("ada@chat.example"), data1(raw, Im.CONTENT_ITEM_TYPE))
        assertEquals("Augusta", data(raw, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, Data.DATA2).single())
    }

    @Test
    fun aRewriteIsSkippedWhenTheContactChangedAfterItsVersionWasRead() {
        shareBook(SystemContactsLevel.FULL)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)
        val versionRead = reader.version(raw)!!

        otherAppSetsData1(raw, Phone.CONTENT_ITEM_TYPE, "+1 555-999-0000")
        val desired = MirrorPlan.toMirrorContact(
            runBlocking { dao.getMirrorSources() }.single(),
            photoStamp = null,
            level = SystemContactsLevel.FULL
        )!!.copy(phones = listOf(MirrorPhone("+1 555-111-2222", 2)))
        val entry = runBlocking { dao.getAll() }.single()
        val provider = MirrorProvider(context, account)
        val writer = MirrorWriter(dao, provider, MirrorDataRows(provider), MirrorPhotos(photoManager, provider, reader) { true }, reader)

        runBlocking { writer.applyUpdates(listOf(desired to entry), mapOf(raw to versionRead)) }

        assertEquals(listOf("+1 555-999-0000"), data1(raw, Phone.CONTENT_ITEM_TYPE))
        assertTrue(isDirty(raw))
    }

    // --- Photos -------------------------------------------------------------------------------

    @Test
    fun aPhotoSetInAnotherAppIsSavedAndOneRemovedThereIsRemoved() {
        shareBook(SystemContactsLevel.CALLER_ID)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        val raw = rawIdOf(ada.id)
        assertNull(reader.photoToken(raw))

        resolver.insert(
            Data.CONTENT_URI,
            ContentValues().apply {
                put(Data.RAW_CONTACT_ID, raw)
                put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                put(Photo.PHOTO, jpeg(Color.RED))
            }
        )
        reconcile()

        val withPhoto = contactInRoom(ada.id)
        assertTrue(withPhoto.hasPhoto)
        assertTrue(photoManager.getPhotoFile(ada.id).exists())
        assertNotNull(reader.photoToken(raw))

        resolver.delete(Data.CONTENT_URI, "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?", arrayOf(raw.toString(), Photo.CONTENT_ITEM_TYPE))
        reconcile()

        assertFalse(contactInRoom(ada.id).hasPhoto)
        assertFalse(photoManager.getPhotoFile(ada.id).exists())
    }

    @Test
    fun aPhotoChangedInCorvidIsWrittenAndRemembered() {
        shareBook(SystemContactsLevel.CALLER_ID)
        val ada = addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        val uri = photoManager.savePhotoToFile(ada.id, jpeg(Color.BLUE))
        saveInRoom(ada.copy(hasPhoto = true, photoUrl = uri))

        reconcile()

        val raw = rawIdOf(ada.id)
        assertNotNull(reader.photoToken(raw))
        val snapshot = MirrorPlan.readSnapshot(runBlocking { dao.getAll() }.single().snapshot)!!
        assertEquals(reader.photoToken(raw), snapshot.systemPhoto)
    }

    // --- Removing the copy --------------------------------------------------------------------

    @Test
    fun removingEverythingRemovesTheAccountAndForgetsTheMapping() {
        shareBook(SystemContactsLevel.FULL)
        addContact("ada", "Ada", "Lovelace", phone = "+1 555-123-4567")
        reconcile()
        assertTrue(accountExists())

        runBlocking { mirror.removeAll() }

        // Android's contacts provider then deletes the account's contacts on its own schedule.
        assertFalse(accountExists())
        assertTrue(runBlocking { dao.getAll() }.isEmpty())
    }

    // --- Helpers ------------------------------------------------------------------------------

    private fun shareBook(level: SystemContactsLevel) = runBlocking {
        database.addressBookDao().insertAddressBooks(
            listOf(
                AddressBookEntity(
                    href = bookHref,
                    displayName = "Main",
                    colorInt = Color.GRAY,
                    shareWithSystem = true,
                    systemContactsLevel = level,
                )
            )
        )
    }

    private fun addContact(
        id: ContactId,
        first: String,
        last: String,
        phone: String,
        email: String? = null,
    ): ContactEntity {
        val contact = ContactEntity(
            id = id,
            displayName = "",
            firstName = first,
            lastName = last,
            phones = listOf(ModelPhone(phone, "CELL")),
            emails = email?.let { listOf(ModelEmail(it, "HOME")) } ?: emptyList(),
            addressBookHref = bookHref,
        )
        saveInRoom(contact)
        return contact
    }

    private fun ContactEntity.withRelationships(vararg relationships: Relationship): ContactEntity =
        copy(relationships = relationships.toList()).also { saveInRoom(it) }

    private fun saveInRoom(contact: ContactEntity) = runBlocking { database.contactDao().insertContacts(listOf(contact)) }

    private fun contactInRoomOrNull(id: ContactId): ContactEntity? =
        runBlocking { database.contactDao().getContactById(id)?.contact }

    private fun contactInRoom(id: ContactId): ContactEntity = requireNotNull(contactInRoomOrNull(id))

    private fun reconcile() = runBlocking { mirror.reconcile(dao.getSharedBooks(), dao.getMirrorSources()) }

    private fun rawIdOf(contactId: ContactId): Long =
        requireNotNull(runBlocking { dao.getAll() }.firstOrNull { it.contactId == contactId }) { "$contactId isn't mirrored" }
            .rawContactId

    private fun isDirty(rawId: Long): Boolean = resolver.query(
        RawContacts.CONTENT_URI, arrayOf(RawContacts.DIRTY), "${RawContacts._ID}=?", arrayOf(rawId.toString()), null
    )?.use { it.moveToFirst() && it.getInt(0) == 1 } ?: false

    private fun accountExists(): Boolean =
        android.accounts.AccountManager.get(context).getAccountsByType(account.type).any { it.name == account.name }

    private fun rawExists(rawId: Long): Boolean = resolver.query(
        RawContacts.CONTENT_URI, arrayOf(RawContacts._ID), "${RawContacts._ID}=?", arrayOf(rawId.toString()), null
    )?.use { it.count > 0 } ?: false

    private fun data1(rawId: Long, mimeType: String): List<String?> = data(rawId, mimeType, Data.DATA1)

    private fun data(rawId: Long, mimeType: String, column: String): List<String?> = resolver.query(
        Data.CONTENT_URI,
        arrayOf(column),
        "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
        arrayOf(rawId.toString(), mimeType),
        null
    )?.use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList() }.orEmpty()

    private fun otherAppSetsData1(rawId: Long, mimeType: String, value: String) {
        resolver.update(
            Data.CONTENT_URI,
            ContentValues().apply { put(Data.DATA1, value) },
            "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
            arrayOf(rawId.toString(), mimeType)
        )
    }

    private fun otherAppAddsNickname(rawId: Long, nickname: String) {
        resolver.insert(
            Data.CONTENT_URI,
            ContentValues().apply {
                put(Data.RAW_CONTACT_ID, rawId)
                put(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                put(Nickname.NAME, nickname)
            }
        )
    }

    private fun jpeg(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            it.toByteArray()
        }
    }

    /** Saves to the same Room database the mirror reads, the way the app's repository would. */
    private inner class DatabaseEditor : SystemContactsEditor {
        val saved = mutableListOf<ContactEntity>()

        override suspend fun get(id: ContactId): ContactEntity? = database.contactDao().getContactById(id)?.contact

        override suspend fun save(contact: ContactEntity): Result<Unit> = runCatching {
            database.contactDao().insertContacts(listOf(contact))
            saved += contact
        }
    }

    private object ContentValuesUri {
        fun rawContact(id: Long) = android.content.ContentUris.withAppendedId(RawContacts.CONTENT_URI, id)
    }
}
