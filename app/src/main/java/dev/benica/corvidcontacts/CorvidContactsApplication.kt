// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts

import android.app.Application
import com.google.android.libraries.places.api.Places
import dev.benica.corvidcontacts.di.AppContainer

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

        // Initialize Places SDK (New)
        if (!Places.isInitialized()) {
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
