// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import android.app.Application
import com.google.android.libraries.places.api.Places
import dev.benica.corvidcontacts.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Custom [Application] class for the app.
 * Initializes the [AppContainer] for dependency injection.
 */
class CorvidContactsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        instance = this

        // Keeps the opt-in system contacts mirror current for as long as the process lives.
        container.systemContactsMirrorManager.start(
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )

        // Initialize Places SDK (New). Skipped without an API key (none in local.properties), since
        // initializing with an empty key crashes; Places is only used if the user picks it for
        // address lookup.
        if (BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank() && !Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                this,
                BuildConfig.GOOGLE_PLACES_API_KEY
            )
        }
    }

    companion object {
        lateinit var instance: CorvidContactsApplication
            private set
    }
}
