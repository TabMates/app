package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.core.designsystem.generated.resources.Res
import tabmatesapp.core.designsystem.generated.resources.hide_password
import tabmatesapp.core.designsystem.generated.resources.ic_eye
import tabmatesapp.core.designsystem.generated.resources.ic_eye_off
import tabmatesapp.core.designsystem.generated.resources.show_password

/**
 * TabMates styled password text field built on top of [BasicSecureTextField] and
 * [TabMatesTextFieldLayout].
 *
 * Provides a consistent password-entry experience across the app with a toggleable
 * visibility icon, placeholder support, error styling, and an optional supporting message.
 *
 * @param state The [TextFieldState] that holds the current text and selection.
 * @param isPasswordVisible Whether the password is currently shown in plain text.
 * @param onToggleVisibilityClick Callback invoked when the user taps the show/hide icon.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param placeholder Optional hint text displayed when the field is empty.
 * @param title Optional label displayed above the text field.
 * @param supportingText Optional helper or error message displayed below the text field.
 * @param isError When `true`, the field is styled to indicate a validation error.
 * @param enabled When `false`, the field is non-editable and visually dimmed.
 * @param onFocusChanged Callback invoked when the field's focus state changes.
 */
@Composable
fun TabMatesPasswordTextField(
    state: TextFieldState,
    isPasswordVisible: Boolean,
    onToggleVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    TabMatesTextFieldLayout(
        title = title,
        isError = isError,
        supportingText = supportingText,
        enabled = enabled,
        onFocusChanged = onFocusChanged,
        modifier = modifier,
    ) { styleModifier, interactionSource ->
        BasicSecureTextField(
            state = state,
            modifier = styleModifier,
            enabled = enabled,
            textObfuscationMode =
                if (isPasswordVisible) {
                    TextObfuscationMode.Visible
                } else {
                    TextObfuscationMode.Hidden
                },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.Unspecified // MaterialTheme.colorScheme.extended.textPlaceholder
                        },
                ),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorator = { innerBox ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.text.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                // color = MaterialTheme.colorScheme.extended.textPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerBox()
                    }

                    Icon(
                        imageVector =
                            if (isPasswordVisible) {
                                vectorResource(Res.drawable.ic_eye_off)
                            } else {
                                vectorResource(Res.drawable.ic_eye)
                            },
                        contentDescription =
                            if (isPasswordVisible) {
                                stringResource(Res.string.hide_password)
                            } else {
                                stringResource(Res.string.show_password)
                            },
                        // tint = MaterialTheme.colorScheme.extended.textDisabled,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication =
                                        ripple(
                                            bounded = false,
                                            radius = 24.dp,
                                        ),
                                    onClick = onToggleVisibilityClick,
                                ),
                    )
                }
            },
        )
    }
}

@PreviewThemes
@Composable
private fun TabMatesPasswordTextFieldDefaultPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesPasswordTextField(
                    state = TextFieldState("s3cureP@ss"),
                    isPasswordVisible = false,
                    onToggleVisibilityClick = {},
                    title = "Password",
                    placeholder = "Enter your password",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesPasswordTextFieldVisiblePreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesPasswordTextField(
                    state = TextFieldState("s3cureP@ss"),
                    isPasswordVisible = true,
                    onToggleVisibilityClick = {},
                    title = "Password",
                    placeholder = "Enter your password",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesPasswordTextFieldPlaceholderPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesPasswordTextField(
                    state = TextFieldState(),
                    isPasswordVisible = false,
                    onToggleVisibilityClick = {},
                    title = "Password",
                    placeholder = "Enter your password",
                    supportingText = "Must be at least 8 characters",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesPasswordTextFieldErrorPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesPasswordTextField(
                    state = TextFieldState("123"),
                    isPasswordVisible = false,
                    onToggleVisibilityClick = {},
                    title = "Password",
                    isError = true,
                    supportingText = "Password must be at least 8 characters",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesPasswordTextFieldDisabledPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesPasswordTextField(
                    state = TextFieldState("disabled"),
                    isPasswordVisible = false,
                    onToggleVisibilityClick = {},
                    title = "Password",
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
