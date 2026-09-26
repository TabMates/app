---
name: android-testing
description: |
  TabMates testing patterns - kotlin-test + Turbine + hand-written fakes (no mocking library), UnconfinedTestDispatcher setup, TextFieldState snapshot flushing, shared fakes in :features:*:testing modules. Use this skill whenever writing or reviewing tests for ViewModels, repositories, or domain logic. Trigger on phrases like "write a test", "unit test", "test the ViewModel", "Turbine", "fake repository", "runTest", "UnconfinedTestDispatcher", or "kotlin-test".
---

# TabMates Testing

## Stack

| Concern | Library |
|---|---|
| Framework/assertions | **kotlin-test** (`@Test`, `@BeforeTest`, `@AfterTest`, `assertEquals`, `assertTrue`, `assertIs`, `assertNull`) |
| Flow/StateFlow | **Turbine** (`flow.test { }`) |
| Coroutines | `kotlinx-coroutines-test` (`runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle`) |
| Doubles | **hand-written fakes** — NO mocking library (no Mockk/Mokkery/JUnit5/AssertK in this project) |

Tests live in `commonTest`. Room/repository tests may live in `desktopTest` (e.g. `features/tabgroup/data/src/desktopTest/.../OfflineFirstSyncRepositoryTest.kt`). Android-specific tests in `androidApp/src/test`.

## ViewModel Test Setup (real pattern — `ForgotPasswordViewModelTest.kt`)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialStateIsCorrect() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.state.test {
                val state = awaitItem()
                assertFalse(state.canSubmit)
                assertNull(state.errorText)
                cancelAndConsumeRemainingEvents()
            }
        }
}
```

Turbine idioms used in the repo: `awaitItem()`, `expectMostRecentItem()`, always end with `cancelAndConsumeRemainingEvents()`.

## Testing TextFieldState Input

State holds Compose `TextFieldState`; after editing it in a test you must flush the snapshot before assertions:

```kotlin
viewModel.state.value.emailTextFieldState.edit { replace(0, length, "test@example.com") }
Snapshot.sendApplyNotifications()   // flush snapshotFlow-driven validation
advanceUntilIdle()

viewModel.state.test {
    assertTrue(expectMostRecentItem().canSubmit)
    cancelAndConsumeRemainingEvents()
}
```

## Fakes

Prefer fakes over mocks — simple in-memory implementations of domain interfaces with failure toggles:

- **Shared across modules** → `:features:<name>:testing` module (e.g. `features/authentication/testing/.../FakeAuthService.kt`, `FakeNotificationService`).
- **Screen-local** → next to the test in `commonTest` (e.g. `FakeGroupRepository`, `FakeSessionStorage` under `features/tabgroup/presentation/src/commonTest/.../testing/`).

```kotlin
class FakeGroupRepository : GroupRepository {
    var shouldReturnError = false
    private val groups = mutableListOf<Group>()

    override suspend fun getGroups(): Result<List<Group>, DataError> =
        if (shouldReturnError) Result.Failure(DataError.Remote.UNKNOWN)
        else Result.Success(groups.toList())
}
```

Note: `Result.Failure`, not `Result.Error` (see android-data-layer skill).

## SavedStateHandle

Instantiate directly, no mocking: `SavedStateHandle(mapOf("groupId" to "123"))`.

## Dispatcher Injection

Only inject a `CoroutineDispatcher` when the class dispatches to a non-main dispatcher AND is directly unit-tested. ViewModels using only `viewModelScope` need `Dispatchers.setMain()` in tests, nothing more.

## Running

- All targets: `./gradlew allTests` (what CI runs). Narrower: `:features:<name>:presentation:desktopTest` or `:androidApp:testPlayDebugUnitTest`.
- Fast compile check of touched test sources: `./gradlew :features:<name>:<layer>:compileAndroidHostTest` or `compileKotlinJvm`.

## What to Test

- Every ViewModel: initial state, intent functions (success + failure via fake toggles), validation flows, event emission.
- Non-trivial domain logic (validators, `CurrencyConverter`-style) and sync/merge logic in repositories.
- Reuse shared fakes before writing new ones.
