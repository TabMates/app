package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
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
 * Renders an optional [title] above the text field content, applies consistent
 * border/background styling based on focus and error state, and shows an optional
 * [supportingText] message below the field. The actual input widget is provided via
 * the [textField] slot, which receives a pre-styled [Modifier] and a shared
 * [MutableInteractionSource] so that focus-driven visuals stay in sync.
 *
 * @param title Optional label displayed above the text field.
 * @param isError When `true`, the border and supporting text are tinted with the error color.
 * @param supportingText Optional helper or error message displayed below the text field.
 * @param enabled When `false`, the background switches to a disabled style.
 * @param onFocusChanged Callback invoked whenever the field gains or loses focus.
 * @param modifier [Modifier] applied to the outer [Column] container.
 * @param textField Slot composable that receives a styled [Modifier] and an
 *   [MutableInteractionSource] to be forwarded to the underlying input component.
 */
@Composable
fun TabMatesTextFieldLayout(
    title: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier, MutableInteractionSource) -> Unit,
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    val textFieldStyleModifier =
        Modifier
            .fillMaxWidth()
            .background(
                color =
                    when {
                        isFocused -> {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.05f,
                            )
                        }

                        enabled -> {
                            MaterialTheme.colorScheme.surface
                        }

                        else -> {
                            Color.Unspecified
                        } // MaterialTheme.colorScheme.extended.secondaryFill
                    },
                shape = RoundedCornerShape(8.dp),
            ).border(
                width = 1.dp,
                color =
                    when {
                        isError -> MaterialTheme.colorScheme.error
                        isFocused -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                shape = RoundedCornerShape(8.dp),
            ).padding(12.dp)

    Column(
        modifier = modifier,
    ) {
        title?.let {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                // color = MaterialTheme.colorScheme.extended.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        textField(textFieldStyleModifier, interactionSource)

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
                    title = "Email",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { modifier, interactionSource ->
                    BasicTextField(
                        value = "user@example.com",
                        onValueChange = {},
                        modifier = modifier,
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
                    title = "Username",
                    supportingText = "Enter your username",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { modifier, interactionSource ->
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        modifier = modifier,
                        interactionSource = interactionSource,
                        decorationBox = { innerTextField ->
                            Text(text = "Type here...")
                            innerTextField()
                        },
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
                    title = "Password",
                    isError = true,
                    supportingText = "Password must be at least 8 characters",
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { modifier, interactionSource ->
                    BasicTextField(
                        value = "123",
                        onValueChange = {},
                        modifier = modifier,
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
                    title = "Read-only Field",
                    enabled = false,
                    onFocusChanged = {},
                    modifier = Modifier.fillMaxWidth(),
                ) { modifier, interactionSource ->
                    BasicTextField(
                        value = "Disabled content",
                        onValueChange = {},
                        enabled = false,
                        modifier = modifier,
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
                ) { modifier, interactionSource ->
                    BasicTextField(
                        value = "No title field",
                        onValueChange = {},
                        modifier = modifier,
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}
