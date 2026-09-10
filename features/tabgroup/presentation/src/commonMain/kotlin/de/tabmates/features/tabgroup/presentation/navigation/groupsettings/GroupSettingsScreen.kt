package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import org.jetbrains.compose.resources.DrawableResource
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_name_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_people
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_saved
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_schedules
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_logout
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_person_add

@Composable
fun GroupSettingsRoot(
    groupId: String,
    onPeopleClick: () -> Unit,
    onSchedulesClick: () -> Unit,
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
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GroupSettingsEvent.Saved -> {
                snackbarHostState.showSnackbar(savedMessage)
            }

            GroupSettingsEvent.Left -> {
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
        onPeopleClick = onPeopleClick,
        onSchedulesClick = onSchedulesClick,
        modifier = modifier,
    )
}

@Composable
private fun GroupSettingsScreen(
    state: GroupSettingsState,
    onAction: (GroupSettingsAction) -> Unit,
    onPeopleClick: () -> Unit,
    onSchedulesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
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
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        TabMatesTextField(
            state = state.descriptionTextState,
            title = stringResource(Res.string.group_settings_description_label),
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            onKeyboardAction = {
                if (!state.isSaving) {
                    focusManager.clearFocus()
                    onAction(GroupSettingsAction.Save)
                }
            },
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        CurrencyRow(
            currencyCode = state.defaultCurrencyCode,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        // Members and placeholders are managed together one level down; this row is the way in.
        NavCard(
            icon = Res.drawable.ic_person_add,
            label = stringResource(Res.string.group_settings_people),
            value = state.peopleCount.toString(),
            onClick = onPeopleClick,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        // The transactions tab surfaces the schedules that are about to produce something. This is
        // the way to the rest of them — ended ones included, which have nothing upcoming to show.
        NavCard(
            icon = Res.drawable.ic_calendar,
            label = stringResource(Res.string.group_settings_schedules),
            onClick = onSchedulesClick,
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

/** One row that opens a screen managing part of the group. */
@Composable
private fun NavCard(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(12.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalSpacer(8.dp)
            }
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
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
