package de.tabmates.features.tabgroup.presentation.navigation.groupsettings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.group.GroupRepository
import de.tabmates.features.tabgroup.domain.group.GroupValidationError
import de.tabmates.features.tabgroup.domain.group.GroupValidator
import de.tabmates.features.tabgroup.domain.models.Group
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import de.tabmates.features.tabgroup.presentation.components.deriveGroupAvatarKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
                    placeholders = group.toPlaceholders(),
                )
            }
        }
    }

    private fun Group.toPlaceholders(): List<GroupSettingsPlaceholder> =
        participants
            .filter { it.participantType == ParticipantType.PLACEHOLDER }
            .map { GroupSettingsPlaceholder(id = it.userId, name = it.username) }

    fun onAction(action: GroupSettingsAction) {
        when (action) {
            GroupSettingsAction.Save -> {
                save()
            }

            GroupSettingsAction.AddPlaceholderClick -> {
                _state.value.newPlaceholderTextState.clearText()
                _state.update { it.copy(isPlaceholderDialogVisible = true) }
            }

            GroupSettingsAction.PlaceholderDialogDismiss -> {
                _state.update { it.copy(isPlaceholderDialogVisible = false) }
            }

            GroupSettingsAction.PlaceholderDialogConfirm -> {
                addPlaceholder()
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

    private fun addPlaceholder() {
        val current = _state.value
        if (current.isAddingPlaceholder) return
        val name =
            current.newPlaceholderTextState.text
                .toString()
                .trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isAddingPlaceholder = true) }
            groupRepository
                .addNewParticipantsToGroup(groupId, listOf(name))
                .onSuccess { group ->
                    _state.value.newPlaceholderTextState.clearText()
                    _state.update {
                        it.copy(
                            isAddingPlaceholder = false,
                            isPlaceholderDialogVisible = false,
                            placeholders = group.toPlaceholders(),
                        )
                    }
                }.onFailure { err ->
                    _state.update { it.copy(isAddingPlaceholder = false) }
                    eventChannel.send(GroupSettingsEvent.Error(err.toUiText()))
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
