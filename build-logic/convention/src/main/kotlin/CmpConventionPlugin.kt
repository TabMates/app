import de.tabmates.buildlogic.convention.configureAndroidLibrary
import de.tabmates.buildlogic.convention.defaultNamespace
import de.tabmates.buildlogic.convention.frameworkBaseName
import de.tabmates.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CmpConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            // First base KMP convention.
            pluginManager.apply("tabmates.convention.kmp")

            // Then Compose side.
            pluginManager.apply(libs.findPlugin("composeMultiplatform").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("composeCompiler").get().get().pluginId)

            extensions.configure<KotlinMultiplatformExtension> {
                configureAndroidLibrary(
                    namespace = defaultNamespace(),
                    compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt(),
                    minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt(),
                    enableAndroidResources = true
                )

                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach { iosTarget ->
                    iosTarget.binaries.framework {
                        baseName = project.frameworkBaseName()
                        isStatic = true
                    }
                }

                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("compose-runtime").get())
                        implementation(libs.findLibrary("compose-ui").get())
                        implementation(libs.findLibrary("compose-foundation").get())
                        implementation(libs.findLibrary("compose-material3").get())

                        // KMP resources (Res.drawable.*)
                        implementation(libs.findLibrary("compose-components-resources").get())

                        // Enable KMP @Preview for @Compose functions.
                        implementation(libs.findLibrary("compose-uiToolingPreview").get())

                        // Coil compose image.
                        implementation(libs.findLibrary("coil-core").get())
                        implementation(libs.findLibrary("coil-compose").get())
                        implementation(libs.findLibrary("coil-composeCore").get())
                        implementation(libs.findLibrary("coil-networkKtor").get())
                    }

                    // Preview / tooling for only Android.
                    androidMain.dependencies {
                        implementation(libs.findLibrary("compose-uiTooling").get())

                        // Ktor is needed for Coil.
                        implementation(libs.findLibrary("ktor-androidClient").get())
                    }

                    iosMain.dependencies {
                        // Ktor is needed for Coil.
                        implementation(libs.findLibrary("ktor-darwinClient").get())
                    }

                    // Add your common test dependencies.
                    commonTest.dependencies {}
                }
            }
        }
    }
}