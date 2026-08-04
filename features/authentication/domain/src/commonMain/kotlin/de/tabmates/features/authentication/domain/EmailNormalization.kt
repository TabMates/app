package de.tabmates.features.authentication.domain

/**
 * The single form an email is compared and sent in. The email fields already lower-case what the
 * user types, but text set programmatically — a prefilled field, platform autofill — bypasses that,
 * so every read that reaches [EmailValidator] or the network goes through here.
 */
fun String.normalizeEmail(): String = trim().lowercase()
