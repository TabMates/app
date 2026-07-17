package de.tabmates.core.designsystem.logo

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.core.designsystem.generated.resources.Res
import tabmatesapp.core.designsystem.generated.resources.ic_logo

@Composable
fun TabMatesLogo(modifier: Modifier = Modifier) {
    Image(
        imageVector = vectorResource(Res.drawable.ic_logo),
        contentDescription = null,
        modifier = modifier,
    )
}
