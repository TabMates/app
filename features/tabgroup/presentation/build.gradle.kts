plugins {
    alias(libs.plugins.tabmates.convention.cmp.feature)
    alias(libs.plugins.tabmates.convention.buildkonfig)
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
                implementation(projects.core.designsystem)
                implementation(projects.core.domain)
                implementation(projects.core.presentation)
                implementation(projects.features.authentication.data)
                implementation(projects.features.authentication.domain)
                implementation(projects.features.tabgroup.data)
                implementation(projects.features.tabgroup.domain)
                implementation(libs.jetbrains.material3.adaptive)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(projects.features.authentication.testing)
            }
        }

        androidMain {
            dependencies {
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
