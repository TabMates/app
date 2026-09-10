import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import de.tabmates.convention.androidDistributionSrcDir
import de.tabmates.convention.isFossDistribution

plugins {
    alias(libs.plugins.tabmates.convention.cmp.application)
    alias(libs.plugins.tabmates.convention.buildkonfig)
    alias(libs.plugins.tabmates.convention.koin)
}

// AGP's KMP library plugin has exactly one Android variant and no product flavors, so the
// Play/FOSS split is a source-directory swap driven by the `tabmates.distribution` property.
// See build-logic/.../de/tabmates/convention/Distribution.kt.
val fossDistribution = isFossDistribution
val distributionSrcDir = androidDistributionSrcDir()

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
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)
            implementation(projects.features.appupdate.data)
            implementation(projects.features.appupdate.domain)
            implementation(projects.features.authentication.data)
            implementation(projects.features.authentication.domain)
            implementation(projects.features.authentication.presentation)
            implementation(projects.features.notifications.data)
            implementation(projects.features.notifications.domain)
            implementation(projects.features.tabgroup.data)
            implementation(projects.features.tabgroup.database)
            implementation(projects.features.tabgroup.domain)
            implementation(projects.features.tabgroup.presentation)

            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.bundles.koin.common)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksafe)
            implementation(libs.jetbrains.material3.adaptive)
        }
        androidMain {
            // Supplies the AppUpdateHandler `actual`: the Play Core flow, or the store-redirect
            // dialog. The directory and the dependency below move together, so a build never has
            // one without the other — that mismatch is a compile error, not a silent leak.
            kotlin.srcDir(distributionSrcDir)
            dependencies {
                implementation(libs.androidx.activity.compose)
                if (!fossDistribution) {
                    // Google Play in-app update flow (native, Play-installed devices only).
                    // Proprietary, so F-Droid builds drop it and fall back to DefaultUpdateHandler.
                    implementation(libs.play.app.update)
                    implementation(libs.play.app.update.ktx)
                }
            }
        }
        iosMain.dependencies {
            // Exposes kmpnotifier iOS extension functions to the Swift AppDelegate bridge
            // (onApplicationDidReceiveRemoteNotification in push-firebase, onNotificationClicked in core).
            implementation(libs.kmpnotifier.push.firebase)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(projects.features.authentication.testing)
        }
    }
}
