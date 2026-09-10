import de.tabmates.convention.androidDistributionSrcDir
import de.tabmates.convention.isFossDistribution

plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
    alias(libs.plugins.tabmates.convention.buildkonfig)
    alias(libs.plugins.tabmates.convention.koin)
}

// AGP's KMP library plugin has exactly one Android variant and no product flavors, so the
// Play/FOSS split is a source-directory swap driven by the `tabmates.distribution` property.
// See build-logic/.../de/tabmates/convention/Distribution.kt.
val fossDistribution = isFossDistribution
val distributionSrcDir = androidDistributionSrcDir()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.features.notifications.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                // Encrypted storage for the cached device token.
                implementation(libs.ksafe)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            // Supplies the PlatformNotificationsModule `actual`: FCM push, or the shared no-op
            // controllers. The directory and the dependencies below move together, so a build
            // never has one without the other — that mismatch is a compile error, not a leak.
            kotlin.srcDir(distributionSrcDir)
            dependencies {
                implementation(libs.ktor.client.okhttp)
                if (!fossDistribution) {
                    // Firebase Cloud Messaging via kmpnotifier-push-firebase (also pulls
                    // kmpnotifier-local + core). Proprietary, so the F-Droid build has no push
                    // at all; androidFossMain binds NoOpPushNotificationController instead.
                    implementation(libs.kmpnotifier.push.firebase)
                    // NotificationManagerCompat for the notification-permission check. Its only
                    // consumer, AndroidNotificationPermissionController, is Play-only: the FOSS
                    // build reports UNSUPPORTED because it never declares POST_NOTIFICATIONS.
                    implementation(libs.androidx.core.ktx)
                }
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        desktopMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                // Local desktop notifications via kmpnotifier-local (no FCM on desktop).
                implementation(libs.kmpnotifier.local)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        iosMain {
            dependencies {
                // Firebase Cloud Messaging via kmpnotifier-push-firebase (also pulls kmpnotifier-local + core).
                implementation(libs.kmpnotifier.push.firebase)
            }
        }

        webMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}
