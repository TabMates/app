package de.tabmates.features.tabgroup.domain.balance

import de.tabmates.features.tabgroup.domain.currency.CurrencyConversion
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.domain.models.TabEntry
import de.tabmates.features.tabgroup.domain.models.TabEntrySplit
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class UserBalanceCalculatorTest {
    @Test
    fun sumsAmountsDirectlyWhenNoConversion() {
        // a pays 100, split equally with b -> b owes a 50.
        val entry = equalExpense(paidBy = "a", amount = 100.0, members = listOf("a", "b"), currency = "EUR")
        assertEquals(50.0, UserBalanceCalculator.computeNet(listOf(entry), "a"), absoluteTolerance = 1e-9)
        assertEquals(-50.0, UserBalanceCalculator.computeNet(listOf(entry), "b"), absoluteTolerance = 1e-9)
    }

    @Test
    fun convertsForeignCurrencyEntryIntoBase() {
        // a pays 100 USD, split equally with b. Group base EUR, 1 USD = 0.92 EUR.
        val entry = equalExpense(paidBy = "a", amount = 100.0, members = listOf("a", "b"), currency = "USD")
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = mapOf("USD" to 1.0, "EUR" to 0.92))
        // b owes 50 USD = 46 EUR.
        assertEquals(
            -46.0,
            UserBalanceCalculator.computeNet(listOf(entry), "b", conversion),
            absoluteTolerance = 1e-9,
        )
        assertEquals(
            46.0,
            UserBalanceCalculator.computeNet(listOf(entry), "a", conversion),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun skipsEntriesWithUnknownRate() {
        val entry = equalExpense(paidBy = "a", amount = 100.0, members = listOf("a", "b"), currency = "JPY")
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = mapOf("USD" to 1.0, "EUR" to 0.92))
        assertEquals(
            0.0,
            UserBalanceCalculator.computeNet(listOf(entry), "b", conversion),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun mixesBaseAndForeignEntries() {
        val eur =
            equalExpense(paidBy = "a", amount = 100.0, members = listOf("a", "b"), currency = "EUR", id = "e1")
        val usd =
            equalExpense(paidBy = "b", amount = 100.0, members = listOf("a", "b"), currency = "USD", id = "e2")
        val conversion = CurrencyConversion(baseCurrency = "EUR", rates = mapOf("USD" to 1.0, "EUR" to 0.92))
        // a is owed 50 EUR from e1, owes 46 EUR from e2 -> net +4 EUR.
        assertEquals(
            4.0,
            UserBalanceCalculator.computeNet(listOf(eur, usd), "a", conversion),
            absoluteTolerance = 1e-9,
        )
    }

    private fun equalExpense(
        paidBy: String,
        amount: Double,
        members: List<String>,
        currency: String,
        id: String = "e1",
    ): TabEntry.Expense {
        val share = amount / members.size
        val timestamp = Instant.fromEpochMilliseconds(0)
        return TabEntry.Expense(
            tabEntryId = id,
            groupId = "g",
            title = "",
            description = "",
            amount = amount,
            currencyCode = currency,
            creatorId = paidBy,
            paidByUserId = paidBy,
            entryDate = LocalDate.parse("1970-01-01"),
            createdAt = timestamp,
            lastModifiedAt = timestamp,
            lastModifiedByUserId = paidBy,
            version = 0,
            deletedAt = null,
            deletedByUserId = null,
            splits =
                members.mapIndexed { index, memberId ->
                    TabEntrySplit(
                        splitId = "$id-s$index",
                        tabEntryId = id,
                        participantId = memberId,
                        splitType = SplitType.EQUAL,
                        value = share,
                        resolvedAmount = share,
                    )
                },
        )
    }
}
