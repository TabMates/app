package de.tabmates.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform() {
    configureDesktopTarget()

    extensions.configure<KotlinMultiplatformExtension> {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = this@configureKotlinMultiplatform.pathToFrameworkName()
            }
        }

        extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            minSdk = 26
            compileSdk = 36
            namespace = pathToPackageName()
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