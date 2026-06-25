plugins {
    alias(libs.plugins.tabmates.convention.android.application.compose)
    alias(libs.plugins.google.services)
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

    val appVersion = providers.gradleProperty("APP_VERSION").get()

    // Release builds in CI are signed with the upload keystore; local builds fall back to debug.
    val runsCIReleaseBuild = System.getenv("SIGNING_STORE_PASSWORD") != null

    defaultConfig {
        applicationId = "de.tabmates.androidapp"
        versionCode = versionNameToCode(appVersion)
        versionName = appVersion
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
            signingConfig = signingConfigs.getByName(if (runsCIReleaseBuild) "release" else "debug")
        }
    }

    lint {
        sarifReport = true
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
