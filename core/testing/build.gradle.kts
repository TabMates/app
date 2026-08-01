// Shared fakes for the :core:* modules, laid out like the per-feature testing modules: one module
// beside the packages it serves, so a fake written for one of them cannot be copied into the next.
plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.domain)
                // For EnvironmentUrls: the fakes derive the same values production does, so a test
                // asserting a websocket URL is asserting the real derivation, not a copy of it.
                implementation(projects.core.data)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
