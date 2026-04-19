plugins {
    alias(libs.plugins.tabmates.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.domain)
                implementation(projects.features.authentication.domain)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
