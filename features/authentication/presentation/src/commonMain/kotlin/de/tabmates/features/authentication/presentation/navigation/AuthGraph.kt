package de.tabmates.features.authentication.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.features.authentication.presentation.emailverification.EmailVerificationRoot
import de.tabmates.features.authentication.presentation.forgotpassword.ForgotPasswordScreenRoot
import de.tabmates.features.authentication.presentation.login.LoginRoot
import de.tabmates.features.authentication.presentation.register.RegisterRoot
import de.tabmates.features.authentication.presentation.registersuccess.RegisterSuccessRoot
import de.tabmates.features.authentication.presentation.resetpassword.ResetPasswordScreenRoot
import de.tabmates.features.authentication.presentation.welcome.WelcomeScreenRoot
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val authSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Welcome::class)
            subclass(Login::class)
            subclass(Register::class)
            subclass(RegisterSuccess::class)
            subclass(EmailVerification::class)
            subclass(ResetPassword::class)
            subclass(ForgotPassword::class)
        }
    }

fun EntryProviderScope<NavKey>.authGraph(
    backStack: NavBackStack<NavKey>,
    onGuestClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    entry<Welcome> {
        WelcomeScreenRoot(
            backStack = backStack,
            onGuestClick = onGuestClick,
        )
    }

    entry<Login> {
        LoginRoot(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            onContinueAsGuestClick = onGuestClick,
            onLoginSuccess = onLoginSuccess,
        )
    }

    entry<Register> {
        RegisterRoot(
            backStack = backStack,
            onGuestClick = onGuestClick,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<RegisterSuccess> {
        RegisterSuccessRoot(
            backStack = backStack,
            email = it.email,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<EmailVerification> {
        EmailVerificationRoot(
            token = it.token,
            backStack = backStack,
        )
    }

    entry<ResetPassword> {
        ResetPasswordScreenRoot(token = it.token)
    }

    entry<ForgotPassword> {
        ForgotPasswordScreenRoot()
    }
}
