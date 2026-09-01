// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import dev.benica.corvidcontacts.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Clears saved credentials via [authRepository] whenever a request comes back `401 Unauthorized`,
 * so an expired session drops the user back to [dev.benica.corvidcontacts.navigation.Destination.Login]
 * (routed reactively by `MainViewModel` off [AuthRepository.credentials]) instead of leaving them
 * stuck on a screen that can never load.
 */
class SessionExpiryInterceptor(
    private val authRepository: AuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            runBlocking { authRepository.clearCredentials() }
        }
        return response
    }
}
