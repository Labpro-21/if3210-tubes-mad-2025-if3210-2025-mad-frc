plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
//    id("dagger.hilt.android.plugin")
    alias(libs.plugins.hilt)
    kotlin("kapt") // Tambahkan ini untuk annotation processing
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.purrytify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.purrytify"
        minSdk = 29
        targetSdk = 34
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
        viewBinding = true
        compose = true
    }
}

dependencies {
    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel & Lifecycle
//    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata) // still needed

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.volley)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.androidx.appcompat)
//    implementation(libs.androidx.security.crypto)
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.ui.tooling)
//    implementation(libs.androidx.activity.compose.v140)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
//    implementation(libs.androidx.lifecycle.livedata.ktx)
//    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.activity.compose.v161)
    implementation("com.google.dagger:hilt-android:2.49")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation ("androidx.security:security-crypto:1.1.0-alpha07")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation ("androidx.media:media:1.6.0")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.rxjava3)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.paging)
    testImplementation(libs.androidx.room.testing)

    implementation(libs.androidx.media3.exoplayer)        // Core ExoPlayer
    implementation(libs.androidx.media3.ui)               // PlayerView
    implementation("androidx.compose.material:material-icons-extended:<version>")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.compose.material3:material3:<latest-version>")
}