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
data class RegisterSuccess(val email: String) : LoggableNavKey()

@Serializable
data class EmailVerification(val token: String) : LoggableNavKey()

@Serializable
data class ResetPassword(val token: String) : LoggableNavKey()
