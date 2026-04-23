package de.tabmates.features.authentication.presentation.welcome

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.preview.PreviewAll
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Register
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.login
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_guest
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_register
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_title

@Composable
fun WelcomeScreenRoot(
    backStack: NavBackStack<NavKey>,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WelcomeScreen(
        onRegisterClick = {
            backStack.add(Register)
        },
        onLoginClick = {
            backStack.add(Login)
        },
        onGuestClick = onGuestClick,
        modifier = modifier,
    )
}

@Composable
private fun WelcomeScreen(
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useTwoPane =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    if (useTwoPane) {
        TwoPane(
            onRegisterClick = onRegisterClick,
            onLoginClick = onLoginClick,
            onGuestClick = onGuestClick,
            modifier = modifier,
        )
    } else {
        SinglePane(
            onRegisterClick = onRegisterClick,
            onLoginClick = onLoginClick,
            onGuestClick = onGuestClick,
            modifier = modifier,
        )
    }
}

@Composable
fun TwoPane(
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Placeholder()
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = stringResource(Res.string.welcome_title),
                style = MaterialTheme.typography.titleLarge,
            )
            VerticalSpacer(16.dp)
            TabMatesButton(
                onClick = onRegisterClick,
                text = stringResource(Res.string.welcome_button_register),
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
            TabMatesButton(
                onClick = onLoginClick,
                style = TabMatesButtonStyle.Secondary,
                text = stringResource(Res.string.login),
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
            TabMatesButton(
                onClick = onGuestClick,
                style = TabMatesButtonStyle.Text,
                text = stringResource(Res.string.welcome_button_guest),
                modifier =
                    Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SinglePane(
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().scrollable(rememberScrollState(), Orientation.Vertical),
    ) {
        Placeholder()
        VerticalSpacer(16.dp)
        Text(
            text = stringResource(Res.string.welcome_title),
            style = MaterialTheme.typography.titleLarge,
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            onClick = onRegisterClick,
            text = stringResource(Res.string.welcome_button_register),
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            onClick = onLoginClick,
            style = TabMatesButtonStyle.Secondary,
            text = stringResource(Res.string.login),
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TabMatesButton(
            onClick = onGuestClick,
            style = TabMatesButtonStyle.Text,
            text = stringResource(Res.string.welcome_button_guest),
            modifier =
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(),
        )
    }
}

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.sizeIn(maxWidth = 300.dp, maxHeight = 300.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewAll
@Composable
private fun WelcomeScreenPreview() {
    TabMatesTheme {
        Surface {
            WelcomeScreen(
                onRegisterClick = {},
                onLoginClick = {},
                onGuestClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
