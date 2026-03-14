package de.tabmates.core.domain.logging

interface TabMatesLogger {
    fun debug(
        tag: String,
        message: String,
    )

    fun info(
        tag: String,
        message: String,
    )

    fun warning(
        tag: String,
        message: String,
    )

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
