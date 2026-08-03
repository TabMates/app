package de.tabmates.features.authentication.presentation.emailverification

data class EmailVerificationState(
    val origin: VerificationOrigin = VerificationOrigin.NoSession,
    val status: VerificationStatus = VerificationStatus.Verifying,
)

/**
 * Which flow the redeemed link belongs to, read from the cached session before the token is spent.
 *
 * The server tells us nothing about it — verifying returns no body — so the account on the device is
 * the only signal, and it is unambiguous: a guest has no address to change and no registration to
 * confirm, a signed-in registered user has already confirmed their registration, and a device with
 * no account at all can only be finishing one.
 */
enum class VerificationOrigin {
    /** A freshly registered account confirming its address. The session ends; they sign in. */
    NoSession,

    /** A guest becoming a full account. That session deliberately survives. */
    Guest,

    /** A registered user confirming a new address. The session ends; they sign in with the new one. */
    Registered,
}

enum class VerificationStatus {
    Verifying,
    Succeeded,
    Failed,
}
