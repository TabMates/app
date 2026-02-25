package de.tabmates.convention

import org.gradle.api.Project

internal fun Project.defaultNamespace(): String {
    val prefix = "de.tabmates"
    val suffix = path
        .trim(':')
        .split(':')
        .joinToString(".") { it.lowercase().replace("-", "_") }
        .ifBlank { name.lowercase().replace("-", "_") }

    return "$prefix.$suffix"
}

internal fun Project.frameworkBaseName(): String {
    val readable = path.trim(':')
        .split(':')
        .joinToString("_") { it.replace("-", "_") }
        .ifBlank { name }

    return readable
}