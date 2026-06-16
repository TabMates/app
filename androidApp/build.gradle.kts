plugins {
    alias(libs.plugins.tabmates.convention.android.application.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "de.tabmates.androidapp"

    defaultConfig {
        applicationId = "de.tabmates.androidapp"
        versionCode = 1
        versionName = providers.gradleProperty("APP_VERSION").get()
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
