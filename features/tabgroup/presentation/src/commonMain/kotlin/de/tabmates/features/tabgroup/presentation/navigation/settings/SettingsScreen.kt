package de.tabmates.features.tabgroup.presentation.navigation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.data.AppBuildInfo
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.domain.legal.LegalUrls
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import de.tabmates.features.tabgroup.presentation.navigation.profile.PendingMigrationBanner
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_dark_mode
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_info
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_light_mode
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_notifications
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_palette
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_shield
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_app_version
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_notifications
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_notifications_blocked
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_notifications_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_open_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_oss_licenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_oss_licenses_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_privacy_policy
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_privacy_policy_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_profile_guest
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_profile_open
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_profile_pending
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_section_about
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_section_preferences
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_caption_dark
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_caption_light
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_caption_system
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_dark
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_light
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_theme_system
import tabmatesapp.features.tabgroup.presentation.generated.resources.settings_title

/** Widest the column ever gets; beyond it the content centres instead of stretching. */
private val ContentMaxWidth = 560.dp

@Composable
fun SettingsRoot(
    onProfileClick: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-check the OS permission whenever the screen resumes (user may grant it in settings), and
    // the account with it — an upgrade is usually confirmed in a mail client, i.e. off this screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationPermission()
        viewModel.refreshAccount()
    }

    SettingsScreen(
        state = state,
        onThemeSelected = viewModel::onThemeSelected,
        onNotificationsToggle = viewModel::onNotificationsToggle,
        onOpenNotificationSettings = viewModel::onOpenNotificationSettings,
        onProfileClick = onProfileClick,
        onUpgradeAccount = onUpgradeAccount,
        onOpenOssLicenses = onOpenOssLicenses,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onThemeSelected: (ThemeMode) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onProfileClick: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The initial state claims a registered account, so rendering it would flash the plain
    // profile card at a guest before it turns into the one carrying the upgrade.
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(4.dp)
            ProfileCard(
                state = state,
                onClick = onProfileClick,
                onUpgradeAccount = onUpgradeAccount,
            )
            // The account is still a guest account until the link is opened. That in-between is
            // easy to forget about — and it is exactly when the user is most likely to lose
            // everything by signing out — so it gets a callout rather than a row.
            state.pendingMigrationEmail?.let { email ->
                PendingMigrationBanner(email = email, onOpenUpgrade = onUpgradeAccount)
            }
            VerticalSpacer(4.dp)
            SectionLabel(stringResource(Res.string.settings_section_preferences))
            ThemeCard(themeMode = state.themeMode, onThemeSelected = onThemeSelected)
            NotificationsCard(
                enabled = state.notificationsEnabled,
                permissionBlocked = state.notificationsPermissionBlocked,
                onToggle = onNotificationsToggle,
                onOpenSettings = onOpenNotificationSettings,
            )
            VerticalSpacer(4.dp)
            SectionLabel(stringResource(Res.string.settings_section_about))
            AppVersionRow()
            PrivacyPolicyRow()
            SettingsRow(
                iconRes = Res.drawable.ic_info,
                title = stringResource(Res.string.settings_oss_licenses),
                subtitle = stringResource(Res.string.settings_oss_licenses_caption),
                onClick = onOpenOssLicenses,
                showChevron = true,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
        }
    }
}

/**
 * Identity plus the way into the account screen.
 *
 * A guest gets the same card in a warmer colour with the upgrade attached, because the upgrade is
 * the one thing they should do and burying it a tap deeper is how accounts get lost. The nested
 * button consumes its own presses, so the rest of the card still opens the profile.
 */
@Composable
private fun ProfileCard(
    state: SettingsState,
    onClick: () -> Unit,
    onUpgradeAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGuest = !state.isRegistered
    val subtitle =
        when {
            isGuest && state.pendingMigrationEmail != null -> stringResource(Res.string.settings_profile_pending)
            isGuest -> stringResource(Res.string.settings_profile_guest)
            else -> state.email
        }
    val openProfileLabel = stringResource(Res.string.settings_profile_open)
    Surface(
        onClick = onClick,
        color =
            if (isGuest) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        contentColor =
            if (isGuest) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        shape = RoundedCornerShape(16.dp),
        // Labels the tap without replacing the name and email the row already reads out.
        modifier = modifier.fillMaxWidth().semantics { onClick(label = openProfileLabel, action = null) },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    initials = state.initials,
                    size = 56.dp,
                    // The guest card is already tertiary, so the avatar has to step up a tone or it
                    // disappears into its own background.
                    containerColor =
                        if (isGuest) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                    contentColor =
                        if (isGuest) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    textStyle = MaterialTheme.typography.titleMedium,
                )
                HorizontalSpacer(16.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        VerticalSpacer(2.dp)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalSpacer(8.dp)
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            // Once the link is out, the banner below carries the call to action (and the resend),
            // so the card would only repeat it.
            if (isGuest && state.pendingMigrationEmail == null) {
                VerticalSpacer(12.dp)
                Button(
                    onClick = onUpgradeAccount,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.profile_upgrade_account_title))
                }
            }
        }
    }
}

