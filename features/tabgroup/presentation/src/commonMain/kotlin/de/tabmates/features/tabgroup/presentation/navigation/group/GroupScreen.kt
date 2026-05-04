package de.tabmates.features.tabgroup.presentation.navigation.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewScreenSizes
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.features.tabgroup.presentation.navigation.CreateGroup
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_balance_owe
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_balance_owed
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_balance_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_empty_create_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_empty_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_empty_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_members_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_add
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_groups
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@Composable
fun GroupRoot(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupScreen(
        state = state,
        onCreateGroupClick = { backStack.add(CreateGroup) },
        modifier = modifier,
    )
}

@Composable
private fun GroupScreen(
    state: GroupState,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isEmpty -> {
            GroupEmptyState(
                onCreateGroupClick = onCreateGroupClick,
                modifier = modifier,
            )
        }

        else -> {
            GroupListContent(
                items = state.items,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun GroupListContent(
    items: List<GroupListItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            GroupListRow(item = item, indexHint = items.indexOf(item))
        }
    }
}

@Composable
private fun GroupListRow(
    item: GroupListItem,
    indexHint: Int,
) {
    val palette = MaterialTheme.colorScheme.containerPalette()
    val tone = palette[indexHint.mod(palette.size)]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(color = tone.background, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_groups),
                    contentDescription = null,
                    tint = tone.foreground,
                    modifier = Modifier.size(28.dp),
                )
            }
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                VerticalSpacer(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarStack(
                        initials = item.memberInitials,
                        extra = item.extraMembers,
                    )
                    HorizontalSpacer(8.dp)
                    Text(
                        text = stringResource(Res.string.group_members_count, item.memberCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalSpacer(8.dp)
            BalanceColumn(
                balance = item.balance,
                currencySymbol = item.currencySymbol,
            )
        }
    }
}

@Composable
private fun AvatarStack(
    initials: List<String>,
    extra: Int,
) {
    Row {
        initials.forEachIndexed { index, value ->
            AvatarChip(
                text = value,
                offset = index,
                background = MaterialTheme.colorScheme.tertiaryContainer,
                foreground = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (extra > 0) {
            AvatarChip(
                text = "+$extra",
                offset = initials.size,
                background = MaterialTheme.colorScheme.surfaceVariant,
                foreground = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AvatarChip(
    text: String,
    offset: Int,
    background: Color,
    foreground: Color,
) {
    Box(
        modifier =
            Modifier
                .offset(x = (offset * AVATAR_OVERLAP_DP).dp)
                .size(AVATAR_SIZE_DP.dp)
                .background(color = background, shape = CircleShape)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BalanceColumn(
    balance: GroupBalance,
    currencySymbol: String,
) {
    val (amountText, labelRes, color) =
        when (balance) {
            GroupBalance.Settled -> {
                Triple(
                    formatAmount(currencySymbol, 0.0),
                    Res.string.group_balance_settled,
                    MaterialTheme.colorScheme.onSurface,
                )
            }

            is GroupBalance.Owed -> {
                Triple(
                    formatAmount(currencySymbol, balance.amount),
                    Res.string.group_balance_owed,
                    MaterialTheme.colorScheme.tertiary,
                )
            }

            is GroupBalance.Owe -> {
                Triple(
                    formatAmount(currencySymbol, balance.amount),
                    Res.string.group_balance_owe,
                    MaterialTheme.colorScheme.error,
                )
            }
        }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupEmptyState(
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(24.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_groups),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        VerticalSpacer(24.dp)
        Text(
            text = stringResource(Res.string.group_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.group_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(24.dp)
        TabMatesButton(
            text = stringResource(Res.string.group_empty_create_action),
            onClick = onCreateGroupClick,
            leadingIcon = {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
    }
}

private data class ContainerTone(
    val background: Color,
    val foreground: Color,
)

@Composable
private fun ColorScheme.containerPalette(): List<ContainerTone> =
    listOf(
        ContainerTone(tertiaryContainer, onTertiaryContainer),
        ContainerTone(secondaryContainer, onSecondaryContainer),
        ContainerTone(primaryContainer, onPrimaryContainer),
        ContainerTone(errorContainer, onErrorContainer),
    )

private fun formatAmount(
    symbol: String,
    amount: Double,
): String {
    val absMinor = (amount.absoluteValue * 100).roundToLong()
    val whole = absMinor / 100
    val frac = (absMinor % 100).toString().padStart(2, '0')
    val sign = if (amount < 0.0) "-" else ""
    return "$sign$symbol$whole.$frac"
}

private const val AVATAR_SIZE_DP = 22
private const val AVATAR_OVERLAP_DP = -8

@PreviewThemes
@Composable
private fun GroupEmptyStateThemesPreview() {
    TabMatesTheme {
        Surface {
            GroupScreen(
                state = GroupState(isLoading = false, items = emptyList()),
                onCreateGroupClick = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun GroupEmptyStateSizesPreview() {
    TabMatesTheme {
        Surface {
            GroupScreen(
                state = GroupState(isLoading = false, items = emptyList()),
                onCreateGroupClick = {},
            )
        }
    }
}

private fun previewItems(): List<GroupListItem> =
    listOf(
        GroupListItem(
            id = "1",
            title = "Weekend in Lisbon",
            memberCount = 4,
            memberInitials = listOf("AB", "CD", "EF"),
            extraMembers = 1,
            currencySymbol = "€",
            balance = GroupBalance.Owed(23.50),
        ),
        GroupListItem(
            id = "2",
            title = "The Flat",
            memberCount = 2,
            memberInitials = listOf("AB", "CD"),
            extraMembers = 0,
            currencySymbol = "€",
            balance = GroupBalance.Owe(12.00),
        ),
        GroupListItem(
            id = "3",
            title = "Office Lunch Club",
            memberCount = 5,
            memberInitials = listOf("AB", "MN", "DE"),
            extraMembers = 2,
            currencySymbol = "€",
            balance = GroupBalance.Owed(21.70),
        ),
        GroupListItem(
            id = "4",
            title = "Bike Trip · Douro",
            memberCount = 3,
            memberInitials = listOf("AB", "MN", "CD"),
            extraMembers = 0,
            currencySymbol = "€",
            balance = GroupBalance.Settled,
        ),
    )

@PreviewThemes
@Composable
private fun GroupListContentThemesPreview() {
    TabMatesTheme {
        Surface {
            GroupScreen(
                state = GroupState(isLoading = false, items = previewItems()),
                onCreateGroupClick = {},
            )
        }
    }
}
