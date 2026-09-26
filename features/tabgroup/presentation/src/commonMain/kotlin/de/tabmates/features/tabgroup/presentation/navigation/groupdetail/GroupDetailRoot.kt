package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupDetailPane
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_back_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_unavailable_action
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
    onRecurringSeriesClick: (String) -> Unit,
    onManageSchedulesClick: () -> Unit,
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
        // Keep a back affordance while the group is still loading — and, once it has, while saying
        // it is not there. A push deep link for a group you were removed from lands here, as does
        // one for a group that was deleted.
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
                if (state.hasLoaded) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.groups_detail_unavailable),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        VerticalSpacer(16.dp)
                        Button(onClick = onBack) {
                            Text(stringResource(Res.string.groups_detail_unavailable_action))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.groups_detail_placeholder_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        GroupDetailPane(
            item = item,
            currentUserId = state.currentUserId,
            members = state.members,
            formerMemberIds = state.formerMemberIds,
            participantsById = state.participantsById,
            entries = state.entries,
            memberNetBalances = state.memberNetBalances,
            hasOutstandingDebts = state.hasOutstandingDebts,
            currencyByCode = state.currencyByCode,
            ratesByCurrency = state.ratesByCurrency,
            historySections = state.historySections,
            canLoadMoreHistory = state.canLoadMoreHistory,
            onLoadMoreHistory = viewModel::loadMoreHistory,
            onBack = onBack,
            onSettingsClick = onSettingsClick,
            onAddEntryClick = onAddEntryClick,
            onSettleUpClick = onSettleUpClick,
            onEntryClick = onEntryClick,
            onSettlementClick = onSettlementClick,
            recurringSeries = state.recurringSeries,
            onRecurringSeriesClick = onRecurringSeriesClick,
            onManageSchedulesClick = onManageSchedulesClick,
            snackbarHostState = snackbarHostState,
            modifier = modifier.fillMaxSize(),
        )
    }
}
