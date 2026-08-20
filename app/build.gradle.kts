plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "me.spoo.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "me.spoo.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig.buildConfigField("String", "SPOO_APP_ID", "\"spoo-mobile\"")

    buildTypes {
        debug {
            // Local backend on the emulator host; see spoo-latest docker compose.
            buildConfigField("String", "SPOO_BASE_URL", "\"http://10.0.2.2:8000\"")
            buildConfigField("String", "SPOO_REDIRECT_URI", "\"spoo://oauth/callback\"")
        }
        release {
            buildConfigField("String", "SPOO_BASE_URL", "\"https://spoo.me\"")
            // Placeholder until assetlinks.json + apps.yaml ship in prod.
            buildConfigField("String", "SPOO_REDIRECT_URI", "\"https://spoo.me/oauth/android-callback\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.zxing.core)
    implementation(libs.materialkolor)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.spoo.sdk)
}
