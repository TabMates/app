package de.tabmates.features.tabgroup.domain.currency

import de.tabmates.features.tabgroup.domain.models.ExchangeRate
import de.tabmates.features.tabgroup.domain.models.TabEntry

/**
 * Converts amounts between currencies using exchange rates.
 *
 * Rates come from the upstream provider with a USD base, so a rate value is "units of that currency
 * per 1 USD" (e.g. EUR = 0.92 means 1 USD = 0.92 EUR). Converting between any two currencies is
 * therefore `amount * rate[to] / rate[from]`, which is base-agnostic as long as both rates exist.
 */
object CurrencyConverter {
    /**
     * Returns [amount] expressed in [to], or `null` when conversion isn't possible (a rate for
     * [from] or [to] is missing or zero). Callers decide how to treat an unconvertible amount —
     * here that means it contributes nothing to a converted total until rates sync.
     */
    fun convert(
        amount: Double,
        from: String,
        to: String,
        rates: Map<String, Double>,
    ): Double? {
        if (from == to) return amount
        val rateFrom = rates[from] ?: return null
        val rateTo = rates[to] ?: return null
        if (rateFrom == 0.0) return null
        return amount * rateTo / rateFrom
    }
}

/**
 * A fixed view of "convert anything into [baseCurrency]" for one computation pass (e.g. one balance
 * recompute). Built from the exchange rates available at that moment, so a later rate change simply
 * produces a new [CurrencyConversion] and a new result.
 */
class CurrencyConversion(
    val baseCurrency: String,
    private val rates: Map<String, Double>,
) {
    /** Multiplier that turns 1 unit of [currencyCode] into [baseCurrency], or `null` if unknown. */
    fun factorToBase(currencyCode: String): Double? =
        CurrencyConverter.convert(amount = 1.0, from = currencyCode, to = baseCurrency, rates = rates)

    companion object {
        fun from(
            baseCurrency: String,
            rates: List<ExchangeRate>,
        ): CurrencyConversion =
            CurrencyConversion(
                baseCurrency = baseCurrency,
                rates = rates.associate { it.currencyCode to it.rateToBase },
            )
    }
}

/**
 * Multiplier that converts 1 unit of [entry]'s currency into this conversion's base currency, or
 * `1.0` when `this` is null (single-currency mode). Prefers the rate locked onto the entry when
 * it was created ([TabEntry.exchangeRate]) over the live rate table, so an entry's contribution
 * to a balance doesn't drift as rates change after the fact — and a settled amount stays settled.
 * Falls back to the live [CurrencyConversion.factorToBase] for entries without a snapshot;
 * returns `null` only when that fallback is needed and the entry's currency has no known rate
 * (callers skip the entry until rates sync).
 */
fun CurrencyConversion?.factorFor(entry: TabEntry): Double? =
    when {
        this == null -> 1.0
        entry.exchangeRate != null -> entry.exchangeRate
        else -> factorToBase(entry.currencyCode)
    }
