import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension

plugins {
    alias(libs.plugins.tabmates.convention.cmp.application)
}

kotlin {
    extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
        androidResources { enable = true }
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
            implementation(projects.core.data)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)
            implementation(projects.features.authentication.data)
            implementation(projects.features.authentication.domain)
            implementation(projects.features.authentication.presentation)
            implementation(projects.features.tabgroup.data)
            implementation(projects.features.tabgroup.database)
            implementation(projects.features.tabgroup.domain)
            implementation(projects.features.tabgroup.presentation)

            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.bundles.koin.common)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
