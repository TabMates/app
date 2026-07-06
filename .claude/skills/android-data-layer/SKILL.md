---
name: android-data-layer
description: |
  TabMates data layer + error handling - Result<D,E> with Success/Failure, DataError (Remote/Local/Connection), Ktor HttpClientExt safe-call helpers, services/repositories, DTOs, mappers, Room, offline-first, UiText error mapping. Use this skill whenever writing or reviewing a service/repository/data source, creating DTOs or Room entities, writing mappers, making network calls, or handling errors with typed Results. Trigger on phrases like "repository", "service", "DAO", "Ktor", "mapper", "DTO", "Room entity", "network call", "Result wrapper", "DataError", "onSuccess", "onFailure", "error handling", or "offline-first".
---

# TabMates Data Layer & Error Handling

## Result Wrapper (`core/domain/.../util/Result.kt`)

```kotlin
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>  // NOTE: Failure, not Error!
}
typealias EmptyResult<E> = Result<Unit, E>
```

Helpers (same file, all chainable): `map`, `onSuccess`, `onFailure`, `asEmptyResult`.

```kotlin
authService.forgotPassword(email)
    .onSuccess { _state.update { it.copy(isEmailSentSuccessfully = true) } }
    .onFailure { error -> _state.update { it.copy(errorText = error.toUiText()) } }
```

Never throw for expected failures — return `Result.Failure`. Catch exceptions in the layer that owns them (network → data layer, business rule → domain) and convert to a typed error. `CancellationException` must always be rethrown.

## Error Types (`core/domain/.../util/DataError.kt`)

```kotlin
sealed interface DataError : Error {
    enum class Remote : DataError { BAD_REQUEST, REQUEST_TIMEOUT, UNAUTHORIZED, FORBIDDEN, NOT_FOUND,
        CONFLICT, TOO_MANY_REQUESTS, NO_INTERNET, PAYLOAD_TOO_LARGE, SERVER_ERROR, SERVICE_UNAVAILABLE,
        SERIALIZATION, UNKNOWN }
    enum class Local : DataError { DISK_FULL, NOT_FOUND, UNKNOWN }
    enum class Connection : DataError { NOT_CONNECTED, MESSAGE_SEND_FAILED }  // websocket/sync
}
```

It is `DataError.Remote` — NOT `DataError.Network`. Feature-specific errors implement `Error` (e.g. validation enums) and return single errors, not lists.

| Scenario | Error type |
|---|---|
| HTTP call | `DataError.Remote` |
| DB access | `DataError.Local` |
| Websocket/sync | `DataError.Connection` |
| Multi-source repository | `DataError` supertype |
| Domain validation | custom `enum : Error` |

## Mapping Errors to UI

`DataError.toUiText()` lives in `core/presentation/.../util/DataErrorToUiText.kt` and returns `UiText.Resource(Res.string.…)` (Compose Resources — no `R.string`). Feature-specific error mappers live in the feature's `presentation` module (often private in the ViewModel). Internal-only errors need no mapping.

## Ktor Helpers (`core/data/.../networking/HttpClientExt.kt`)

Typed extensions `HttpClient.get/post/put/patch/delete(route, queryParams, builder)` return `Result<Response, DataError.Remote>`. They wrap `safeCall` (exception → `DataError.Remote`) with an `expect platformSafeCall` for platform quirks, and `constructRoute` prefixes `BuildKonfig.BASE_URL` (NOT `BuildConfig`). Don't reimplement — call site is one line:

```kotlin
@Single(binds = [AuthService::class])
class KtorAuthService(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
) : AuthService {
    override suspend fun register(email: String, username: String, password: String): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/register",
            body = RegisterRequest(email = email, username = username, password = password),
        )
    }
}
```

`HttpClientFactory` (`core:data`) configures ContentNegotiation/Auth once; tokens live in `SessionStorage`, 401 refresh handled by the Ktor Auth plugin.

## Interfaces, Impls, Naming

- Interface in feature `domain`: `AuthService`, `GroupRepository`. Every dependency a ViewModel uses must have a domain interface.
- Impl in feature `data`, named for what makes it unique — never `Impl` suffix: `KtorAuthService`, `OfflineFirstGroupRepository`, `OfflineFirstCurrencyRepository`.
- Bind via `@Single(binds = [Interface::class])`.

## DTOs, Mappers, Room

- DTOs live in `data` (`dto/requests/`), suffix `Request`/`Response`; shared serializable models in `core:data` use `Serializable` suffix (`UserSerializable`). Domain models never go over the wire or into Room directly.
- Mappers = extension functions in `data`, `toDomain()` direction (see `core/data/.../mappers/`).
- Room lives in the feature's `:database` module (`features/tabgroup/database`: `TabMatesDatabase.kt`, `entities/`, `dao/`, `migrations/`). Entities suffix `Entity`.

## Offline-First (tabgroup)

Room is the single source of truth: network fetch → persist to Room → ViewModel observes DB `Flow`. See `OfflineFirstGroupRepository` and `features/tabgroup/data/.../sync/` (delta sync via `/api/sync` cursor). ViewModels never observe network responses directly.

## Checklist: New Service/Repository

- [ ] Domain model + interface + error type in `features:<name>:domain`
- [ ] DTOs (`Request`/`Response`) + mappers (`toDomain()`) in `features:<name>:data`
- [ ] Impl named for uniqueness (`Ktor…`/`OfflineFirst…`), `@Single(binds = [...])`
- [ ] Return `Result<D, DataError.…>` / `EmptyResult<…>` from every operation
- [ ] User-facing errors have a `toUiText()` mapping in presentation
