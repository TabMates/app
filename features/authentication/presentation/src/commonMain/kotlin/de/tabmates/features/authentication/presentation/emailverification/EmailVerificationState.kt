package de.tabmates.features.authentication.presentation.emailverification

data class EmailVerificationState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    // True when the token upgraded an anonymous account: that session deliberately stays valid, so
    // the screen offers a way back into the app instead of a sign-in.
    val retainsSession: Boolean = false,
)
