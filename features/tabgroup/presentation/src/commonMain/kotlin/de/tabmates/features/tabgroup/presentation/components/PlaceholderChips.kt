package de.tabmates.features.tabgroup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
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

@Composable
fun AddPlaceholderDialog(
    textState: TextFieldState,
    title: String,
    nameLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TabMatesTextField(
                state = textState,
                title = nameLabel,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(text = confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
    )
}
