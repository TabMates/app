import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Thin JVM launcher + packaging module for the desktop app. Kept separate from the multiplatform
// composeApp because Conveyor's cross-OS dependency configurations would otherwise leak into
// composeApp's iOS/wasm variant resolution. Owns Conveyor packaging + silent self-update.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.conveyor)
}

// Conveyor reads project.version as the package version. Store formats require MAJOR.MINOR.PATCH,
// so APP_VERSION ("1.0") is normalized to "1.0.0".
version = providers.gradleProperty("APP_VERSION").get().toThreePartVersion()

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.composeApp)
    implementation(compose.desktop.currentOs)

    // Per-OS Compose/Skiko native bundles so Conveyor can cross-build every desktop target from one
    // machine. These configurations are created by the Conveyor Gradle plugin.
    "macAarch64"(compose.desktop.macos_arm64)
    "macAmd64"(compose.desktop.macos_x64)
    "windowsAmd64"(compose.desktop.windows_x64)
    "linuxAmd64"(compose.desktop.linux_x64)
}

compose.desktop {
    application {
        // Entry point lives in composeApp (src/desktopMain/.../main.kt).
        mainClass = "de.tabmates.composeapp.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TabMates"
            packageVersion = project.version.toString()
        }
    }
}

fun String.toThreePartVersion(): String =
    when (split(".").size) {
        1 -> "$this.0.0"
        2 -> "$this.0"
        else -> this
    }
