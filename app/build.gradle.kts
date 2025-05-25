plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)

    alias(libs.plugins.hilt)
    kotlin("kapt") 
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)


    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)



    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)


    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.volley)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.androidx.appcompat)

    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.ui.tooling)

    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)


    implementation(libs.androidx.activity.compose.v161)
    implementation(libs.hilt.android.v249)
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose.v287)
    implementation (libs.androidx.security.crypto.v110alpha07)


    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.rxjava3)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.paging)
    testImplementation(libs.androidx.room.testing)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.material.icons.extended)
    implementation (libs.androidx.datastore.preferences)

    implementation(libs.material3)
    implementation(libs.core)
    implementation(libs.zxing.android.embedded)

    implementation("androidx.core:core-ktx:1.10.1")

    implementation(libs.play.services.location)

    implementation("androidx.core:core:1.10.1")
    implementation (libs.androidx.material.icons.extended.v143)

    implementation("org.osmdroid:osmdroid-android:6.1.12")
}