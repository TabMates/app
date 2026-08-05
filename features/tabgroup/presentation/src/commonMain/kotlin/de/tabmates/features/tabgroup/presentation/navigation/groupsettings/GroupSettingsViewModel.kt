package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.GroupValidationError
import de.tabmates.features.tabgroup.domain.group.GroupValidator
import de.tabmates.features.tabgroup.presentation.components.deriveGroupAvatarKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_currency_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_too_long

@KoinViewModel
class GroupSettingsViewModel(
    @InjectedParam private val groupId: String,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupSettingsState())
    val state: StateFlow<GroupSettingsState> = _state.asStateFlow()

    private val eventChannel = Channel<GroupSettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadGroup()
        observePeopleCount()
    }

    /**
     * Observed rather than folded into [loadGroup]'s one-shot read: this screen stays on the back
     * stack while People is open, so a placeholder added there has to reach the row on the way back.
     * The text fields deliberately stay out of this — re-hydrating them would fight the user typing.
     */
    private fun observePeopleCount() {
        viewModelScope.launch {
            groupRepository
                .getGroups()
                .map { groups -> groups.firstOrNull { it.id == groupId }?.participants?.size ?: 0 }
                .distinctUntilChanged()
                .collect { count -> _state.update { it.copy(peopleCount = count) } }
        }
    }

    private fun loadGroup() {
        viewModelScope.launch {
            val group = groupRepository.getGroups().first().firstOrNull { it.id == groupId }
            if (group == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val (iconKey, colorKey) = deriveGroupAvatarKey(group.id)
            _state.update {
                it.copy(
                    isLoading = false,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    nameTextState = TextFieldState(group.title),
                    descriptionTextState = TextFieldState(group.description.orEmpty()),
                    defaultCurrencyCode = group.defaultCurrencyCode,
                )
            }
        }
    }

    fun onAction(action: GroupSettingsAction) {
        when (action) {
            GroupSettingsAction.Save -> {
                save()
            }

            GroupSettingsAction.RequestLeave -> {
                _state.update { it.copy(showLeaveDialog = true) }
            }

            GroupSettingsAction.DismissLeaveDialog -> {
                _state.update { it.copy(showLeaveDialog = false) }
            }

            GroupSettingsAction.ConfirmLeave -> {
                leave()
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        val title =
            current.nameTextState.text
                .toString()
                .trim()
        val description = current.descriptionTextState.text.toString()
        val error = GroupValidator.validate(title, description, current.defaultCurrencyCode)
        if (error != null) {
            viewModelScope.launch { eventChannel.send(GroupSettingsEvent.Error(error.toUiText())) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            groupRepository
                .updateGroup(
                    groupId = groupId,
                    title = title,
                    description = description.ifBlank { null },
                    defaultCurrencyCode = current.defaultCurrencyCode,
                ).onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(GroupSettingsEvent.Saved)
                }.onFailure { err ->
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(GroupSettingsEvent.Error(err.toUiText()))
                }
        }
    }

    private fun leave() {
        val current = _state.value
        if (current.isLeaving) return
        _state.update { it.copy(isLeaving = true, showLeaveDialog = false) }
        viewModelScope.launch {
            groupRepository
                .leaveGroup(groupId)
                .onSuccess { eventChannel.send(GroupSettingsEvent.Left) }
                .onFailure { err ->
                    _state.update { it.copy(isLeaving = false) }
                    eventChannel.send(GroupSettingsEvent.Error(err.toUiText()))
                }
        }
    }

    private fun GroupValidationError.toUiText(): UiText =
        when (this) {
            GroupValidationError.TitleRequired -> {
                UiText.Resource(Res.string.create_group_error_title_required)
            }

            GroupValidationError.TitleTooLong -> {
                UiText.Resource(Res.string.create_group_error_title_too_long)
            }

            GroupValidationError.DescriptionTooLong -> {
                UiText.Resource(Res.string.create_group_error_description_too_long)
            }

            GroupValidationError.CurrencyRequired -> {
                UiText.Resource(Res.string.create_group_error_currency_required)
            }
        }
}
