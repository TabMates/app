package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_danger_zone
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_default_currency
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_description_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_leave_group
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_left
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_name_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_saved
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_logout

@Composable
fun GroupSettingsRoot(
    groupId: String,
    onLeft: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingsViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedMessage = stringResource(Res.string.group_settings_saved)
    val leftMessage = stringResource(Res.string.group_settings_left)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GroupSettingsEvent.Saved -> {
                snackbarHostState.showSnackbar(savedMessage)
            }

            GroupSettingsEvent.Left -> {
                snackbarHostState.showSnackbar(leftMessage)
                onLeft()
            }

            is GroupSettingsEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }
    GroupSettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun GroupSettingsScreen(
    state: GroupSettingsState,
    onAction: (GroupSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderRow(
            iconKey = state.iconKey,
            colorKey = state.colorKey,
            title = state.nameTextState.text.toString(),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        TabMatesTextField(
            state = state.nameTextState,
            title = stringResource(Res.string.group_settings_name_label),
            singleLine = true,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        TabMatesTextField(
            state = state.descriptionTextState,
            title = stringResource(Res.string.group_settings_description_label),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        CurrencyRow(
            currencyCode = state.defaultCurrencyCode,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.group_settings_danger_zone),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        LeaveGroupCard(
            isLeaving = state.isLeaving,
            onClick = { onAction(GroupSettingsAction.RequestLeave) },
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.group_settings_leave_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        Button(
            onClick = { onAction(GroupSettingsAction.Save) },
            enabled = !state.isSaving,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Text(stringResource(Res.string.group_settings_save))
        }
    }
    if (state.showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { onAction(GroupSettingsAction.DismissLeaveDialog) },
            title = { Text(stringResource(Res.string.group_settings_leave_dialog_title)) },
            text = { Text(stringResource(Res.string.group_settings_leave_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(GroupSettingsAction.ConfirmLeave) }) {
                    Text(stringResource(Res.string.group_settings_leave_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(GroupSettingsAction.DismissLeaveDialog) }) {
                    Text(stringResource(Res.string.group_settings_leave_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun HeaderRow(
    iconKey: String,
    colorKey: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupAvatar(
            iconKey = iconKey,
            colorKey = colorKey,
            size = 64.dp,
            cornerRadius = 18.dp,
            iconSize = 32.dp,
        )
        HorizontalSpacer(16.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CurrencyRow(
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.group_settings_default_currency) + " · $currencyCode",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LeaveGroupCard(
    isLeaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        enabled = !isLeaving,
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_logout),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(12.dp)
            Text(
                text = stringResource(Res.string.group_settings_leave_group),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
