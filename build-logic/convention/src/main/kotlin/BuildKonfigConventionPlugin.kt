import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import de.tabmates.convention.pathToPackageName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure
import java.util.Properties

class BuildKonfigConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("buildkonfig"))
            }

            val localProperties: Properties by lazy {
                gradleLocalProperties(rootDir, rootProject.providers)
            }

            fun requireProperty(key: String): String =
                System.getenv(key)
                    ?: localProperties.getProperty(key)
                    ?: throw IllegalStateException(
                        "Missing \"$key\". Define it as an environment variable " +
                            "or in local.properties.",
                    )

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()
                defaultConfigs {
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", requireProperty("API_KEY"))
                    buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "false")
                }
                targetConfigs {
                    create("debug") {
                        buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
                    }
                }
            }
        }
    }
}
