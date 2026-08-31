/**
 * Monotonic code from a semver name: 1.2.3 -> 10203. Play requires this to
 * increase forever, so it is derived rather than hand-maintained.
 */
fun versionCodeOf(name: String): Int {
    val (major, minor, patch) = name.split("-")[0].split(".").map(String::toInt)
    return major * 10_000 + minor * 100 + patch
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "me.spoo.android"
    compileSdk = 37

    // The git tag is the version: CI passes it in before the tag exists,
    // and any tagged checkout (F-Droid builders included) learns it from
    // git describe, so nothing is ever committed back. The code is derived
    // from the name so it can only ever go up.
    val describedVersion =
        providers
            .exec {
                commandLine("git", "describe", "--tags", "--match", "v*")
                isIgnoreExitValue = true
            }.standardOutput.asText
            .map { it.trim().removePrefix("v").substringBefore("-") }
            .orNull
            ?.takeIf { it.isNotEmpty() }
    val appVersionName =
        System.getenv("SPOO_VERSION_NAME")?.removePrefix("v")
            ?: describedVersion
            // Positive versionCode floor: CI PR checkouts are shallow and
            // tagless, so describe can come up empty there.
            ?: "0.0.1"

    defaultConfig {
        applicationId = "me.spoo.android"
        minSdk = 26
        targetSdk = 36
        versionName = appVersionName
        versionCode = versionCodeOf(appVersionName)
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

    // Only present when CI hands us a keystore; an unsigned release APK
    // still builds so the R8 path stays covered on every PR.
    val keystore = System.getenv("SPOO_KEYSTORE_PATH")?.let(::file)?.takeIf { it.exists() }
    if (keystore != null) {
        signingConfigs.create("release") {
            storeFile = keystore
            storePassword = System.getenv("SPOO_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("SPOO_KEY_ALIAS")
            keyPassword = System.getenv("SPOO_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // Local backend over the Mac's tailnet IP: reachable from the
            // emulator (host NAT) AND a physical phone on the tailnet.
            buildConfigField("String", "SPOO_BASE_URL", "\"http://100.78.133.82:8000\"")
            buildConfigField("String", "SPOO_REDIRECT_URI", "\"spoo://oauth/callback\"")
        }
        release {
            // Absent locally and on PR builds, where assembleRelease only
            // has to prove that R8 still works.
            signingConfig = signingConfigs.findByName("release")
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
