package de.tabmates.features.authentication.presentation.environment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.designsystem.textfields.TabMatesPasswordTextField
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.core.presentation.util.UiText
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.environment_api_key_hint
import tabmatesapp.features.authentication.presentation.generated.resources.environment_api_key_title
import tabmatesapp.features.authentication.presentation.generated.resources.environment_apply
import tabmatesapp.features.authentication.presentation.generated.resources.environment_desc
import tabmatesapp.features.authentication.presentation.generated.resources.environment_row_active
import tabmatesapp.features.authentication.presentation.generated.resources.environment_row_custom
import tabmatesapp.features.authentication.presentation.generated.resources.environment_row_custom_empty
import tabmatesapp.features.authentication.presentation.generated.resources.environment_row_default
import tabmatesapp.features.authentication.presentation.generated.resources.environment_section_choose
import tabmatesapp.features.authentication.presentation.generated.resources.environment_switched
import tabmatesapp.features.authentication.presentation.generated.resources.environment_url_hint
import tabmatesapp.features.authentication.presentation.generated.resources.environment_url_title

private val ContentWidth = 480.dp

@Composable
fun EnvironmentRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    viewModel: EnvironmentViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EnvironmentEvent.Switched -> {
                backStack.removeLastOrNull()
                snackbarHostState.showSnackbar(getString(Res.string.environment_switched))
            }
        }
    }

    EnvironmentScreen(
        state = state,
        onModeSelected = viewModel::onModeSelected,
        onToggleApiKeyVisibility = viewModel::onToggleApiKeyVisibility,
        onSubmitClick = viewModel::onSubmit,
    )
}

@Composable
private fun EnvironmentScreen(
    state: EnvironmentState,
    onModeSelected: (EnvironmentMode) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSubmitClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val contentModifier = Modifier.widthIn(max = ContentWidth).fillMaxWidth()

        Text(
            modifier = contentModifier,
            text = stringResource(Res.string.environment_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(4.dp)
        SectionLabel(
            text = stringResource(Res.string.environment_section_choose),
            modifier = contentModifier,
        )
        Column(
            modifier = contentModifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnvironmentModeRow(
                title = stringResource(Res.string.environment_row_default),
                subtitle = state.defaultUrl,
                selected = state.selectedMode == EnvironmentMode.DEFAULT,
                isActive = !state.isCustomActive,
                enabled = !state.isApplying,
                onClick = { onModeSelected(EnvironmentMode.DEFAULT) },
            )
            EnvironmentModeRow(
                title = stringResource(Res.string.environment_row_custom),
                subtitle =
                    state.storedCustomUrl ?: stringResource(Res.string.environment_row_custom_empty),
                selected = state.selectedMode == EnvironmentMode.CUSTOM,
                isActive = state.isCustomActive,
                enabled = !state.isApplying,
                onClick = { onModeSelected(EnvironmentMode.CUSTOM) },
            )
        }
        AnimatedVisibility(visible = state.selectedMode == EnvironmentMode.CUSTOM) {
            CustomEnvironmentForm(
                state = state,
                onToggleApiKeyVisibility = onToggleApiKeyVisibility,
                onSubmit = {
                    if (state.canSubmit) {
                        focusManager.clearFocus()
                        onSubmitClick()
                    }
                },
                modifier = contentModifier,
            )
        }
        VerticalSpacer(4.dp)
        TabMatesButton(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            text = stringResource(Res.string.environment_apply),
            onClick = onSubmitClick,
            enabled = state.canSubmit,
            isLoading = state.isApplying,
        )
        VerticalSpacer(16.dp)
    }
}

/**
 * [selected] is what the user is pointing at, [isActive] what the app talks to right now — they
 * only differ between picking a row and confirming it, which is exactly when the badge matters.
 */
@Composable
private fun EnvironmentModeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The row owns the click, so the button must not take one of its own.
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isActive) {
                HorizontalSpacer(8.dp)
                ActiveBadge()
            }
        }
    }
}

@Composable
private fun ActiveBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.environment_row_active),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CustomEnvironmentForm(
    state: EnvironmentState,
    onToggleApiKeyVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabMatesTextField(
                modifier = Modifier.fillMaxWidth(),
                state = state.urlTextFieldState,
                title = stringResource(Res.string.environment_url_title),
                placeholder = stringResource(Res.string.environment_url_hint),
                enabled = !state.isApplying,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                isError = state.urlError != null,
                supportingText = state.urlError?.asString(),
            )
            TabMatesPasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                state = state.apiKeyTextFieldState,
                isPasswordVisible = state.isApiKeyVisible,
                onToggleVisibilityClick = onToggleApiKeyVisibility,
                title = stringResource(Res.string.environment_api_key_title),
                placeholder = stringResource(Res.string.environment_api_key_hint),
                enabled = !state.isApplying,
                imeAction = ImeAction.Go,
                // A server key, not a login secret — keep it out of the platform password manager.
                contentType = null,
                isError = state.apiKeyError != null,
                supportingText = state.apiKeyError?.asString(),
                onKeyboardAction = onSubmit,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EnvironmentScreenDefaultSelectedPreview() {
    TabMatesTheme {
        Surface {
            EnvironmentScreen(
                state = previewState(),
                onModeSelected = {},
                onToggleApiKeyVisibility = {},
                onSubmitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EnvironmentScreenCustomSelectedPreview() {
    TabMatesTheme {
        Surface {
            EnvironmentScreen(
                state = previewState(selectedMode = EnvironmentMode.CUSTOM, canApply = true),
                onModeSelected = {},
                onToggleApiKeyVisibility = {},
                onSubmitClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EnvironmentScreenCustomErrorPreview() {
    TabMatesTheme {
        Surface {
            EnvironmentScreen(
                state =
                    previewState(
                        selectedMode = EnvironmentMode.CUSTOM,
                        canApply = true,
                        apiKeyError = UiText.DynamicString("The server rejected this API key"),
                    ),
                onModeSelected = {},
                onToggleApiKeyVisibility = {},
                onSubmitClick = {},
            )
        }
    }
}

private fun previewState(
    selectedMode: EnvironmentMode = EnvironmentMode.DEFAULT,
    canApply: Boolean = false,
    apiKeyError: UiText? = null,
) = EnvironmentState(
    urlTextFieldState = TextFieldState("https://staging.example.com"),
    apiKeyTextFieldState = TextFieldState("dev-api-key"),
    isCustomActive = false,
    defaultUrl = "https://api.tabmates.de",
    storedCustomUrl = "https://staging.example.com",
    selectedMode = selectedMode,
    canApply = canApply,
    apiKeyError = apiKeyError,
)
