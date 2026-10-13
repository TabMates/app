package de.tabmates.features.tabgroup.presentation.navigation.entrydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.presentation.components.DetailHero
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.components.formatRate
import de.tabmates.features.tabgroup.presentation.components.rateUpdatedLabel
import de.tabmates.features.tabgroup.presentation.navigation.addentry.EntryKind
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_locked_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.entry_detail_delete_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.entry_detail_delete_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.entry_detail_received_by_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.entry_detail_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_message
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_delete_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_edit_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_paid_by_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_removed_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_split_between_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_delete
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_edit
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_redeem
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_restaurant

@Composable
fun EntryDetailRoot(
    entryId: String,
    groupId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryDetailViewModel =
        koinViewModel(
            key = entryId,
            parameters = { parametersOf(entryId, groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Resolved here because the event lambda below is not composable.
    val unavailableMessage = stringResource(Res.string.entry_detail_unavailable)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EntryDetailEvent.EntryDeleted -> {
                onBack()
            }

            EntryDetailEvent.EntryUnavailable -> {
                onBack()
                snackbarHostState.showSnackbar(unavailableMessage)
            }

            is EntryDetailEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            entryKind = state.entryKind,
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

    EntryDetailScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun EntryDetailScreen(
    state: EntryDetailState,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    val entry = state.entry
    val isIncome = state.entryKind == EntryKind.INCOME

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (entry == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        VerticalSpacer(8.dp)
        DetailHero(
            icon = if (isIncome) Res.drawable.ic_redeem else Res.drawable.ic_restaurant,
            title = entry.title,
            amountFormatted =
                formatMoney(
                    state.entryCurrencySymbol,
                    entry.amount,
                    state.entryCurrencyDecimalDigits,
                ),
            subtitle = formatEntryDate(entry.entryDate, monthLabels),
            description = entry.description,
            isPendingSync = entry.isPendingSync,
        )
        if (state.isForeignCurrency) {
            VerticalSpacer(4.dp)
            ForeignCurrencyDetails(state = state)
        }
        VerticalSpacer(24.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel(
                text =
                    stringResource(
                        if (isIncome) {
                            Res.string.entry_detail_received_by_section
                        } else {
                            Res.string.expense_detail_paid_by_section
                        },
                    ),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            val payer = state.membersById[entry.paidByUserId]
            val payerName =
                when {
                    payer == null -> stringResource(Res.string.expense_detail_removed_member)
                    payer.userId == state.currentUserId -> stringResource(Res.string.add_entry_paid_by_you)
                    else -> payer.username
                }
            DetailRow(
                initials = payer?.initials ?: "?",
                primary = payerName,
                trailingValue =
                    formatMoney(state.entryCurrencySymbol, entry.amount, state.entryCurrencyDecimalDigits),
                isRemoved = payer == null,
            )
            VerticalSpacer(20.dp)
            SectionLabel(
                text = stringResource(Res.string.expense_detail_split_between_section),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            state.splits.forEach { split ->
                SplitRow(
                    split = split,
                    state = state,
                )
                VerticalSpacer(8.dp)
            }
        }
        VerticalSpacer(24.dp)
    }
}

@Composable
private fun ForeignCurrencyDetails(state: EntryDetailState) {
    val entry = state.entry ?: return
    // The rate locked in when the entry was added wins over the live table; only legacy
    // entries without a snapshot fall back to live rates (and show when those were updated).
    val lockedRate = entry.exchangeRate
    val effectiveRate =
        lockedRate
            ?: CurrencyConverter.convert(
                amount = 1.0,
                from = state.entryCurrencyCode,
                to = state.groupCurrencyCode,
                rates = state.ratesByCurrency,
            )
    val converted = effectiveRate?.let { entry.amount * it }
    val rateText = effectiveRate?.let { formatRate(it) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (converted != null && rateText != null) {
            Text(
                text = "≈ ${formatMoney(state.groupCurrencySymbol, converted, state.groupCurrencyDecimalDigits)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(
                        Res.string.currency_rate_label,
                        state.entryCurrencyCode,
                        rateText,
                        state.groupCurrencyCode,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lockedRate != null) {
                Text(
                    text = stringResource(Res.string.currency_rate_locked_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.ratesLastUpdatedAt?.let { lastUpdatedAt ->
                    Text(
                        text = rateUpdatedLabel(lastUpdatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.currency_rate_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(
    initials: String,
    primary: String,
    trailingValue: String,
    isRemoved: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(initials = initials)
        HorizontalSpacer(12.dp)
        Text(
            text = primary,
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
private fun SplitRow(
    split: TabEntrySplit,
    state: EntryDetailState,
) {
    val member = state.membersById[split.participantId]
    val name =
        when {
            member == null -> stringResource(Res.string.expense_detail_removed_member)
            member.userId == state.currentUserId -> stringResource(Res.string.add_entry_paid_by_you)
            else -> member.username
        }
    DetailRow(
        initials = member?.initials ?: "?",
        primary = name,
        trailingValue =
            formatMoney(state.entryCurrencySymbol, split.resolvedAmount, state.entryCurrencyDecimalDigits),
        isRemoved = member == null,
    )
}

@Composable
private fun DeleteConfirmDialog(
    entryKind: EntryKind,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isIncome = entryKind == EntryKind.INCOME
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isIncome) {
                        Res.string.entry_detail_delete_dialog_title
                    } else {
                        Res.string.expense_detail_delete_dialog_title
                    },
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    if (isIncome) {
                        Res.string.entry_detail_delete_dialog_message
                    } else {
                        Res.string.expense_detail_delete_dialog_message
                    },
                ),
            )
        },
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
