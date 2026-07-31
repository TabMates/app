package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.sync.PendingWrites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePendingWrites(
    initialCount: Int = 0,
) : PendingWrites {
    private val count = MutableStateFlow(initialCount)

    override fun observeCount(): Flow<Int> = count

    fun emit(value: Int) {
        count.value = value
    }
}
