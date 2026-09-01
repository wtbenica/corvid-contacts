// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.benica.corvidcontacts.data.model.AddressLookupMode
import dev.benica.corvidcontacts.data.model.StructuredAddress
import dev.benica.corvidcontacts.data.remote.PhotonService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale

/**
 * Repository for address lookups using Google Places SDK or Photon (OpenStreetMap).
 */
class GeocoderRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val moshi = Moshi
        .Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient by lazy {
        OkHttpClient
            .Builder()
            .build()
    }
    private val photonService: PhotonService by lazy {
        Retrofit
            .Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PhotonService::class.java)
    }

    private val placesClient: PlacesClient by lazy { Places.createClient(context) }

    private var cachedBias: Pair<Double, Double>? = null

    /**
     * Returns a (latitude, longitude) pair approximating the user's country, used to bias Photon
     * search results toward the user's region. Resolved once via [Geocoder] from the device's
     * default locale and cached for the lifetime of this repository instance.
     */
    private suspend fun getLocationBias(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        cachedBias?.let { return@withContext it }
        return@withContext try {
            val geocoder = Geocoder(context)
            val locale = Locale.getDefault()

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(
                locale.displayCountry,
                1
            )
            val bias = addresses
                ?.firstOrNull()
                ?.let {
                    // If it returns (0,0), the geocoder failed to find a real location.
                    // Return null to avoid "Null Island" bias.
                    if (it.latitude == 0.0 && it.longitude == 0.0) null
                    else it.latitude to it.longitude
                }
            cachedBias = bias
            bias
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches address autocomplete suggestions for [query], using Google Places or Photon
     * (OpenStreetMap) depending on the user's [SettingsRepository.addressLookupMode] preference.
     * Returns an empty list if [query] is blank, address lookup is disabled entirely, or the
     * underlying request fails.
     */
    suspend fun getAutocompleteSuggestions(query: String): List<AddressSuggestion> {
        if (query.isBlank()) return emptyList()
        val mode = settingsRepository.addressLookupMode.first()
        if (mode == AddressLookupMode.OFF) return emptyList()

        return if (mode == AddressLookupMode.GOOGLE) {
            getGoogleSuggestions(query)
        } else {
            getPhotonSuggestions(query)
        }
    }

    /** Fetches autocomplete predictions from the Google Places SDK for [query]. */
    private suspend fun getGoogleSuggestions(query: String): List<AddressSuggestion> {
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest
            .builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        return try {
            val response = placesClient
                .findAutocompletePredictions(request)
                .await()
            response.autocompletePredictions.map { prediction ->
                AddressSuggestion(
                    id = prediction.placeId,
                    displayTitle = prediction
                        .getPrimaryText(null)
                        .toString(),
                    displaySubtitle = prediction
                        .getSecondaryText(null)
                        .toString(),
                    source = SuggestionSource.GOOGLE
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Google Autocomplete failed",
                e
            )
            emptyList()
        }
    }

    /**
     * Fetches autocomplete suggestions from the free Photon (OpenStreetMap) API for [query],
     * biased toward the user's approximate region via [getLocationBias].
     */
    private suspend fun getPhotonSuggestions(query: String): List<AddressSuggestion> =
        withContext(Dispatchers.IO) {
            val bias = getLocationBias()
            val lang = getSystemLang()

            try {
                val response = photonService.search(
                    query,
                    lat = bias?.first,
                    lon = bias?.second,
                    lang = lang
                )
                response.features.map { feature ->
                    val p = feature.properties
                    val title = p.name ?: p.street ?: ""
                    val subtitle = listOfNotNull(
                        p.housenumber,
                        p.street,
                        p.city,
                        p.state,
                        p.country
                    )
                        .distinct()
                        .filter { it != title }
                        .joinToString(", ")

                    AddressSuggestion(
                        id = feature.geometry.coordinates.toString(), // Dummy ID for OSM
                        displayTitle = title,
                        displaySubtitle = subtitle,
                        source = SuggestionSource.PHOTON,
                        fullAddress = StructuredAddress(
                            type = "HOME",
                            street = listOfNotNull(
                                p.housenumber,
                                p.street
                            ).joinToString(" "),
                            city = p.city,
                            state = p.state,
                            postalCode = p.postcode,
                            country = p.country
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Photon suggestions failed",
                    e
                )
                emptyList()
            }
        }

    /**
     * Resolves an autocomplete [suggestion] into a full [StructuredAddress].
     *
     * For Photon suggestions, the address was already fully resolved at search time, so this
     * just returns [AddressSuggestion.fullAddress] directly. For Google suggestions, this issues
     * a follow-up Places "fetch place" request to retrieve and parse address components.
     */
    suspend fun resolveSuggestion(suggestion: AddressSuggestion): StructuredAddress? {
        if (suggestion.source == SuggestionSource.PHOTON) return suggestion.fullAddress

        val fields = listOf(
            Place.Field.ADDRESS_COMPONENTS,
            Place.Field.DISPLAY_NAME
        )
        val request = FetchPlaceRequest.newInstance(
            suggestion.id,
            fields
        )

        return try {
            val response = placesClient
                .fetchPlace(request)
                .await()
            val place = response.place
            val components = place.addressComponents?.asList() ?: return null

            var streetNumber = ""
            var route = ""
            var city = ""
            var state = ""
            var zip = ""
            var country = ""

            for (component in components) {
                when {
                    component.types.contains("street_number") -> streetNumber = component.name
                    component.types.contains("route") -> route = component.name
                    component.types.contains("locality") -> city = component.name
                    component.types.contains("administrative_area_level_1") -> state =
                        component.shortName ?: component.name

                    component.types.contains("postal_code") -> zip = component.name
                    component.types.contains("country") -> country = component.name
                }
            }

            StructuredAddress(
                type = "HOME",
                street = listOfNotNull(
                    streetNumber,
                    route
                )
                    .joinToString(" ")
                    .trim(),
                city = city.ifBlank { null },
                state = state.ifBlank { null },
                postalCode = zip.ifBlank { null },
                country = country.ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Place resolution failed",
                e
            )
            null
        }
    }

    /** Returns the device's language if Photon supports it, else falls back to `"en"`. */
    private fun getSystemLang(): String {
        val lang = Locale.getDefault().language
        return if (lang in listOf(
                "en",
                "de",
                "fr",
                "it"
            )
        ) lang else "en"
    }

    companion object {
        private const val TAG = "GeocoderRepository"

        /**
         * The official website for the Photon project.
         */
        const val PHOTON_DATA_HANDLING_URL = "https://photon.komoot.io/"
    }
}

/**
 * A single address autocomplete result shown to the user while typing an address.
 *
 * @param id Provider-specific identifier: a Google Places `placeId` for [SuggestionSource.GOOGLE]
 *   suggestions, or a dummy coordinate-based string for [SuggestionSource.PHOTON] ones.
 * @param displayTitle Primary line shown in the suggestion list (e.g. street or place name).
 * @param displaySubtitle Secondary line shown below [displayTitle] (e.g. city/region).
 * @param source Which provider produced this suggestion.
 * @param fullAddress The fully-resolved address, pre-populated for Photon suggestions since Photon
 *   returns complete address data up front. `null` for Google suggestions, which require a
 *   separate [GeocoderRepository.resolveSuggestion] call to fetch full details.
 */
data class AddressSuggestion(
    val id: String,
    val displayTitle: String,
    val displaySubtitle: String,
    val source: SuggestionSource,
    val fullAddress: StructuredAddress? = null,
)

/** Identifies which address-lookup provider produced an [AddressSuggestion]. */
enum class SuggestionSource { GOOGLE, PHOTON }
