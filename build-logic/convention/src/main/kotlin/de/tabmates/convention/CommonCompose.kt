package de.tabmates.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCommonCompose() {
    dependencies {
        "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui-tooling-preview").get())

        "androidRuntimeClasspath"(libs.findLibrary("jetbrains-compose-ui-tooling").get())
    }
}
