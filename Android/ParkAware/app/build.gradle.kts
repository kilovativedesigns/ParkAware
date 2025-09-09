plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kilovativedesigns.parkaware"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kilovativedesigns.parkaware"
        minSdk = 24
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
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        viewBinding = true
        compose = true               // ✅ TURN ON COMPOSE
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"   // ✅ Compose compiler
    }
}

dependencies {
    // --- Compose BOM keeps versions in sync ---
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Core Compose (no versions needed thanks to BOM)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Optional: Google Fonts helper for Compose
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Accompanist (match to Compose 1.6/1.7 line)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Your existing deps (kept) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("com.leinardi.android:speed-dial:3.3.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Splashscreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // GeoFire
    implementation("com.firebase:geofire-android-common:3.2.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}