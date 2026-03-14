package de.tabmates.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform() {
    configureDesktopTarget()

    extensions.configure<KotlinMultiplatformExtension> {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = this@configureKotlinMultiplatform.pathToFrameworkName()
            }
        }

        extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            minSdk =
                libs
                    .findVersion("android-sdk-min")
                    .get()
                    .toString()
                    .toInt()
            compileSdk {
                version =
                    release(
                        libs
                            .findVersion("android-sdk-compile-major")
                            .get()
                            .toString()
                            .toInt(),
                    ) {
                        minorApiLevel =
                            libs
                                .findVersion("android-sdk-compile-minor")
                                .get()
                                .toString()
                                .toInt()
                    }
            }
            namespace = pathToPackageName()

            withDeviceTestBuilder {
                sourceSetTreeName = "test"
            }.configure {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        js {
            browser()
            binaries.executable()
        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser()
            binaries.executable()
        }

        applyHierarchyTemplate()

        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.time.ExperimentalTime",
            )
        }
    }
}
