package de.tabmates.composeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.tabmates.features.tabgroup.presentation.navigation.Home

/**
 * wasmJs's compose-resources loader wipes its entire resource cache (browser Cache Storage) on
 * every fresh page session (JetBrains `ResourceWebCache`), and any resource load fired
 * concurrently with that wipe races it — losing whichever entries lost the race, which is why nav
 * labels/icons could go blank on refresh. Awaiting exactly one resource load here forces that
 * reset to finish before anything else requests a resource, so nothing after this ever overlaps
 * with the wipe. Do not warm more than one resource here — firing several concurrently puts them
 * back in the same race instead of avoiding it.
 */
@Composable
actual fun rememberResourcesPrimed(): Boolean {
    var primed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Home.label.asStringAsync()
        primed = true
    }
    return primed
}
