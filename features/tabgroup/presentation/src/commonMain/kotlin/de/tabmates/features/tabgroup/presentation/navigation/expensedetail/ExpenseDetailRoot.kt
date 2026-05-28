package de.tabmates.features.tabgroup.presentation.navigation.expensedetail

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import de.tabmates.features.tabgroup.presentation.components.SectionLabel
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.formatExpenseDate
import de.tabmates.features.tabgroup.presentation.navigation.addexpense.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_paid_by_you
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
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_restaurant

@Composable
fun ExpenseDetailRoot(
    expenseId: String,
    groupId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseDetailViewModel =
        koinViewModel(
            key = expenseId,
            parameters = { parametersOf(expenseId, groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ExpenseDetailEvent.ExpenseDeleted -> onBack()
            is ExpenseDetailEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
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

    ExpenseDetailScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun ExpenseDetailScreen(
    state: ExpenseDetailState,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    val expense = state.expense

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (expense == null) {
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
            title = expense.title,
            amountFormatted =
                formatMoney(
                    state.groupCurrencySymbol,
                    expense.amount,
                    state.groupCurrencyDecimalDigits,
                ),
            dateText = formatExpenseDate(expense.createdAt, monthLabels),
            description = expense.description,
        )
        VerticalSpacer(24.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel(
                text = stringResource(Res.string.expense_detail_paid_by_section),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            val payer = state.membersById[expense.paidByUserId]
            val payerName =
                when {
                    payer == null -> stringResource(Res.string.expense_detail_removed_member)
                    payer.userId == state.currentUserId -> stringResource(Res.string.add_expense_paid_by_you)
                    else -> payer.username
                }
            DetailRow(
                initials = payer?.initials ?: "?",
                primary = payerName,
                trailingValue =
                    formatMoney(state.groupCurrencySymbol, expense.amount, state.groupCurrencyDecimalDigits),
                isRemoved = payer == null,
            )
            VerticalSpacer(20.dp)
            SectionLabel(
                text = stringResource(Res.string.expense_detail_split_between_section),
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            expense.splits.forEach { split ->
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
private fun HeroSection(
    title: String,
    amountFormatted: String,
    dateText: String,
    description: String,
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
                imageVector = vectorResource(Res.drawable.ic_restaurant),
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
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
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
    state: ExpenseDetailState,
) {
    val member = state.membersById[split.participantId]
    val name =
        when {
            member == null -> stringResource(Res.string.expense_detail_removed_member)
            member.userId == state.currentUserId -> stringResource(Res.string.add_expense_paid_by_you)
            else -> member.username
        }
    DetailRow(
        initials = member?.initials ?: "?",
        primary = name,
        trailingValue =
            formatMoney(state.groupCurrencySymbol, split.resolvedAmount, state.groupCurrencyDecimalDigits),
        isRemoved = member == null,
    )
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.expense_detail_delete_dialog_title)) },
        text = { Text(stringResource(Res.string.expense_detail_delete_dialog_message)) },
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
