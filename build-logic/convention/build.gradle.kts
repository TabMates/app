plugins {
    `kotlin-dsl`
}

group = "de.tabmates.buildlogic.convention"

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