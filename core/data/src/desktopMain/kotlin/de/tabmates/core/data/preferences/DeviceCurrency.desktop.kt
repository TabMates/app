package de.tabmates.core.data.preferences

import java.util.Currency
import java.util.Locale

actual fun deviceCurrencyCode(): String? =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
