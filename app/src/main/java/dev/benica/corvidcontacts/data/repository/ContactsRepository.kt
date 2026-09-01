// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookDao
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactDao
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.remote.DavAddressBook
import dev.benica.corvidcontacts.data.remote.DavParser
import dev.benica.corvidcontacts.data.remote.NextcloudApiProvider
import dev.benica.corvidcontacts.data.remote.NextcloudService
import dev.benica.corvidcontacts.extensions.oklch
import dev.benica.corvidcontacts.extensions.toOklch
import dev.benica.corvidcontacts.ui.contacts.PhoneFormatter
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults
import dev.benica.corvidcontacts.widgets.SingleContactWidget
import ezvcard.Ezvcard
import ezvcard.VCardVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

/**
 * Stats for a sync session.
 * @property photoCount URL photos converted to embedded data.
 * @property phoneCount Phone numbers reformatted.
 */
data class SyncRepairStats(
    val photoCount: Int = 0,
    val phoneCount: Int = 0,
) {
    val total: Int get() = photoCount + phoneCount
}

/**
 * Result of [ContactsRepository.importVCardText].
 */
data class ImportResult(
    val imported: Int,
    val failed: Int,
)

/**
 * Result of [ContactsRepository.uploadLocalAddressBook].
 */
data class AddressBookUploadResult(
    val uploadedCount: Int,
    val failedCount: Int,
    val fullyCompleted: Boolean,
)

/**
 * Central repository for contact data, handling sync and local storage.
 *
 * @property context Application context.
 * @property contactDao DAO for contacts.
 * @property addressBookDao DAO for address books.
 * @property authRepository Repository for authentication.
 * @property settingsRepository Repository for settings.
 * @property photoManager Manager for contact photos.
 * @property vCardMapper Mapper for VCard conversions.
 */
