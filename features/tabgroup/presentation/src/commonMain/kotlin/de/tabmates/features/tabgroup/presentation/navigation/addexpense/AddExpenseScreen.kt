package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_amount_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_amount_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_close_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_date_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_date_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_date_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_description_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_paid_by_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_paid_by_sheet_done
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_paid_by_sheet_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_people_plural
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_people_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_split_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_split_summary_equal
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_split_summary_exact
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_split_summary_percentage
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_split_summary_shares
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_expense_title_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_pie_chart

@Composable
fun AddExpenseRoot(
    groupId: String,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AddExpenseEvent.ExpenseCreated -> onSaved()
            is AddExpenseEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    AddExpenseScreen(
        state = state,
        onClose = onClose,
        onPaidByClick = viewModel::onPaidByClick,
        onPaidByPickerDismiss = viewModel::onPaidByPickerDismiss,
        onPaidBySelected = viewModel::onPaidBySelected,
        onSplitOpen = viewModel::onSplitOpen,
        onSplitDismiss = viewModel::onSplitDismiss,
        onSplitTypeChange = viewModel::onSplitTypeChange,
        onSplitParticipantToggle = viewModel::onSplitParticipantToggle,
        onSplitConfirm = viewModel::onSplitConfirm,
        onDateClick = viewModel::onDateClick,
        onDatePickerDismiss = viewModel::onDatePickerDismiss,
        onDateSelected = viewModel::onDateSelected,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExpenseScreen(
    state: AddExpenseState,
    onClose: () -> Unit,
    onPaidByClick: () -> Unit,
    onPaidByPickerDismiss: () -> Unit,
    onPaidBySelected: (String) -> Unit,
    onSplitOpen: () -> Unit,
    onSplitDismiss: () -> Unit,
    onSplitTypeChange: (SplitType) -> Unit,
    onSplitParticipantToggle: (String) -> Unit,
    onSplitConfirm: () -> Unit,
    onDateClick: () -> Unit,
    onDatePickerDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AddExpenseTopBar(
            onClose = onClose,
            onSave = onSaveClick,
            isSubmitting = state.isSubmitting,
        )
        VerticalSpacer(8.dp)
        AmountInput(
            amountState = state.amountTextState,
            symbol = state.groupCurrencySymbol,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(24.dp)
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabMatesTextField(
                state = state.titleTextState,
                placeholder = stringResource(Res.string.add_expense_title_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TabMatesTextField(
                state = state.descriptionTextState,
                placeholder = stringResource(Res.string.add_expense_description_placeholder),
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            FieldRow(
                label = stringResource(Res.string.add_expense_paid_by_label),
                value = paidByDisplay(state),
                onClick = onPaidByClick,
                leadingIcon = null,
            )
            FieldRow(
                label = stringResource(Res.string.add_expense_split_label),
                value = splitSummary(state),
                onClick = onSplitOpen,
                leadingIcon = Res.drawable.ic_pie_chart,
            )
            FieldRow(
                label = stringResource(Res.string.add_expense_date_label),
                value = formatExpenseDate(state.createdAt, monthLabels),
                onClick = onDateClick,
                leadingIcon = Res.drawable.ic_calendar,
            )
        }
        VerticalSpacer(24.dp)
    }

    if (state.isPaidByPickerVisible) {
        PaidByPickerSheet(
            members = state.members,
            currentUserId = state.currentUserId,
            selectedUserId = state.paidByUserId,
            onSelect = onPaidBySelected,
            onDismiss = onPaidByPickerDismiss,
        )
    }

    if (state.isDatePickerVisible) {
        DatePickerSheet(
            initialEpochMillis = state.createdAt.toEpochMilliseconds(),
            onDismiss = onDatePickerDismiss,
            onConfirm = onDateSelected,
        )
    }

    if (state.isSplitEditorVisible) {
        SplitEditorScreen(
            state = state,
            onBack = onSplitDismiss,
            onTypeChange = onSplitTypeChange,
            onToggleParticipant = onSplitParticipantToggle,
            onDone = onSplitConfirm,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseTopBar(
    onClose: () -> Unit,
    onSave: () -> Unit,
    isSubmitting: Boolean,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.add_expense_title),
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.add_expense_close_cd),
                )
            }
        },
        actions = {
            TextButton(
                onClick = onSave,
                enabled = !isSubmitting,
            ) {
                Text(
                    text = stringResource(Res.string.add_expense_save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

@Composable
private fun rememberAmountFieldWidth(
    text: String,
    style: TextStyle,
    minWidth: Dp,
    maxWidth: Dp,
    horizontalPadding: Dp,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val sample = text.ifEmpty { "0" }
    val measured = remember(sample, style) { measurer.measure(sample, style).size.width }
    val contentDp = with(density) { measured.toDp() }
    return (contentDp + horizontalPadding).coerceIn(minWidth, maxWidth)
}

@Composable
private fun rememberTextWidth(
    text: String,
    style: TextStyle,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measured = remember(text, style) { measurer.measure(text, style).size.width }
    return with(density) { measured.toDp() }
}

@Composable
private fun AmountInput(
    amountState: TextFieldState,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.add_expense_amount_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(8.dp)
        val amountTextStyle =
            MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        val symbolStyle =
            MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold)
        val symbolWidth = rememberTextWidth(symbol.ifEmpty { " " }, symbolStyle)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxFieldWidth = (maxWidth - symbolWidth - 8.dp).coerceAtLeast(120.dp)
            val fieldWidth =
                rememberAmountFieldWidth(
                    text = amountState.text.toString(),
                    style = amountTextStyle,
                    minWidth = 120.dp,
                    maxWidth = maxFieldWidth,
                    horizontalPadding = 12.dp,
                )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = symbol,
                    style = symbolStyle,
                )
                HorizontalSpacer(4.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicTextField(
                        state = amountState,
                        textStyle = amountTextStyle,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorator = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (amountState.text.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.add_expense_amount_placeholder),
                                        style = amountTextStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.width(fieldWidth),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp).width(fieldWidth),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    leadingIcon: DrawableResource?,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
            overlineContent = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent =
                leadingIcon?.let {
                    {
                        Icon(
                            imageVector = vectorResource(it),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            trailingContent = {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun paidByDisplay(state: AddExpenseState): String {
    val paidBy = state.members.firstOrNull { it.userId == state.paidByUserId }
    return when {
        paidBy == null -> "—"
        paidBy.userId == state.currentUserId -> stringResource(Res.string.add_expense_paid_by_you)
        else -> paidBy.username
    }
}

@Composable
private fun splitSummary(state: AddExpenseState): String {
    return when (state.splitType) {
        SplitType.EQUAL -> {
            val count = state.splitInputs.count { it.included }
            val unit =
                if (count == 1) {
                    stringResource(Res.string.add_expense_people_singular)
                } else {
                    stringResource(Res.string.add_expense_people_plural)
                }
            stringResource(Res.string.add_expense_split_summary_equal, count, unit)
        }

        SplitType.EXACT_AMOUNT -> {
            stringResource(Res.string.add_expense_split_summary_exact)
        }

        SplitType.PERCENTAGE -> {
            stringResource(Res.string.add_expense_split_summary_percentage)
        }

        SplitType.SHARES -> {
            stringResource(Res.string.add_expense_split_summary_shares)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaidByPickerSheet(
    members: List<GroupParticipant>,
    currentUserId: String,
    selectedUserId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.add_expense_paid_by_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(12.dp)
            members.forEach { member ->
                PaidByRow(
                    participant = member,
                    isCurrentUser = member.userId == currentUserId,
                    selected = member.userId == selectedUserId,
                    onClick = { onSelect(member.userId) },
                )
            }
            VerticalSpacer(8.dp)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.add_expense_paid_by_sheet_done))
            }
        }
    }
}

@Composable
private fun PaidByRow(
    participant: GroupParticipant,
    isCurrentUser: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { UserAvatar(initials = participant.initials) },
        headlineContent = {
            Text(
                text =
                    if (isCurrentUser) {
                        stringResource(
                            Res.string.add_expense_paid_by_you,
                        )
                    } else {
                        participant.username
                    },
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailingContent = { RadioButton(selected = selected, onClick = onClick) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialEpochMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = pickerState.selectedDateMillis ?: initialEpochMillis
                    onConfirm(selected)
                },
            ) {
                Text(stringResource(Res.string.add_expense_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.add_expense_date_cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
