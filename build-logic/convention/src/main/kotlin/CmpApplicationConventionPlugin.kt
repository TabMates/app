import de.tabmates.convention.applyHierarchyTemplate
import de.tabmates.convention.configureDesktopTarget
import de.tabmates.convention.configureIosTargets
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CmpApplicationConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("android-kmp-library"))
                apply(libs.findPluginId("kotlin-multiplatform"))
                apply(libs.findPluginId("compose-multiplatform"))
                apply(libs.findPluginId("compose-compiler"))
                apply(libs.findPluginId("kotlin-serialization"))
            }

            configureIosTargets()
            configureDesktopTarget()

            extensions.configure<KotlinMultiplatformExtension> {
                applyHierarchyTemplate()
            }

            dependencies {
                // Single-variant model: use androidMainImplementation instead of debugImplementation
                "androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}