import androidx.room3.gradle.RoomExtension
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("ksp"))
                apply(libs.findPluginId("room3"))
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "commonMainApi"(libs.findLibrary("androidx-room3-runtime").get())
                "kspAndroid"(libs.findLibrary("androidx-room3-compiler").get())
                "kspIosSimulatorArm64"(libs.findLibrary("androidx-room3-compiler").get())
                "kspIosArm64"(libs.findLibrary("androidx-room3-compiler").get())
                "kspDesktop"(libs.findLibrary("androidx-room3-compiler").get())
                "kspWasmJs"(libs.findLibrary("androidx-room3-compiler").get())
                "kspJs"(libs.findLibrary("androidx-room3-compiler").get())
            }
        }
    }
}
