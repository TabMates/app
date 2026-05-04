package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
import de.tabmates.core.presentation.share.LinkShareResult
import de.tabmates.core.presentation.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_currency_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_description_too_long
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_error_title_too_long
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsEmpty() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertEquals("", state.nameTextState.text.toString())
                assertEquals("", state.descriptionTextState.text.toString())
                assertEquals("", state.defaultCurrencyCode)
                assertTrue(state.members.isEmpty())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun supportedCurrenciesAreLoadedOnStart() =
        runTest(testDispatcher) {
            val currencyRepository = FakeCurrencyRepository()
            val viewModel = createViewModel(currencyRepository = currencyRepository)

            assertEquals(1, currencyRepository.getCurrenciesCalls)
            assertEquals(
                FakeCurrencyRepository.DEFAULT_CURRENCIES,
                viewModel.state.value.supportedCurrencies,
            )
        }

    @Test
    fun onCreateClickPassesTitleDescriptionAndCurrency() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            fillRequiredFields(viewModel, title = "Trip", description = "Lisbon weekend")

            viewModel.onCreateClick()
            advanceUntilIdle()

            assertEquals(1, groupRepository.createGroupCalls.size)
            val call = groupRepository.createGroupCalls.single()
            assertEquals("Trip", call.title)
            assertEquals("Lisbon weekend", call.description)
            assertEquals(emptySet(), call.otherUserIds)
        }

    @Test
    fun onCreateClickSuccessSendsGroupCreatedEvent() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            fillRequiredFields(viewModel)

            viewModel.events.test {
                viewModel.onCreateClick()
                assertEquals(CreateGroupEvent.GroupCreated, awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun onCreateClickFailureSendsErrorEvent() =
        runTest(testDispatcher) {
            val groupRepository =
                FakeGroupRepository(createGroupResult = Result.Failure(DataError.Remote.UNKNOWN))
            val viewModel = createViewModel(groupRepository = groupRepository)
            fillRequiredFields(viewModel)

            viewModel.events.test {
                viewModel.onCreateClick()
                assertIs<CreateGroupEvent.Error>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun onCreateClickEmitsErrorWhenTitleBlank() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            viewModel.onCurrencySelected("EUR")

            viewModel.events.test {
                viewModel.onCreateClick()
                val event = assertIs<CreateGroupEvent.Error>(awaitItem())
                assertEquals(
                    Res.string.create_group_error_title_required,
                    assertIs<UiText.Resource>(event.message).id,
                )
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(groupRepository.createGroupCalls.isEmpty())
        }

    @Test
    fun onCreateClickEmitsErrorWhenTitleTooLong() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            viewModel.onCurrencySelected("EUR")
            viewModel.state.value.nameTextState
                .edit { replace(0, length, "x".repeat(256)) }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onCreateClick()
                val event = assertIs<CreateGroupEvent.Error>(awaitItem())
                assertEquals(
                    Res.string.create_group_error_title_too_long,
                    assertIs<UiText.Resource>(event.message).id,
                )
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(groupRepository.createGroupCalls.isEmpty())
        }

    @Test
    fun onCreateClickEmitsErrorWhenDescriptionTooLong() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            fillRequiredFields(viewModel)
            viewModel.state.value.descriptionTextState
                .edit { replace(0, length, "x".repeat(1001)) }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onCreateClick()
                val event = assertIs<CreateGroupEvent.Error>(awaitItem())
                assertEquals(
                    Res.string.create_group_error_description_too_long,
                    assertIs<UiText.Resource>(event.message).id,
                )
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(groupRepository.createGroupCalls.isEmpty())
        }

    @Test
    fun onCreateClickEmitsErrorWhenCurrencyMissing() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            viewModel.state.value.nameTextState
                .edit { replace(0, length, "Trip") }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.onCreateClick()
                val event = assertIs<CreateGroupEvent.Error>(awaitItem())
                assertEquals(
                    Res.string.create_group_error_currency_required,
                    assertIs<UiText.Resource>(event.message).id,
                )
                cancelAndConsumeRemainingEvents()
            }
            assertTrue(groupRepository.createGroupCalls.isEmpty())
        }

    @Test
    fun onLinkSharedWithCopiedSendsLinkCopiedEvent() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onLinkShared(LinkShareResult.Copied)
                assertEquals(CreateGroupEvent.LinkCopied, awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun onLinkSharedWithSharedSendsLinkSharedEvent() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onLinkShared(LinkShareResult.Shared)
                assertEquals(CreateGroupEvent.LinkShared, awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun onCurrencyClickShowsPicker() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onCurrencyClick()

            assertTrue(viewModel.state.value.isCurrencyPickerVisible)
        }

    @Test
    fun onCurrencyPickerDismissHidesPicker() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onCurrencyClick()

            viewModel.onCurrencyPickerDismiss()

            assertFalse(viewModel.state.value.isCurrencyPickerVisible)
        }

    @Test
    fun onCurrencySelectedSetsCodeAndHidesPicker() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onCurrencyClick()

            viewModel.onCurrencySelected("USD")

            val state = viewModel.state.value
            assertEquals("USD", state.defaultCurrencyCode)
            assertFalse(state.isCurrencyPickerVisible)
        }

    @Test
    fun onCreateClickUsesSelectedCurrency() =
        runTest(testDispatcher) {
            val groupRepository = FakeGroupRepository()
            val viewModel = createViewModel(groupRepository = groupRepository)
            viewModel.onCurrencySelected("USD")
            viewModel.state.value.nameTextState
                .edit { replace(0, length, "Trip") }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.onCreateClick()
            advanceUntilIdle()

            assertEquals("USD", groupRepository.createGroupCalls.single().defaultCurrencyCode)
        }

    private fun TestScope.activateState(viewModel: CreateGroupViewModel) {
        backgroundScope.launch {
            viewModel.state.collect { }
        }
        advanceUntilIdle()
    }

    private fun TestScope.createViewModel(
        groupRepository: FakeGroupRepository = FakeGroupRepository(),
        currencyRepository: FakeCurrencyRepository = FakeCurrencyRepository(),
    ): CreateGroupViewModel {
        val viewModel =
            CreateGroupViewModel(
                groupRepository = groupRepository,
                currencyRepository = currencyRepository,
            )
        activateState(viewModel)
        return viewModel
    }

    private fun TestScope.fillRequiredFields(
        viewModel: CreateGroupViewModel,
        title: String = "Trip",
        description: String = "",
        currencyCode: String = "EUR",
    ) {
        viewModel.onCurrencySelected(currencyCode)
        viewModel.state.value.nameTextState
            .edit { replace(0, length, title) }
        if (description.isNotEmpty()) {
            viewModel.state.value.descriptionTextState
                .edit { replace(0, length, description) }
        }
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }
}
