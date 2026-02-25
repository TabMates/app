import de.tabmates.convention.configureAndroidLibrary
import de.tabmates.convention.defaultNamespace
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        // Core KMP plugins
        pluginManager.apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("androidKmpLibrary").get().get().pluginId)

        extensions.configure<KotlinMultiplatformExtension> {
            // Android KMP Library extension defaults
            configureAndroidLibrary(
                namespace = defaultNamespace(),
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt(),
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt(),

                // This can be `true` for only Compose related convention plugins.
                enableAndroidResources = false
            )

            // iOS targets (core/domain can be compiled in iOS)
            iosX64()
            iosArm64()
            iosSimulatorArm64()

            // No Compose dependency in base KMP plugin.

            // You can add kotlin dependencies according to your needs, such as:
            /*
            sourceSets.apply {
                commonMain {
                    dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutinesCore").get())
                    }
                }
                commonTest {
                    dependencies {
                        implementation(libs.findLibrary("kotlin-test").get())
                        implementation(libs.findLibrary("kotlinx-coroutinesTest").get())
                    }
                }
            }*/
        }
    }
}
