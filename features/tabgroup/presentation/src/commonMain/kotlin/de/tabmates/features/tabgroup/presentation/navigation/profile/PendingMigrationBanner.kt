package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_mail
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_pending_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_pending_desc
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_pending_title

/**
 * Shown on both Settings and Profile while a guest's migration link is still unredeemed.
 *
 * It is on Settings because the user should not have to go looking for it, and on Profile because
 * that is where Sign out lives — the one action that makes the unconfirmed state expensive.
 */
@Composable
internal fun PendingMigrationBanner(
    email: String,
    onOpenUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_mail),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                HorizontalSpacer(12.dp)
                Text(
                    text = stringResource(Res.string.profile_upgrade_account_pending_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(Res.string.profile_upgrade_account_pending_desc, email),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenUpgrade,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.profile_upgrade_account_pending_action))
            }
        }
    }
}
