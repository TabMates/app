package de.tabmates.features.tabgroup.presentation.navigation.group

data class GroupState(
    val isLoading: Boolean = true,
    val items: List<GroupListItem> = emptyList(),
) {
    val isEmpty: Boolean = !isLoading && items.isEmpty()
}
