package de.tabmates.features.tabgroup.presentation.navigation.expensedetail

import de.tabmates.core.presentation.util.UiText

sealed interface ExpenseDetailEvent {
    data object ExpenseDeleted : ExpenseDetailEvent

    data class Error(val message: UiText) : ExpenseDetailEvent
}
