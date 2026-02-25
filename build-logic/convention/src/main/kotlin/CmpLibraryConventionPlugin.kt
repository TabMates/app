import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("tabmates-convention-kmp-library"))
                apply(libs.findPluginId("compose-compiler"))
                apply(libs.findPluginId("compose-multiplatform"))
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-foundation").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material-icons-core").get())

                "androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}