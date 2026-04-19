package de.tabmates.features.authentication.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.features.authentication.presentation.emailverification.EmailVerificationRoot
import de.tabmates.features.authentication.presentation.login.LoginRoot
import de.tabmates.features.authentication.presentation.register.RegisterRoot
import de.tabmates.features.authentication.presentation.registersuccess.RegisterSuccessRoot
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
        PlaceholderScreen("Reset Password – token: ${it.token}")
    }

    entry<ForgotPassword> {
        PlaceholderScreen("Forgot Password")
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    onNext: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = { onNext?.invoke() },
            enabled = onNext != null,
        ) {
            Text(text = title)
        }
    }
}
