package de.tabmates.composeapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform