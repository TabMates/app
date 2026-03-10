package de.tabmates.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

internal fun Project.configureKtlint() {
    with(pluginManager) {
        apply(libs.findPluginId("ktlint"))
    }

    extensions.configure<KtlintExtension> {
        version.set(libs.findVersion("ktlint-version").get().toString())
        android.set(false)
        ignoreFailures.set(false)
        enableExperimentalRules.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
        filter {
            exclude { element ->
                element.file.path.contains("/build/generated/")
            }
        }
    }
}
