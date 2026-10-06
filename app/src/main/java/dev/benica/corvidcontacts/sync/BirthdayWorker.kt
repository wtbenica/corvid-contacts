// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AppDatabase
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks for upcoming contact birthdays once per day.
 * If a birthday is today or tomorrow, it triggers a local notification.
 * See [BirthdayDates] for the date forms it reads.
 */
class BirthdayWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(
    context,
    params
) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(
                TAG,
                "Checking for birthdays..."
            )
            val database = AppDatabase.getDatabase(applicationContext)
            val contacts = database
                .contactDao()
                .getAllContactsSync()

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            contacts.forEach { contactWithBook ->
                val contact = contactWithBook.contact
                val birthday = BirthdayDates.monthDay(contact.birthday) ?: return@forEach
                when {
                    BirthdayDates.fallsOn(birthday, today) -> showNotification(
                        contact.getEffectiveDisplayName(),
                        applicationContext.getString(R.string.birthday_notification_today)
                    )

                    BirthdayDates.fallsOn(birthday, tomorrow) -> showNotification(
                        contact.getEffectiveDisplayName(),
                        applicationContext.getString(R.string.birthday_notification_tomorrow)
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error in BirthdayWorker: ${e.message}"
            )
            Result.failure()
        }
    }

    private fun showNotification(
        name: String,
        message: String,
    ) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.birthday_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                applicationContext.getString(R.string.birthday_notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat
            .Builder(
                applicationContext,
                CHANNEL_ID
            )
            .setSmallIcon(android.R.drawable.ic_menu_myplaces) // Placeholder icon
            .setContentTitle(applicationContext.getString(R.string.birthday_notification_title))
            .setContentText("$name $message")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            name.hashCode(),
            notification
        )
    }

    companion object {
        private const val TAG = "BirthdayWorker"
        private const val CHANNEL_ID = "birthday_reminders"
        private const val WORK_NAME = "birthday_check_work"

        /** Schedules the birthday check to run once per 24 hours. */
        fun scheduleDailyCheck(context: Context) {
            val constraints = Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<BirthdayWorker>(
                1,
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        /** Cancels the periodic birthday check, e.g. when the user disables birthday notifications. */
        fun cancelDailyCheck(context: Context) {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }
}
