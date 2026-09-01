// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.di

import android.content.Context
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.GeocoderRepository
import dev.benica.corvidcontacts.data.repository.PhotoManager
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.data.repository.VCardMapper

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
}
