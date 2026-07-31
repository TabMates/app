package de.tabmates.composeapp

/**
 * What the app shell should do with the current session.
 *
 * [Stale] is the reason this is not a boolean: an expired session leaves no credentials but still
 * owns a device full of local data — including writes that have not reached the server — so the
 * app stays usable on that data instead of dumping the user back at the welcome screen.
 */
enum class SessionShellState {
    /** No session and no account to return to: the auth flow owns the screen. */
    SignedOut,

    /** Signed in and able to talk to the server. */
    Active,

    /** Credentials expired, local data intact. The shell stays up behind a re-auth banner. */
    Stale,
    ;

    /** Whether the device holds an account's data, valid credentials or not. */
    val hasLocalSession: Boolean get() = this != SignedOut
}
