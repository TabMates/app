package de.tabmates.features.authentication.presentation.navigation

import de.tabmates.core.presentation.navigation.LoggableNavKey
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : LoggableNavKey()

@Serializable
data object Login : LoggableNavKey()

@Serializable
data object Register : LoggableNavKey()

@Serializable
data object EmailVerification : LoggableNavKey()
