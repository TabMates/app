package de.tabmates.composeapp.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import de.tabmates.core.presentation.navigation.ScreenWithTopBar
import de.tabmates.core.presentation.navigation.TopBarAction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.composeapp.generated.resources.Res
import tabmatesapp.composeapp.generated.resources.back
import tabmatesapp.composeapp.generated.resources.close
import tabmatesapp.composeapp.generated.resources.ic_arrow_back
import tabmatesapp.composeapp.generated.resources.ic_close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    config: ScreenWithTopBar,
    onNavigationClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(config.topBarTitle.asString()) },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = vectorResource(config.topBarAction.iconRes),
                    contentDescription = stringResource(config.topBarAction.contentDescriptionRes),
                )
            }
        },
    )
}

private val TopBarAction.iconRes: DrawableResource
    get() =
        when (this) {
            TopBarAction.Close -> Res.drawable.ic_close
            TopBarAction.Back -> Res.drawable.ic_arrow_back
        }

private val TopBarAction.contentDescriptionRes: StringResource
    get() =
        when (this) {
            TopBarAction.Close -> Res.string.close
            TopBarAction.Back -> Res.string.back
        }
