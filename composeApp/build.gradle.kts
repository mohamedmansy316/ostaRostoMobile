import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
    // FCM: uncomment after adding composeApp/google-services.json (see README).
    // alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.multiplatform.settings)
            implementation(libs.napier)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
            implementation(libs.multiplatform.settings.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.security.crypto)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.ostarosto.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ostarosto.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            // R8 enabled — see composeApp/proguard-rules.pro. Smoke-test a real
            // release build before shipping and add keeps for anything stripped.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Base URLs per build. Pass -PPROD_BASE_URL=https://api.example.com/api/v1 (or
// edit the fallback below) before building the prod flavor.
val prodBaseUrl: String =
    (project.findProperty("PROD_BASE_URL") as? String) ?: "https://TODO-set-production-domain/api/v1"

buildkonfig {
    packageName = "com.ostarosto.app.config"

    defaultConfigs {
        // Dev API host. This machine's Wi-Fi LAN IP — the phone must be on the
        // same network and the host must run:
        //   php artisan serve --host=0.0.0.0 --port=8000
        // Android emulator (AVD) instead: use http://10.0.2.2:8000/api/v1
        buildConfigField(STRING, "BASE_URL", "http://192.168.8.3:8000/api/v1")
        buildConfigField(STRING, "ENV", "dev")
    }

    // `./gradlew ... -Pbuildkonfig.flavor=prod` to build against production.
    defaultConfigs("prod") {
        buildConfigField(STRING, "BASE_URL", prodBaseUrl)
        buildConfigField(STRING, "ENV", "prod")
    }
}

// A release *artifact* build must target production over HTTPS — never ship the
// dev host. Only the tasks that actually produce a shippable APK/AAB trip this;
// debug builds, IDE sync, lint, and `compileReleaseKotlin*` are unaffected.
// To build a local release APK anyway: -Pbuildkonfig.flavor=prod -PPROD_BASE_URL=https://…
gradle.taskGraph.whenReady {
    val releaseArtifactTasks = setOf("assembleRelease", "bundleRelease", "packageRelease")
    val buildingRelease = allTasks.any { t ->
        t.project == project && t.name in releaseArtifactTasks
    }
    if (buildingRelease) {
        val flavor = (project.findProperty("buildkonfig.flavor") as? String).orEmpty()
        val problems = buildList {
            if (flavor != "prod") {
                add("flavor is '${flavor.ifEmpty { "dev (default)" }}', not 'prod' (dev points at a plaintext-HTTP host)")
            }
            if (prodBaseUrl.contains("TODO")) add("PROD_BASE_URL is not set")
            if (!prodBaseUrl.startsWith("https://")) add("PROD_BASE_URL is not https:// ('$prodBaseUrl')")
        }
        if (problems.isNotEmpty()) {
            val msg = "Release artifact build with a non-production config:\n  - " + problems.joinToString("\n  - ") +
                "\n  Fix: -Pbuildkonfig.flavor=prod -PPROD_BASE_URL=https://…"
            // Hard-fail in CI (where real artifacts ship); warn locally so dev builds aren't blocked.
            val strict = System.getenv("CI") != null || project.findProperty("release.strict") == "true"
            if (strict) error(msg) else logger.warn("\n⚠️  $msg\n")
        }
    }
}
