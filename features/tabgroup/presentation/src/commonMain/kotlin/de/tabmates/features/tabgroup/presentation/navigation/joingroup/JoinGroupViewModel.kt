package de.tabmates.features.tabgroup.presentation.navigation.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class JoinGroupViewModel(
    @InjectedParam private val token: String,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<JoinGroupState>(JoinGroupState.Loading)
    val state: StateFlow<JoinGroupState> = _state.asStateFlow()

    private val eventChannel = Channel<JoinGroupEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadPreview()
    }

    private fun loadPreview() {
        viewModelScope.launch {
            groupRepository
                .previewInvite(token)
                .onSuccess { preview ->
                    _state.value = JoinGroupState.Ready(
                        preview = preview,
                        token = token,
                        selectedOption = JoinOption.NewMember,
                    )
                }.onFailure { error ->
                    if (error == DataError.Remote.NOT_FOUND) {
                        _state.value = JoinGroupState.Expired
                    } else {
                        _state.value = JoinGroupState.Ready(
                            preview = null,
                            token = token,
                            selectedOption = JoinOption.NewMember,
                        )
                    }
                }
        }
    }

    fun onAction(action: JoinGroupAction) {
        when (action) {
            is JoinGroupAction.SelectOption -> selectOption(action.option)
            JoinGroupAction.Submit -> submit()
        }
    }

    private fun selectOption(option: JoinOption) {
        _state.update { current ->
            if (current is JoinGroupState.Ready) current.copy(selectedOption = option) else current
        }
    }

    private fun submit() {
        val current = _state.value as? JoinGroupState.Ready ?: return
        if (current.isJoining) return
        _state.value = current.copy(isJoining = true)
        viewModelScope.launch {
            val claimPlaceholderId =
                (current.selectedOption as? JoinOption.ClaimPlaceholder)?.placeholderId
            groupRepository
                .joinGroup(token, claimPlaceholderId)
                .onSuccess { group ->
                    eventChannel.send(JoinGroupEvent.Joined(group.id))
                }.onFailure { error ->
                    _state.value = current.copy(isJoining = false)
                    when (error) {
                        DataError.Remote.NOT_FOUND, DataError.Remote.BAD_REQUEST -> {
                            _state.value = JoinGroupState.Expired
                        }
                        else -> eventChannel.send(JoinGroupEvent.Error(error.toUiText()))
                    }
                }
        }
    }
}
