plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
    alias(libs.plugins.tabmates.convention.buildkonfig)
    alias(libs.plugins.tabmates.convention.koin)
}

kotlin {
    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.features.authentication.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        webMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}
