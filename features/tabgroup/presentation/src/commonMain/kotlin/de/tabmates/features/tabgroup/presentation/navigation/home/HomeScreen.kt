package de.tabmates.features.tabgroup.presentation.navigation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.core.presentation.format.AmountSign
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.components.SyncStatusChip
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatAmount
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.formatSignedAmount
import kotlinx.coroutines.delay
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

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
    val showLoading = rememberMinimumLoading(state.isLoading)
    val balance = GroupBalance.fromNet(state.netAmount)
    val (container, content) =
        when {
            showLoading -> extended.settledContainer to extended.onSettledContainer
            balance is GroupBalance.Owed -> extended.positiveContainer to extended.onPositiveContainer
            balance is GroupBalance.Owe -> extended.negativeContainer to extended.onNegativeContainer
            else -> extended.settledContainer to extended.onSettledContainer
        }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = stringResource(Res.string.home_net_balance), style = MaterialTheme.typography.bodyMedium)
            if (showLoading) {
                NetBalanceHeroPlaceholder(content = content)
            } else {
                NetBalanceHeroValue(balance = balance, state = state)
            }
        }
    }
}

@Composable
private fun rememberMinimumLoading(isLoading: Boolean): Boolean {
    var show by remember { mutableStateOf(isLoading) }
    var shownAt by remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            if (!show) shownAt = TimeSource.Monotonic.markNow()
            show = true
        } else if (show) {
            val elapsed = shownAt?.elapsedNow() ?: Duration.ZERO
            val remaining = SHIMMER_DURATION_MS.milliseconds - elapsed
            if (remaining.isPositive()) delay(remaining)
            show = false
        }
    }
    return show
}

@Composable
private fun NetBalanceHeroValue(
    balance: GroupBalance,
    state: HomeState,
) {
    val value =
        when (balance) {
            is GroupBalance.Owed -> {
                formatSignedAmount(balance.amount, state.displaySymbol, state.displayDecimals, AmountSign.Positive)
            }

            is GroupBalance.Owe -> {
                formatSignedAmount(balance.amount, state.displaySymbol, state.displayDecimals, AmountSign.Negative)
            }

            GroupBalance.Settled -> {
                formatAmount(0.0, state.displaySymbol, state.displayDecimals)
            }
        }
    val subtitle =
        when (balance) {
            is GroupBalance.Owed -> stringResource(Res.string.home_subtitle_owed, state.contributingGroupCount)
            is GroupBalance.Owe -> stringResource(Res.string.home_subtitle_owe, state.contributingGroupCount)
            GroupBalance.Settled -> stringResource(Res.string.home_subtitle_settled)
        }
    VerticalSpacer(4.dp)
    Text(
        text = value,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
    )
    VerticalSpacer(4.dp)
    Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun NetBalanceHeroPlaceholder(content: Color) {
    val density = LocalDensity.current
    val valueHeight =
        with(density) {
            MaterialTheme.typography.displaySmall.lineHeight
                .toDp()
        }
    val subtitleHeight =
        with(density) {
            MaterialTheme.typography.bodySmall.lineHeight
                .toDp()
        }
    VerticalSpacer(4.dp)
    PlaceholderBar(widthFraction = 0.45f, height = valueHeight, color = content)
    VerticalSpacer(4.dp)
    PlaceholderBar(widthFraction = 0.6f, height = subtitleHeight, color = content)
}

@Composable
private fun PlaceholderBar(
    widthFraction: Float,
    height: Dp,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(RoundedCornerShape(percent = 30))
                .background(shimmerBrush(color)),
    )
}

private const val SHIMMER_DURATION_MS = 500
private const val SHIMMER_BAND_PX = 250f
private const val SHIMMER_SWEEP_PX = 600f

@Composable
private fun shimmerBrush(color: Color): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer-progress",
    )
    val band = SHIMMER_BAND_PX
    val start = progress * (SHIMMER_SWEEP_PX + band) - band
    return Brush.linearGradient(
        colors =
            listOf(
                color.copy(alpha = 0.1f),
                color.copy(alpha = 0.3f),
                color.copy(alpha = 0.1f),
            ),
        start = Offset(start, 0f),
        end = Offset(start + band, 0f),
    )
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupAvatar(iconKey = group.iconKey, colorKey = group.colorKey, size = 44.dp)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (group.hasPendingSync) {
                        HorizontalSpacer(8.dp)
                        SyncStatusChip()
                    }
                }
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
                formatSignedAmount(
                    balance.amount,
                    group.currencySymbol,
                    group.currencyDecimals,
                    AmountSign.Positive,
                ) to extended.positive
            }

            is GroupBalance.Owe -> {
                formatSignedAmount(
                    balance.amount,
                    group.currencySymbol,
                    group.currencyDecimals,
                    AmountSign.Negative,
                ) to extended.negative
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

@PreviewThemes
@Composable
private fun NetBalanceHeroLoadingPreview() {
    TabMatesTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                NetBalanceHero(state = HomeState(isLoading = true))
            }
        }
    }
}