class ContactsRepository(
    private val context: Context,
    private val contactDao: ContactDao,
    private val addressBookDao: AddressBookDao,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val photoManager: PhotoManager = PhotoManager(context),
    private val vCardMapper: VCardMapper = VCardMapper(photoManager),
) {
    private val syncMutex = Mutex()

    val allContacts: Flow<List<ContactWithAddressBook>> = contactDao.getAllVisibleContacts()

    val archivedContacts: Flow<List<ContactWithAddressBook>> = contactDao.getArchivedContacts()

    val userManageableAddressBooks: Flow<List<AddressBookEntity>> =
        addressBookDao.getUserManageableAddressBooks()

    /** Delegates photo copying to [PhotoManager]. */
    suspend fun copyContactPhoto(
        fromContactId: String,
        toContactId: String,
    ): String? =
        photoManager.copyContactPhoto(
            fromContactId,
            toContactId
        )

    private fun normalizeHref(href: String?): String? {
        if (href == null) return null
        return if (href.startsWith("/") || href.startsWith("http")) href else "/$href"
    }

    private fun assignInitialColors(
        addressBookDATs: List<DavAddressBook>,
        existingBooks: List<AddressBookEntity> = emptyList(),
        additionalUsedColors: List<Int> = emptyList(),
    ): List<AddressBookEntity> {
        val newEntities = addressBookDATs.filter { book ->
            existingBooks.none { it.href == book.href }
        }

        val matchedBooks: List<AddressBookEntity> = addressBookDATs.mapNotNull { book ->
            val existing = existingBooks.find { it.href == book.href }
            val serverColor = try {
                book.color?.toColorInt()
            } catch (_: Exception) {
                null
            }
            val resolvedColor = serverColor ?: existing?.colorInt

            resolvedColor?.let {
                AddressBookEntity(
                    href = book.href,
                    displayName = book.displayName,
                    isVisible = existing?.isVisible ?: true,
                    colorInt = it,
                    sortOrder = existing?.sortOrder ?: 0
                )
            }
        }

        val usedHues: MutableList<Float> = (matchedBooks.map { it.colorInt } + additionalUsedColors)
            .map { Color(it).toOklch().h }
            .toMutableList()

        val newBooks: List<AddressBookEntity> = newEntities
            .sortedBy { it.href }
            .map { entity ->
                val hue = farthestHue(usedHues)
                usedHues.add(hue)
                val color = oklch(
                    HueSliderDefaults.LIGHTNESS,
                    HueSliderDefaults.CHROMA,
                    hue
                ).toArgb()

                AddressBookEntity(
                    href = entity.href,
                    displayName = entity.displayName,
                    isVisible = true,
                    colorInt = color,
                    sortOrder = 0
                )
            }

        return matchedBooks + newBooks
    }

    private fun farthestHue(usedHues: List<Float>): Float {
        if (usedHues.isEmpty()) return 0f
        var bestHue = 0f
        var bestDistance = -1f
        var candidate = 0f
        while (candidate < 360f) {
            val distance = usedHues.minOf {
                circularHueDistance(
                    candidate,
                    it
                )
            }
            if (distance > bestDistance) {
                bestDistance = distance
                bestHue = candidate
            }
            candidate += 1f
        }
        return bestHue
    }

    private fun circularHueDistance(
        a: Float,
        b: Float,
    ): Float {
        val diff = abs(a - b) % 360f
        return min(
            diff,
            360f - diff
        )
    }

    /**
     * Syncs contacts with the remote server.
     */
    suspend fun syncContacts(): Result<SyncRepairStats> = syncMutex.withLock {
        val credentials = authRepository.credentials.first() ?: run {
            Log.e(
                TAG,
                "Sync failed: Not logged in"
            )
            return Result.failure(Exception("Not logged in"))
        }
        val includeCountryCode = settingsRepository.alwaysAddCountryCode.first()
        val autoLoadRemotePhotos = settingsRepository.autoLoadRemotePhotos.first()

        return try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            val addressBookEntities = syncAddressBookList(
                service,
                credentials
            )

            val allEntitiesMap = mutableMapOf<String, ContactEntity>()
            var photoRepairCount = 0
            var phoneRepairCount = 0

            for (book in addressBookEntities) {
                try {
                    val bookPath = book.href
                        .removePrefix(credentials.serverUrl)
                        .removePrefix("/")
                    val reportResponse = service.report(
                        bookPath,
                        body = createReportBody()
                    )

                    if (reportResponse.isSuccessful) {
                        val rawXml = reportResponse
                            .body()
                            ?.string() ?: continue
                        DavParser
                            .parseContacts(rawXml)
                            .forEach { davContact ->
                                try {
                                    Ezvcard
                                        .parse(davContact.vcardData)
                                        .all()
                                        .forEach { vcard ->
                                            val entity = vCardMapper.mapVCardToEntity(
                                                vcard,
                                                normalizeHref(book.href)!!,
                                                normalizeHref(davContact.href)!!,
                                                davContact.etag,
                                            )

                                            var finalEntity = downloadPhotoIfNeeded(
                                                entity,
                                                autoLoadRemotePhotos
                                            )

                                            val isReadOnly = isReadOnlyAddressBook(
                                                book.href,
                                                book.displayName
                                            )

                                            val phonesToFormat = finalEntity.phones ?: emptyList()
                                            val formattedPhones = phonesToFormat.map {
                                                it.copy(
                                                    value = PhoneFormatter.format(
                                                        it.value,
                                                        includeCountryCode,
                                                        context,
                                                        it.region
                                                    )
                                                )
                                            }
                                            if (phonesToFormat != formattedPhones) {
                                                if (!isReadOnly) phoneRepairCount++
                                                finalEntity =
                                                    finalEntity.copy(phones = formattedPhones)
                                            }

                                            if (finalEntity.photoUrl != entity.photoUrl || finalEntity.phones != entity.phones) {
                                                if (!isReadOnly) {
                                                    if (finalEntity.photoUrl != entity.photoUrl) photoRepairCount++
                                                    uploadContact(
                                                        finalEntity,
                                                        credentials
                                                    )
                                                }
                                            }

                                            val existingInCurrentSync =
                                                allEntitiesMap[finalEntity.id]
                                            if (existingInCurrentSync != null) {
                                                val existingBook =
                                                    addressBookEntities.find { it.href == existingInCurrentSync.addressBookHref }
                                                if (existingBook?.isVisible == true && !book.isVisible) return@forEach
                                            }
                                            allEntitiesMap[finalEntity.id] = finalEntity
                                        }
                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Error parsing contact ${davContact.href}: ${e.message}"
                                    )
                                }
                            }
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error syncing address book ${book.displayName}: ${e.message}"
                    )
                }
            }

            updateLocalDatabase(allEntitiesMap.values.toList())
            SingleContactWidget.updateAll(context)

            Result.success(
                SyncRepairStats(
                    photoRepairCount,
                    phoneRepairCount
                )
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "CRITICAL SYNC ERROR: ${e.message}",
                e
            )
            Result.failure(e)
        }
    }

    private suspend fun uploadContact(
        contact: ContactEntity,
        credentials: NextcloudCredentials,
    ) {
        try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            val vcard = vCardMapper.mapEntityToVCard(contact)
            val vcardString = Ezvcard
                .write(vcard)
                .version(VCardVersion.V4_0)
                .go()

            val fullPath = contact.contactHref
                ?.removePrefix(credentials.serverUrl)
                ?.removePrefix("/")
                ?: "remote.php/dav/addressbooks/users/${credentials.username}/contacts/${contact.id}.vcf"

            val response = service.putContactByPath(
                fullPath,
                vcardString.toRequestBody("text/vcard".toMediaType())
            )
            if (!response.isSuccessful) {
                Log.e(
                    TAG,
                    "Failed to upload repaired contact ${contact.id}: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to upload repaired contact ${contact.id}: ${e.message}"
            )
        }
    }

    @SuppressLint("UseKtx")
    private suspend fun syncAddressBookList(
        service: NextcloudService,
        credentials: NextcloudCredentials,
    ): List<AddressBookEntity> {
        val listBooksPath = discoverAddressBookHomeSetPath(
            service,
            credentials
        )
        val listBooksXml = service
            .propfind(
                listBooksPath,
                depth = 1,
                body = createPropfindBody(
                    listOf(
                        "d:displayname",
                        "d:resourcetype",
                        "ical:addressbook-color"
                    ),
                    "DAV:",
                    "http://apple.com/ns/ical/"
                )
            )
            .body()
            ?.string() ?: throw Exception("Failed Step 3")

        val addressBooks = DavParser
            .parseAddressBooks(listBooksXml)
            .filter {
                !isIgnoredAddressBook(
                    it.href,
                    it.displayName
                )
            }

        val existingBooks = addressBookDao
            .getAllAddressBooks()
            .first()

        val serverHrefs = addressBooks
            .map { it.href }
            .toSet()
        val booksToDelete = existingBooks.filter { !serverHrefs.contains(it.href) && !it.isLocal }
        if (booksToDelete.isNotEmpty()) {
            booksToDelete.forEach { book -> contactDao.deleteContactsByAddressBook(book.href) }
            addressBookDao.deleteAddressBooks(booksToDelete)
        }

        val localBookColors = existingBooks
            .filter { it.isLocal }
            .map { it.colorInt }
        val addressBookEntities = assignInitialColors(
            addressBooks,
            existingBooks,
            localBookColors
        )
        addressBookDao.insertAddressBooks(addressBookEntities)
        return addressBookEntities
    }

    private suspend fun discoverAddressBookHomeSetPath(
        service: NextcloudService,
        credentials: NextcloudCredentials,
    ): String {
        val principalXml = service
            .getPrincipal(body = createPropfindBody("current-user-principal"))
            .body()
            ?.string() ?: throw Exception("Failed Step 1")
        val principalHref =
            DavParser.parsePrincipal(principalXml)?.href ?: throw Exception("No Principal Href")

        val homeSetPath = principalHref
            .removePrefix(credentials.serverUrl)
            .removePrefix("/")
        val homeSetXml = service
            .propfind(
                homeSetPath,
                body = createPropfindBody(
                    "card:addressbook-home-set",
                    "DAV:",
                    "urn:ietf:params:xml:ns:carddav"
                )
            )
            .body()
            ?.string() ?: throw Exception("Failed Step 2")
        val homeSetHref = DavParser.parsePrincipal(homeSetXml)?.addressbookHomeSetHref
            ?: throw Exception("No Home Set")

        return homeSetHref
            .removePrefix(credentials.serverUrl)
            .removePrefix("/")
    }

    private fun createPropfindBody(
        prop: String,
        vararg namespaces: String,
    ): RequestBody {
        val tag = if (prop.contains(":")) prop else "d:$prop"
        return createPropfindBody(
            listOf(tag),
            *namespaces
        )
    }

    private fun createPropfindBody(
        props: List<String>,
        vararg namespaces: String,
    ): RequestBody {
        val nsString = namespaces
            .distinct()
            .joinToString(" ") { ns ->
                when (ns) {
                    "DAV:" -> "xmlns:d=\"DAV:\""
                    "urn:ietf:params:xml:ns:carddav" -> "xmlns:card=\"urn:ietf:params:xml:ns:carddav\""
                    "http://apple.com/ns/ical/" -> "xmlns:ical=\"http://apple.com/ns/ical/\""
                    else -> ""
                }
            }
            .ifBlank { "xmlns:d=\"DAV:\"" }

        val propsString = props.joinToString("\n") { "<$it />" }
        val body = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <d:propfind $nsString>
                <d:prop>
                    ${
            propsString
                .prependIndent("        ")
                .trimStart()
        }
                </d:prop>
            </d:propfind>
        """
            .trimIndent()
            .trim()
        return body.toRequestBody("application/xml".toMediaType())
    }

    private fun createReportBody() = """
        <?xml version="1.0" encoding="UTF-8"?>
        <card:addressbook-query xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
            <d:prop>
                <d:getetag />
                <card:address-data />
            </d:prop>
        </card:addressbook-query>
    """
        .trimIndent()
        .trim()
        .toRequestBody("application/xml".toMediaType())

    private fun createMkcolAddressBookBody(displayName: String): RequestBody = """
        <?xml version="1.0" encoding="UTF-8"?>
        <mkcol xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
            <set>
                <prop>
                    <resourcetype>
                        <collection/>
                        <card:addressbook/>
                    </resourcetype>
                    <displayname>${escapeXml(displayName)}</displayname>
                </prop>
            </set>
        </mkcol>
    """
        .trimIndent()
        .trim()
        .toRequestBody("application/xml".toMediaType())

    private fun createProppatchDisplayNameBody(displayName: String): RequestBody = """
        <?xml version="1.0" encoding="UTF-8"?>
        <propertyupdate xmlns="DAV:">
            <set>
                <prop>
                    <displayname>${escapeXml(displayName)}</displayname>
                </prop>
            </set>
        </propertyupdate>
    """
        .trimIndent()
        .trim()
        .toRequestBody("application/xml".toMediaType())

    private fun escapeXml(value: String): String = value
        .replace(
            "&",
            "&amp;"
        )
        .replace(
            "<",
            "&lt;"
        )
        .replace(
            ">",
            "&gt;"
        )

    private fun slugifyAddressBookName(name: String): String = name
        .lowercase()
        .replace(
            Regex("[^a-z0-9]+"),
            "-"
        )
        .trim('-')

    private suspend fun updateLocalDatabase(allEntities: List<ContactEntity>) {
        val serverIds = allEntities
            .map { it.id }
            .toSet()
        val localContacts = contactDao.getAllContactsSync()
        val toDelete = localContacts.filter {
            !serverIds.contains(it.contact.id) &&
                    it.contact.addressBookHref?.let { href -> isLocalAddressBook(href) } != true
        }

        contactDao.insertContacts(allEntities)
        toDelete.forEach { contactDao.deleteContact(it.contact) }
    }

    /** Logs out the user and clears session state. */
    suspend fun logout() {
        authRepository.clearCredentials()
        contactDao.clearAllExceptLocal()
        addressBookDao.clearAllExceptLocal()
        settingsRepository.apply {
            saveSelfContactId(null)
            saveLastOnboardedAccountKey(null)
            saveLocalOnlyMode(false)
            saveLocalOnboardingCompleted(false)
        }
    }

    /** Deletes all app data (contacts and address books) and resets session-specific state. */
    suspend fun deleteAllLocalData() {
        contactDao.clearAll()
        addressBookDao.clearAll()
        photoManager.deleteAllPhotos()
        settingsRepository.apply {
            saveSelfContactId(null)
            saveLocalOnlyMode(false)
            saveLocalOnboardingCompleted(false)
            saveLastOnboardedAccountKey(null)
            clearResolvedLocalBookHrefs()
            saveGroupOrder(emptyList())
        }
    }

    /** Saves a contact locally or to the server. */
    suspend fun saveContact(contact: ContactEntity): Result<Unit> = syncMutex.withLock {
        val credentials = authRepository.credentials.first()
        val includeCountryCode = settingsRepository.alwaysAddCountryCode.first()

        val contactIsLocal = contact.addressBookHref?.let { isLocalAddressBook(it) } == true
        if (credentials == null || contactIsLocal) {
            return saveContactLocally(
                contact,
                includeCountryCode
            )
        }

        return try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            var updatedContact = contact

            if (updatedContact.addressBookHref == null) {
                val availableBooks = addressBookDao
                    .getAllAddressBooks()
                    .first()
                val targetBook =
                    availableBooks.find { it.isVisible } ?: availableBooks.firstOrNull()
                if (targetBook != null) {
                    updatedContact =
                        updatedContact.copy(addressBookHref = normalizeHref(targetBook.href))
                }
            } else {
                updatedContact =
                    updatedContact.copy(addressBookHref = normalizeHref(updatedContact.addressBookHref))
            }

            val phones = updatedContact.phones ?: emptyList()
            val formattedPhones = phones.map {
                it.copy(
                    value = PhoneFormatter.format(
                        it.value,
                        includeCountryCode,
                        context,
                        it.region
                    )
                )
            }
            updatedContact = updatedContact.copy(phones = formattedPhones)

            if (updatedContact.photoUrl?.startsWith("file://") == true) {
                val clean = updatedContact.photoUrl.substringBefore("?")
                updatedContact =
                    updatedContact.copy(photoUrl = "$clean?t=${System.currentTimeMillis()}")
            }

            val vcard = vCardMapper.mapEntityToVCard(updatedContact)
            val vcardString = Ezvcard
                .write(vcard)
                .version(VCardVersion.V4_0)
                .go()

            val fullPath = updatedContact.contactHref
                ?.removePrefix(credentials.serverUrl)
                ?.removePrefix("/")
                ?: if (updatedContact.addressBookHref != null) {
                    val base = updatedContact.addressBookHref
                        .removePrefix(credentials.serverUrl)
                        .removePrefix("/")
                    if (base.endsWith("/")) "$base${updatedContact.id}.vcf" else "$base/${updatedContact.id}.vcf"
                } else {
                    "remote.php/dav/addressbooks/users/${credentials.username}/contacts/${updatedContact.id}.vcf"
                }

            val response = service.putContactByPath(
                fullPath,
                vcardString.toRequestBody("text/vcard".toMediaType())
            )

            if (response.isSuccessful) {
                contactDao.insertContacts(
                    listOf(
                        updatedContact.copy(
                            contactHref = normalizeHref(
                                fullPath
                            )
                        )
                    )
                )
                SingleContactWidget.updateAll(context)
                Result.success(Unit)
            } else Result.failure(Exception("Server returned ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveContactLocally(
        contact: ContactEntity,
        includeCountryCode: Boolean,
    ): Result<Unit> {
        var updatedContact = contact
        if (updatedContact.addressBookHref == null) {
            val availableBooks = addressBookDao
                .getAllAddressBooks()
                .first()
            val targetBook = availableBooks.find { it.isVisible } ?: availableBooks.firstOrNull()
            if (targetBook != null) {
                updatedContact = updatedContact.copy(addressBookHref = targetBook.href)
            }
        }

        val phones = updatedContact.phones ?: emptyList()
        val formattedPhones = phones.map {
            it.copy(
                value = PhoneFormatter.format(
                    it.value,
                    includeCountryCode,
                    context,
                    it.region
                )
            )
        }
        updatedContact = updatedContact.copy(phones = formattedPhones)

        if (updatedContact.photoUrl?.startsWith("file://") == true) {
            val clean = updatedContact.photoUrl.substringBefore("?")
            updatedContact =
                updatedContact.copy(photoUrl = "$clean?t=${System.currentTimeMillis()}")
        }

        return try {
            contactDao.insertContacts(listOf(updatedContact))
            SingleContactWidget.updateAll(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Exports all contacts as a VCard string. */
    suspend fun exportAllContacts(): String {
        val allContacts = contactDao
            .getAllContactsSync()
            .map { it.contact }
        return getVCardStringForContacts(allContacts)
    }

    /** Returns VCard string for a single contact. */
    fun getVCardString(contact: ContactEntity): String {
        val vcard = vCardMapper.mapEntityToVCard(contact)
        return Ezvcard
            .write(vcard)
            .version(VCardVersion.V3_0)
            .go()
    }

    /** Returns VCard string for multiple contacts. */
    fun getVCardStringForContacts(contacts: List<ContactEntity>): String {
        val vcards = contacts.map { vCardMapper.mapEntityToVCard(it) }
        return Ezvcard
            .write(vcards)
            .version(VCardVersion.V3_0)
            .go()
    }

    /** Whether any vCard in [vcardText] references its photo by URL instead of embedding it. */
    fun vCardTextHasRemotePhotoUrls(vcardText: String): Boolean =
        Ezvcard.parse(vcardText).all().any { vcard ->
            vcard.photos.any { it.data == null && it.url != null }
        }

    /** Imports every vCard in [vcardText] into [targetAddressBookHref] as new contacts. */
    suspend fun importVCardText(
        vcardText: String,
        targetAddressBookHref: String,
        downloadRemotePhotos: Boolean,
    ): ImportResult {
        val includeCountryCode = settingsRepository.alwaysAddCountryCode.first()
        val vcards = Ezvcard.parse(vcardText).all()
        val imported = vcards.mapNotNull { vcard ->
            try {
                val entity = vCardMapper.mapVCardToEntity(
                    vcard = vcard,
                    bookHref = targetAddressBookHref,
                    contactHref = "",
                    etag = null,
                )
                val formattedPhones = entity.phones?.map {
                    it.copy(
                        value = PhoneFormatter.format(
                            it.value,
                            includeCountryCode,
                            context,
                            it.region
                        )
                    )
                }
                downloadPhotoIfNeeded(entity.copy(phones = formattedPhones), downloadRemotePhotos)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import a vCard entry: ${e.message}")
                null
            }
        }
        contactDao.insertContacts(imported)
        return ImportResult(
            imported = imported.size,
            failed = vcards.size - imported.size
        )
    }

    /** Fetches [entity]'s photo now if it's still a remote URL and [autoLoad] is true. */
    private suspend fun downloadPhotoIfNeeded(entity: ContactEntity, autoLoad: Boolean): ContactEntity {
        val url = entity.photoUrl?.takeIf { autoLoad && it.startsWith("http") } ?: return entity
        return try {
            val photoPath = photoManager.fetchPhotoAndSave(entity.id, url)
            if (photoPath != null) entity.copy(photoUrl = photoPath) else entity
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert URL photo: ${e.message}")
            entity
        }
    }

    /** Downloads every contact's still-remote photo, across all address books. */
    suspend fun downloadAllPendingRemotePhotos(): Int {
        val pending = contactDao.getContactsWithPendingRemotePhotos()
        return pending.count { downloadContactPhoto(it) }
    }

    /** Downloads a contact photo if it's external. */
    suspend fun downloadContactPhoto(contact: ContactEntity): Boolean {
        val url = contact.photoUrl?.takeIf { it.startsWith("http") } ?: return false
        val photoPath = photoManager.fetchPhotoAndSave(
            contact.id,
            url
        ) ?: return false
        return saveContact(
            contact.copy(
                photoUrl = photoPath,
                hasPhoto = true
            )
        ).isSuccess
    }

    /** Deletes a contact from the server and local storage. */
    suspend fun deleteContact(contact: ContactEntity): Result<Unit> {
        val credentials = authRepository.credentials.first()
        val contactIsLocal = contact.addressBookHref?.let { isLocalAddressBook(it) } == true
        if (credentials == null || contactIsLocal) {
            return try {
                contactDao.deleteContact(contact)
                SingleContactWidget.updateAll(context)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        val service = NextcloudApiProvider.createService(
            credentials,
            authRepository
        )
        val fullPath = contact.contactHref
            ?.removePrefix(credentials.serverUrl)
            ?.removePrefix("/")
            ?: return Result.failure(Exception("No path"))

        return try {
            if (service.deleteContactByPath(fullPath).isSuccessful) {
                contactDao.deleteContact(contact)
                SingleContactWidget.updateAll(context)
                Result.success(Unit)
            } else Result.failure(Exception("Delete failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Updates visibility for an address book. */
    suspend fun updateAddressBookVisibility(addressBook: AddressBookEntity) =
        addressBookDao.updateAddressBook(addressBook)

    /**
     * Transitions the app into local-only mode.
     * Ensures any lingering server data is cleared first.
     */
    suspend fun enterLocalOnlyMode() {
        if (authRepository.credentials.first() != null) return

        // Wipe lingering server data before entering local mode
        contactDao.clearAllExceptLocal()
        addressBookDao.clearAllExceptLocal()

        ensureLocalAddressBookExists()
        settingsRepository.apply {
            saveLocalOnlyMode(true)
            saveLocalOnboardingCompleted(false)
        }
    }

    /** Ensures the default local address book exists. */
    suspend fun ensureLocalAddressBookExists() {
        val entity = assignInitialColors(
            addressBookDATs = listOf(
                DavAddressBook(
                    href = DEFAULT_LOCAL_ADDRESS_BOOK_HREF,
                    displayName = context.getString(R.string.local_address_book_name)
                )
            )
        )
        addressBookDao.insertAddressBooks(entity)
    }

    /** Returns all local address books. */
    suspend fun getLocalAddressBooks(): List<AddressBookEntity> =
        addressBookDao
            .getAllAddressBooks()
            .first()
            .filter { it.isLocal }

    /** Returns the number of contacts in a specific address book. */
    suspend fun getContactCountInAddressBook(href: String): Int =
        contactDao.getContactCountInAddressBook(href)

    /** Creates a new address book. */
    suspend fun createAddressBook(
        displayName: String,
        colorInt: Int,
        forceLocal: Boolean = false,
    ): Result<AddressBookEntity> = syncMutex.withLock {
        val credentials = authRepository.credentials.first()

        if (credentials == null || forceLocal) {
            return try {
                val entity = AddressBookEntity(
                    href = "$LOCAL_ADDRESS_BOOK_PREFIX${UUID.randomUUID()}",
                    displayName = displayName,
                    colorInt = colorInt
                )
                addressBookDao.insertAddressBooks(listOf(entity))
                settingsRepository.markLocalBooksResolved(listOf(entity.href))
                Result.success(entity)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            val homeSetPath = discoverAddressBookHomeSetPath(
                service,
                credentials
            )
            val slug = slugifyAddressBookName(displayName)
            val candidateSegments = if (slug.isNotEmpty()) {
                listOf(
                    slug,
                    "$slug-${
                        UUID
                            .randomUUID()
                            .toString()
                            .take(4)
                    }",
                    UUID
                        .randomUUID()
                        .toString()
                )
            } else {
                listOf(
                    UUID
                        .randomUUID()
                        .toString()
                )
            }

            var createResponse: Response<ResponseBody>? = null
            for (segment in candidateSegments) {
                val attempt = service.mkcol(
                    "$homeSetPath$segment/",
                    body = createMkcolAddressBookBody(displayName)
                )
                if (attempt.isSuccessful) {
                    createResponse = attempt
                    break
                }
                if (attempt.code() != 405) return Result.failure(Exception("Failed to create book: ${attempt.code()}"))
            }
            if (createResponse == null) return Result.failure(Exception("Failed to create book: name taken"))

            val hrefsBeforeSync = addressBookDao
                .getAllAddressBooks()
                .first()
                .map { it.href }
                .toSet()
            syncAddressBookList(
                service,
                credentials
            )
            val newBook = addressBookDao
                .getAllAddressBooks()
                .first()
                .firstOrNull { it.href !in hrefsBeforeSync }
                ?: return Result.failure(Exception("Address book created but not found after sync"))

            val coloredBook = newBook.copy(colorInt = colorInt)
            addressBookDao.updateAddressBook(coloredBook)
            Result.success(coloredBook)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Uploads a local address book to the server. */
    suspend fun uploadLocalAddressBook(
        localBook: AddressBookEntity,
        newDisplayName: String,
    ): Result<AddressBookUploadResult> {
        if (authRepository.credentials.first() == null) return Result.failure(Exception("Not logged in"))

        val localContacts = contactDao
            .getAllContactsSync()
            .map { it.contact }
            .filter { it.addressBookHref == localBook.href }
        if (localContacts.isEmpty()) {
            addressBookDao.deleteAddressBooks(listOf(localBook))
            return Result.success(
                AddressBookUploadResult(
                    0,
                    0,
                    true
                )
            )
        }

        val newBook = createAddressBook(
            newDisplayName,
            localBook.colorInt
        ).getOrElse { return Result.failure(it) }

        var succeeded = 0
        var failed = 0
        for (contact in localContacts) {
            val uploadResult = saveContact(
                contact.copy(
                    addressBookHref = newBook.href,
                    contactHref = null,
                    etag = null
                )
            )
            if (uploadResult.isSuccess) succeeded++ else failed++
        }

        val fullyCompleted = failed == 0
        if (fullyCompleted) addressBookDao.deleteAddressBooks(listOf(localBook))
        return Result.success(
            AddressBookUploadResult(
                succeeded,
                failed,
                fullyCompleted
            )
        )
    }

    /** Renames an address book. */
    suspend fun renameAddressBook(
        addressBook: AddressBookEntity,
        newDisplayName: String,
    ): Result<Unit> {
        val credentials = authRepository.credentials.first()
        if (credentials == null || addressBook.isLocal) {
            return try {
                addressBookDao.updateDisplayName(
                    addressBook.href,
                    newDisplayName
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            val path = addressBook.href
                .removePrefix(credentials.serverUrl)
                .removePrefix("/")
            val response = service.proppatch(
                path,
                body = createProppatchDisplayNameBody(newDisplayName)
            )
            if (!response.isSuccessful) return Result.failure(Exception("Failed to rename: ${response.code()}"))

            addressBookDao.updateDisplayName(
                addressBook.href,
                newDisplayName
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAddressBook(addressBook: AddressBookEntity): Result<Unit> {
        val credentials = authRepository.credentials.first()
        if (credentials == null || addressBook.isLocal) {
            return try {
                contactDao.deleteContactsByAddressBook(addressBook.href)
                addressBookDao.deleteAddressBooks(listOf(addressBook))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            val service = NextcloudApiProvider.createService(
                credentials,
                authRepository
            )
            val path = addressBook.href
                .removePrefix(credentials.serverUrl)
                .removePrefix("/")
            val response = service.deleteCollection(path)
            if (!response.isSuccessful) return Result.failure(Exception("Failed to delete: ${response.code()}"))

            contactDao.deleteContactsByAddressBook(addressBook.href)
            addressBookDao.deleteAddressBooks(listOf(addressBook))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Synchronously returns a contact by ID. */
    suspend fun getContactByIdSync(id: String) = contactDao.getContactById(id)

    /** Moves a contact to another address book. */
    suspend fun moveContact(
        contact: ContactEntity,
        targetAddressBookHref: String,
    ): Result<Unit> {
        val sourceIsLocal = contact.addressBookHref?.let { isLocalAddressBook(it) } == true
        val targetIsLocal = isLocalAddressBook(targetAddressBookHref)

        if (sourceIsLocal || targetIsLocal) {
            val oldContactHref = contact.contactHref
            val hadServerCopy = !sourceIsLocal && oldContactHref != null
            val result = saveContact(
                contact.copy(
                    addressBookHref = targetAddressBookHref,
                    contactHref = null,
                    etag = null
                )
            )

            if (result.isSuccess && hadServerCopy) {
                val credentials = authRepository.credentials.first()
                if (credentials != null) {
                    try {
                        val service = NextcloudApiProvider.createService(
                            credentials,
                            authRepository
                        )
                        val path = oldContactHref
                            .removePrefix(credentials.serverUrl)
                            .removePrefix("/")
                        service.deleteContactByPath(path)
                    } catch (_: Exception) {
                    }
                }
            }
            return result
        }

        val credentials =
            authRepository.credentials.first() ?: return Result.failure(Exception("Not logged in"))
        val service = NextcloudApiProvider.createService(
            credentials,
            authRepository
        )

        return try {
            val sourcePath = contact.contactHref
                ?.removePrefix(credentials.serverUrl)
                ?.removePrefix("/")
                ?: return Result.failure(Exception("No source"))
            val filename = sourcePath.substringAfterLast("/")
            val destBase =
                if (targetAddressBookHref.startsWith("/")) targetAddressBookHref else "/${
                    targetAddressBookHref
                        .removePrefix(credentials.serverUrl)
                        .removePrefix("/")
                }"
            val destPath =
                if (destBase.endsWith("/")) "$destBase$filename" else "$destBase/$filename"

            if (service.move(
                    sourcePath,
                    destPath
                ).isSuccessful
            ) {
                val updated = contact.copy(
                    addressBookHref = targetAddressBookHref,
                    contactHref = destPath
                )
                contactDao.insertContacts(listOf(updated))
                Result.success(Unit)
            } else Result.failure(Exception("Move failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Archives a contact. */
    suspend fun archiveContact(contact: ContactEntity) =
        saveContact(contact.copy(isArchived = true))

    /** Unarchives a contact. */
    suspend fun unarchiveContact(contact: ContactEntity) =
        saveContact(contact.copy(isArchived = false))

    /** Observes a contact by ID. */
    fun observeContactById(contactId: ContactId): Flow<ContactWithAddressBook?> =
        contactDao.getContactByIdFlow(contactId)

    companion object {
        const val FAVORITE_CATEGORY = "Favorites"
        private const val TAG = "ContactsRepository"

        // Keep in sync with `clearAllExceptLocal()` in `ContactDao.kt`
        const val LOCAL_ADDRESS_BOOK_PREFIX = "local://"
        const val DEFAULT_LOCAL_ADDRESS_BOOK_HREF = "${LOCAL_ADDRESS_BOOK_PREFIX}contacts"

        fun isLocalAddressBook(href: String): Boolean = href.startsWith(LOCAL_ADDRESS_BOOK_PREFIX)

        private fun isIgnoredAddressBook(
            href: String,
            displayName: String?,
        ): Boolean {
            val indicators = listOf(
                "z-server",
                "z-app-generated",
                "/addressbooks/system/",
                "/system/",
                "system"
            )
            return indicators.any {
                href.contains(
                    it,
                    ignoreCase = true
                ) || displayName?.contains(
                    it,
                    ignoreCase = true
                ) == true
            }
        }

        private fun isReadOnlyAddressBook(
            href: String,
            displayName: String?,
        ): Boolean = isIgnoredAddressBook(
            href,
            displayName
        )
    }
}
