package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewScreenSizes
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.AddPlaceholderButton
import de.tabmates.features.tabgroup.presentation.components.AddPlaceholderDialog
import de.tabmates.features.tabgroup.presentation.components.GroupAvatar
import de.tabmates.features.tabgroup.presentation.components.PlaceholderChip
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_add_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_change_icon_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_default_currency
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_description_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_edit_icon_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_name_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_name_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_remove_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholders_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholders_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_submit_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_down
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_edit

@Composable
fun CreateGroupRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyPickerState by viewModel.currencyPickerState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateGroupEvent.GroupCreated -> backStack.removeLastOrNull()
            is CreateGroupEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    CreateGroupScreen(
        state = state,
        currencyPickerState = currencyPickerState,
        onPickIconClick = viewModel::onPickIconClick,
        onIconPickerDismiss = viewModel::onIconPickerDismiss,
        onIconPickerSave = viewModel::onIconPickerSave,
        onIconDraftSelected = viewModel::onIconDraftSelected,
        onColorDraftSelected = viewModel::onColorDraftSelected,
        onIconCategorySelected = viewModel::onIconCategorySelected,
        onCurrencyClick = viewModel::onCurrencyClick,
        onAddPlaceholderClick = viewModel::onAddPlaceholderClick,
        onPlaceholderDialogConfirm = viewModel::onPlaceholderDialogConfirm,
        onPlaceholderDialogDismiss = viewModel::onPlaceholderDialogDismiss,
        onRemovePlaceholder = viewModel::onRemovePlaceholder,
        onCreateClick = viewModel::onCreateClick,
        onCurrencySelected = viewModel::onCurrencySelected,
        onCurrencyPickerDismiss = viewModel::onCurrencyPickerDismiss,
        modifier = modifier,
    )
}

@Composable
private fun CreateGroupScreen(
    state: CreateGroupState,
    currencyPickerState: CurrencyPickerUiState,
    onPickIconClick: () -> Unit,
    onIconPickerDismiss: () -> Unit,
    onIconPickerSave: () -> Unit,
    onIconDraftSelected: (String) -> Unit,
    onColorDraftSelected: (String) -> Unit,
    onIconCategorySelected: (IconCategory) -> Unit,
    onCurrencyClick: () -> Unit,
    onAddPlaceholderClick: () -> Unit,
    onPlaceholderDialogConfirm: () -> Unit,
    onPlaceholderDialogDismiss: () -> Unit,
    onRemovePlaceholder: (String) -> Unit,
    onCreateClick: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onCurrencyPickerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isCurrencyPickerVisible) {
        CurrencyPickerBottomSheet(
            queryState = state.currencyQueryState,
            state = currencyPickerState,
            onCurrencySelected = onCurrencySelected,
            onDismiss = onCurrencyPickerDismiss,
        )
    }
    if (state.iconPicker.isVisible) {
        IconPickerBottomSheet(
            state = state.iconPicker,
            onIconSelected = onIconDraftSelected,
            onColorSelected = onColorDraftSelected,
            onCategorySelected = onIconCategorySelected,
            onSave = onIconPickerSave,
            onCancel = onIconPickerDismiss,
        )
    }
    if (state.isPlaceholderDialogVisible) {
        AddPlaceholderDialog(
            textState = state.newPlaceholderTextState,
            title = stringResource(Res.string.create_group_placeholder_dialog_title),
            nameLabel = stringResource(Res.string.create_group_placeholder_name_label),
            confirmLabel = stringResource(Res.string.create_group_placeholder_dialog_confirm),
            cancelLabel = stringResource(Res.string.create_group_placeholder_dialog_cancel),
            onConfirm = onPlaceholderDialogConfirm,
            onDismiss = onPlaceholderDialogDismiss,
        )
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalSpacer(8.dp)
        GroupIconAvatar(
            iconKey = state.iconKey,
            colorKey = state.colorKey,
            onClick = onPickIconClick,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.create_group_change_icon_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(24.dp)
        TabMatesTextField(
            state = state.nameTextState,
            title = stringResource(Res.string.create_group_name_label),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            singleLine = true,
        )
        VerticalSpacer(12.dp)
        TabMatesTextField(
            state = state.descriptionTextState,
            title = stringResource(Res.string.create_group_description_placeholder),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            singleLine = true,
        )
        VerticalSpacer(12.dp)
        CurrencyRow(
            code = state.defaultCurrencyCode,
            symbol = state.supportedCurrencies.firstOrNull { it.code == state.defaultCurrencyCode }?.nativeSymbol,
            onClick = onCurrencyClick,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        )
        VerticalSpacer(24.dp)
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.create_group_placeholders_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.create_group_placeholders_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(12.dp)
            PlaceholderChipsRow(
                placeholders = state.placeholders,
                onRemove = onRemovePlaceholder,
                onAddClick = onAddPlaceholderClick,
            )
        }
        VerticalSpacer(32.dp)
        TabMatesButton(
            text = stringResource(Res.string.create_group_submit_action),
            onClick = onCreateClick,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            isLoading = state.isSubmitting,
        )
        VerticalSpacer(16.dp)
    }
}

