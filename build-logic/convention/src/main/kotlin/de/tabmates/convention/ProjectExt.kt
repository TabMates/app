package de.tabmates.convention

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.findPluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/**
 * The single version every platform reports — `BuildKonfig.APP_VERSION` for the shared code and
 * Android's `versionName`/`versionCode` alike.
 *
 * It lives here because those two used to read it independently, and the two readers did not
 * accept the same inputs: `providers.gradleProperty` never sees a plain `APP_VERSION` environment
 * variable, only `ORG_GRADLE_PROJECT_APP_VERSION`. A CI job setting the wrong one got an app whose
 * `versionName` and `BuildKonfig.APP_VERSION` disagreed — which the backend's client-version gate
 * turns into a hard failure. One chain, one answer, every caller.
 *
 * Release CI sets `ORG_GRADLE_PROJECT_APP_VERSION` from the tag; `gradle.properties` supplies the
 * fallback for local and PR builds.
 */
val Project.appVersion: String
    get() =
        System.getenv("APP_VERSION")
            ?: gradleLocalProperties(rootDir, rootProject.providers).getProperty("APP_VERSION")
            ?: providers.gradleProperty("APP_VERSION").orNull
            ?: error("Missing \"APP_VERSION\". It is declared in gradle.properties; do not remove it.")
