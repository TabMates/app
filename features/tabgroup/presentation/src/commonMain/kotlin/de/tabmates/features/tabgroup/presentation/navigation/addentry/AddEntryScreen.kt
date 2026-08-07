package de.tabmates.features.tabgroup.presentation.navigation.addentry

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.format.LocalNumberSymbols
import de.tabmates.core.presentation.navigation.OverrideTopBar
import de.tabmates.core.presentation.navigation.TopBarAction
import de.tabmates.core.presentation.navigation.TopBarActions
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.core.presentation.util.UiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.components.formatRate
import de.tabmates.features.tabgroup.presentation.components.parseAmount
import de.tabmates.features.tabgroup.presentation.components.rateUpdatedLabel
import de.tabmates.features.tabgroup.presentation.components.rememberAmountInputTransformation
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CurrencyPickerBottomSheet
import de.tabmates.features.tabgroup.presentation.navigation.creategroup.CurrencyPickerUiState
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_amount_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_amount_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_currency_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_date_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_date_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_date_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_description_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_kind_expense
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_kind_income
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_sheet_done
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_sheet_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_people_plural
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_people_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_rate_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_received_by_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_received_by_sheet_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_split_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_split_summary_equal
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_split_summary_exact
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_split_summary_percentage
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_split_summary_shares
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_title_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.currency_rate_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_removed_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_calendar
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_pie_chart
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_done
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_title

