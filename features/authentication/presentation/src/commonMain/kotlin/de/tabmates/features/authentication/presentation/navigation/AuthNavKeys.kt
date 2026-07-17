package de.tabmates.features.authentication.presentation.navigation

import de.tabmates.core.presentation.navigation.LoggableNavKey
import de.tabmates.core.presentation.navigation.ScreenWithTopBar
import de.tabmates.core.presentation.util.UiText
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : LoggableNavKey()

@Serializable
data object Login : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data object Register : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data object RegisterGuest : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data class RegisterSuccess(val email: String) : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data class EmailVerification(val token: String) : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data object ForgotPassword : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

@Serializable
data class ResetPassword(val token: String) : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}
