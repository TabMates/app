plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
    alias(libs.plugins.tabmates.convention.buildkonfig)
    alias(libs.plugins.tabmates.convention.koin)
}

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
            dependencies {
                implementation(libs.ktor.client.okhttp)
                // Firebase Cloud Messaging via kmpnotifier-push-firebase (also pulls kmpnotifier-local + core).
                implementation(libs.kmpnotifier.push.firebase)
                // NotificationManagerCompat for the notification-permission check.
                implementation(libs.androidx.core.ktx)
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
