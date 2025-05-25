# Libraries

## OH! Hi there! You must be here to find out what libraries we uses right? well, here it is:

1.  **AndroidX Core & UI**
    * `androidx.core:core-ktx` - Core library extensions for Kotlin.
    * `androidx.appcompat:appcompat` - Provides backward-compatible versions of Android framework APIs.
    * `androidx.lifecycle:lifecycle-runtime-ktx` - Lifecycle-aware components.
    * `androidx.activity:activity-compose` (v1.6.1, v1.4.0) - Integration for Jetpack Compose within an Activity.

2.  **Jetpack Compose**
    * `androidx.compose.bom` (platform) - Bill of Materials for managing Compose library versions.
    * `androidx.compose.ui:ui` - Core Compose UI library.
    * `androidx.compose.ui:ui-graphics` - Compose UI graphics library.
    * `androidx.compose.ui:ui-tooling` - Compose UI tooling support.
    * `androidx.compose.ui:ui-tooling-preview` - Compose UI preview support.
    * `androidx.compose.material3:material3` - Material Design 3 components for Compose.
    * `androidx.compose.material:material-icons-extended` - Extended Material icons for Compose.
    * `androidx.compose.material:material` (transitively or directly) - Material Design components for Compose.

3.  **Navigation**
    * `androidx.navigation:navigation-compose` - Navigation support for Compose.
    * `androidx.hilt:hilt-navigation-compose` - Hilt integration for Compose navigation.

4.  **Lifecycle & ViewModel**
    * `androidx.lifecycle:lifecycle-viewmodel-ktx` (implied by `lifecycle-viewmodel-compose`) - ViewModel KTX extensions.
    * `androidx.lifecycle:lifecycle-viewmodel-compose` (v2.8.7) - ViewModel integration for Compose.
    * `androidx.lifecycle:lifecycle-livedata-ktx` - LiveData integration for Kotlin.
    * `androidx.compose.runtime:runtime-livedata` - Observe LiveData as Compose State.

5.  **Room Database**
    * `androidx.room:room-runtime` - Room database runtime.
    * `androidx.room:room-ktx` - Kotlin extensions for Room.
    * `ksp("androidx.room:room-compiler")` - Annotation processor for Room (using KSP).
    * `androidx.room:room-rxjava2` - RxJava2 support for Room.
    * `androidx.room:room-rxjava3` - RxJava3 support for Room.
    * `androidx.room:room-guava` - Guava support for Room.
    * `androidx.room:room-paging` - Paging support for Room.

6.  **Media Playback**
    * `androidx.media3:media3-exoplayer` - ExoPlayer for media playback.
    * `androidx.media3:media3-ui` - UI components for ExoPlayer.

7.  **Networking**
    * `com.squareup.retrofit2:retrofit` - Retrofit for HTTP requests.
    * `com.squareup.retrofit2:converter-gson` - Gson converter for Retrofit.
    * `com.squareup.okhttp3:logging-interceptor` - Logging interceptor for HTTP requests.
    * `com.android.volley:volley` - Networking library for Android.

8.  **Dependency Injection (Hilt)**
    * `com.google.dagger:hilt-android` (v2.49) - Hilt for dependency injection.
    * `kapt("com.google.dagger:hilt-compiler")` - Annotation processor for Hilt (using Kapt).
    * `androidx.hilt:hilt-navigation-compose` - Hilt integration for Compose navigation (listed again, often with Hilt itself).

9.  **Image Loading**
    * `io.coil-kt:coil-compose` - Image loading library for Compose.
    * `com.github.bumptech.glide:glide` - Image loading and caching library.

10. **Security**
    * `androidx.security:security-crypto` (v1.1.0-alpha07) - Encrypted shared preferences and file storage.

11. **DataStore Preferences**
    * `androidx.datastore:datastore-preferences` - Jetpack DataStore for storing key-value pairs.

12. **Utilities & Other**
    * `com.journeyapps:zxing-android-embedded` - QR code scanning library.
    * `com.google.android.gms:play-services-location` - Google Play Services for location functionalities.
    * `org.osmdroid:osmdroid-android` (v6.1.12) - OpenStreetMap library for Android maps.

13. **Testing**
    * `junit:junit` - Unit testing framework.
    * `androidx.test.ext:junit` - AndroidX JUnit extensions.
    * `androidx.test.espresso:espresso-core` - UI testing framework for Android.
    * `androidx.room:room-testing` - Testing utilities for Room.