package de.tabmates.features.tabgroup.presentation.navigation.settlementdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.presentation.components.SyncStatusChip
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.entry_detail_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_edit_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_paid_by_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_removed_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_delete
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_edit
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_swap_horiz
import tabmatesapp.features.tabgroup.presentation.generated.resources.settlement_detail_delete_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.settlement_detail_delete_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settlement_detail_received_by_section

@Composable
fun SettlementDetailRoot(
    settlementId: String,
    groupId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettlementDetailViewModel =
        koinViewModel(
            key = settlementId,
            parameters = { parametersOf(settlementId, groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Resolved here because the event lambda below is not composable.
    val unavailableMessage = stringResource(Res.string.entry_detail_unavailable)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettlementDetailEvent.SettlementDeleted -> {
                onBack()
            }

            SettlementDetailEvent.SettlementUnavailable -> {
                onBack()
                snackbarHostState.showSnackbar(unavailableMessage)
            }

            is SettlementDetailEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.onConfirmDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    TopBarActions(navKey) {
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_edit),
                contentDescription = stringResource(Res.string.expense_detail_edit_cd),
            )
        }
        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.expense_detail_delete_cd),
            )
        }
    }

    SettlementDetailScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun SettlementDetailScreen(
    state: SettlementDetailState,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    val settlement = state.settlement

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (settlement == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        VerticalSpacer(8.dp)
        HeroSection(
            title = settlement.title,
            amountFormatted =
                formatMoney(
                    state.groupCurrencySymbol,
                    settlement.amount,
                    state.groupCurrencyDecimalDigits,
                ),
            dateText = formatEntryDate(settlement.entryDate, monthLabels),
            isPendingSync = settlement.isPendingSync,
        )
        VerticalSpacer(24.dp)
        val amountFormatted =
            formatMoney(state.groupCurrencySymbol, settlement.amount, state.groupCurrencyDecimalDigits)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel(
                text = stringResource(Res.string.expense_detail_paid_by_section),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            ParticipantRow(
                participant = state.membersById[settlement.paidByUserId],
                currentUserId = state.currentUserId,
                trailingValue = amountFormatted,
            )
            VerticalSpacer(20.dp)
            SectionLabel(
                text = stringResource(Res.string.settlement_detail_received_by_section),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            ParticipantRow(
                participant = state.membersById[settlement.receivedByUserId],
                currentUserId = state.currentUserId,
                trailingValue = amountFormatted,
            )
        }
        VerticalSpacer(24.dp)
    }
}

@Composable
private fun HeroSection(
    title: String,
    amountFormatted: String,
    dateText: String,
    isPendingSync: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_swap_horiz),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (isPendingSync) {
            SyncStatusChip()
        }
        Text(
            text = amountFormatted,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParticipantRow(
    participant: GroupParticipant?,
    currentUserId: String,
    trailingValue: String,
) {
    val name =
        when {
            participant == null -> stringResource(Res.string.expense_detail_removed_member)
            participant.userId == currentUserId -> stringResource(Res.string.add_entry_paid_by_you)
            else -> participant.username
        }
    val isRemoved = participant == null
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(initials = participant?.initials ?: "?")
        HorizontalSpacer(12.dp)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isRemoved) FontWeight.Normal else FontWeight.SemiBold,
            fontStyle = if (isRemoved) FontStyle.Italic else FontStyle.Normal,
            color =
                if (isRemoved) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailingValue,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settlement_detail_delete_dialog_title)) },
        text = { Text(stringResource(Res.string.settlement_detail_delete_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.expense_detail_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.expense_detail_delete_dialog_cancel))
            }
        },
    )
}
