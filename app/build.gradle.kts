plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Firebase
    id("com.google.gms.google-services")

    // Room — KSP annotation processor
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}

android {
    namespace = "com.example.alohi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alohi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.foundation)
    
    // AppCompat & Material Components (Required for Zego UIKit Android Views)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // QR Code
    implementation("com.google.zxing:core:3.5.3")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Image & Video Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation("io.coil-kt.coil3:coil-video:3.0.4")

    // Video Player
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Networking (Retrofit + OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Socket.IO
    implementation("io.socket:socket.io-client:2.1.0")

    // Room — offline-first local cache (giống Zalo)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // WebRTC (Using GetStream's maintained fork of Google WebRTC which maps to org.webrtc namespace)
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    // ZEGOCLOUD: Prebuilt Call UIKits (Video/Voice calls with UI)
    implementation("com.github.ZEGOCLOUD:zego_uikit_prebuilt_call_android:+")
    implementation("com.github.ZEGOCLOUD:zego_uikit_signaling_plugin_android:+")
}