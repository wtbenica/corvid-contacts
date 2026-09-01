// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val username: String,
    private val appPassword: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain
            .request()
            .newBuilder()
            .addHeader(
                "Authorization",
                Credentials.basic(
                    username,
                    appPassword
                )
            )
            .addHeader(
                "OCS-APIRequest",
                "true"
            ) // Required for CardDAV OCS API endpoints
            .build()
        return chain.proceed(request)
    }
}
