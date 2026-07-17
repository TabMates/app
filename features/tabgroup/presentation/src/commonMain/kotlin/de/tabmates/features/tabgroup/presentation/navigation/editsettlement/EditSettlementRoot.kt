package de.tabmates.features.tabgroup.presentation.navigation.editsettlement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.DatePickerSheet
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.FieldRow
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.formatExpenseDate
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.rememberMonthAbbreviations
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_date_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_removed_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_dialog_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_label

@Composable
fun EditSettlementRoot(
    groupId: String,
    settlementId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditSettlementViewModel =
        koinViewModel(
            key = settlementId,
            parameters = { parametersOf(groupId, settlementId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EditSettlementEvent.SettlementSaved -> onSaved()
            is EditSettlementEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    TopBarActions(navKey) {
        TextButton(
            onClick = viewModel::onSaveClick,
            enabled = !state.isSubmitting,
        ) {
            Text(
                text = stringResource(Res.string.add_expense_save),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    EditSettlementScreen(
        state = state,
        onDateClick = viewModel::onDateClick,
        onDatePickerDismiss = viewModel::onDatePickerDismiss,
        onDateSelected = viewModel::onDateSelected,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier,
    )
}

@Composable
private fun EditSettlementScreen(
    state: EditSettlementState,
    onDateClick: () -> Unit,
    onDatePickerDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val monthLabels = rememberMonthAbbreviations()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        VerticalSpacer(16.dp)
        Text(
            text =
                stringResource(
                    Res.string.settle_up_amount_dialog_subtitle,
                    participantLabel(state.membersById[state.paidByUserId]),
                    participantLabel(state.membersById[state.receivedByUserId]),
                ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        VerticalSpacer(16.dp)
        TabMatesTextField(
            state = state.amountTextState,
            title = stringResource(Res.string.settle_up_amount_label),
            singleLine = true,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            onKeyboardAction = {
                if (!state.isSubmitting) {
                    focusManager.clearFocus()
                    onSaveClick()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(12.dp)
        FieldRow(
            label = stringResource(Res.string.add_expense_date_label),
            value = formatExpenseDate(state.entryDate, monthLabels),
            onClick = onDateClick,
            leadingIcon = Res.drawable.ic_calendar,
        )
        VerticalSpacer(24.dp)
    }

    if (state.isDatePickerVisible) {
        DatePickerSheet(
            initialEpochMillis = state.entryDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            onDismiss = onDatePickerDismiss,
            onConfirm = onDateSelected,
        )
    }
}

@Composable
private fun participantLabel(participant: GroupParticipant?): String =
    participant?.username ?: stringResource(Res.string.expense_detail_removed_member)
