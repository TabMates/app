package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
 * @param singleLine When `true` (default), the field is restricted to a single line of input,
 *   letting hardware Tab/Enter reach focus traversal and the keyboard action. Use
 *   [TabMatesMultiLineTextField] for intentional multi-line input.
 * @param enabled When `false`, the field is non-editable and visually dimmed.
 * @param keyboardType The [KeyboardType] to use for the software keyboard.
 * @param imeAction The [ImeAction] shown on the software-keyboard action button.
 * @param capitalization The [KeyboardCapitalization] the software keyboard applies by default.
 * @param contentType The [ContentType] for autofill support.
 * @param onFocusChanged Callback invoked when the field's focus state changes.
 * @param onKeyboardAction Callback invoked when the IME action button is pressed. When `null`
 *   (default), the platform default is performed (e.g. [ImeAction.Next] moves focus).
 */
@Composable
fun TabMatesTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    contentType: ContentType? = null,
    inputTransformation: InputTransformation? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onKeyboardAction: (() -> Unit)? = null,
) {
    TabMatesTextFieldLayout(
        isError = isError,
        supportingText = supportingText,
        onFocusChanged = onFocusChanged,
        modifier = modifier,
    ) { interactionSource ->
        OutlinedTextField(
            state = state,
            enabled = enabled,
            label =
                if (title != null) {
                    { Text(text = title) }
                } else {
                    null
                },
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
                    imeAction = imeAction,
                    capitalization = capitalization,
                ),
            inputTransformation = inputTransformation,
            onKeyboardAction = onKeyboardAction?.let { action -> KeyboardActionHandler { action() } },
            interactionSource = interactionSource,
            placeholder =
                if (state.text.isEmpty() && placeholder != null) {
                    {
                        Text(
                            text = placeholder,
                            // color = MaterialTheme.colorScheme.extended.textPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    null
                },
            modifier =
                if (contentType != null) {
                    Modifier
                        .fillMaxWidth()
                        .semantics { this.contentType = contentType }
                } else {
                    Modifier.fillMaxWidth()
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
