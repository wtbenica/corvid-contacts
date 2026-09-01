// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.benica.corvidcontacts.navigation.Destination
import dev.benica.corvidcontacts.sync.BirthdayWorker
import dev.benica.corvidcontacts.sync.SyncWorker
import dev.benica.corvidcontacts.ui.contacts.PickContent
import dev.benica.corvidcontacts.ui.navigation.AppNavigation
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.utils.IntentParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main activity of the app. Handles:
 * - Background worker scheduling (Sync and Birthday checks, gated by the user's
 *   birthday-notifications preference set during onboarding).
 * - Incoming Intent processing (Insert contact, Share vCard).
 * - Root UI orchestration via [AppNavigation].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lock phones to portrait to avoid layout issues in dual-pane mode.
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val app = application as CorvidContactsApplication
        val container = app.container

        // Detect if this is a pick request
        val pickType =
            if (intent.action == Intent.ACTION_PICK || intent.action == Intent.ACTION_GET_CONTENT) {
                when (intent.type) {
                    "vnd.android.cursor.dir/email_v2", "vnd.android.cursor.item/email_v2" -> PickContent.EMAIL
                    "vnd.android.cursor.dir/phone_v2", "vnd.android.cursor.item/phone_v2" -> PickContent.PHONE
                    "vnd.android.cursor.dir/postal-address_v2", "vnd.android.cursor.item/postal-address_v2" -> PickContent.ADDRESS
                    else -> PickContent.ALL
                }
            } else null

        // Process incoming contact data from intents
        val initialContact = IntentParser.parse(
            intent,
            contentResolver,
            container.vCardMapper
        )

        // Handle custom setup URL schemes
        val setupUrl = if (intent?.scheme == "cccontacts" && intent.data?.host == "setup") {
            intent.data?.getQueryParameter("url")
        } else null

        // Handle contact deep link
        val initialContactId =
            if (intent?.scheme == "cccontacts" && intent.data?.host == "contact") {
                intent.data?.lastPathSegment
            } else null

        // Schedule essential background maintenance tasks
        SyncWorker.startPeriodicSync(applicationContext)
        lifecycleScope.launch {
            if (container.settingsRepository.birthdayNotificationsEnabled.first()) {
                BirthdayWorker.scheduleDailyCheck(applicationContext)
            } else {
                BirthdayWorker.cancelDailyCheck(applicationContext)
            }
        }

        val mainViewModel: MainViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        container.authRepository,
                        container.contactsRepository,
                        container.settingsRepository
                    ) as T
                }
            }
        )[MainViewModel::class.java]

        setContent {
            val startDestination by mainViewModel.startDestination.collectAsState()
            val themeMode by mainViewModel.themeMode.collectAsState()

            CorvidContactsTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val currentStartDestination = startDestination
                    if (currentStartDestination != null) {
                        // Override start destination if a setup URL is provided via deep link
                        val actualStart =
                            if (currentStartDestination is Destination.Login && setupUrl != null) {
                                Destination.Login(prefilledUrl = setupUrl)
                            } else currentStartDestination

                        key(actualStart) {
                            AppNavigation(
                                startDestination = actualStart,
                                authRepository = container.authRepository,
                                contactsRepository = container.contactsRepository,
                                settingsRepository = container.settingsRepository,
                                geocoderRepository = container.geocoderRepository,
                                onContinueLocally = mainViewModel::continueWithoutAccount,
                                initialIntentContact = initialContact,
                                pickType = pickType,
                                initialContactId = initialContactId
                            )
                        }
                    }
                }
            }
        }
    }

}
