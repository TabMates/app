package de.tabmates.features.authentication.presentation.navigation

import de.tabmates.core.presentation.navigation.LoggableNavKey
import de.tabmates.core.presentation.navigation.LoggedIn
import de.tabmates.core.presentation.navigation.ScreenWithTopBar
import de.tabmates.core.presentation.util.UiText
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : LoggableNavKey()

@Serializable
data object Login : LoggableNavKey(), ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

/**
 * Sign back in after the session expired.
 *
 * Marked [LoggedIn] even though there are no credentials: it is reached *from* the app shell,
 * which stays up on the device's local data while the session is stale, and the shell branches on
 * whether the current route is a [LoggedIn] one.
 */
@Serializable
data object Reauth : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
    override val topBarTitle: UiText get() = UiText.DynamicString("")
}

/**
 * Password reset reached from [Reauth]. A [LoggedIn] twin of [ForgotPassword] so it renders in the
 * same shell: pushing the plain key would flip the app to the logged-out navigation graph, which
 * has no entries for the routes still sitting underneath it on the back stack.
 */
@Serializable
data object ReauthForgotPassword : LoggableNavKey(), LoggedIn, ScreenWithTopBar {
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
