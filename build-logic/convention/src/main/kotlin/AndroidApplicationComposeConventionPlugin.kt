import com.android.build.api.dsl.ApplicationExtension
import de.tabmates.convention.configureAndroidCompose
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationComposeConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("tabmates-convention-android-application"))
                apply(libs.findPluginId("compose-compiler"))
            }

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }
}