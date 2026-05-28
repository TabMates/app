package de.tabmates.features.tabgroup.presentation.navigation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatAmount
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_member_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_members_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_status_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_empty
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_net_balance
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_see_all
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_subtitle_owe
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_subtitle_owed
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_subtitle_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.home_your_groups

@Composable
fun HomeRoot(
    onGroupClick: (String) -> Unit,
    onSeeAllGroups: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onGroupClick = onGroupClick,
        onSeeAllGroups = onSeeAllGroups,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeState,
    onGroupClick: (String) -> Unit,
    onSeeAllGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
            VerticalSpacer(8.dp)
            HomeTopBar()
            VerticalSpacer(8.dp)
            NetBalanceHero(state = state)
            VerticalSpacer(20.dp)
            GroupsHeader(onSeeAllGroups = onSeeAllGroups)
            VerticalSpacer(8.dp)
            if (state.topGroups.isEmpty()) {
                if (!state.isLoading) {
                    Text(
                        text = stringResource(Res.string.home_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.topGroups.forEach { group ->
                        GroupRow(group = group, onClick = { onGroupClick(group.id) })
                    }
                }
            }
            VerticalSpacer(24.dp)
        }
    }
}

@Composable
private fun HomeTopBar() {
    Text(
        text = stringResource(Res.string.home_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

@Composable
private fun NetBalanceHero(state: HomeState) {
    val extended = MaterialTheme.colorScheme.extended
    val balance = GroupBalance.fromNet(state.netAmount)
    val (container, content) =
        when (balance) {
            is GroupBalance.Owed -> extended.positiveContainer to extended.onPositiveContainer
            is GroupBalance.Owe -> extended.negativeContainer to extended.onNegativeContainer
            GroupBalance.Settled -> extended.settledContainer to extended.onSettledContainer
        }
    val value =
        when (balance) {
            is GroupBalance.Owed -> "+${formatAmount(balance.amount, state.displaySymbol, state.displayDecimals)}"
            is GroupBalance.Owe -> "−${formatAmount(balance.amount, state.displaySymbol, state.displayDecimals)}"
            GroupBalance.Settled -> formatAmount(0.0, state.displaySymbol, state.displayDecimals)
        }
    val subtitle =
        when (balance) {
            is GroupBalance.Owed -> stringResource(Res.string.home_subtitle_owed, state.contributingGroupCount)
            is GroupBalance.Owe -> stringResource(Res.string.home_subtitle_owe, state.contributingGroupCount)
            GroupBalance.Settled -> stringResource(Res.string.home_subtitle_settled)
        }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = stringResource(Res.string.home_net_balance), style = MaterialTheme.typography.bodyMedium)
            VerticalSpacer(4.dp)
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            VerticalSpacer(4.dp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GroupsHeader(onSeeAllGroups: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_your_groups),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.home_see_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSeeAllGroups),
        )
    }
}

@Composable
private fun GroupRow(
    group: HomeGroup,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupAvatar(iconKey = group.iconKey, colorKey = group.colorKey, size = 44.dp)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group.memberInitials.isNotEmpty()) {
                        MemberAvatarStack(initials = group.memberInitials)
                        HorizontalSpacer(8.dp)
                    }
                    Text(
                        text = memberCountText(group.memberCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalSpacer(8.dp)
            GroupBalanceText(group)
        }
    }
}

@Composable
private fun GroupBalanceText(group: HomeGroup) {
    val extended = MaterialTheme.colorScheme.extended
    val (text, color) =
        when (val balance = group.balance) {
            is GroupBalance.Owed -> {
                "+${formatAmount(balance.amount, group.currencySymbol, group.currencyDecimals)}" to
                    extended.positive
            }

            is GroupBalance.Owe -> {
                "−${formatAmount(balance.amount, group.currencySymbol, group.currencyDecimals)}" to
                    extended.negative
            }

            GroupBalance.Settled -> {
                stringResource(Res.string.groups_status_settled) to MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun MemberAvatarStack(initials: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        initials.forEach { value ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(26.dp),
            ) {
                Row(
                    modifier = Modifier.size(26.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UserAvatar(initials = value, size = 22.dp)
                }
            }
        }
    }
}

@Composable
private fun memberCountText(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.groups_member_count_singular)
    } else {
        stringResource(Res.string.groups_members_count, count)
    }
