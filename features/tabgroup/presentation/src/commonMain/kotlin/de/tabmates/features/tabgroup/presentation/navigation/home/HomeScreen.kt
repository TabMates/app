package de.tabmates.features.tabgroup.presentation.navigation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.tabmates.core.designsystem.buttons.TabMatesButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(homeViewModel: HomeViewModel = koinViewModel()) {
    HomeScreen(
        onLogoutClick = homeViewModel::onLogoutClick,
    )
}

@Composable
private fun HomeScreen(onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Home Screen")
        TabMatesButton(
            text = "Logout",
            onClick = onLogoutClick,
        )
    }
}
