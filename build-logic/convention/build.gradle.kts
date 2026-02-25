import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "de.tabmates.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
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