package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tabmates.features.tabgroup.domain.models.Currency
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_all_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_clear_search_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_picker_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_recent_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_search_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_selected_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_check_circle
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    queryState: TextFieldState,
    state: CurrencyPickerUiState,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .imePadding(),
        ) {
            PickerHeader()
            SearchField(state = queryState)
            CurrencyList(
                state = state,
                onCurrencySelected = onCurrencySelected,
            )
        }
    }
}

@Composable
private fun CurrencyList(
    state: CurrencyPickerUiState,
    onCurrencySelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (state.showSections) {
            if (state.recents.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(Res.string.create_group_currency_recent_section))
                }
                items(items = state.recents, key = { "recent-${it.code}" }) { currency ->
                    CurrencyRow(
                        currency = currency,
                        isSelected = currency.code == state.selectedCode,
                        onClick = { onCurrencySelected(currency.code) },
                    )
                }
            }
            if (state.results.isNotEmpty()) {
                item {
                    SectionLabel(text = stringResource(Res.string.create_group_currency_all_section))
                }
            }
            items(items = state.results, key = { "all-${it.code}" }) { currency ->
                CurrencyRow(
                    currency = currency,
                    isSelected = currency.code == state.selectedCode,
                    onClick = { onCurrencySelected(currency.code) },
                )
            }
        } else {
            items(items = state.results, key = { it.code }) { currency ->
                CurrencyRow(
                    currency = currency,
                    isSelected = currency.code == state.selectedCode,
                    onClick = { onCurrencySelected(currency.code) },
                )
            }
        }
    }
}

@Composable
private fun PickerHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.create_group_currency_picker_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchField(state: TextFieldState) {
    OutlinedTextField(
        state = state,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = stringResource(Res.string.create_group_currency_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_search),
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (state.text.isNotEmpty()) {
                IconButton(onClick = { state.edit { replace(0, length, "") } }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.create_group_currency_clear_search_cd),
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        }
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = currency.code,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = { CurrencyAvatar(currency = currency) },
        trailingContent =
            if (isSelected) {
                {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_check_circle),
                        contentDescription = stringResource(Res.string.create_group_currency_selected_cd),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
        colors = ListItemDefaults.colors(containerColor = background),
    )
}

@Composable
private fun CurrencyAvatar(currency: Currency) {
    val (background, content) = avatarColors(currency.code)
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = currency.nativeSymbol.ifBlank { currency.code.take(1) },
            style = MaterialTheme.typography.titleMedium,
            color = content,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun avatarColors(code: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.surfaceVariant to scheme.onSurfaceVariant,
        )
    val index = (code.hashCode().rem(palette.size) + palette.size) % palette.size
    return palette[index]
}

internal fun filterCurrencies(
    currencies: List<Currency>,
    query: String,
): List<Currency> {
    if (query.isBlank()) return currencies
    val trimmed = query.trim()
    return currencies.filter { c ->
        c.code.contains(trimmed, ignoreCase = true) ||
            c.name.contains(trimmed, ignoreCase = true)
    }
}
