plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
    alias(libs.plugins.tabmates.convention.koin)
    alias(libs.plugins.tabmates.convention.cmp.resources)
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
                implementation(projects.features.tabgroup.database)
                implementation(projects.features.tabgroup.domain)

                implementation(libs.androidx.room3.runtime)
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
                implementation(libs.koin.android)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        wasmJsMain {
            dependencies {
                implementation(projects.features.tabgroup.sqliteWasmWorker)

                implementation(libs.androidx.sqlite.web)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.androidx.sqlite.bundled)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.androidx.sqlite.bundled)
            }
        }
    }
}
