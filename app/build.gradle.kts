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

    lint {
        warningsAsErrors = true
        // Pre-existing warnings live in the baseline; new ones fail CI.
        baseline = file("lint-baseline.xml")
        // Dependency freshness is Dependabot's job. These checks query the
        // network, so they turn any upstream release into a red build.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }

    buildTypes {
        debug {
            // Local backend over the Mac's tailnet IP: reachable from the
            // emulator (host NAT) AND a physical phone on the tailnet.
            buildConfigField("String", "SPOO_BASE_URL", "\"http://100.78.133.82:8000\"")
            buildConfigField("String", "SPOO_REDIRECT_URI", "\"spoo://oauth/callback\"")
        }
        release {
            buildConfigField("String", "SPOO_BASE_URL", "\"https://spoo.me\"")
            // Custom-scheme deep link, the house pattern for native
            // clients; a verified https App Link can supersede it once
            // assetlinks.json ships.
            buildConfigField("String", "SPOO_REDIRECT_URI", "\"spoo://oauth/callback\"")
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
