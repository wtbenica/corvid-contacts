// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface NextcloudService {
    @GET("ocs/v2.php/cloud/user?format=json")
    suspend fun getUserInfo(): OcsResponse<UserInfo>

    @PUT("{path}")
    suspend fun putContactByPath(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Body vcard: okhttp3.RequestBody,
    ): retrofit2.Response<Unit>

    @DELETE("{path}")
    suspend fun deleteContactByPath(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
    ): retrofit2.Response<Unit>

    /** Deletes a WebDAV collection (e.g. an address book) and everything inside it. Functionally
     *  identical to [deleteContactByPath] - split out only so call sites read clearly. */
    @DELETE("{path}")
    suspend fun deleteCollection(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
    ): retrofit2.Response<Unit>

    /** Creates a new WebDAV collection at [path] via Extended MKCOL (RFC 5689), e.g. a new address book. */
    @HTTP(
        method = "MKCOL",
        path = "{path}",
        hasBody = true
    )
    suspend fun mkcol(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    /** Updates WebDAV properties (e.g. `displayname`) on an existing resource at [path] via PROPPATCH. */
    @HTTP(
        method = "PROPPATCH",
        path = "{path}",
        hasBody = true
    )
    suspend fun proppatch(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(
        method = "PROPFIND",
        path = "remote.php/dav/",
        hasBody = true
    )
    suspend fun getPrincipal(
        @Header("Depth") depth: Int = 0,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(
        method = "PROPFIND",
        path = "./",
        hasBody = true
    )
    suspend fun getPrincipalGeneric(
        @Header("Depth") depth: Int = 0,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(
        method = "PROPFIND",
        path = "{path}",
        hasBody = true
    )
    suspend fun propfind(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Header("Depth") depth: Int = 0,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(
        method = "REPORT",
        path = "{path}",
        hasBody = true
    )
    suspend fun report(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Header("Depth") depth: Int = 1,
        @Header("Content-Type") contentType: String = "application/xml; charset=utf-8",
        @Body body: okhttp3.RequestBody,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @HTTP(
        method = "MOVE",
        path = "{path}"
    )
    suspend fun move(
        @Path(
            value = "path",
            encoded = true
        ) path: String,
        @Header("Destination") destination: String,
    ): retrofit2.Response<Unit>
}

data class OcsResponse<T>(
    @Json(name = "ocs") val ocs: OcsBody<T>,
)

data class OcsBody<T>(
    @Json(name = "meta") val meta: OcsMeta,
    @Json(name = "data") val data: T? = null,
)

data class OcsMeta(
    @Json(name = "status") val status: String? = null,
    @Json(name = "statuscode") val statusCode: Int? = null,
    @Json(name = "message") val message: String? = null,
)

data class UserInfo(
    @Json(name = "id") val id: String,
    @Json(name = "displayname") val displayName: String,
    @Json(name = "email") val email: String? = null,
)