@Composable
private fun AppVersionRow() {
    SettingsRow(
        iconRes = Res.drawable.ic_info,
        title = stringResource(Res.string.settings_app_version),
        subtitle = AppBuildInfo.version,
        onClick = null,
        showChevron = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The policy is hosted on the marketing site, so this row leaves the app — which the chevron alone
 * cannot say. The subtitle does, so the row still looks like its siblings without misleading.
 */
@Composable
private fun PrivacyPolicyRow() {
    val uriHandler = LocalUriHandler.current
    SettingsRow(
        iconRes = Res.drawable.ic_shield,
        title = stringResource(Res.string.settings_privacy_policy),
        subtitle = stringResource(Res.string.settings_privacy_policy_caption),
        onClick = { uriHandler.openUri(LegalUrls.PRIVACY_POLICY) },
        showChevron = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeCard(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
) {
    val caption =
        when (themeMode) {
            ThemeMode.LIGHT -> stringResource(Res.string.settings_theme_caption_light)
            ThemeMode.DARK -> stringResource(Res.string.settings_theme_caption_dark)
            ThemeMode.SYSTEM -> stringResource(Res.string.settings_theme_caption_system)
        }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                HorizontalSpacer(12.dp)
                Column {
                    Text(
                        text = stringResource(Res.string.settings_theme),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            VerticalSpacer(12.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemePill(
                    iconRes = Res.drawable.ic_light_mode,
                    label = stringResource(Res.string.settings_theme_light),
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                ThemePill(
                    iconRes = Res.drawable.ic_dark_mode,
                    label = stringResource(Res.string.settings_theme_dark),
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
                ThemePill(
                    iconRes = Res.drawable.ic_settings,
                    label = stringResource(Res.string.settings_theme_system),
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemePill(
    iconRes: DrawableResource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val content =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            HorizontalSpacer(6.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NotificationsCard(
    enabled: Boolean,
    permissionBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_notifications),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                HorizontalSpacer(12.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.settings_notifications_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalSpacer(8.dp)
                Switch(
                    // Permission denied -> show off and disabled; the banner explains why.
                    checked = enabled && !permissionBlocked,
                    onCheckedChange = onToggle,
                    enabled = !permissionBlocked,
                )
            }
        }
        if (permissionBlocked) {
            NotificationPermissionBanner(onOpenSettings = onOpenSettings)
        }
    }
}

@Composable
private fun NotificationPermissionBanner(onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_notifications_blocked),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.settings_open_settings))
            }
        }
    }
}

@PreviewThemes
@Composable
private fun SettingsScreenPreview() {
    TabMatesTheme {
        Surface {
            SettingsScreen(
                state =
                    SettingsState(
                        isLoading = false,
                        username = "Dennis Bauer",
                        email = "dennis@tabmates.de",
                        initials = "DE",
                    ),
                onThemeSelected = {},
                onNotificationsToggle = {},
                onOpenNotificationSettings = {},
                onProfileClick = {},
                onUpgradeAccount = {},
                onOpenOssLicenses = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun ProfileCardPreview() {
    val base = SettingsState(isLoading = false, username = "Dennis Bauer", initials = "DE")
    TabMatesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileCard(
                    state = base.copy(email = "dennis@tabmates.de"),
                    onClick = {},
                    onUpgradeAccount = {},
                )
                ProfileCard(
                    state = base.copy(isRegistered = false),
                    onClick = {},
                    onUpgradeAccount = {},
                )
                ProfileCard(
                    state = base.copy(isRegistered = false, pendingMigrationEmail = "dennis@tabmates.de"),
                    onClick = {},
                    onUpgradeAccount = {},
                )
            }
        }
    }
}
