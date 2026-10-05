// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.di

import android.content.Context
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactId
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.PhotoManager
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.data.repository.VCardMapper
import dev.benica.corvidcontacts.data.system.SystemContactsEditor
import dev.benica.corvidcontacts.data.system.SystemContactsMirror
import dev.benica.corvidcontacts.data.system.SystemContactsMirrorManager

/**
 * Dependency injection container for the application.
 * Provides singleton instances of repositories and databases.
 */
class AppContainer(context: Context) {
    val authRepository = AuthRepository(context)
    val settingsRepository = SettingsRepository(context)
    val geocoderRepository = GeocoderRepository(
        context,
        settingsRepository
    )
    private val database = AppDatabase.getDatabase(context)
    val photoManager = PhotoManager(context)
    val vCardMapper = VCardMapper(photoManager)
    val contactsRepository = ContactsRepository(
        context,
        database.contactDao(),
        database.addressBookDao(),
        authRepository,
        settingsRepository,
        photoManager,
        vCardMapper
    )

    val systemContactMirrorDao = database.systemContactMirrorDao()

    val systemContactsMirrorManager = SystemContactsMirrorManager(
        context,
        database.systemContactMirrorDao(),
        SystemContactsMirror(
            context,
            database.systemContactMirrorDao(),
            photoManager,
            object : SystemContactsEditor {
                override suspend fun get(id: ContactId): ContactEntity? =
                    database.contactDao().getContactById(id)?.contact

                override suspend fun save(contact: ContactEntity): Result<Unit> =
                    contactsRepository.saveContact(contact)
            }
        )
    )
}
