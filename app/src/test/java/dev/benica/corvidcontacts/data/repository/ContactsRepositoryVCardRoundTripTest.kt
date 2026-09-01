// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.data.local.AddressBookDao
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactDao
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress
import ezvcard.Ezvcard
import ezvcard.VCardVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Round-trip fidelity tests for [ContactsRepository.mapEntityToVCard] /
 * [ContactsRepository.mapVCardToEntity].
 *
 * These tests exist to answer a specific question: does *this app's own* vCard read/write path
 * ever lose data on its own, independent of anything the server does? A [ContactEntity] is
 * converted to a vCard, written to text (exactly like [ContactsRepository.saveContact] does),
 * re-parsed, and converted back to a [ContactEntity] (exactly like a sync would). If fields differ
 * before and after, the bug is in this app's mapping code, not in the server or in "sync healing."
 *
 * Runs under Robolectric (rather than a plain JVM unit test) because the mapping functions touch
 * a handful of real Android APIs along the way ([Context.getFilesDir] for photo caching,
 * [android.util.Base64], [android.net.Uri]) that throw "not mocked" under the default Android
 * unit-test stubs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsRepositoryVCardRoundTripTest {

    private lateinit var repository: ContactsRepository
    private lateinit var vCardMapper: VCardMapper
    private lateinit var photoManager: PhotoManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        photoManager = PhotoManager(context)
        vCardMapper = VCardMapper(photoManager)
        repository = ContactsRepository(
            context = context,
            contactDao = NoOpContactDao(),
            addressBookDao = NoOpAddressBookDao(),
            authRepository = AuthRepository(context),
            settingsRepository = SettingsRepository(context),
            photoManager = photoManager,
            vCardMapper = vCardMapper
        )
    }

    @Test
    fun `round trip preserves names, phones, emails, addresses, and other fields`() {
        val original = ContactEntity(
            id = "test-id-1",
            displayName = "",
            firstName = "Johnny",
            lastName = "Appleseed",
            middleName = "Middle",
            prefix = "Dr.",
            suffix = "Jr.",
            emails = listOf(
                Email(
                    "home@example.com",
                    "HOME"
                ),
                Email(
                    "work@example.com",
                    "WORK"
                ),
            ),
            phones = listOf(
                Phone(
                    "+15551234567",
                    "MOBILE"
                ),
                Phone(
                    "+15559876543",
                    "HOME"
                ),
            ),
            photoUrl = null,
            hasPhoto = false,
            etag = "orig-etag",
            addressBookHref = "/original/",
            contactHref = "/original/test-id-1.vcf",
            colorInt = -65536,
            categories = listOf(
                "Friends",
                "VIP"
            ),
            company = "Acme Corp",
            jobTitle = "Engineer",
            birthday = "1990-01-15",
            nickname = "Johnny Apple",
            notes = "Met at a conference.",
            websites = listOf("https://example.com"),
            socialProfiles = listOf(
                SocialProfile(
                    "@johnny",
                    "TWITTER"
                )
            ),
            relationships = listOf(
                Relationship(
                    "FRIEND",
                    "Jane Doe",
                    isUid = false
                )
            ),
            structuredAddresses = listOf(
                StructuredAddress(
                    type = "HOME",
                    street = "123 Main St",
                    city = "Springfield",
                    state = "IL",
                    postalCode = "62704",
                    country = "USA"
                )
            )
        )

        val roundTripped = roundTrip(original)

        assertEquals(
            original.firstName,
            roundTripped.firstName
        )
        assertEquals(
            original.lastName,
            roundTripped.lastName
        )
        assertEquals(
            original.middleName,
            roundTripped.middleName
        )
        assertEquals(
            original.prefix,
            roundTripped.prefix
        )
        assertEquals(
            original.suffix,
            roundTripped.suffix
        )
        assertEquals(
            original.emails?.toSet(),
            roundTripped.emails?.toSet()
        )
        assertEquals(
            original.phones?.toSet(),
            roundTripped.phones?.toSet()
        )
        assertEquals(
            original.categories?.toSet(),
            roundTripped.categories?.toSet()
        )
        assertEquals(
            original.company,
            roundTripped.company
        )
        assertEquals(
            original.jobTitle,
            roundTripped.jobTitle
        )
        assertEquals(
            original.birthday,
            roundTripped.birthday
        )
        assertEquals(
            original.nickname,
            roundTripped.nickname
        )
        assertEquals(
            original.notes,
            roundTripped.notes
        )
        assertEquals(
            original.websites,
            roundTripped.websites
        )
        assertEquals(
            original.socialProfiles,
            roundTripped.socialProfiles
        )
        assertEquals(
            original.relationships,
            roundTripped.relationships
        )
        assertEquals(
            original.structuredAddresses,
            roundTripped.structuredAddresses
        )
        assertEquals(
            original.colorInt,
            roundTripped.colorInt
        )
    }

    @Test
    fun `round trip preserves an embedded photo losslessly`() {
        val photoBytes = ByteArray(256) { it.toByte() }
        val original = minimalContact(id = "test-id-photo").copy(
            photoUrl = "data:image/jpeg;base64," +
                    Base64.encodeToString(
                        photoBytes,
                        Base64.NO_WRAP
                    )
        )

        val roundTripped = roundTrip(original)

        val photoUrl = roundTripped.photoUrl
        assertNotNull(
            "Expected a photo URL after round-tripping",
            photoUrl
        )
        val cleanPath = photoUrl!!
            .substringBefore("?")
            .removePrefix("file://")
        val bytesOnDisk = File(cleanPath).readBytes()
        assertArrayEquals(
            photoBytes,
            bytesOnDisk
        )
    }

    @Test
    fun `explicit display name override survives round trip when it differs from the computed name`() {
        val original = minimalContact(id = "test-id-display").copy(
            displayName = "The Big Cheese",
            firstName = "Norm",
            lastName = "Cheeseman"
        )

        val roundTripped = roundTrip(original)

        assertEquals(
            "The Big Cheese",
            roundTripped.getEffectiveDisplayName()
        )
    }

    /** Mirrors what [ContactsRepository.saveContact] and a sync cycle actually do: entity -> text -> entity. */
    private fun roundTrip(entity: ContactEntity): ContactEntity {
        val vcard = vCardMapper.mapEntityToVCard(entity)
        val vcardString = Ezvcard
            .write(vcard)
            .version(VCardVersion.V4_0)
            .go()
        val reparsed = Ezvcard
            .parse(vcardString)
            .first()
        return vCardMapper.mapVCardToEntity(
            reparsed,
            entity.addressBookHref ?: "/book/",
            entity.contactHref ?: "/book/${entity.id}.vcf",
            entity.etag
        )
    }

    private fun minimalContact(id: String) = ContactEntity(
        id = id,
        displayName = "",
        firstName = "First",
        lastName = "Last",
        emails = emptyList(),
        phones = emptyList(),
        photoUrl = null,
        etag = "etag"
    )
}

