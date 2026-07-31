package de.tabmates.features.tabgroup.presentation.testing

import de.tabmates.core.domain.auth.CurrentAccount

/** Defaults to the same account [FakeSessionStorage] signs in, so balances resolve the same way. */
class FakeCurrentAccount(
    var id: String? = FakeSessionStorage.DEFAULT_USER.id,
) : CurrentAccount {
    override fun userId(): String? = id
}
