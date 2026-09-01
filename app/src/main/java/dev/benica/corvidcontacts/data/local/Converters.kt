// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.data.local

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.model.Relationship
import dev.benica.corvidcontacts.data.model.SocialProfile
import dev.benica.corvidcontacts.data.model.StructuredAddress

class Converters {
    private val moshi = Moshi
        .Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private inline fun <reified T> getListAdapter() = moshi.adapter<List<T>>(
        Types.newParameterizedType(
            List::class.java,
            T::class.java
        )
    )

    @TypeConverter
    fun fromEmailList(value: List<Email>?): String? =
        getListAdapter<Email>().toJson(value ?: emptyList())

    @TypeConverter
    fun toEmailList(value: String?): List<Email>? = getListAdapter<Email>().fromJson(value ?: "[]")

    @TypeConverter
    fun fromPhoneList(value: List<Phone>?): String? =
        getListAdapter<Phone>().toJson(value ?: emptyList())

    @TypeConverter
    fun toPhoneList(value: String?): List<Phone>? = getListAdapter<Phone>().fromJson(value ?: "[]")

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        getListAdapter<String>().toJson(value ?: emptyList())

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        getListAdapter<String>().fromJson(value ?: "[]")

    @TypeConverter
    fun fromRelationshipList(value: List<Relationship>?): String? =
        getListAdapter<Relationship>().toJson(value ?: emptyList())

    @TypeConverter
    fun toRelationshipList(value: String?): List<Relationship>? =
        getListAdapter<Relationship>().fromJson(value ?: "[]")

    @TypeConverter
    fun fromStructuredAddressList(value: List<StructuredAddress>?): String? =
        getListAdapter<StructuredAddress>().toJson(value ?: emptyList())

    @TypeConverter
    fun toStructuredAddressList(value: String?): List<StructuredAddress>? =
        getListAdapter<StructuredAddress>().fromJson(value ?: "[]")

    @TypeConverter
    fun fromSocialProfileList(value: List<SocialProfile>?): String? =
        getListAdapter<SocialProfile>().toJson(value ?: emptyList())

    @TypeConverter
    fun toSocialProfileList(value: String?): List<SocialProfile>? =
        getListAdapter<SocialProfile>().fromJson(value ?: "[]")
}
