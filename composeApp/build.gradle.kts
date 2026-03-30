import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension

plugins {
    alias(libs.plugins.tabmates.convention.cmp.application)
}

kotlin {
    extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
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
            implementation(projects.core.presentation)
            implementation(projects.core.designsystem)
            implementation(projects.features.authentication.presentation)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
