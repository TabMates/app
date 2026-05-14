package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import de.tabmates.core.domain.util.DataError
import de.tabmates.core.domain.util.Result
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
                assertTrue(state.placeholders.isEmpty())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun supportedCurrenciesAreLoadedOnStart() =
        runTest(testDispatcher) {
            val currencyRepository = FakeCurrencyRepository()
            val viewModel = createViewModel(currencyRepository = currencyRepository)

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
    fun onAddPlaceholderClickShowsDialog() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onAddPlaceholderClick()

            assertTrue(viewModel.state.value.isPlaceholderDialogVisible)
        }

    @Test
    fun onPlaceholderDialogConfirmAddsPlaceholder() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddPlaceholderClick()
            viewModel.state.value.newPlaceholderTextState
                .edit { replace(0, length, "Ben") }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            viewModel.onPlaceholderDialogConfirm()

            val placeholders = viewModel.state.value.placeholders
            assertEquals(1, placeholders.size)
            assertEquals("Ben", placeholders.single().name)
            assertFalse(viewModel.state.value.isPlaceholderDialogVisible)
        }

    @Test
    fun onPlaceholderDialogConfirmIgnoresBlankName() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddPlaceholderClick()

            viewModel.onPlaceholderDialogConfirm()

            assertTrue(
                viewModel.state.value.placeholders
                    .isEmpty(),
            )
        }

    @Test
    fun onPickIconClickOpensPickerWithCurrentValuesAsDraft() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onIconDraftSelected("car")
            viewModel.onColorDraftSelected("mint")
            viewModel.onIconPickerSave()

            viewModel.onPickIconClick()

            val picker = viewModel.state.value.iconPicker
            assertTrue(picker.isVisible)
            assertEquals("car", picker.draftIconKey)
            assertEquals("mint", picker.draftColorKey)
            assertEquals(null, picker.selectedCategory)
        }

    @Test
    fun onIconPickerSaveCommitsDraftsAndClosesPicker() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()
            viewModel.onIconDraftSelected("coffee")
            viewModel.onColorDraftSelected("teal")

            viewModel.onIconPickerSave()

            val state = viewModel.state.value
            assertEquals("coffee", state.iconKey)
            assertEquals("teal", state.colorKey)
            assertFalse(state.iconPicker.isVisible)
        }

    @Test
    fun onIconPickerDismissDiscardsDraftsAndClosesPicker() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val originalIcon = viewModel.state.value.iconKey
            val originalColor = viewModel.state.value.colorKey
            viewModel.onPickIconClick()
            viewModel.onIconDraftSelected("pizza")
            viewModel.onColorDraftSelected("pink")

            viewModel.onIconPickerDismiss()

            val state = viewModel.state.value
            assertEquals(originalIcon, state.iconKey)
            assertEquals(originalColor, state.colorKey)
            assertFalse(state.iconPicker.isVisible)
            assertEquals(IconCatalog.defaultIconKey, state.iconPicker.draftIconKey)
            assertEquals(IconCatalog.defaultColorKey, state.iconPicker.draftColorKey)
        }

    @Test
    fun onIconDraftSelectedUpdatesDraftIcon() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()

            viewModel.onIconDraftSelected("train")

            assertEquals("train", viewModel.state.value.iconPicker.draftIconKey)
        }

    @Test
    fun onColorDraftSelectedUpdatesDraftColor() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()

            viewModel.onColorDraftSelected("blush")

            assertEquals("blush", viewModel.state.value.iconPicker.draftColorKey)
        }

    @Test
    fun onIconCategorySelectedSetsCategory() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()

            viewModel.onIconCategorySelected(IconCategory.FOOD)

            assertEquals(IconCategory.FOOD, viewModel.state.value.iconPicker.selectedCategory)
        }

    @Test
    fun onIconCategorySelectedTogglesOffWhenSameCategory() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()
            viewModel.onIconCategorySelected(IconCategory.FOOD)

            viewModel.onIconCategorySelected(IconCategory.FOOD)

            assertEquals(null, viewModel.state.value.iconPicker.selectedCategory)
        }

    @Test
    fun onIconCategorySelectedSwitchesWhenDifferentCategory() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onPickIconClick()
            viewModel.onIconCategorySelected(IconCategory.FOOD)

            viewModel.onIconCategorySelected(IconCategory.HOME)

            assertEquals(IconCategory.HOME, viewModel.state.value.iconPicker.selectedCategory)
        }

    @Test
    fun onRemovePlaceholderDropsMatchingEntry() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAddPlaceholderClick()
            viewModel.state.value.newPlaceholderTextState
                .edit { replace(0, length, "Ben") }
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()
            viewModel.onPlaceholderDialogConfirm()
            val placeholderId =
                viewModel.state.value.placeholders
                    .single()
                    .id

            viewModel.onRemovePlaceholder(placeholderId)

            assertTrue(
                viewModel.state.value.placeholders
                    .isEmpty(),
            )
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
