@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.declarative.dsl.schema.FqName.Empty.packageName
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)

    id("com.google.gms.google-services")
    id("com.codingfeline.buildkonfig") version "0.15.1"
}

// Secrets and environment-specific config live in local.properties (git-ignored),
// so they never reach version control. Loaded once here and shared by the
// android manifest-placeholder and buildkonfig blocks below.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

kotlin {
    // Suppress Beta warning for expect/actual classes (KT-61573)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget()

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            //Firebase
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:34.6.0"))
            implementation("com.google.firebase:firebase-auth")
            implementation("com.google.firebase:firebase-firestore")
            implementation("com.google.firebase:firebase-database")
            implementation("com.google.firebase:firebase-messaging")
            implementation("com.google.firebase:firebase-storage")

            // CameraX + ML Kit (QR scanner)
            implementation("androidx.camera:camera-camera2:1.4.1")
            implementation("androidx.camera:camera-lifecycle:1.4.1")
            implementation("androidx.camera:camera-view:1.4.1")
            implementation("com.google.mlkit:barcode-scanning:17.3.0")
            // ListenableFuture needed by ProcessCameraProvider.getInstance()
            implementation("com.google.guava:guava:33.3.1-android")

            // Google Maps Compose
            implementation("com.google.maps.android:maps-compose:4.3.3")
            implementation("com.google.android.gms:play-services-maps:18.2.0")
            implementation("com.google.android.gms:play-services-location:21.1.0")

            // Ktor (HTTP Client)
            implementation(libs.ktor.client.okhttp)

            // SQLDelight
            implementation(libs.sqldelight.android.driver)

            // Koin
            implementation(libs.koin.android)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)

        }

        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Navigation
            implementation(libs.navigation.compose)

            // Ktor (HTTP Client)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            // Nav3 navigation
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.material3.adaptiveNav3)
            implementation(libs.jetbrains.lifecycle.viewmodelNav3)

            // QR code generation (Compose Multiplatform)
            implementation("io.github.alexzhirkevich:qrose:1.1.2")

            // Coil (Image Loading — multiplatform, works on Android + JS/WASM)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jsMain.dependencies {
            // Ktor
            implementation(libs.ktor.client.js)

            // Firebase (GitLive KMP SDK)
            implementation("dev.gitlive:firebase-auth:2.4.0")
            implementation("dev.gitlive:firebase-firestore:2.4.0")
            implementation("dev.gitlive:firebase-database:2.4.0")
            implementation("dev.gitlive:firebase-common:2.4.0")
            implementation("dev.gitlive:firebase-storage:2.4.0")
        }
    }
}

android {
    namespace = "com.domedemok.travelplanner"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.domedemok.travelplanner"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Google Maps key injected from local.properties into AndroidManifest
        // via the ${MAPS_API_KEY} placeholder — keeps the key out of source control.
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY") ?: "MISSING_MAPS_API_KEY"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// SQLDelight Configuration
sqldelight {
    databases {
        create("TravelDatabase") {
            packageName.set("com.domedemok.travelplanner.data.local.database")
        }
    }
}

//API key Configuration
buildkonfig {
    packageName = "com.domedemok.travelplanner"

    defaultConfigs {
        fun stringField(name: String, fallback: String = "MISSING_API_KEY") {
            buildConfigField(
                com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
                name,
                localProperties.getProperty(name) ?: "\"$fallback\""
            )
        }

        // External service keys
        stringField("GEMINI_API_KEY")
        stringField("FOURSQUARE_API_KEY")

        // Web Firebase config — public by design (shipped in the JS bundle), but
        // externalised here to keep it out of source control and easy to rotate.
        stringField("FIREBASE_API_KEY")
        stringField("FIREBASE_AUTH_DOMAIN")
        stringField("FIREBASE_DATABASE_URL")
        stringField("FIREBASE_PROJECT_ID")
        stringField("FIREBASE_STORAGE_BUCKET")
        stringField("FIREBASE_GCM_SENDER_ID")
        stringField("FIREBASE_APP_ID")

        // CORS proxy used by the JS Foursquare client. Defaults to the public
        // corsproxy.io, but SHOULD be overridden with a self-hosted proxy in
        // local.properties, because the Foursquare Bearer key transits this host.
        stringField("FOURSQUARE_CORS_PROXY", fallback = "https://corsproxy.io/?")
    }
}