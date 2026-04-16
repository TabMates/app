import de.tabmates.convention.configureKtlint
import org.gradle.api.Plugin
import org.gradle.api.Project

class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKtlint()
        }
    }
}
