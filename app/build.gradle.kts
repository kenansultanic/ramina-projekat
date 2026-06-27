plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KSP generates Room's database code at build time. (No kotlin-android plugin needed:
    // AGP 9 has built-in Kotlin support and compiles our Kotlin automatically.)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ba.rma.myapplication"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ba.rma.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- Added for our two features ---
    // Retrofit + Gson: call the cards REST API and turn its JSON into Player objects.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // Room: store the user's collection (which stickers they own and how many) on the device.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Coil: load the (large ~2.7MB) sticker images from the API, with downsampling + caching.
    implementation(libs.coil.compose)
    // Lets us get our ViewModel inside a Composable with viewModel().
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Coroutines: run the network/database work off the main thread.
    implementation(libs.kotlinx.coroutines.android)
    // Navigation-Compose: moving between screens (splash, home, detail, settings) + passing arguments.
    implementation(libs.androidx.navigation.compose)
    // Extended Material icons used by the bottom navigation bar and the top app bar actions.
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
