package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_confirm_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_done_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_name_hint
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_name_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_check
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_person_add

@Composable
fun PlaceholderChip(
    name: String,
    initial: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    removeContentDescription: String? = null,
) {
    InputChip(
        selected = false,
        onClick = onRemove ?: {},
        label = { Text(name) },
        modifier = modifier,
        avatar = {
            Box(
                modifier =
                    Modifier
                        .size(InputChipDefaults.AvatarSize)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        trailingIcon =
            onRemove?.let {
                {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = removeContentDescription,
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                }
            },
    )
}

@Composable
fun AddPlaceholderButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_person_add),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

/**
 * Sits where the next placeholder will appear. Enter commits and leaves the field open, so the item
 * that pops up above is the cue that another name can follow.
 */
@Composable
fun NewPlaceholderRow(
    textState: TextFieldState,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TabMatesTextField(
            state = textState,
            placeholder = stringResource(Res.string.group_people_add_name_placeholder),
            supportingText = stringResource(Res.string.group_people_add_name_hint),
            // Deliberately never disabled while a name is in flight: dropping enabled takes the
            // focus with it, the keyboard closes, and the run of names is broken. The ViewModel
            // already rejects a second submit while one is running.
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            onKeyboardAction = { onSubmit() },
            trailingIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.group_people_add_done_cd),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
        )
        // Same job as the keyboard's enter key, for anyone not reaching for it.
        FilledIconButton(onClick = onSubmit) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.group_people_add_confirm_cd),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
