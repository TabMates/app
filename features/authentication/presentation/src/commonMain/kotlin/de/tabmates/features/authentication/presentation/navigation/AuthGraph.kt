package de.tabmates.features.authentication.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
            subclass(EmailVerification::class)
        }
    }

fun EntryProviderScope<NavKey>.authGraph(
    backStack: NavBackStack<NavKey>,
    onGuestClick: () -> Unit,
) {
    entry<Welcome> {
        WelcomeScreenRoot(
            backStack = backStack,
            onGuestClick = onGuestClick,
        )
    }

    entry<Login> {
        PlaceholderScreen("Login") {
            backStack.add(Register)
        }
    }

    entry<Register> {
        PlaceholderScreen("Register") {
            backStack.add(EmailVerification)
        }
    }

    entry<EmailVerification> {
        PlaceholderScreen("Email Verification")
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
