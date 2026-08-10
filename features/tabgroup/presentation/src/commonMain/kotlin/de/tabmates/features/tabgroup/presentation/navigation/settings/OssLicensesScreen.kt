package de.tabmates.features.tabgroup.presentation.navigation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res

/**
 * Shows the third-party open-source libraries and their licenses, generated at build time by the
 * AboutLibraries Gradle plugin (`composeResources/files/aboutlibraries.json`).
 */
@Composable
fun OssLicensesRoot(modifier: Modifier = Modifier) {
    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize(),
    )
}
