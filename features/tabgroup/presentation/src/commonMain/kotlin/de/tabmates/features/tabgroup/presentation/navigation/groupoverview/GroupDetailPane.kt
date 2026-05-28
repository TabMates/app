package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.core.presentation.share.LinkShareResult
import de.tabmates.core.presentation.share.rememberLinkSharer
import de.tabmates.features.tabgroup.domain.currency.CurrencyConverter
import de.tabmates.features.tabgroup.domain.models.Currency
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.buildInviteUrl
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.shortInviteUrl
import de.tabmates.features.tabgroup.presentation.navigation.groupsettings.GroupSettingsRoot
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_settings_open_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_across_people
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_add_expense
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_empty_expenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_copied
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_copy
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_link_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_rotate_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_share
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_share_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_owes_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_owner_badge
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_paid_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_pending_badge
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_pending_not_claimed
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_balances
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_expenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_members
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_tab_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_total_spent
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_you_owe
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_you_owe_them
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_youre_owed
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_member_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_members_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_status_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_content_copy
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_link
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_person_add
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_refresh
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_restaurant
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_send
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_action
import kotlin.math.abs

private enum class DetailTab { EXPENSES, BALANCES, MEMBERS, SETTINGS }

@Composable
internal fun GroupDetailPane(
    item: GroupOverviewItem,
    currentUserId: String,
    members: List<GroupParticipant>,
    expenses: List<TabEntry.Expense>,
    perPersonBalances: Map<String, Double>,
    currencyByCode: Map<String, Currency>,
    ratesByCurrency: Map<String, Double>,
    onRotateInvite: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddExpenseClick: () -> Unit = {},
    onSettleUpClick: () -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    val visibleTabs =
        if (isExpanded) DetailTab.entries.toList() else DetailTab.entries.filterNot { it == DetailTab.SETTINGS }
    var selectedTab by rememberSaveable(item.id) { mutableStateOf(DetailTab.EXPENSES) }
    if (selectedTab !in visibleTabs) selectedTab = DetailTab.EXPENSES
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
    val onCopyInvite: () -> Unit = {
        if (item.inviteToken.isNotBlank()) {
            showResultSnackbar(linkSharer.copy(inviteUrl))
        }
    }
    val onShareInvite: () -> Unit = {
        if (item.inviteToken.isNotBlank()) {
            showResultSnackbar(linkSharer.share(inviteUrl))
        }
    }
    Column(modifier = modifier) {
        DetailHeader(
            item = item,
            isExpanded = isExpanded,
            onInviteClick = onShareInvite,
            onSettingsClick = onSettingsClick,
            onAddExpenseClick = onAddExpenseClick,
        )
        PrimaryTabRow(
            selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label()) },
                )
            }
        }
        VerticalSpacer(16.dp)
        when (selectedTab) {
            DetailTab.EXPENSES -> {
                ExpensesTab(
                    item = item,
                    currentUserId = currentUserId,
                    members = members,
                    expenses = expenses,
                    currencyByCode = currencyByCode,
                    ratesByCurrency = ratesByCurrency,
                    onExpenseClick = onExpenseClick,
                )
            }

            DetailTab.BALANCES -> {
                BalancesTab(
                    item = item,
                    members = members,
                    perPersonBalances = perPersonBalances,
                    onSettleUpClick = onSettleUpClick,
                )
            }

            DetailTab.MEMBERS -> {
                MembersTab(
                    item = item,
                    members = members,
                    currentUserId = currentUserId,
                    perPersonBalances = perPersonBalances,
                    onCopyInvite = onCopyInvite,
                    onShareInvite = onShareInvite,
                    onSharePendingInvite = onShareInvite,
                    onRotateInvite = onRotateInvite,
                )
            }

            DetailTab.SETTINGS -> {
                GroupSettingsRoot(
                    groupId = item.id,
                    onLeft = {},
                    snackbarHostState = snackbarHostState,
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
    onAddExpenseClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
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
            if (isExpanded) {
                HeaderActions(
                    modifier = Modifier,
                    onInviteClick = onInviteClick,
                    onAddExpenseClick = onAddExpenseClick,
                )
            } else {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_settings),
                        contentDescription = stringResource(Res.string.group_settings_open_cd),
                    )
                }
            }
        }
        if (!isExpanded) {
            VerticalSpacer(16.dp)
            HeaderActions(
                modifier = Modifier.fillMaxWidth(),
                onInviteClick = onInviteClick,
                onAddExpenseClick = onAddExpenseClick,
            )
        }
    }
}