@Composable
fun AddEntryRoot(
    groupId: String,
    navKey: NavKey,
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    entryId: String = "",
    viewModel: AddEntryViewModel =
        koinViewModel(
            key = entryId.ifBlank { groupId },
            parameters = { parametersOf(groupId, entryId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyPickerState by viewModel.currencyPickerState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AddEntryEvent.EntrySaved -> onSaved()
            is AddEntryEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    if (state.isSplitEditorVisible) {
        // Split editor is an in-screen sub-view; take over the scaffold top bar while it is open.
        OverrideTopBar(
            key = navKey,
            title = UiText.Resource(Res.string.split_screen_title),
            navigationAction = TopBarAction.Back,
            onNavigationClick = viewModel::onSplitDismiss,
        ) {
            TextButton(onClick = viewModel::onSplitConfirm) {
                Text(
                    text = stringResource(Res.string.split_screen_done),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    } else {
        TopBarActions(navKey) {
            TextButton(
                onClick = viewModel::onSaveClick,
                enabled = !state.isSubmitting,
            ) {
                Text(
                    text = stringResource(Res.string.add_entry_save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    AddEntryScreen(
        state = state,
        currencyPickerState = currencyPickerState,
        onKindChange = viewModel::onKindChange,
        onPaidByClick = viewModel::onPaidByClick,
        onPaidByPickerDismiss = viewModel::onPaidByPickerDismiss,
        onPaidBySelected = viewModel::onPaidBySelected,
        onCurrencyClick = viewModel::onCurrencyClick,
        onCurrencyPickerDismiss = viewModel::onCurrencyPickerDismiss,
        onCurrencySelected = viewModel::onCurrencySelected,
        onSplitOpen = viewModel::onSplitOpen,
        onSplitDismiss = viewModel::onSplitDismiss,
        onSplitTypeChange = viewModel::onSplitTypeChange,
        onSplitParticipantToggle = viewModel::onSplitParticipantToggle,
        onDateClick = viewModel::onDateClick,
        onDatePickerDismiss = viewModel::onDatePickerDismiss,
        onDateSelected = viewModel::onDateSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEntryScreen(
    state: AddEntryState,
    currencyPickerState: CurrencyPickerUiState,
    onKindChange: (EntryKind) -> Unit,
    onPaidByClick: () -> Unit,
    onPaidByPickerDismiss: () -> Unit,
    onPaidBySelected: (String) -> Unit,
    onCurrencyClick: () -> Unit,
    onCurrencyPickerDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onSplitOpen: () -> Unit,
    onSplitDismiss: () -> Unit,
    onSplitTypeChange: (SplitType) -> Unit,
    onSplitParticipantToggle: (String) -> Unit,
    onDateClick: () -> Unit,
    onDatePickerDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabels = rememberMonthAbbreviations()

    // Intercept the back gesture while the split editor sub-view is open so it dismisses the
    // sub-view instead of leaving the whole add-expense screen. Hoisting the state lets the split
    // editor follow the predictive-back gesture progress.
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationEventHandler(
        state = backState,
        isForwardEnabled = false,
        isBackEnabled = state.isSplitEditorVisible,
        onBackCompleted = onSplitDismiss,
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalSpacer(8.dp)
        EntryKindToggle(
            selected = state.entryKind,
            // Locked once editing — the entry's kind cannot change on update.
            enabled = !state.isEditing,
            onKindChange = onKindChange,
        )
        VerticalSpacer(8.dp)
        AmountInput(
            amountState = state.amountTextState,
            symbol = state.entryCurrencySymbol,
            decimals = state.entryCurrencyDecimalDigits,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        CurrencySelector(
            state = state,
            onCurrencyClick = onCurrencyClick,
        )
        VerticalSpacer(24.dp)
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabMatesTextField(
                state = state.titleTextState,
                placeholder = stringResource(Res.string.add_entry_title_placeholder),
                singleLine = true,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
            TabMatesTextField(
                state = state.descriptionTextState,
                placeholder = stringResource(Res.string.add_entry_description_placeholder),
                singleLine = false,
                capitalization = KeyboardCapitalization.Words,
                modifier = Modifier.fillMaxWidth(),
            )
            FieldRow(
                label =
                    if (state.entryKind == EntryKind.INCOME) {
                        stringResource(Res.string.add_entry_received_by_label)
                    } else {
                        stringResource(Res.string.add_entry_paid_by_label)
                    },
                value = paidByDisplay(state),
                onClick = onPaidByClick,
                leadingIcon = null,
            )
            FieldRow(
                label = stringResource(Res.string.add_entry_split_label),
                value = splitSummary(state),
                onClick = onSplitOpen,
                leadingIcon = Res.drawable.ic_pie_chart,
            )
            FieldRow(
                label = stringResource(Res.string.add_entry_date_label),
                value = formatEntryDate(state.entryDate, monthLabels),
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
            isIncome = state.entryKind == EntryKind.INCOME,
            onSelect = onPaidBySelected,
            onDismiss = onPaidByPickerDismiss,
        )
    }

    if (state.isDatePickerVisible) {
        DatePickerSheet(
            initialEpochMillis = state.entryDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            onDismiss = onDatePickerDismiss,
            onConfirm = onDateSelected,
        )
    }

    if (state.isCurrencyPickerVisible) {
        CurrencyPickerBottomSheet(
            queryState = state.currencyQueryState,
            state = currencyPickerState,
            onCurrencySelected = onCurrencySelected,
            onDismiss = onCurrencyPickerDismiss,
        )
    }

    if (state.isSplitEditorVisible) {
        SplitEditorScreen(
            state = state,
            onTypeChange = onSplitTypeChange,
            onToggleParticipant = onSplitParticipantToggle,
            modifier =
                Modifier.graphicsLayer {
                    val progress =
                        (backState.transitionState as? NavigationEventTransitionState.InProgress)
                            ?.latestEvent
                            ?.progress ?: 0f
                    translationX = size.width * progress
                    alpha = 1f - progress * 0.3f
                },
        )
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryKindToggle(
    selected: EntryKind,
    enabled: Boolean,
    onKindChange: (EntryKind) -> Unit,
) {
    val kinds = EntryKind.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        kinds.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onKindChange(kind) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                label = {
                    Text(
                        text =
                            when (kind) {
                                EntryKind.EXPENSE -> stringResource(Res.string.add_entry_kind_expense)
                                EntryKind.INCOME -> stringResource(Res.string.add_entry_kind_income)
                            },
                    )
                },
            )
        }
    }
}

@Composable
private fun AmountInput(
    amountState: TextFieldState,
    symbol: String,
    decimals: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.add_entry_amount_label),
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
                // Locales that write "1.234,56 €" need the symbol trailing, or the hero field
                // would contradict every other amount on screen.
                val symbolLeads = LocalNumberSymbols.current.currencyBeforeAmount
                if (symbolLeads) {
                    Text(text = symbol, style = symbolStyle)
                    HorizontalSpacer(SYMBOL_GAP)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicTextField(
                        state = amountState,
                        textStyle = amountTextStyle,
                        inputTransformation = rememberAmountInputTransformation(decimals = decimals),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorator = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (amountState.text.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.add_entry_amount_placeholder),
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
                if (!symbolLeads) {
                    HorizontalSpacer(SYMBOL_GAP)
                    Text(text = symbol, style = symbolStyle)
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    state: AddEntryState,
    onCurrencyClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AssistChip(
            onClick = onCurrencyClick,
            label = { Text(state.entryCurrencyCode.ifEmpty { "—" }) },
            trailingIcon = {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_chevron_right),
                    contentDescription = stringResource(Res.string.add_entry_currency_cd),
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        if (state.isForeignCurrency) {
            VerticalSpacer(4.dp)
            ConvertedAmountHint(state = state)
            ExchangeRateHint(state = state)
        }
    }
}

@Composable
private fun ExchangeRateHint(state: AddEntryState) {
    val rate =
        CurrencyConverter.convert(
            amount = 1.0,
            from = state.entryCurrencyCode,
            to = state.baseCurrencyCode,
            rates = state.ratesByCurrency,
        )
    // Missing-rate messaging is owned by ConvertedAmountHint; render nothing here.
    val rateText = rate?.let { formatRate(it) } ?: return
    Text(
        text =
            stringResource(
                Res.string.currency_rate_label,
                state.entryCurrencyCode,
                rateText,
                state.baseCurrencyCode,
            ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.ratesLastUpdatedAt?.let { lastUpdatedAt ->
        Text(
            text = rateUpdatedLabel(lastUpdatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConvertedAmountHint(state: AddEntryState) {
    val amount = parseAmount(state.amountTextState.text.toString())
    val converted =
        amount?.let {
            CurrencyConverter.convert(
                amount = it,
                from = state.entryCurrencyCode,
                to = state.baseCurrencyCode,
                rates = state.ratesByCurrency,
            )
        }
    val text =
        when {
            amount == null -> {
                null
            }

            converted != null -> {
                "≈ ${formatMoney(state.baseCurrencySymbol, converted, state.baseCurrencyDecimalDigits)}"
            }

            else -> {
                stringResource(Res.string.add_entry_rate_unavailable)
            }
        } ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun FieldRow(
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
private fun paidByDisplay(state: AddEntryState): String {
    // Resolved through the wider map, not the member list: the payer of an edited entry may have
    // been removed from the group since, and their real name is still known.
    val paidBy = state.participantsById[state.paidByUserId]
    return when {
        paidBy == null -> stringResource(Res.string.expense_detail_removed_member)
        paidBy.userId == state.currentUserId -> stringResource(Res.string.add_entry_paid_by_you)
        else -> paidBy.username
    }
}

@Composable
private fun splitSummary(state: AddEntryState): String {
    return when (state.splitType) {
        SplitType.EQUAL -> {
            val count = state.splitInputs.count { it.included }
            val unit =
                if (count == 1) {
                    stringResource(Res.string.add_entry_people_singular)
                } else {
                    stringResource(Res.string.add_entry_people_plural)
                }
            stringResource(Res.string.add_entry_split_summary_equal, count, unit)
        }

        SplitType.EXACT_AMOUNT -> {
            stringResource(Res.string.add_entry_split_summary_exact)
        }

        SplitType.PERCENTAGE -> {
            stringResource(Res.string.add_entry_split_summary_percentage)
        }

        SplitType.SHARES -> {
            stringResource(Res.string.add_entry_split_summary_shares)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaidByPickerSheet(
    members: List<GroupParticipant>,
    currentUserId: String,
    selectedUserId: String,
    isIncome: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text =
                    if (isIncome) {
                        stringResource(Res.string.add_entry_received_by_sheet_title)
                    } else {
                        stringResource(Res.string.add_entry_paid_by_sheet_title)
                    },
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
                Text(stringResource(Res.string.add_entry_paid_by_sheet_done))
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
                            Res.string.add_entry_paid_by_you,
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
internal fun DatePickerSheet(
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
                Text(stringResource(Res.string.add_entry_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.add_entry_date_cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** Gap between the hero amount and its currency symbol, on whichever side the locale puts it. */
private val SYMBOL_GAP = 4.dp
