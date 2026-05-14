package de.tabmates.features.tabgroup.domain.models

import kotlin.math.abs

sealed class GroupBalance {
    data object Settled : GroupBalance()

    data class Owed(val amount: Double) : GroupBalance()

    data class Owe(val amount: Double) : GroupBalance()

    companion object {
        private const val EPSILON = 0.005

        fun fromNet(net: Double): GroupBalance =
            when {
                abs(net) < EPSILON -> Settled
                net > 0 -> Owed(net)
                else -> Owe(-net)
            }
    }
}
