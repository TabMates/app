import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "de.tabmates.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform"){
            id = "tabmates.convention.kmp"
            implementationClass = "KmpConventionPlugin"
        }
        register("composeMultiplatform"){
            id = "tabmates.convention.cmp"
            implementationClass = "CmpConventionPlugin"
        }
    }
}