/** Unused by the mapping functions under test; only exists to satisfy [ContactsRepository]'s constructor. */
private class NoOpContactDao : ContactDao {
    override fun getAllVisibleContacts(): Flow<List<ContactWithAddressBook>> = flowOf(emptyList())
    override fun getArchivedContacts(): Flow<List<ContactWithAddressBook>> = flowOf(emptyList())
    override fun getAllContacts(): Flow<List<ContactWithAddressBook>> = flowOf(emptyList())
    override suspend fun getAllContactsSync(): List<ContactWithAddressBook> = emptyList()
    override suspend fun getContactById(id: String): ContactWithAddressBook? = null
    override suspend fun insertContacts(contacts: List<ContactEntity>) = Unit
    override suspend fun deleteContact(contact: ContactEntity) = Unit
    override suspend fun deleteContactsByAddressBook(addressBookHref: String) = Unit
    override suspend fun clearAll() = Unit
    override suspend fun clearAllExceptLocal() = Unit
    override fun getContactByIdFlow(contactId: ContactId): Flow<ContactWithAddressBook?> =
        flowOf(null)
}

/** Unused by the mapping functions under test; only exists to satisfy [ContactsRepository]'s constructor. */
private class NoOpAddressBookDao : AddressBookDao {
    override fun getAllAddressBooks(): Flow<List<AddressBookEntity>> = flowOf(emptyList())
    override fun getUserManageableAddressBooks(): Flow<List<AddressBookEntity>> =
        flowOf(emptyList())

    override suspend fun insertAddressBooks(addressBooks: List<AddressBookEntity>) = Unit
    override suspend fun updateAddressBook(addressBook: AddressBookEntity) = Unit
    override suspend fun deleteAddressBooks(addressBooks: List<AddressBookEntity>) = Unit
    override suspend fun clearAll() = Unit
    override suspend fun clearAllExceptLocal() = Unit
    override suspend fun updateDisplayName(
        href: String,
        displayName: String?,
    ) = Unit
}
