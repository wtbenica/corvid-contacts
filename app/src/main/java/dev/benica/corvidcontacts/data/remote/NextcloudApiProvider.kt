// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.benica.corvidcontacts.data.model.NextcloudCredentials
import dev.benica.corvidcontacts.data.repository.AuthRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NextcloudApiProvider {
    private val moshi = Moshi
        .Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * @param authRepository Used to clear the saved session on a `401` response, so an expired
     * app password drops the user back to the login screen instead of failing silently.
     */
    fun createService(
        credentials: NextcloudCredentials,
        authRepository: AuthRepository,
    ): NextcloudService {
        val client = OkHttpClient
            .Builder()
            .addInterceptor(
                AuthInterceptor(
                    credentials.username,
                    credentials.appPassword
                )
            )
            .addInterceptor(SessionExpiryInterceptor(authRepository))
            .build()

        return Retrofit
            .Builder()
            .baseUrl(credentials.serverUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NextcloudService::class.java)
    }
}
