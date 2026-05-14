package de.tabmates.core.presentation.navigation

/**
 * NavKey marker for screens that should render a floating action button. The host scaffold
 * reads [fabAction] to decide what the FAB does on click.
 */
interface ScreenWithFab {
    val fabAction: FabAction
}

sealed interface FabAction {
    data object CreateGroup : FabAction

    data class AddExpense(val groupId: String) : FabAction
}
