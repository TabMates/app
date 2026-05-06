package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.share.LinkShareResult
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_currency_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_too_long
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class CreateGroupViewModel(
    private val groupService: GroupService,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateGroupState())
    private var hasLoadedInitialData = false
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    hasLoadedInitialData = true
                    loadSupportedCurrencies()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private fun loadSupportedCurrencies() {
        currencyRepository
            .getCurrencies()
            .onEach { currencies ->
                _state.update { it.copy(supportedCurrencies = currencies) }
            }.launchIn(viewModelScope)
    }

    private val eventChannel = Channel<CreateGroupEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onPickIconClick() {
        // TODO: open icon picker
    }

    fun onCurrencyClick() {
        _state.update { it.copy(isCurrencyPickerVisible = true) }
    }

    fun onCurrencyPickerDismiss() {
        _state.update { it.copy(isCurrencyPickerVisible = false) }
    }

    fun onCurrencySelected(code: String) {
        _state.update { it.copy(defaultCurrencyCode = code, isCurrencyPickerVisible = false) }
    }

    fun onInviteByEmail() {
        // TODO: send invite via repository once available
    }

    fun onLinkShared(result: LinkShareResult) {
        viewModelScope.launch {
            val event =
                when (result) {
                    LinkShareResult.Copied -> CreateGroupEvent.LinkCopied
                    LinkShareResult.Shared -> CreateGroupEvent.LinkShared
                }
            eventChannel.send(event)
        }
    }

    fun onToggleMember(id: String) {
        _state.update { current ->
            current.copy(
                members =
                    current.members.map { member ->
                        if (member.id == id) member.copy(isSelected = !member.isSelected) else member
                    },
            )
        }
    }

    fun onCreateClick() {
        if (state.value.isSubmitting) return
        val current = _state.value
        val validationError = validate(current)
        if (validationError != null) {
            viewModelScope.launch { eventChannel.send(CreateGroupEvent.Error(validationError)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            groupService
                .createGroup(
                    title =
                        current.nameTextState.text
                            .toString()
                            .trim(),
                    description =
                        current.descriptionTextState.text
                            .toString()
                            .ifBlank { null },
                    defaultCurrencyCode = current.defaultCurrencyCode,
                    otherUserIds =
                        current.members
                            .filter { it.isSelected }
                            .map { it.id }
                            .toSet(),
                ).onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(CreateGroupEvent.GroupCreated)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(CreateGroupEvent.Error(error.toUiText()))
                }
        }
    }

    private fun validate(state: CreateGroupState): UiText? {
        val title =
            state.nameTextState.text
                .toString()
                .trim()
        return when {
            title.isEmpty() -> {
                UiText.Resource(Res.string.create_group_error_title_required)
            }

            title.length > MAX_TITLE_LENGTH -> {
                UiText.Resource(Res.string.create_group_error_title_too_long)
            }

            state.descriptionTextState.text
                .toString()
                .length > MAX_DESCRIPTION_LENGTH -> {
                UiText.Resource(Res.string.create_group_error_description_too_long)
            }

            state.defaultCurrencyCode.isBlank() -> {
                UiText.Resource(Res.string.create_group_error_currency_required)
            }

            else -> {
                null
            }
        }
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 1000
    }
}
