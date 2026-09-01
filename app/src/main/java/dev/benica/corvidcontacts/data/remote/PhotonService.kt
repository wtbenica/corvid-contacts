// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Photon (OpenStreetMap) geocoding API.
 */
interface PhotonService {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("lang") lang: String = "en",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
    ): PhotonResponse
}

@JsonClass(generateAdapter = true)
data class PhotonResponse(
    val features: List<PhotonFeature>,
)

@JsonClass(generateAdapter = true)
data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: PhotonGeometry,
)

@JsonClass(generateAdapter = true)
data class PhotonProperties(
    val housenumber: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class PhotonGeometry(
    val coordinates: List<Double>,
)
