package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupDetailPane
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_back_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_arrow_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailRoot(
    groupId: String,
    snackbarHostState: SnackbarHostState,
    // No defaults on purpose: a silently defaulted callback is what left settle-up, settlement
    // taps and leave-group dead in the tablet two-pane layout. Every call site states its intent.
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    onSettlementClick: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    if (item == null) {
        // Keep a back affordance while the group is still loading.
        Column(modifier = modifier.fillMaxSize()) {
            TopAppBar(
                title = {},
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
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.groups_detail_placeholder_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        GroupDetailPane(
            item = item,
            currentUserId = state.currentUserId,
            members = state.members,
            entries = state.entries,
            perPersonBalances = state.perPersonBalances,
            memberNetBalances = state.memberNetBalances,
            hasOutstandingDebts = state.hasOutstandingDebts,
            currencyByCode = state.currencyByCode,
            ratesByCurrency = state.ratesByCurrency,
            historySections = state.historySections,
            canLoadMoreHistory = state.canLoadMoreHistory,
            onLoadMoreHistory = viewModel::loadMoreHistory,
            onRotateInvite = viewModel::rotateInvite,
            onBack = onBack,
            onSettingsClick = onSettingsClick,
            onAddEntryClick = onAddEntryClick,
            onSettleUpClick = onSettleUpClick,
            onEntryClick = onEntryClick,
            onSettlementClick = onSettlementClick,
            onLeaveGroup = onLeaveGroup,
            snackbarHostState = snackbarHostState,
            modifier = modifier.fillMaxSize(),
        )
    }
}
