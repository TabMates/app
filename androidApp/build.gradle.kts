import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import de.tabmates.convention.appVersion
import de.tabmates.convention.distribution
import de.tabmates.convention.isFossDistribution
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.language.base.plugins.LifecycleBasePlugin

plugins {
    alias(libs.plugins.tabmates.convention.android.application.compose)
    // Declared but not applied: `plugins { }` takes no conditionals, and this one is Play-only.
    // `apply false` still resolves it onto the build classpath, so pluginManager can switch it on
    // below. See the FOSS note there.
    alias(libs.plugins.google.services) apply false
}

val fossDistribution = isFossDistribution
val distributionFlavor = distribution.flavorName

// Google Services is Play-only: the plugin fails the build when google-services.json is missing,
// and the FOSS flavor ships neither that file nor any Firebase dependency to configure.
if (!fossDistribution) {
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
}

// Derive a monotonically increasing versionCode from the version name (e.g. "1.2.3" -> 10203)
// so every Play Store upload gets a unique, higher code without manual bumping.
fun versionNameToCode(name: String): Int {
    val (major, minor, patch) =
        (name.split(".") + listOf("0", "0", "0"))
            .take(3)
            .map { it.toIntOrNull() ?: 0 }
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "de.tabmates.androidapp"

    // Same resolver BuildKonfig.APP_VERSION uses, so versionName can never disagree with the
    // version the app reports to the backend.
    val appVersion = project.appVersion

    // Release builds in CI are signed with the upload keystore; local builds fall back to debug.
    val runsCIReleaseBuild = System.getenv("SIGNING_STORE_PASSWORD") != null

    defaultConfig {
        applicationId = "de.tabmates.androidapp"
        versionCode = versionNameToCode(appVersion)
        versionName = appVersion
    }

    // Exactly one flavor exists per invocation, derived from the same `tabmates.distribution`
    // property the KMP modules read — so the manifest overlay and Kotlin here can never disagree
    // with the source-set swap over there about which build this is. Declaring both flavors would
    // reintroduce that possibility, and the KMP modules could not honour the second one anyway.
    //
    // It buys the two things a property alone cannot: a manifest overlay (src/play for the
    // Firebase meta-data, whose app-specific values no AAR can supply) and flavored Kotlin/res
    // source sets. It also puts the flavor in the output path, so a build that forgot
    // `-Ptabmates.distribution=foss` is visible as `outputs/apk/play/release/` rather than
    // shipping Firebase under a FOSS label.
    flavorDimensions += "distribution"
    productFlavors {
        create(distributionFlavor) { dimension = "distribution" }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/upload_keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // The FOSS release is deliberately left unsigned — F-Droid builds are signed with a
            // separate key, out of band from this pipeline. The explicit null matters: without it
            // the `else` branch would quietly sign the FOSS release with the *debug* key, whose
            // keystore ships with the Android SDK and is identical for every developer on earth.
            // That artifact would still be called `-release`, F-Droid would reject it, and anyone
            // who sideloaded it would be trusting a publicly known signing key.
            signingConfig =
                when {
                    fossDistribution -> null
                    runsCIReleaseBuild -> signingConfigs.getByName("release")
                    else -> signingConfigs.getByName("debug")
                }
        }
    }

    lint {
        if (System.getenv("CI") != null) {
            disable +=
                setOf(
                    "GradleDependency",
                    "GradlePluginVersion",
                    "AndroidGradlePluginVersion",
                )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.composeApp)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

// Proves the claim the F-Droid listing rests on: no proprietary code in the shipped artifact.
//
// The source-set swap already makes "dependency gone, callers left behind" a compile error. This
// covers the other direction, which the compiler cannot see: a dependency that returns to the
// classpath — re-added ungated, or pulled in transitively by some future module — and ships in
// the APK even though nothing calls it.
if (fossDistribution) {
    val forbiddenGroups =
        setOf(
            // Play Core (in-app updates) and everything Firebase Cloud Messaging drags in.
            "com.google.android.gms",
            "com.google.firebase",
            "com.google.android.play",
            // kmpnotifier: -local and -core reach Android only via -push-firebase.
            "io.github.mirzemehdi",
        )
    // com.google.android.material is deliberately absent: Apache-2.0, and fine for F-Droid.

    // Reached through the variant API rather than `configurations.named(...)`: AGP creates the
    // variant classpath configurations after this script is evaluated, so looking one up by name
    // here fails outright.
    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants(selector().withBuildType("release")) { variant ->
            // Walks the resolved dependency graph rather than the resolved *artifacts*: artifact
            // resolution has to pick one published variant per dependency, and the KMP libraries
            // publish several (jar, android-res, android-symbol, ...) that tie unless the view
            // names an artifactType. The graph needs no such choice, and identifies the same
            // modules.
            val offenders =
                variant.runtimeConfiguration.incoming.resolutionResult.rootComponent.map { root ->
                    val seen = mutableSetOf<ComponentIdentifier>()
                    val queue = ArrayDeque(listOf(root))
                    val found = sortedSetOf<String>()
                    while (queue.isNotEmpty()) {
                        val component = queue.removeFirst()
                        if (!seen.add(component.id)) continue
                        val id = component.id
                        if (id is ModuleComponentIdentifier && id.group in forbiddenGroups) {
                            found += "${id.group}:${id.module}:${id.version}"
                        }
                        component.dependencies
                            .filterIsInstance<ResolvedDependencyResult>()
                            .forEach { queue.addLast(it.selected) }
                    }
                    found.toList()
                }

            val checkFossClasspath =
                tasks.register("checkFossClasspath") {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    description = "Fails if a proprietary dependency reaches the FOSS release runtime classpath."
                    doLast {
                        val found = offenders.get()
                        if (found.isNotEmpty()) {
                            throw GradleException(
                                buildString {
                                    appendLine("Proprietary dependencies on the FOSS release runtime classpath:")
                                    found.forEach { appendLine("  - $it") }
                                    appendLine()
                                    append("F-Droid rejects these. Gate them behind `if (!fossDistribution)`.")
                                },
                            )
                        }
                    }
                }

            tasks.named("check") { dependsOn(checkFossClasspath) }
        }
    }
}
