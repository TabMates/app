package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.core.presentation.format.AmountSign
import de.tabmates.core.presentation.share.LinkShareResult
import de.tabmates.core.presentation.share.rememberLinkSharer
import de.tabmates.features.tabgroup.domain.balance.UserBalanceCalculator
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.recurring.RecurringOccurrenceCalculator
import de.tabmates.features.tabgroup.domain.recurring.RecurringSeries
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.components.SyncStatusChip
import de.tabmates.features.tabgroup.presentation.navigation.activity.ActivitySection
import de.tabmates.features.tabgroup.presentation.navigation.activity.LoadMoreOnApproachingEnd
import de.tabmates.features.tabgroup.presentation.navigation.activity.activityFeed
import de.tabmates.features.tabgroup.presentation.navigation.addentry.formatEntryDate
import de.tabmates.features.tabgroup.presentation.navigation.addentry.rememberMonthAbbreviations
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.buildInviteUrl
import de.tabmates.features.tabgroup.presentation.navigation.recurringdetail.frequencyLabel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.expense_detail_removed_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_open_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_across_people
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_add_entry
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_back_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_empty_expenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_gets_back
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_copied
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_owes
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_paid_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_received_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_received_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_no_balance
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_not_involved
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_you_borrowed
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_you_lent
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_you_owe
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_row_you_receive
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_settlement_paid_other
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_settlement_paid_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_settlement_you_paid
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_balances
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_history
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_transactions
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_total_spent
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_you_owe
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_youre_owed
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_expense_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_expense_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_member_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_members_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_status_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_arrow_back
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_person_add
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_redeem
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_restaurant
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_swap_horiz
import tabmatesapp.features.tabgroup.presentation.generated.resources.member_label_former
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_chip_ended
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_chip_needs_attention
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_chip_scheduled
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_empty_hint
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_next_on
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_section_active
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_section_ended
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_tab_needs_attention_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.recurring_tab_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_action
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock

private enum class DetailTab { TRANSACTIONS, RECURRING, HISTORY, BALANCES }

/** How far a not-yet-written occurrence is faded relative to a real entry. */
private const val SCHEDULED_ROW_ALPHA = 0.6f

