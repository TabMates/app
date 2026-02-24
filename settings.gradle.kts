rootProject.name = "TabMatesApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

includeBuild("build-logic")
include(":androidApp")
include(":composeApp")
include(":core:data")
include(":core:designsystem")
include(":core:domain")
include(":core:presentation")
include(":features:authentication:data")
include(":features:authentication:domain")
include(":features:authentication:presentation")
include(":features:tabgroup:data")
include(":features:tabgroup:database")
include(":features:tabgroup:domain")
include(":features:tabgroup:presentation")
