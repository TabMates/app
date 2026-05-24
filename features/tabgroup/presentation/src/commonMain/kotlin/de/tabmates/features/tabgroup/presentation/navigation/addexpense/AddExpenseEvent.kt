package de.tabmates.features.tabgroup.presentation.navigation.addexpense

import de.tabmates.core.presentation.util.UiText

sealed interface AddExpenseEvent {
    data object ExpenseCreated : AddExpenseEvent

    data class Error(val message: UiText) : AddExpenseEvent
}