@Composable
private fun GroupIconAvatar(
    iconKey: String,
    colorKey: String,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(72.dp).clickable(onClick = onClick)) {
        GroupAvatar(
            iconKey = iconKey,
            colorKey = colorKey,
            size = 64.dp,
            cornerRadius = 16.dp,
            iconSize = 32.dp,
            modifier = Modifier.align(Alignment.TopStart),
        )
        EditBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun EditBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_edit),
            contentDescription = stringResource(Res.string.create_group_edit_icon_cd),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    symbol: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 20.dp),
            )
            HorizontalSpacer(12.dp)
            Text(
                text =
                    if (code.isNotEmpty()) {
                        "${stringResource(Res.string.create_group_default_currency)} · $code"
                    } else {
                        stringResource(Res.string.create_group_default_currency)
                    },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceholderChipsRow(
    placeholders: List<CreateGroupPlaceholder>,
    onRemove: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    val removeContentDescription = stringResource(Res.string.create_group_placeholder_remove_cd)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        placeholders.forEach { placeholder ->
            PlaceholderChip(
                name = placeholder.name,
                initial = placeholder.initial,
                onRemove = { onRemove(placeholder.id) },
                removeContentDescription = removeContentDescription,
            )
        }
        AddPlaceholderButton(
            label = stringResource(Res.string.create_group_add_placeholder),
            onClick = onAddClick,
        )
    }
}

private fun previewState(): CreateGroupState =
    CreateGroupState(
        nameTextState = TextFieldState("Weekend in Lisbon"),
        defaultCurrencyCode = "EUR",
        iconKey = "airplane",
        placeholders =
            listOf(
                CreateGroupPlaceholder(id = "p1", name = "Ben"),
                CreateGroupPlaceholder(id = "p2", name = "Clara"),
            ),
    )

@PreviewThemes
@Composable
private fun CreateGroupScreenThemesPreview() {
    TabMatesTheme {
        Surface {
            CreateGroupScreen(
                state = previewState(),
                currencyPickerState = CurrencyPickerUiState(),
                onPickIconClick = {},
                onIconPickerDismiss = {},
                onIconPickerSave = {},
                onIconDraftSelected = {},
                onColorDraftSelected = {},
                onIconCategorySelected = {},
                onCurrencyClick = {},
                onAddPlaceholderClick = {},
                onPlaceholderDialogConfirm = {},
                onPlaceholderDialogDismiss = {},
                onRemovePlaceholder = {},
                onCreateClick = {},
                onCurrencySelected = {},
                onCurrencyPickerDismiss = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CreateGroupScreenSizesPreview() {
    TabMatesTheme {
        Surface {
            CreateGroupScreen(
                state = previewState(),
                currencyPickerState = CurrencyPickerUiState(),
                onPickIconClick = {},
                onIconPickerDismiss = {},
                onIconPickerSave = {},
                onIconDraftSelected = {},
                onColorDraftSelected = {},
                onIconCategorySelected = {},
                onCurrencyClick = {},
                onAddPlaceholderClick = {},
                onPlaceholderDialogConfirm = {},
                onPlaceholderDialogDismiss = {},
                onRemovePlaceholder = {},
                onCreateClick = {},
                onCurrencySelected = {},
                onCurrencyPickerDismiss = {},
            )
        }
    }
}
