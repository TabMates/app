package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme

/**
 * Shows optional [supportingText] message below the field. The actual input widget is provided via
 * the [textField] slot, which receives a pre-styled [Modifier] and a shared
 * [MutableInteractionSource] so that focus-driven visuals stay in sync.
 *
 * @param isError When `true`, the border and supporting text are tinted with the error color.
 * @param supportingText Optional helper or error message displayed below the text field.
 * @param onFocusChanged Callback invoked whenever the field gains or loses focus.
 * @param modifier [Modifier] applied to the outer [Column] container.
 * @param textField Slot composable that receives a styled [Modifier] and an
 *   [MutableInteractionSource] to be forwarded to the underlying input component.
 */
@Composable
fun TabMatesTextFieldLayout(
    isError: Boolean = false,
    supportingText: String? = null,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (MutableInteractionSource) -> Unit,
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    Column(
        modifier = modifier,
    ) {
        textField(interactionSource)

        supportingText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified // MaterialTheme.colorScheme.extended.textTertiary
                    },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldLayoutDefaultPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextFieldLayout(
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { interactionSource ->
                    OutlinedTextField(
                        value = "user@example.com",
                        onValueChange = {},
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldLayoutEmptyPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextFieldLayout(
                    supportingText = "Enter your username",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { interactionSource ->
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        interactionSource = interactionSource,
                        placeholder = { Text(text = "Type here...") },
                    )
                }
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldLayoutErrorPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextFieldLayout(
                    isError = true,
                    supportingText = "Password must be at least 8 characters",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { interactionSource ->
                    OutlinedTextField(
                        value = "123",
                        onValueChange = {},
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldLayoutDisabledPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextFieldLayout(
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { interactionSource ->
                    OutlinedTextField(
                        value = "Disabled content",
                        onValueChange = {},
                        enabled = false,
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesTextFieldLayoutNoTitlePreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesTextFieldLayout(
                    supportingText = "Helper text without a title",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { interactionSource ->
                    OutlinedTextField(
                        value = "No title field",
                        onValueChange = {},
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}
