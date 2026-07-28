package de.tabmates.core.domain.util

sealed interface DataError : Error {
    enum class Remote : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,

        // Web-only: 403 with body { "code": "TURNSTILE_VERIFICATION_FAILED" } on the auth
        // endpoints; distinguished from FORBIDDEN by reading the response body.
        TURNSTILE_FAILED,

        // 426: this build is older than the minimum the backend still serves. Retrying cannot
        // help — the only way out is a new build, so it is surfaced as the update prompt rather
        // than a generic error. See UpgradeRequiredNotifier.
        UPGRADE_REQUIRED,
        UNKNOWN,
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }

    enum class Connection : DataError {
        NOT_CONNECTED,
        MESSAGE_SEND_FAILED,
    }
}
