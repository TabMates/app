package de.tabmates.features.authentication.presentation.welcome

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import de.tabmates.core.designsystem.logo.TabMatesLogo
import de.tabmates.core.designsystem.preview.PreviewAll
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.headlineMediumSemiBold
import de.tabmates.features.authentication.presentation.navigation.EnvironmentSettings
import de.tabmates.features.authentication.presentation.navigation.Login
import de.tabmates.features.authentication.presentation.navigation.Register
import de.tabmates.features.authentication.presentation.navigation.RegisterGuest
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.app_name
import tabmatesapp.features.authentication.presentation.generated.resources.environment_default_label
import tabmatesapp.features.authentication.presentation.generated.resources.ic_settings
import tabmatesapp.features.authentication.presentation.generated.resources.login
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_environment
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_guest
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_button_register
import tabmatesapp.features.authentication.presentation.generated.resources.welcome_title_prefix

@Composable
fun WelcomeScreenRoot(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    WelcomeScreen(
        isEnvironmentSwitchSupported = state.isEnvironmentSwitchSupported,
        customEnvironmentHost = state.customEnvironmentHost,
        onRegisterClick = {
            backStack.add(Register)
        },
        onLoginClick = {
            backStack.add(Login)
        },
        onGuestClick = {
            backStack.add(RegisterGuest)
        },
        onEnvironmentClick = {
            backStack.add(EnvironmentSettings)
        },
        modifier = modifier,
    )
}

@Composable
private fun WelcomeScreen(
    isEnvironmentSwitchSupported: Boolean,
    customEnvironmentHost: String?,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGuestClick: () -> Unit,
    onEnvironmentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useTwoPane =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    // Overlaid rather than placed in either pane: the panes lay their content out centred, and the
    // action belongs to the screen, not to the column it would otherwise sit in.
    Box(modifier = modifier.fillMaxSize()) {
        if (useTwoPane) {
            TwoPane(
                onRegisterClick = onRegisterClick,
                onLoginClick = onLoginClick,
                onGuestClick = onGuestClick,
            )
        } else {
            SinglePane(
                onRegisterClick = onRegisterClick,
                onLoginClick = onLoginClick,
                onGuestClick = onGuestClick,
            )
        }
        if (isEnvironmentSwitchSupported) {
            EnvironmentButton(
                customEnvironmentHost = customEnvironmentHost,
                onClick = onEnvironmentClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
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
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Logo()
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            WelcomeTitle()
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
        Logo()
        VerticalSpacer(16.dp)
        WelcomeTitle()
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

/**
 * Tinted while a custom backend is active, and labelled with its host: the icon alone cannot say
 * *which* server the app talks to, and that is the answer to "why does my account not exist here".
 */
@Composable
private fun EnvironmentButton(
    customEnvironmentHost: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultLabel = stringResource(Res.string.environment_default_label)
    val label = stringResource(Res.string.welcome_button_environment, customEnvironmentHost ?: defaultLabel)
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_settings),
            contentDescription = label,
            tint =
                if (customEnvironmentHost != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.sizeIn(maxWidth = 140.dp, maxHeight = 140.dp),
    ) {
        TabMatesLogo(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun WelcomeTitle(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.welcome_title_prefix),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMediumSemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewAll
@Composable
private fun WelcomeScreenPreview() {
    TabMatesTheme {
        Surface {
            WelcomeScreen(
                isEnvironmentSwitchSupported = true,
                customEnvironmentHost = null,
                onRegisterClick = {},
                onLoginClick = {},
                onGuestClick = {},
                onEnvironmentClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
