package de.tabmates.features.tabgroup.presentation.navigation.groupoverview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.preview.PreviewScreenSizes
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.designsystem.theme.extended
import de.tabmates.features.tabgroup.domain.models.GroupBalance
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.GroupDetailRoot
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_empty_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_empty_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_expense_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_expense_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_filter_active
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_filter_all
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_filter_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_member_count_singular
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_members_and_expenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_members_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_search_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_search_close_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_search_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_status_settled
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_search
import kotlin.time.Clock

@Composable
fun GroupOverviewRoot(
    onGroupOpen: (String) -> Unit,
    viewModel: GroupOverviewViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupOverviewScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onGroupSelected = viewModel::onGroupSelected,
        onGroupOpen = onGroupOpen,
    )
}

@Composable
private fun GroupOverviewScreen(
    state: GroupOverviewState,
    onFilterSelected: (GroupFilter) -> Unit,
    onGroupSelected: (String) -> Unit,
    onGroupOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    if (isExpanded) {
        ExpandedLayout(
            state = state,
            onFilterSelected = onFilterSelected,
            onGroupSelected = onGroupSelected,
            modifier = modifier,
        )
    } else {
        CompactLayout(
            state = state,
            onFilterSelected = onFilterSelected,
            onGroupOpen = onGroupOpen,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactLayout(
    state: GroupOverviewState,
    onFilterSelected: (GroupFilter) -> Unit,
    onGroupOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        GroupsHeader(
            currentUserInitials = state.currentUserInitials,
            showAvatar = true,
            searchQueryState = state.searchQueryState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        FilterChipsRow(
            filter = state.filter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VerticalSpacer(8.dp)
        GroupList(
            items = state.displayedItems,
            selectedGroupId = null,
            onGroupSelected = onGroupOpen,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ExpandedLayout(
    state: GroupOverviewState,
    onFilterSelected: (GroupFilter) -> Unit,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .widthIn(min = 280.dp, max = 360.dp)
                    .fillMaxHeight(),
        ) {
            GroupsHeader(
                currentUserInitials = state.currentUserInitials,
                showAvatar = false,
                searchQueryState = state.searchQueryState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            FilterChipsRow(
                filter = state.filter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            VerticalSpacer(8.dp)
            GroupList(
                items = state.displayedItems,
                selectedGroupId = state.selectedGroupId,
                onGroupSelected = onGroupSelected,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            )
        }
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val selectedId = state.selectedGroupId
            if (selectedId == null) {
                DetailPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                key(selectedId) {
                    GroupDetailRoot(
                        groupId = selectedId,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupsHeader(
    currentUserInitials: String,
    showAvatar: Boolean,
    searchQueryState: TextFieldState,
    modifier: Modifier = Modifier,
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSearchActive) {
            SearchField(
                state = searchQueryState,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
            HorizontalSpacer(4.dp)
            IconButton(
                onClick = {
                    searchQueryState.edit { replace(0, length, "") }
                    isSearchActive = false
                },
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.groups_search_close_cd),
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.groups_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { isSearchActive = true }) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_search),
                    contentDescription = stringResource(Res.string.groups_search_cd),
                )
            }
            if (showAvatar) {
                HorizontalSpacer(4.dp)
                UserAvatar(initials = currentUserInitials)
            }
        }
    }
}

@Composable
private fun SearchField(
    state: TextFieldState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    TextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { Text(stringResource(Res.string.groups_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_search),
                contentDescription = null,
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
internal fun UserAvatar(initials: String) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FilterChipsRow(
    filter: GroupFilter,
    onFilterSelected: (GroupFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupFilter.entries.forEach { entry ->
            FilterChip(
                selected = filter == entry,
                onClick = { onFilterSelected(entry) },
                label = { Text(entry.label()) },
                shape = RoundedCornerShape(20.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        }
    }
}

@Composable
private fun GroupFilter.label(): String =
    when (this) {
        GroupFilter.ALL -> stringResource(Res.string.groups_filter_all)
        GroupFilter.ACTIVE -> stringResource(Res.string.groups_filter_active)
        GroupFilter.SETTLED -> stringResource(Res.string.groups_filter_settled)
    }

@Composable
private fun GroupList(
    items: List<GroupOverviewItem>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyState(modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            GroupCard(
                item = item,
                selected = item.id == selectedGroupId,
                onClick = { onGroupSelected(item.id) },
            )
        }
    }
}

@Composable
private fun GroupCard(
    item: GroupOverviewItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupAvatar(iconKey = item.iconKey, colorKey = item.colorKey)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = membersAndExpensesText(item.memberCount, item.expenseCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalSpacer(8.dp)
            BalanceLabel(item = item)
        }
    }
}

@Composable
private fun membersAndExpensesText(
    memberCount: Int,
    expenseCount: Int,
): String {
    val members =
        if (memberCount == 1) {
            stringResource(Res.string.groups_member_count_singular)
        } else {
            stringResource(Res.string.groups_members_count, memberCount)
        }
    if (expenseCount <= 0) return members
    val expenses =
        if (expenseCount == 1) {
            stringResource(Res.string.groups_expense_count_singular)
        } else {
            stringResource(Res.string.groups_expense_count, expenseCount)
        }
    return stringResource(Res.string.groups_members_and_expenses, members, expenses)
}

@Composable
private fun BalanceLabel(item: GroupOverviewItem) {
    val balance = item.balance
    val extended = MaterialTheme.colorScheme.extended
    val (text, color) =
        when (balance) {
            is GroupBalance.Settled -> {
                stringResource(Res.string.groups_status_settled) to extended.settled
            }

            is GroupBalance.Owed -> {
                "+${formatAmount(item, balance.amount)}" to extended.positive
            }

            is GroupBalance.Owe -> {
                "−${formatAmount(item, balance.amount)}" to extended.negative
            }
        }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.groups_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.groups_empty_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.groups_detail_placeholder_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.groups_detail_placeholder_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewThemes
@Composable
private fun GroupOverviewPreviewThemes() {
    TabMatesTheme {
        Surface {
            GroupOverviewScreen(
                state = previewState(),
                onFilterSelected = {},
                onGroupSelected = {},
                onGroupOpen = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun GroupOverviewPreviewSizes() {
    TabMatesTheme {
        Surface {
            GroupOverviewScreen(
                state = previewState(),
                onFilterSelected = {},
                onGroupSelected = {},
                onGroupOpen = {},
            )
        }
    }
}

private fun previewState(): GroupOverviewState {
    val now = Clock.System.now()
    val items =
        listOf(
            GroupOverviewItem(
                id = "1",
                title = "Weekend in Lisbon",
                iconKey = "airplane",
                colorKey = "coral",
                memberCount = 4,
                expenseCount = 12,
                balance = GroupBalance.Owed(23.50),
                totalSpent = 521.20,
                currencyCode = "EUR",
                currencySymbol = "€",
                currencyDecimalDigits = 2,
                lastActivityAt = now,
            ),
            GroupOverviewItem(
                id = "2",
                title = "The Flat",
                iconKey = "house",
                colorKey = "mint",
                memberCount = 3,
                expenseCount = 8,
                balance = GroupBalance.Owe(12.0),
                totalSpent = 0.0,
                currencyCode = "EUR",
                currencySymbol = "€",
                currencyDecimalDigits = 2,
                lastActivityAt = now,
            ),
            GroupOverviewItem(
                id = "3",
                title = "Office Lunch Club",
                iconKey = "restaurant",
                colorKey = "blush",
                memberCount = 8,
                expenseCount = 0,
                balance = GroupBalance.Settled,
                totalSpent = 0.0,
                currencyCode = "EUR",
                currencySymbol = "€",
                currencyDecimalDigits = 2,
                lastActivityAt = now,
            ),
            GroupOverviewItem(
                id = "4",
                title = "Bike Trip · Douro",
                iconKey = "bike",
                colorKey = "pink",
                memberCount = 6,
                expenseCount = 4,
                balance = GroupBalance.Owed(8.0),
                totalSpent = 0.0,
                currencyCode = "EUR",
                currencySymbol = "€",
                currencyDecimalDigits = 2,
                lastActivityAt = now,
            ),
            GroupOverviewItem(
                id = "5",
                title = "Mom's 60th",
                iconKey = "cake",
                colorKey = "tan",
                memberCount = 5,
                expenseCount = 5,
                balance = GroupBalance.Owe(42.0),
                totalSpent = 0.0,
                currencyCode = "EUR",
                currencySymbol = "€",
                currencyDecimalDigits = 2,
                lastActivityAt = now,
            ),
        )
    return GroupOverviewState(
        isLoading = false,
        allItems = items,
        displayedItems = items,
        filter = GroupFilter.ALL,
        searchQueryState = TextFieldState(),
        selectedGroupId = "1",
        currentUserInitials = "SR",
    )
}
