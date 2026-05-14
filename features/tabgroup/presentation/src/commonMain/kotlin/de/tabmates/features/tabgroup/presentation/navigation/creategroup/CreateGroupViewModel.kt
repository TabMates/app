package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tabmates.core.domain.util.onFailure
import de.tabmates.core.domain.util.onSuccess
import de.tabmates.core.presentation.util.UiText
import de.tabmates.core.presentation.util.toUiText
import de.tabmates.features.tabgroup.domain.currency.CurrencyRepository
import de.tabmates.features.tabgroup.domain.group.GroupService
import de.tabmates.features.tabgroup.domain.models.Currency
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
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
    private var placeholderCounter = 0
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
        viewModelScope.launch {
            currencyRepository
                .fetchCurrencies()
                .onFailure { error ->
                    eventChannel.send(CreateGroupEvent.Error(error.toUiText()))
                }
        }
    }

    private val eventChannel = Channel<CreateGroupEvent>()
    val events = eventChannel.receiveAsFlow()

    private val currencyQueryFlow =
        snapshotFlow {
            _state.value.currencyQueryState.text
                .toString()
        }

    val currencyPickerState: StateFlow<CurrencyPickerUiState> =
        combine(_state, currencyQueryFlow) { state, query ->
            buildCurrencyPickerState(
                currencies = state.supportedCurrencies,
                recentCodes = state.recentCurrencyCodes,
                selectedCode = state.defaultCurrencyCode,
                query = query,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = CurrencyPickerUiState(),
        )

    private fun buildCurrencyPickerState(
        currencies: List<Currency>,
        recentCodes: List<String>,
        selectedCode: String,
        query: String,
    ): CurrencyPickerUiState {
        val recents = recentCodes.mapNotNull { code -> currencies.firstOrNull { it.code == code } }
        val showSections = query.isBlank()
        val filteredRecents = if (showSections) recents else filterCurrencies(recents, query)
        val results =
            if (showSections) {
                currencies.filter { it.code !in recentCodes }
            } else {
                filterCurrencies(currencies, query)
            }
        return CurrencyPickerUiState(
            recents = filteredRecents,
            results = results,
            showSections = showSections,
            selectedCode = selectedCode,
        )
    }

    fun onPickIconClick() {
        _state.update { current ->
            current.copy(
                iconPicker =
                    IconPickerUiState(
                        isVisible = true,
                        draftIconKey = current.iconKey,
                        draftColorKey = current.colorKey,
                        selectedCategory = null,
                    ),
            )
        }
    }

    fun onIconPickerDismiss() {
        _state.update { it.copy(iconPicker = IconPickerUiState()) }
    }

    fun onIconPickerSave() {
        _state.update { current ->
            current.copy(
                iconKey = current.iconPicker.draftIconKey,
                colorKey = current.iconPicker.draftColorKey,
                iconPicker = IconPickerUiState(),
            )
        }
    }

    fun onIconDraftSelected(key: String) {
        _state.update { it.copy(iconPicker = it.iconPicker.copy(draftIconKey = key)) }
    }

    fun onColorDraftSelected(key: String) {
        _state.update { it.copy(iconPicker = it.iconPicker.copy(draftColorKey = key)) }
    }

    fun onIconCategorySelected(category: IconCategory) {
        _state.update { current ->
            val newCategory = if (current.iconPicker.selectedCategory == category) null else category
            current.copy(iconPicker = current.iconPicker.copy(selectedCategory = newCategory))
        }
    }

    fun onCurrencyClick() {
        _state.update { it.copy(isCurrencyPickerVisible = true) }
    }

    fun onCurrencyPickerDismiss() {
        _state.update { it.copy(isCurrencyPickerVisible = false) }
    }

    fun onCurrencySelected(code: String) {
        _state.update { current ->
            val newRecents =
                (listOf(code) + current.recentCurrencyCodes)
                    .distinct()
                    .take(MAX_RECENT_CURRENCIES)
            current.copy(
                defaultCurrencyCode = code,
                recentCurrencyCodes = newRecents,
                isCurrencyPickerVisible = false,
            )
        }
    }

    fun onAddPlaceholderClick() {
        _state.update { it.copy(isPlaceholderDialogVisible = true) }
    }

    fun onPlaceholderDialogDismiss() {
        _state.value.newPlaceholderTextState.clearText()
        _state.update { it.copy(isPlaceholderDialogVisible = false) }
    }

    fun onPlaceholderDialogConfirm() {
        val name =
            _state.value.newPlaceholderTextState.text
                .toString()
                .trim()
        if (name.isEmpty()) return
        val id = "placeholder-${placeholderCounter++}"
        _state.update {
            it.copy(
                placeholders = it.placeholders + CreateGroupPlaceholder(id = id, name = name),
                isPlaceholderDialogVisible = false,
            )
        }
        _state.value.newPlaceholderTextState.clearText()
    }

    fun onRemovePlaceholder(id: String) {
        _state.update { current ->
            current.copy(placeholders = current.placeholders.filterNot { it.id == id })
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
                    otherUserIds = emptySet(),
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

    private fun TextFieldState.clearText() {
        edit { replace(0, length, "") }
    }

    private companion object {
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 1000
        private const val MAX_RECENT_CURRENCIES = 5
    }
}
