package de.tabmates.features.authentication.presentation.emailverification

data class EmailVerificationState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
)
