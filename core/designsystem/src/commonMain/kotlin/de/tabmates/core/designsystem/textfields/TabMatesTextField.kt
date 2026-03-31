package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme

/**
 * TabMates styled text field built on top of [BasicTextField] and [TabMatesTextFieldLayout].
 *
 * Provides a consistent look-and-feel across the app with support for titles,
 * placeholder text, error states, and supporting/helper text.
 *
 * @param state The [TextFieldState] that holds the current text and selection.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param placeholder Optional hint text displayed when the field is empty.
 * @param title Optional label displayed above the text field.
 * @param supportingText Optional helper or error message displayed below the text field.
 * @param isError When `true`, the field is styled to indicate a validation error.
 * @param singleLine When `true`, the field is restricted to a single line of input.
 * @param enabled When `false`, the field is non-editable and visually dimmed.
 * @param keyboardType The [KeyboardType] to use for the software keyboard.
 * @param onFocusChanged Callback invoked when the field's focus state changes.
 */
@Composable
fun TabMatesTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
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
        BasicTextField(
            state = state,
            enabled = enabled,
            lineLimits =
                if (singleLine) {
                    TextFieldLineLimits.SingleLine
                } else {
                    TextFieldLineLimits.Default
                },
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.Unspecified // MaterialTheme.colorScheme.extended.textPlaceholder
                        },
                ),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = keyboardType,
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            interactionSource = interactionSource,
            modifier = styleModifier,
            decorator = { innerBox ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
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
            },
        )
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldDefaultPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextField(
                    state = TextFieldState("user@example.com"),
                    title = "Email",
                    placeholder = "Enter your email",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldPlaceholderPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextField(
                    state = TextFieldState(),
                    title = "Username",
                    placeholder = "Type here...",
                    supportingText = "Enter your username",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldErrorPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextField(
                    state = TextFieldState("123"),
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
private fun TabMatesTextFieldDisabledPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextField(
                    state = TextFieldState("Disabled content"),
                    title = "Read-only Field",
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldNoTitlePreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextField(
                    state = TextFieldState("No title field"),
                    placeholder = "Placeholder",
                    supportingText = "Helper text without a title",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
