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

        // 400 with body { "code": "CANNOT_REMOVE_SELF" } when removing a group participant. Not
        // reachable through the UI — your own row offers no remove — but mapped so a mistake
        // reads as itself rather than a generic bad request. Leaving is a separate endpoint.
        CANNOT_REMOVE_SELF,

        // 403 with body { "code": "CANNOT_REMOVE_GROUP_CREATOR" }: the group's creator can only
        // leave voluntarily. Distinguished from FORBIDDEN, which means the caller is not a member.
        CANNOT_REMOVE_GROUP_CREATOR,

        // 400 with body { "code": "INVALID_RECURRING_RULE" }: the recurring template or schedule
        // is not something the server will store. Covers both a malformed template and the cap on
        // how many active schedules one member may own, which the server states under this code.
        INVALID_RECURRING_RULE,

        // 503 with body { "code": "RECURRING_ENTRIES_DISABLED" }: the feature is switched off in
        // this environment. Distinguished from SERVICE_UNAVAILABLE, which means the server is
        // struggling — here the request was fine and only a config change makes it work.
        RECURRING_ENTRIES_DISABLED,
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
