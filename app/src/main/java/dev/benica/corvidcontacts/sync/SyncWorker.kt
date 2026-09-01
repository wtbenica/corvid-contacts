// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.benica.corvidcontacts.data.local.AppDatabase
import dev.benica.corvidcontacts.data.repository.AuthRepository
import dev.benica.corvidcontacts.data.repository.ContactsRepository
import dev.benica.corvidcontacts.data.repository.PhotoManager
import dev.benica.corvidcontacts.data.repository.SettingsRepository
import dev.benica.corvidcontacts.data.repository.VCardMapper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(
    context,
    params
) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val authRepository = AuthRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val photoManager = PhotoManager(applicationContext)
        val vCardMapper = VCardMapper(photoManager)
        val contactsRepository = ContactsRepository(
            context = applicationContext,
            contactDao = database.contactDao(),
            addressBookDao = database.addressBookDao(),
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            photoManager = photoManager,
            vCardMapper = vCardMapper
        )

        // No account configured: nothing to sync.
        if (authRepository.credentials.first() == null) {
            return Result.success()
        }

        return try {
            val result = contactsRepository.syncContacts()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "periodic_contacts_sync"

        fun startPeriodicSync(context: Context) {
            val constraints = Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName = SYNC_WORK_NAME,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
                    request = syncRequest
                )
        }
    }
}
