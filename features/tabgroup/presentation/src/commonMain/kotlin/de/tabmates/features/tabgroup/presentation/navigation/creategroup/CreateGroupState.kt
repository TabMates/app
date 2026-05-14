package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.text.input.TextFieldState
import de.tabmates.features.tabgroup.domain.models.Currency

data class CreateGroupState(
    val nameTextState: TextFieldState = TextFieldState(),
    val descriptionTextState: TextFieldState = TextFieldState(),
    val currencyQueryState: TextFieldState = TextFieldState(),
    val newPlaceholderTextState: TextFieldState = TextFieldState(),
    val defaultCurrencyCode: String = "",
    val supportedCurrencies: List<Currency> = emptyList(),
    val recentCurrencyCodes: List<String> = emptyList(),
    val isCurrencyPickerVisible: Boolean = false,
    val isPlaceholderDialogVisible: Boolean = false,
    val iconKey: String = IconCatalog.defaultIconKey,
    val colorKey: String = IconCatalog.defaultColorKey,
    val iconPicker: IconPickerUiState = IconPickerUiState(),
    val placeholders: List<CreateGroupPlaceholder> = emptyList(),
    val isSubmitting: Boolean = false,
)

data class CreateGroupPlaceholder(
    val id: String,
    val name: String,
) {
    val initial: String get() = name.firstOrNull()?.uppercase().orEmpty()
}

data class CurrencyPickerUiState(
    val recents: List<Currency> = emptyList(),
    val results: List<Currency> = emptyList(),
    val showSections: Boolean = true,
    val selectedCode: String = "",
)

data class IconPickerUiState(
    val isVisible: Boolean = false,
    val draftIconKey: String = IconCatalog.defaultIconKey,
    val draftColorKey: String = IconCatalog.defaultColorKey,
    val selectedCategory: IconCategory? = null,
)
