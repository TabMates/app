package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.theme.TabMatesTheme

/**
 * TabMates styled multi-line text field built on top of [BasicTextField].
 *
 * Provides a rounded, bordered container that expands vertically up to
 * [maxHeightInLines] as the user types. An optional [bottomContent] slot
 * allows placing action icons or auxiliary controls beneath the text input.
 *
 * Tapping anywhere inside the container (including the bottom-content area)
 * moves focus to the underlying text field.
 *
 * @param state The [TextFieldState] that holds the current text and selection.
 * @param modifier [Modifier] applied to the outer layout container.
 * @param placeholder Optional hint text displayed when the field is empty.
 * @param enabled When `false`, the field is non-editable and visually dimmed.
 * @param keyboardOptions Software-keyboard configuration such as capitalization and IME action.
 * @param onKeyboardAction Callback invoked when the IME action button is pressed. When `null`
 *   (default), the platform default is performed.
 * @param maxHeightInLines Maximum number of visible lines before the field scrolls (default **3**).
 * @param bottomContent Optional composable row rendered below the text input, useful for
 *   action buttons or character counters.
 */
@Composable
fun TabMatesMultiLineTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: (() -> Unit)? = null,
    maxHeightInLines: Int = 3,
    bottomContent: @Composable (RowScope.() -> Unit)? = null,
) {
    val textFieldFocusRequester =
        remember {
            FocusRequester()
        }
    Column(
        modifier =
            modifier
                .background(
                    color = Color.Unspecified, // MaterialTheme.colorScheme.extended.surfaceLower,
                    shape = RoundedCornerShape(16.dp),
                ).border(
                    width = 1.dp,
                    color = Color.Unspecified, // MaterialTheme.colorScheme.extended.surfaceOutline,
                    shape = RoundedCornerShape(16.dp),
                ).clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {
                        textFieldFocusRequester.requestFocus()
                    },
                ).padding(
                    vertical = 12.dp,
                    horizontal = 16.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            state = state,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(textFieldFocusRequester),
            /*textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                ),*/
            lineLimits =
                TextFieldLineLimits.MultiLine(
                    minHeightInLines = 1,
                    maxHeightInLines = maxHeightInLines,
                ),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction?.let { action -> KeyboardActionHandler { action() } },
            placeholder =
                if (placeholder != null && state.text.isEmpty()) {
                    {
                        Text(
                            text = placeholder,
                            // color = MaterialTheme.colorScheme.extended.textPlaceholder,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    null
                },
        )
        bottomContent?.let {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomContent(this)
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesMultiLineTextFieldEmptyPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesMultiLineTextField(
                    state = TextFieldState(),
                    placeholder = "Write something…",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesMultiLineTextFieldFilledPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesMultiLineTextField(
                    state =
                        TextFieldState(
                            "Hello, this is a multi-line text field with some content. Hello, this is a multi-line text field with some content.Hello, this is a multi-line text field with some content.",
                        ),
                    placeholder = "Write something…",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesMultiLineTextFieldDisabledPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesMultiLineTextField(
                    state = TextFieldState("Disabled content"),
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TabMatesMultiLineTextFieldWithBottomContentPreview() {
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TabMatesMultiLineTextField(
                    state = TextFieldState("Message with actions"),
                    placeholder = "Write something…",
                    modifier = Modifier.fillMaxWidth(),
                    bottomContent = {
                        Text(
                            text = "19/200",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}
