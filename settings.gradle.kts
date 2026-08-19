pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "spoo-android"

include(":app")

// me.spoo:spoo is live on Maven Central. Uncomment to hack on the SDK from
// the sibling checkout — the composite build substitutes the coordinates.
// includeBuild("../spoo-kotlin")
