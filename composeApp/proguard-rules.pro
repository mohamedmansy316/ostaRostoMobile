# ---------------------------------------------------------------------------
# R8 / ProGuard rules for the release build.
# NOTE: release minification was just enabled — smoke-test a real release
# build (login, menu, cart, checkout, Paymob redirect, order detail, push)
# before shipping and add keeps here for anything that misbehaves.
# ---------------------------------------------------------------------------

-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, Exceptions

# --- kotlinx.serialization -------------------------------------------------
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.ostarosto.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the @Serializable model/DTO classes themselves (names used in reflection).
-keep,includedescriptorclasses class com.ostarosto.app.data.dto.** { *; }
-keep,includedescriptorclasses class com.ostarosto.app.domain.model.** { *; }
-keep class com.ostarosto.app.core.network.ApiEnvelope { *; }
-keep class com.ostarosto.app.core.network.RawEnvelope { *; }
-keep class com.ostarosto.app.core.network.PageMeta { *; }

# --- Ktor ---------------------------------------------------------------------
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
# The no-arg HttpClient() resolves its engine via ServiceLoader.
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { *; }
-keep class * implements io.ktor.client.engine.HttpClientEngineContainer { *; }

# --- OkHttp / Okio ----------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# --- Coroutines -----------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Koin ---------------------------------------------------------------------
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# --- Coil 3 ---------------------------------------------------------------------
-dontwarn coil3.**

# --- Firebase Messaging ----------------------------------------------------
-keep class com.ostarosto.app.push.OstaFirebaseMessagingService { *; }
-dontwarn com.google.firebase.**

# --- Compose / lifecycle ViewModels --------------------------------------
-keep class com.ostarosto.app.**ViewModel { *; }
