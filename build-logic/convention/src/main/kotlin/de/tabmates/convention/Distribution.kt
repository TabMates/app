package de.tabmates.convention

import org.gradle.api.Project

/** Gradle property selecting the [Distribution]; see [Project.distribution]. */
const val DISTRIBUTION_PROPERTY = "tabmates.distribution"

/**
 * Which app store this build is destined for.
 *
 * F-Droid refuses proprietary dependencies, so [FOSS] drops Google Play Core (the in-app update
 * flow) and Firebase Cloud Messaging (push) entirely. That build simply has no push: the
 * controller is a no-op, `POST_NOTIFICATIONS` is neither requested nor declared, and updates fall
 * back to the store-redirect dialog every non-Play platform already uses.
 */
enum class Distribution {
    /** Google Play. Firebase Cloud Messaging push + Play Core in-app updates. */
    PLAY,

    /** F-Droid and other FOSS channels. No Google dependencies of any kind. */
    FOSS,
    ;

    /** Android source directory suffix, e.g. `androidPlayMain`. */
    internal val sourceSetName: String get() = "android${name.lowercase().replaceFirstChar(Char::uppercase)}Main"

    /** AGP product-flavor name, e.g. `play`. */
    val flavorName: String get() = name.lowercase()
}

/**
 * The distribution this invocation builds, from the [DISTRIBUTION_PROPERTY] Gradle property
 * (`gradle.properties` supplies the `play` default; CI passes `-Ptabmates.distribution=foss`).
 *
 * A single property drives everything, because the two modules that carry the Google dependencies
 * — `:composeApp` and `:features:notifications:data` — cannot use product flavors: AGP's KMP
 * library plugin (`KotlinMultiplatformAndroidLibraryExtension`) declares no `productFlavors` /
 * `buildTypes` and exposes exactly one Android variant. `:androidApp` derives its single flavor
 * from this same property rather than declaring both, so the manifest overlay and the source-set
 * swap below can never disagree about which build this is.
 *
 * An unrecognised value is a hard error, not a silent fall back to [Distribution.PLAY]: a typo
 * would otherwise ship Firebase in an artifact labelled FOSS.
 */
val Project.distribution: Distribution
    get() {
        val raw =
            providers.gradleProperty(DISTRIBUTION_PROPERTY).orNull
                ?: error(
                    "Missing \"$DISTRIBUTION_PROPERTY\". It is declared in gradle.properties; do not remove it.",
                )
        return Distribution.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: error(
                "Unknown $DISTRIBUTION_PROPERTY \"$raw\". Expected one of " +
                    Distribution.entries.joinToString { it.flavorName } + ".",
            )
    }

/** True when this build must contain no proprietary Google dependencies. See [distribution]. */
val Project.isFossDistribution: Boolean get() = distribution == Distribution.FOSS

/**
 * The Android source directory this distribution contributes, added to `androidMain` on top of the
 * shared one. Holds the `actual` declarations that differ between distributions, so a build is
 * always missing either both the Google dependency and its callers, or neither — never one of the
 * two. Getting it wrong is a compile error (a missing or duplicate `actual`), never a silent leak.
 */
fun Project.androidDistributionSrcDir(): String = "src/${distribution.sourceSetName}/kotlin"
