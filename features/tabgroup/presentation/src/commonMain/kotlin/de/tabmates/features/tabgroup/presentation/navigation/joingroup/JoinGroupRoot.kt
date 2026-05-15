package de.tabmates.features.tabgroup.presentation.navigation.joingroup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.presentation.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinGroupRoot(
    token: String,
    onClose: () -> Unit,
    onJoined: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinGroupViewModel =
        koinViewModel(
            key = token,
            parameters = { parametersOf(token) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is JoinGroupEvent.Joined -> onJoined(event.groupId)
            is JoinGroupEvent.Error -> Unit
        }
    }
    JoinGroupScreen(
        state = state,
        onAction = viewModel::onAction,
        onCloseClick = onClose,
        modifier = modifier,
    )
}
