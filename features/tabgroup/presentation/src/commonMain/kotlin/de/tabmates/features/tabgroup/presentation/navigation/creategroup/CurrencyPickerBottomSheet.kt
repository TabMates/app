package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tabmates.features.tabgroup.domain.currency.Currency
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_search_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_currency_selected_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    queryState: TextFieldState,
    currencies: List<Currency>,
    selectedCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        CurrencyPickerContent(
            queryState = queryState,
            currencies = currencies,
            selectedCode = selectedCode,
            onCurrencySelected = onCurrencySelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerContent(
    queryState: TextFieldState,
    currencies: List<Currency>,
    selectedCode: String,
    onCurrencySelected: (String) -> Unit,
) {
    val filtered by remember(queryState, currencies) {
        derivedStateOf { filterCurrencies(currencies, queryState.text.toString()) }
    }

    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                state = queryState,
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                placeholder = {
                    Text(stringResource(Res.string.create_group_currency_search_placeholder))
                },
            )
        },
        expanded = true,
        onExpandedChange = {},
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(
                items = filtered,
                key = { currency -> currency.code },
            ) { currency ->
                CurrencyRow(
                    currency = currency,
                    isSelected = currency.code == selectedCode,
                    onClick = { onCurrencySelected(currency.code) },
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.size(width = 56.dp, height = 24.dp),
        )
        Text(
            text = currency.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.create_group_currency_selected_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
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
