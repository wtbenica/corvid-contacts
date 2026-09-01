# SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

# Moshi
-if @com.squareup.moshi.JsonClass class *
-keepnames class <1>
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn org.jetbrains.annotations.**

# Moshi Reflective Models & Entities
-keep class dev.benica.corvidcontacts.data.model.** { *; }
-keep class dev.benica.corvidcontacts.data.remote.** { *; }
-keep class dev.benica.corvidcontacts.data.local.ContactEntity { *; }
-keep class dev.benica.corvidcontacts.data.local.AddressBookEntity { *; }

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.AnnotationFieldTest
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization (Navigation)
-keep,includedescriptorclasses class dev.benica.corvidcontacts.navigation.Destination { *; }
-keep,includedescriptorclasses class dev.benica.corvidcontacts.navigation.Destination$** { *; }

# WorkManager
-keep public class dev.benica.corvidcontacts.sync.SyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep public class dev.benica.corvidcontacts.sync.BirthdayWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ez-vcard
-keep class ezvcard.VCard { *; }
-keep class ezvcard.VCardVersion { *; }
-keep class ezvcard.property.** { *; }
-keep class ezvcard.parameter.** { *; }
-keep class ezvcard.io.** { *; }
-keepclassmembers enum ezvcard.** { *; }

-dontwarn com.fasterxml.jackson.**
-dontwarn org.jsoup.**
-dontwarn freemarker.**
-dontwarn ezvcard.io.json.**
-dontwarn ezvcard.io.html.**

# AboutLibraries
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# Metadata Attributes
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
