package de.tabmates.features.tabgroup.presentation.navigation.groupdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupDetailPane
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_placeholder_title

@Composable
fun GroupDetailRoot(
    groupId: String,
    snackbarHostState: SnackbarHostState,
    onSettingsClick: () -> Unit = {},
    onAddExpenseClick: () -> Unit = {},
    onSettleUpClick: () -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
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
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.groups_detail_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        GroupDetailPane(
            item = item,
            currentUserId = state.currentUserId,
            members = state.members,
            expenses = state.expenses,
            perPersonBalances = state.perPersonBalances,
            currencyByCode = state.currencyByCode,
            ratesByCurrency = state.ratesByCurrency,
            onRotateInvite = viewModel::rotateInvite,
            onSettingsClick = onSettingsClick,
            onAddExpenseClick = onAddExpenseClick,
            onSettleUpClick = onSettleUpClick,
            onExpenseClick = onExpenseClick,
            snackbarHostState = snackbarHostState,
            modifier = modifier.fillMaxSize(),
        )
    }
}
