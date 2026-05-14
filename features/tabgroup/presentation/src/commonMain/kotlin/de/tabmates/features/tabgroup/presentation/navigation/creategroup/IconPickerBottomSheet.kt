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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.buttons.TabMatesButtonStyle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_picker_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_picker_save
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_picker_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerBottomSheet(
    state: IconPickerUiState,
    onIconSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onCategorySelected: (IconCategory) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .imePadding(),
        ) {
            Header()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            ) {
                item(key = "preview") {
                    LivePreview(
                        iconKey = state.draftIconKey,
                        colorKey = state.draftColorKey,
                    )
                }
                item(key = "colors") {
                    ColorSwatches(
                        selectedKey = state.draftColorKey,
                        onSelected = onColorSelected,
                    )
                }
                item(key = "chips") {
                    CategoryChips(
                        selected = state.selectedCategory,
                        onSelected = onCategorySelected,
                    )
                }
                val visibleCategories =
                    if (state.selectedCategory != null) {
                        listOf(state.selectedCategory)
                    } else {
                        IconCatalog.categories
                    }
                visibleCategories.forEach { cat ->
                    val icons = IconCatalog.iconsByCategory[cat].orEmpty()
                    if (icons.isEmpty()) return@forEach
                    item(key = "header-${cat.key}") {
                        Text(
                            text = stringResource(cat.titleRes).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "grid-${cat.key}") {
                        IconGrid(
                            icons = icons,
                            selectedIconKey = state.draftIconKey,
                            onIconSelected = onIconSelected,
                        )
                    }
                }
            }
            Actions(onCancel = onCancel, onSave = onSave)
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.create_group_icon_picker_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LivePreview(
    iconKey: String,
    colorKey: String,
) {
    val icon = IconCatalog.iconOption(iconKey) ?: return
    val color = IconCatalog.colorOption(colorKey)
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(
                text = stringResource(icon.labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(color.labelRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(color.color, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(icon.drawable),
                    contentDescription = null,
                    tint = color.onColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ColorSwatches(
    selectedKey: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconCatalog.colors.forEach { option ->
            ColorSwatch(
                option = option,
                isSelected = option.key == selectedKey,
                onClick = { onSelected(option.key) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    option: IconColorOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color = option.color, shape = CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: IconCategory?,
    onSelected: (IconCategory) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconCatalog.categories.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelected(category) },
                label = { Text(stringResource(category.titleRes)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid(
    icons: List<IconOption>,
    selectedIconKey: String,
    onIconSelected: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 6,
    ) {
        icons.forEach { option ->
            IconTile(
                option = option,
                isSelected = option.key == selectedIconKey,
                onClick = { onIconSelected(option.key) },
            )
        }
    }
}

@Composable
private fun IconTile(
    option: IconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border =
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vectorResource(option.drawable),
                contentDescription = stringResource(option.labelRes),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Actions(
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabMatesButton(
            text = stringResource(Res.string.create_group_icon_picker_cancel),
            onClick = onCancel,
            style = TabMatesButtonStyle.Secondary,
            modifier = Modifier.weight(1f),
        )
        TabMatesButton(
            text = stringResource(Res.string.create_group_icon_picker_save),
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
    }
}
