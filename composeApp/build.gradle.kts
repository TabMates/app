plugins {
    alias(libs.plugins.tabmates.convention.cmp.application)
}

kotlin {
    android {
        namespace = "de.tabmates.composeApp"
        minSdk = libs.versions.android.sdk.min.get().toInt()
        compileSdk {
            version = release(libs.versions.android.sdk.compile.major.get().toInt()) {
                minorApiLevel = libs.versions.android.sdk.compile.minor.get().toInt()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