/** Bottom space reserved so the last row can scroll clear of the host "Add Entry" FAB. */
private val FabBottomClearance = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailPane(
    item: GroupOverviewItem,
    currentUserId: String,
    members: List<GroupParticipant>,
    formerMemberIds: Set<String>,
    participantsById: Map<String, GroupParticipant>,
    entries: List<TabEntry>,
    memberNetBalances: Map<String, Double>,
    hasOutstandingDebts: Boolean,
    currencyByCode: Map<String, Currency>,
    ratesByCurrency: Map<String, Double>,
    historySections: List<ActivitySection>,
    canLoadMoreHistory: Boolean,
    onLoadMoreHistory: () -> Unit,
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onAddEntryClick: () -> Unit = {},
    onSettleUpClick: () -> Unit = {},
    onEntryClick: (String) -> Unit = {},
    onSettlementClick: (String) -> Unit = {},
    recurringSeries: List<RecurringSeries> = emptyList(),
    onRecurringSeriesClick: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    var selectedTab by rememberSaveable(item.id) { mutableStateOf(DetailTab.TRANSACTIONS) }
    val linkSharer = rememberLinkSharer()
    val scope = rememberCoroutineScope()
    val inviteUrl = remember(item.inviteToken) { buildInviteUrl(item.inviteToken) }
    val showResultSnackbar: (LinkShareResult) -> Unit = { result ->
        if (result == LinkShareResult.Copied) {
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.groups_detail_invite_copied))
            }
        }
    }
    val onShareInvite: () -> Unit = {
        if (item.inviteToken.isNotBlank()) {
            showResultSnackbar(linkSharer.share(inviteUrl))
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(headerHeightPx) {
        if (headerHeightPx > 0) {
            scrollBehavior.state.heightOffsetLimit = -headerHeightPx.toFloat()
        }
    }
    // Re-expand the header when switching tabs so a shorter tab can't leave it stranded collapsed.
    LaunchedEffect(selectedTab) { scrollBehavior.state.heightOffset = 0f }

    Column(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
        TopAppBar(
            title = {
                Text(
                    text = item.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = scrollBehavior.state.collapsedFraction },
                )
            },
            windowInsets = WindowInsets(0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.groups_detail_back_cd),
                    )
                }
            },
        )
        // Collapsing header: measured at full height, then slides up and fades as the user scrolls.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val collapse = (-scrollBehavior.state.heightOffset).roundToInt()
                        val visibleHeight = (placeable.height - collapse).coerceIn(0, placeable.height)
                        layout(placeable.width, visibleHeight) {
                            placeable.place(0, -collapse)
                        }
                    },
        ) {
            DetailHeader(
                item = item,
                isExpanded = isExpanded,
                onInviteClick = onShareInvite,
                onSettingsClick = onSettingsClick,
                onAddEntryClick = onAddEntryClick,
                modifier =
                    Modifier
                        .onSizeChanged { headerHeightPx = it.height }
                        .graphicsLayer { alpha = 1f - scrollBehavior.state.collapsedFraction },
            )
        }
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            val hasParkedSchedule = recurringSeries.any { it.needsAttention }
            val parkedScheduleLabel = stringResource(Res.string.recurring_tab_needs_attention_cd)
            DetailTab.entries.forEach { tab ->
                val needsAttention = tab == DetailTab.RECURRING && hasParkedSchedule
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.label())
                            // A parked schedule creates nothing until someone repairs it, and
                            // nothing else on this screen would say so. The dot is the only carrier
                            // of that, so it has to be readable rather than merely visible.
                            if (needsAttention) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                        .semantics { contentDescription = parkedScheduleLabel },
                                )
                            }
                        }
                    },
                )
            }
        }
        when (selectedTab) {
            DetailTab.TRANSACTIONS -> {
                TransactionsTab(
                    item = item,
                    currentUserId = currentUserId,
                    participantsById = participantsById,
                    entries = entries,
                    currencyByCode = currencyByCode,
                    ratesByCurrency = ratesByCurrency,
                    onEntryClick = onEntryClick,
                    onSettlementClick = onSettlementClick,
                )
            }

            DetailTab.RECURRING -> {
                RecurringTab(
                    series = recurringSeries,
                    currencyByCode = currencyByCode,
                    onSeriesClick = onRecurringSeriesClick,
                )
            }

            DetailTab.HISTORY -> {
                HistoryTab(
                    sections = historySections,
                    canLoadMore = canLoadMoreHistory,
                    onLoadMore = onLoadMoreHistory,
                    onEntryClick = onEntryClick,
                    onSettlementClick = onSettlementClick,
                )
            }

            DetailTab.BALANCES -> {
                BalancesTab(
                    item = item,
                    members = members,
                    formerMemberIds = formerMemberIds,
                    currentUserId = currentUserId,
                    memberNetBalances = memberNetBalances,
                    hasOutstandingDebts = hasOutstandingDebts,
                    onSettleUpClick = onSettleUpClick,
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    item: GroupOverviewItem,
    isExpanded: Boolean,
    onInviteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(
                iconKey = item.iconKey,
                colorKey = item.colorKey,
                size = 48.dp,
                cornerRadius = 14.dp,
                iconSize = 26.dp,
            )
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = memberCountText(item.memberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Settings is no longer a tab on any width, so the gear is the single way in.
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.group_settings_open_cd),
                )
            }
            if (isExpanded) {
                HorizontalSpacer(8.dp)
                HeaderActions(
                    modifier = Modifier,
                    onInviteClick = onInviteClick,
                    onAddEntryClick = onAddEntryClick,
                )
            }
        }
        if (!isExpanded) {
            VerticalSpacer(16.dp)
            HeaderActions(
                modifier = Modifier.fillMaxWidth(),
                onInviteClick = onInviteClick,
                onAddEntryClick = onAddEntryClick,
            )
        }
    }
}

@Composable
private fun HeaderActions(
    modifier: Modifier = Modifier,
    onInviteClick: () -> Unit,
    onAddEntryClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onInviteClick,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_person_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            HorizontalSpacer(8.dp)
            Text(stringResource(Res.string.groups_detail_invite))
        }
        FilledTonalButton(
            onClick = onAddEntryClick,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text("+ ${stringResource(Res.string.groups_detail_add_entry)}")
        }
    }
}

/**
 * This group's slice of the activity log — the same rows as the account-wide Activity tab, filtered
 * to this group and without repeating its name in every subtitle.
 */
