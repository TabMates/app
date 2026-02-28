import de.tabmates.convention.configureKotlinMultiplatform
import de.tabmates.convention.configureKtlint
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("android-kmp-library"))
                apply(libs.findPluginId("kotlin-multiplatform"))
                apply(libs.findPluginId("kotlin-serialization"))
            }

            configureKotlinMultiplatform()
            configureKtlint()

            dependencies {
                "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-json").get())
                "commonTestImplementation"(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}