@Composable
private fun HeaderActions(
    modifier: Modifier = Modifier,
    onInviteClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
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
            onClick = onAddExpenseClick,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text("+ ${stringResource(Res.string.groups_detail_add_expense)}")
        }
    }
}

@Composable
private fun ExpensesTab(
    item: GroupOverviewItem,
    currentUserId: String,
    members: List<GroupParticipant>,
    expenses: List<TabEntry.Expense>,
    currencyByCode: Map<String, Currency>,
    ratesByCurrency: Map<String, Double>,
    onExpenseClick: (String) -> Unit,
) {
    val payerById = remember(members) { members.associateBy { it.userId } }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCardsRow(item = item)
        if (expenses.isEmpty()) {
            EmptyTabHint(text = stringResource(Res.string.groups_detail_empty_expenses))
        } else {
            expenses.forEach { expense ->
                ExpenseRow(
                    expense = expense,
                    currentUserId = currentUserId,
                    payerName = payerById[expense.paidByUserId]?.username.orEmpty(),
                    item = item,
                    currency = currencyByCode[expense.currencyCode],
                    ratesByCurrency = ratesByCurrency,
                    onClick = { onExpenseClick(expense.tabEntryId) },
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: TabEntry.Expense,
    currentUserId: String,
    payerName: String,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpenseIcon()
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle =
                if (expense.paidByUserId == currentUserId) {
                    stringResource(Res.string.groups_detail_paid_by_you)
                } else {
                    stringResource(Res.string.groups_detail_paid_by, payerName.ifEmpty { "?" })
                }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalSpacer(8.dp)
        Text(
            text = expenseAmountLabel(expense, item, currency, ratesByCurrency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Amount shown on an expense row. For a foreign-currency expense, the converted amount in the
 * group's base currency leads, followed by the original amount in brackets
 * (e.g. `€18.40 - ($20.00)`). Same-currency expenses just show the single amount, and if no rate
 * is available the original amount is shown on its own.
 */
private fun expenseAmountLabel(
    expense: TabEntry.Expense,
    item: GroupOverviewItem,
    currency: Currency?,
    ratesByCurrency: Map<String, Double>,
): String {
    val originalSymbol = currency?.nativeSymbol ?: expense.currencyCode
    val originalDecimals = currency?.decimalDigits ?: item.currencyDecimalDigits
    val original = formatAmount(expense.amount, originalSymbol, originalDecimals)
    if (expense.currencyCode == item.currencyCode) return original

    val converted =
        CurrencyConverter.convert(
            amount = expense.amount,
            from = expense.currencyCode,
            to = item.currencyCode,
            rates = ratesByCurrency,
        ) ?: return original
    return "${formatAmount(item, converted)} ≈ ($original)"
}

@Composable
private fun ExpenseIcon() {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_restaurant),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BalancesTab(
    item: GroupOverviewItem,
    members: List<GroupParticipant>,
    perPersonBalances: Map<String, Double>,
    onSettleUpClick: () -> Unit,
) {
    val owesSomeone = perPersonBalances.values.any { it < 0 }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BalanceHero(item = item)
        if (owesSomeone) {
            Button(
                onClick = onSettleUpClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settle_up_action))
            }
        }
        if (perPersonBalances.isEmpty()) {
            EmptyTabHint(text = stringResource(Res.string.groups_status_settled))
        } else {
            members
                .filter { it.userId in perPersonBalances.keys }
                .forEach { participant ->
                    val net = perPersonBalances[participant.userId] ?: 0.0
                    PerPersonRow(
                        participant = participant,
                        net = net,
                        item = item,
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
                    value = "+${formatAmount(item, item.balance.amount)}",
                    container = extended.positive,
                    onContainer = MaterialTheme.colorScheme.surface,
                )
            }

            is GroupBalance.Owe -> {
                HeroPalette(
                    title = stringResource(Res.string.groups_detail_you_owe),
                    value = "−${formatAmount(item, item.balance.amount)}",
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
    net: Double,
    item: GroupOverviewItem,
) {
    val isPositive = net >= 0
    val (verb, sign, color) =
        if (isPositive) {
            Triple(
                stringResource(Res.string.groups_detail_owes_you),
                "+",
                MaterialTheme.colorScheme.extended.positive,
            )
        } else {
            Triple(
                stringResource(Res.string.groups_detail_you_owe_them),
                "−",
                MaterialTheme.colorScheme.extended.negative,
            )
        }
    Card(
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
            Text(
                text = "$sign${formatAmount(item, abs(net))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable
private fun MembersTab(
    item: GroupOverviewItem,
    members: List<GroupParticipant>,
    currentUserId: String,
    perPersonBalances: Map<String, Double>,
    onCopyInvite: () -> Unit,
    onShareInvite: () -> Unit,
    onSharePendingInvite: () -> Unit,
    onRotateInvite: () -> Unit,
) {
    val active = members.filter { it.participantType != ParticipantType.PLACEHOLDER }
    val pending = members.filter { it.participantType == ParticipantType.PLACEHOLDER }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = memberCountText(active.size).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        active.forEach { participant ->
            MemberRow(
                participant = participant,
                isCurrentUser = participant.userId == currentUserId,
                isCreator = participant.userId == item.creatorUserId,
                net = perPersonBalances[participant.userId] ?: 0.0,
                item = item,
            )
        }
        pending.forEach { participant ->
            PendingMemberRow(
                participant = participant,
                onShareClick = onSharePendingInvite,
            )
        }
        if (item.inviteToken.isNotBlank()) {
            InviteLinkCard(
                inviteToken = item.inviteToken,
                onCopy = onCopyInvite,
                onShare = onShareInvite,
                onRotate = onRotateInvite,
            )
        }
    }
}

@Composable
private fun PendingMemberRow(
    participant: GroupParticipant,
    onShareClick: () -> Unit,
) {
    Card(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    HorizontalSpacer(8.dp)
                    AssistChip(
                        onClick = onShareClick,
                        label = {
                            Text(stringResource(Res.string.groups_detail_pending_badge))
                        },
                    )
                }
                Text(
                    text = stringResource(Res.string.groups_detail_pending_not_claimed),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_send),
                    contentDescription = stringResource(Res.string.groups_detail_invite_share_cd),
                )
            }
        }
    }
}

@Composable
private fun InviteLinkCard(
    inviteToken: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRotate: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_link),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                HorizontalSpacer(8.dp)
                Text(
                    text = stringResource(Res.string.groups_detail_invite_link_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRotate) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_refresh),
                        contentDescription = stringResource(Res.string.groups_detail_invite_rotate_cd),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            VerticalSpacer(8.dp)
            Text(
                text = shortInviteUrl(inviteToken),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalSpacer(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCopy) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_content_copy),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    HorizontalSpacer(6.dp)
                    Text(stringResource(Res.string.groups_detail_invite_copy))
                }
                FilledTonalButton(onClick = onShare) {
                    Text(stringResource(Res.string.groups_detail_invite_share))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    participant: GroupParticipant,
    isCurrentUser: Boolean,
    isCreator: Boolean,
    net: Double,
    item: GroupOverviewItem,
) {
    val balanceText =
        when {
            isCurrentUser -> null
            net > 0 -> "+${formatAmount(item, net)}"
            net < 0 -> "−${formatAmount(item, abs(net))}"
            else -> null
        }
    val balanceColor =
        when {
            net > 0 -> MaterialTheme.colorScheme.extended.positive
            net < 0 -> MaterialTheme.colorScheme.extended.negative
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(initials = participant.initials)
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCurrentUser) "You" else participant.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isCreator) {
                    HorizontalSpacer(8.dp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = stringResource(Res.string.groups_detail_owner_badge),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = participant.username,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (balanceText != null) {
            Text(
                text = balanceText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = balanceColor,
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
            is GroupBalance.Owed -> "+${formatAmount(item, item.balance.amount)}"
            is GroupBalance.Owe -> "−${formatAmount(item, item.balance.amount)}"
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
private fun EmptyTabHint(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
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
        DetailTab.EXPENSES -> stringResource(Res.string.groups_detail_tab_expenses)
        DetailTab.BALANCES -> stringResource(Res.string.groups_detail_tab_balances)
        DetailTab.MEMBERS -> stringResource(Res.string.groups_detail_tab_members)
        DetailTab.SETTINGS -> stringResource(Res.string.groups_detail_tab_settings)
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
        "1 expense"
    } else {
        "$count expenses"
    }