@Composable
private fun HistoryTab(
    sections: List<ActivitySection>,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onEntryClick: (String) -> Unit,
    onSettlementClick: (String) -> Unit,
) {
    val monthLabels = rememberMonthAbbreviations()
    val listState = rememberLazyListState()
    LoadMoreOnApproachingEnd(listState, canLoadMore, onLoadMore)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                top = 8.dp,
                bottom = FabBottomClearance,
            ),
    ) {
        activityFeed(
            sections = sections,
            isLoading = false,
            monthLabels = monthLabels,
            // Every row here belongs to this group already, so the id that travels with it is moot.
            onEntryClick = { _, entryId -> onEntryClick(entryId) },
            onSettlementClick = { _, settlementId -> onSettlementClick(settlementId) },
        )
    }
}

@Composable
private fun TransactionsTab(
    item: GroupOverviewItem,
    currentUserId: String,
    participantsById: Map<String, GroupParticipant>,
    entries: List<TabEntry>,
    currencyByCode: Map<String, Currency>,
    ratesByCurrency: Map<String, Double>,
    onEntryClick: (String) -> Unit,
    onSettlementClick: (String) -> Unit,
) {
    val removedMemberName = stringResource(Res.string.expense_detail_removed_member)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FabBottomClearance),
    ) {
        VerticalSpacer(16.dp)
        StatCardsRow(item = item, modifier = Modifier.padding(horizontal = 24.dp))
        VerticalSpacer(12.dp)
        if (entries.isEmpty()) {
            EmptyTabHint(
                text = stringResource(Res.string.groups_detail_empty_expenses),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            entries.forEach { entry ->
                // A scheduled placeholder is an occurrence the server owes but has not written yet.
                // It counts in the balances above — that is the point, the numbers must not jump
                // when the sweep lands — but there is nothing to open: it has no id on the server,
                // and every action lives on its schedule instead.
                val isScheduled = entry.isScheduledPlaceholder
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = if (isScheduled) SCHEDULED_ROW_ALPHA else 1f
                        },
                ) {
                    when (entry) {
                        is TabEntry.Expense -> {
                            ExpenseRow(
                                expense = entry,
                                currentUserId = currentUserId,
                                payerName = participantsById[entry.paidByUserId]?.username ?: removedMemberName,
                                item = item,
                                currency = currencyByCode[entry.currencyCode],
                                ratesByCurrency = ratesByCurrency,
                                onClick = { onEntryClick(entry.tabEntryId) }.takeIf { !isScheduled },
                            )
                        }

                        is TabEntry.Settlement -> {
                            SettlementRow(
                                settlement = entry,
                                currentUserId = currentUserId,
                                payerName = participantsById[entry.paidByUserId]?.username ?: removedMemberName,
                                recipientName =
                                    participantsById[entry.receivedByUserId]?.username ?: removedMemberName,
                                item = item,
                                currency = currencyByCode[entry.currencyCode],
                                ratesByCurrency = ratesByCurrency,
                                onClick = { onSettlementClick(entry.tabEntryId) }.takeIf { !isScheduled },
                            )
                        }

                        is TabEntry.Income -> {
                            IncomeRow(
                                income = entry,
                                currentUserId = currentUserId,
                                payerName = participantsById[entry.paidByUserId]?.username ?: removedMemberName,
                                item = item,
                                currency = currencyByCode[entry.currencyCode],
                                ratesByCurrency = ratesByCurrency,
                                onClick = { onEntryClick(entry.tabEntryId) }.takeIf { !isScheduled },
                            )
                        }
                    }
                    // Drawn last so it sits above the row rather than under its amount column.
                    if (isScheduled) {
                        ScheduledRowChip(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Makes an entry row tappable, or leaves it inert when there is nothing to open.
 *
 * A null [onClick] has to mean *no* [clickable] rather than an empty one: a scheduled placeholder
 * has no server id and no detail screen, and a no-op handler would still ripple under the finger and
 * still announce itself to a screen reader as something that can be activated.
 */
private fun Modifier.rowClick(onClick: (() -> Unit)?): Modifier =
    then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)

/** Marks a row as an occurrence that is due but not yet written by the server. */
@Composable
private fun ScheduledRowChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.recurring_chip_scheduled),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun ExpenseRow(
    expense: TabEntry.Expense,
    currentUserId: String,
    payerName: String,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .rowClick(onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(Res.drawable.ic_restaurant)
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (expense.isPendingSync) {
                    HorizontalSpacer(8.dp)
                    SyncStatusChip()
                }
            }
            val amountLabel = entryAmountLabel(expense, item, currency, ratesByCurrency)
            val subtitle =
                if (expense.paidByUserId == currentUserId) {
                    stringResource(Res.string.groups_detail_paid_by_you, amountLabel)
                } else {
                    stringResource(Res.string.groups_detail_paid_by, payerName.ifEmpty { "?" }, amountLabel)
                }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(8.dp)
        Column(horizontalAlignment = Alignment.End) {
            val involved =
                expense.paidByUserId == currentUserId ||
                    expense.splits.any { it.participantId == currentUserId }
            val net = UserBalanceCalculator.entryNet(expense, currentUserId)
            when {
                !involved -> {
                    Text(
                        text = stringResource(Res.string.groups_detail_row_not_involved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                net == 0.0 -> {
                    Text(
                        text = stringResource(Res.string.groups_detail_row_no_balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    val netColor =
                        if (net > 0) {
                            MaterialTheme.colorScheme.extended.positive
                        } else {
                            MaterialTheme.colorScheme.extended.negative
                        }
                    Text(
                        text =
                            stringResource(
                                if (net > 0) {
                                    Res.string.groups_detail_row_you_lent
                                } else {
                                    Res.string.groups_detail_row_you_borrowed
                                },
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = netColor,
                    )
                    val sign = if (net > 0) "+" else "−"
                    Text(
                        text = "$sign${shareAmountLabel(abs(net), expense, item, currency, ratesByCurrency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = netColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeRow(
    income: TabEntry.Income,
    currentUserId: String,
    payerName: String,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .rowClick(onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(
            icon = Res.drawable.ic_redeem,
            containerColor = MaterialTheme.colorScheme.extended.positiveContainer,
            contentColor = MaterialTheme.colorScheme.extended.onPositiveContainer,
        )
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = income.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (income.isPendingSync) {
                    HorizontalSpacer(8.dp)
                    SyncStatusChip()
                }
            }
            val amountLabel = entryAmountLabel(income, item, currency, ratesByCurrency)
            val subtitle =
                if (income.paidByUserId == currentUserId) {
                    stringResource(Res.string.groups_detail_received_by_you, amountLabel)
                } else {
                    stringResource(Res.string.groups_detail_received_by, payerName.ifEmpty { "?" }, amountLabel)
                }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(8.dp)
        Column(horizontalAlignment = Alignment.End) {
            val involved =
                income.paidByUserId == currentUserId ||
                    income.splits.any { it.participantId == currentUserId }
            val net = UserBalanceCalculator.entryNet(income, currentUserId)
            when {
                !involved -> {
                    Text(
                        text = stringResource(Res.string.groups_detail_row_not_involved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                net == 0.0 -> {
                    Text(
                        text = stringResource(Res.string.groups_detail_row_no_balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    val netColor =
                        if (net > 0) {
                            MaterialTheme.colorScheme.extended.positive
                        } else {
                            MaterialTheme.colorScheme.extended.negative
                        }
                    Text(
                        text =
                            stringResource(
                                if (net > 0) {
                                    Res.string.groups_detail_row_you_receive
                                } else {
                                    Res.string.groups_detail_row_you_owe
                                },
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = netColor,
                    )
                    val sign = if (net > 0) "+" else "−"
                    Text(
                        text = "$sign${shareAmountLabel(abs(net), income, item, currency, ratesByCurrency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = netColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(
    settlement: TabEntry.Settlement,
    currentUserId: String,
    payerName: String,
    recipientName: String,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
    onClick: (() -> Unit)?,
) {
    val extended = MaterialTheme.colorScheme.extended
    // Direction colors mirror the balance stat card: money in = positive, money out = negative,
    // settlements between other members = neutral settled.
    val subtitle: String
    val iconContainerColor: Color
    val iconContentColor: Color
    val amountColor: Color
    val amountPrefix: String
    when (currentUserId) {
        settlement.paidByUserId -> {
            subtitle =
                stringResource(
                    Res.string.groups_detail_settlement_you_paid,
                    recipientName.ifEmpty { "?" },
                )
            iconContainerColor = extended.negativeContainer
            iconContentColor = extended.onNegativeContainer
            amountColor = extended.negative
            amountPrefix = "−"
        }

        settlement.receivedByUserId -> {
            subtitle =
                stringResource(
                    Res.string.groups_detail_settlement_paid_you,
                    payerName.ifEmpty { "?" },
                )
            iconContainerColor = extended.positiveContainer
            iconContentColor = extended.onPositiveContainer
            amountColor = extended.positive
            amountPrefix = "+"
        }

        else -> {
            subtitle =
                stringResource(
                    Res.string.groups_detail_settlement_paid_other,
                    payerName.ifEmpty { "?" },
                    recipientName.ifEmpty { "?" },
                )
            iconContainerColor = extended.settledContainer
            iconContentColor = extended.onSettledContainer
            amountColor = MaterialTheme.colorScheme.onSurface
            amountPrefix = ""
        }
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .rowClick(onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(
            icon = Res.drawable.ic_swap_horiz,
            containerColor = iconContainerColor,
            contentColor = iconContentColor,
        )
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = settlement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (settlement.isPendingSync) {
                    HorizontalSpacer(8.dp)
                    SyncStatusChip()
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(8.dp)
        Text(
            text = "$amountPrefix${entryAmountLabel(settlement, item, currency, ratesByCurrency)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}

/**
 * Amount shown on a transaction row. For a foreign-currency entry, the amount actually booked
 * leads, followed by the converted amount in the group's base currency in brackets
 * (e.g. `$20.00 (≈ €18.40)`). Same-currency entries just show the single amount, and if no rate
 * is available the original amount is shown on its own. The entry's locked-in rate wins over the
 * live rate table, so displayed values match the balance math and don't drift with rate updates.
 */
@Composable
@ReadOnlyComposable
private fun entryAmountLabel(
    entry: TabEntry,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
): String {
    val originalSymbol = currency?.nativeSymbol ?: entry.currencyCode
    val originalDecimals = currency?.decimalDigits ?: item.currencyDecimalDigits
    val original = formatAmount(entry.amount, originalSymbol, originalDecimals)
    if (entry.currencyCode == item.currencyCode) return original

    val converted = convertEntryAmount(entry.amount, entry, item, ratesByCurrency) ?: return original
    return "$original (≈ ${formatAmount(item, converted)})"
}

/**
 * Like [entryAmountLabel] but for an arbitrary share [amount] in the entry's currency: converted
 * into the group's base currency when they differ (no bracketed original), falling back to the
 * entry's own currency when no rate is available.
 */
@Composable
@ReadOnlyComposable
private fun shareAmountLabel(
    amount: Double,
    entry: TabEntry,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
): String {
    val originalSymbol = currency?.nativeSymbol ?: entry.currencyCode
    val originalDecimals = currency?.decimalDigits ?: item.currencyDecimalDigits
    if (entry.currencyCode == item.currencyCode) return formatAmount(amount, originalSymbol, originalDecimals)
    val converted =
        convertEntryAmount(amount, entry, item, ratesByCurrency)
            ?: return formatAmount(amount, originalSymbol, originalDecimals)
    return formatAmount(item, converted)
}

/** [amount] converted into the group currency, preferring [TabEntry.exchangeRate] over live rates. */
private fun convertEntryAmount(
    amount: Double,
    entry: TabEntry,
    item: GroupOverviewItem,
    ratesByCurrency: Map<String, Double>,
): Double? =
    entry.exchangeRate?.let { amount * it }
        ?: CurrencyConverter.convert(
            amount = amount,
            from = entry.currencyCode,
            to = item.currencyCode,
            rates = ratesByCurrency,
        )

@Composable
private fun EntryIcon(
    icon: DrawableResource,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(containerColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BalancesTab(
    item: GroupOverviewItem,
    members: List<GroupParticipant>,
    formerMemberIds: Set<String>,
    currentUserId: String,
    memberNetBalances: Map<String, Double>,
    hasOutstandingDebts: Boolean,
    onSettleUpClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = FabBottomClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VerticalSpacer(16.dp)
        BalanceHero(item = item)
        if (hasOutstandingDebts) {
            Button(
                onClick = onSettleUpClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settle_up_action))
            }
        }
        val otherMembers = members.filter { it.userId != currentUserId }
        if (!hasOutstandingDebts && otherMembers.isEmpty()) {
            EmptyTabHint(text = stringResource(Res.string.groups_status_settled))
        } else {
            otherMembers
                .sortedBy { memberNetBalances[it.userId] ?: 0.0 }
                .forEach { participant ->
                    PerPersonRow(
                        participant = participant,
                        isFormerMember = participant.userId in formerMemberIds,
                        balance = GroupBalance.fromNet(memberNetBalances[participant.userId] ?: 0.0),
                        item = item,
                        onClick = onSettleUpClick,
                    )
                }
        }
    }
}

@Composable
private fun BalanceHero(item: GroupOverviewItem) {
    val extended = MaterialTheme.colorScheme.extended
    val (title, value, container, onContainer) =
        when (item.balance) {
            is GroupBalance.Owed -> {
                HeroPalette(
                    title = stringResource(Res.string.groups_detail_youre_owed),
                    value = formatSignedAmount(item, item.balance.amount, AmountSign.Positive),
                    container = extended.positive,
                    onContainer = MaterialTheme.colorScheme.surface,
                )
            }

            is GroupBalance.Owe -> {
                HeroPalette(
                    title = stringResource(Res.string.groups_detail_you_owe),
                    value = formatSignedAmount(item, item.balance.amount, AmountSign.Negative),
                    container = extended.negative,
                    onContainer = MaterialTheme.colorScheme.surface,
                )
            }

            GroupBalance.Settled -> {
                HeroPalette(
                    title = stringResource(Res.string.groups_status_settled),
                    value = stringResource(Res.string.groups_status_settled),
                    container = extended.settledContainer,
                    onContainer = extended.onSettledContainer,
                )
            }
        }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            VerticalSpacer(4.dp)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(4.dp)
            Text(
                text =
                    stringResource(
                        Res.string.groups_detail_across_people,
                        (item.memberCount - 1).coerceAtLeast(0),
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private data class HeroPalette(
    val title: String,
    val value: String,
    val container: Color,
    val onContainer: Color,
)

@Composable
private fun PerPersonRow(
    participant: GroupParticipant,
    isFormerMember: Boolean,
    balance: GroupBalance,
    item: GroupOverviewItem,
    onClick: () -> Unit,
) {
    val balanceVerb =
        when (balance) {
            is GroupBalance.Owed -> stringResource(Res.string.groups_detail_gets_back)
            is GroupBalance.Owe -> stringResource(Res.string.groups_detail_owes)
            GroupBalance.Settled -> stringResource(Res.string.groups_status_settled)
        }
    // A former member is only here because they still owe or are owed; saying so beats a row that
    // looks like any other member's.
    val verb =
        if (isFormerMember) {
            "${stringResource(Res.string.member_label_former)} · $balanceVerb"
        } else {
            balanceVerb
        }
    val amountText =
        when (balance) {
            is GroupBalance.Owed -> formatSignedAmount(item, balance.amount, AmountSign.Positive)
            is GroupBalance.Owe -> formatSignedAmount(item, balance.amount, AmountSign.Negative)
            GroupBalance.Settled -> null
        }
    val amountColor =
        when (balance) {
            is GroupBalance.Owed -> MaterialTheme.colorScheme.extended.positive
            is GroupBalance.Owe -> MaterialTheme.colorScheme.extended.negative
            GroupBalance.Settled -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(initials = participant.initials)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = verb,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (amountText != null) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                )
            }
            HorizontalSpacer(8.dp)
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StatCardsRow(
    item: GroupOverviewItem,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.colorScheme.extended
    val balanceTitle =
        when (item.balance) {
            is GroupBalance.Owed -> stringResource(Res.string.groups_detail_youre_owed)
            is GroupBalance.Owe -> stringResource(Res.string.groups_detail_you_owe)
            GroupBalance.Settled -> stringResource(Res.string.groups_status_settled)
        }
    val balanceValue =
        when (item.balance) {
            is GroupBalance.Owed -> formatSignedAmount(item, item.balance.amount, AmountSign.Positive)
            is GroupBalance.Owe -> formatSignedAmount(item, item.balance.amount, AmountSign.Negative)
            GroupBalance.Settled -> stringResource(Res.string.groups_status_settled)
        }
    val balanceColor =
        when (item.balance) {
            is GroupBalance.Owed -> extended.positiveContainer
            is GroupBalance.Owe -> extended.negativeContainer
            GroupBalance.Settled -> extended.settledContainer
        }
    val balanceTextColor =
        when (item.balance) {
            is GroupBalance.Owed -> extended.onPositiveContainer
            is GroupBalance.Owe -> extended.onNegativeContainer
            GroupBalance.Settled -> extended.onSettledContainer
        }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCard(
            title = balanceTitle,
            value = balanceValue,
            caption =
                stringResource(
                    Res.string.groups_detail_across_people,
                    (item.memberCount - 1).coerceAtLeast(0),
                ),
            container = balanceColor,
            onContainer = balanceTextColor,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = stringResource(Res.string.groups_detail_total_spent),
            value = formatAmount(item, item.totalSpent),
            caption = expenseCaption(item.expenseCount),
            container = MaterialTheme.colorScheme.surfaceVariant,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    caption: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = container,
                contentColor = onContainer,
            ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            VerticalSpacer(4.dp)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(4.dp)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyTabHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailTab.label(): String =
    when (this) {
        DetailTab.TRANSACTIONS -> stringResource(Res.string.groups_detail_tab_transactions)
        DetailTab.HISTORY -> stringResource(Res.string.groups_detail_tab_history)
        DetailTab.RECURRING -> stringResource(Res.string.recurring_tab_title)
        DetailTab.BALANCES -> stringResource(Res.string.groups_detail_tab_balances)
    }

@Composable
private fun memberCountText(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.groups_member_count_singular)
    } else {
        stringResource(Res.string.groups_members_count, count)
    }

@Composable
private fun expenseCaption(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.groups_expense_count_singular)
    } else {
        stringResource(Res.string.groups_expense_count, count)
    }

/**
 * The group's schedules, read-only — the same shape as the transactions tab.
 *
 * Everything you can do to a schedule lives on its detail screen, so a row is a link and nothing
 * else. Ended schedules stay in the list rather than vanishing: they explain entries that already
 * exist, and hiding them would make those entries look like they came from nowhere.
 */
@Composable
private fun RecurringTab(
    series: List<RecurringSeries>,
    currencyByCode: Map<String, Currency>,
    onSeriesClick: (String) -> Unit,
) {
    if (series.isEmpty()) {
        EmptyTabHint(text = stringResource(Res.string.recurring_empty_hint))
        return
    }

    val monthLabels = rememberMonthAbbreviations()
    val today =
        remember {
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.UTC)
                .date
        }
    val (active, ended) = series.partition { it.isActive }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = FabBottomClearance)) {
        if (active.isNotEmpty()) {
            SectionLabel(
                text = stringResource(Res.string.recurring_section_active),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            active.forEach { candidate ->
                RecurringRow(
                    series = candidate,
                    currency = currencyByCode[candidate.rule.currencyCode],
                    monthLabels = monthLabels,
                    today = today,
                    onClick = { onSeriesClick(candidate.seriesId) },
                )
            }
        }
        if (ended.isNotEmpty()) {
            SectionLabel(
                text = stringResource(Res.string.recurring_section_ended),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ended.forEach { candidate ->
                RecurringRow(
                    series = candidate,
                    currency = currencyByCode[candidate.rule.currencyCode],
                    monthLabels = monthLabels,
                    today = today,
                    onClick = { onSeriesClick(candidate.seriesId) },
                )
            }
        }
    }
}

@Composable
private fun RecurringRow(
    series: RecurringSeries,
    currency: Currency?,
    monthLabels: List<String>,
    today: LocalDate,
    onClick: () -> Unit,
) {
    // A parked schedule has no next occurrence to promise — the server writes nothing for it until
    // the template is repaired — so the subtitle falls back to the cadence alone. Remembered because
    // finding the date walks the schedule slot by slot from its start.
    val nextOccurrence =
        remember(series, today) {
            if (series.isActive && !series.needsAttention) {
                RecurringOccurrenceCalculator
                    .upcomingOccurrences(
                        rule = series.rule,
                        after = today,
                        limit = 1,
                        skippedDates = series.skippedOccurrenceDates,
                    ).firstOrNull()
            } else {
                null
            }
        }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(series.rule.title) },
        supportingContent = {
            Text(
                text =
                    listOfNotNull(
                        frequencyLabel(series.rule.frequency, series.rule.interval),
                        nextOccurrence?.let {
                            stringResource(Res.string.recurring_next_on, formatEntryDate(it, monthLabels))
                        },
                    ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text =
                        formatAmount(
                            series.rule.amount,
                            currency?.nativeSymbol ?: series.rule.currencyCode,
                            currency?.decimalDigits ?: 2,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                when {
                    series.needsAttention -> {
                        RecurringStateChip(
                            text = stringResource(Res.string.recurring_chip_needs_attention),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    !series.isActive -> {
                        RecurringStateChip(
                            text = stringResource(Res.string.recurring_chip_ended),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun RecurringStateChip(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
