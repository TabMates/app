package de.tabmates.features.tabgroup.presentation.navigation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.domain.biometric.BiometricPromptStrings
import de.tabmates.core.domain.preferences.ThemeMode
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.SectionLabel
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_alternate_email
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_prompt_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_prompt_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_prompt_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_unavailable
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_unlock
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_biometric_unlock_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_dark_mode
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_info
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_light_mode
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_lock
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_logout
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_mail
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_notifications
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_palette
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_email
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_password
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_username
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_delete_account
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_footer
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_nav_about
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_nav_appearance
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_nav_notifications
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_nav_security
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_notifications
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_notifications_blocked
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_notifications_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_open_settings
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_oss_licenses
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_oss_licenses_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_password_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_section_about
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_section_account
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_section_appearance
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_section_security
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_settings_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_caption_dark
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_caption_light
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_caption_system
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_dark
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_light
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_theme_system
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_title

@Composable
fun ProfileRoot(
    snackbarHostState: SnackbarHostState,
    onEditUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-check OS-level state whenever the screen resumes (user may change it in system settings).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationPermission()
        viewModel.refreshBiometricAvailability()
    }

    val biometricPromptStrings =
        BiometricPromptStrings(
            title = stringResource(Res.string.profile_biometric_prompt_title),
            subtitle = stringResource(Res.string.profile_biometric_prompt_subtitle),
            cancel = stringResource(Res.string.profile_biometric_prompt_cancel),
        )

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // Session is cleared on sign-out; the app shell observes it and returns to Welcome.
            ProfileEvent.SignedOut -> Unit

            is ProfileEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    ProfileScreen(
        state = state,
        onSectionSelected = viewModel::onSectionSelected,
        onThemeSelected = viewModel::onThemeSelected,
        onNotificationsToggle = viewModel::onNotificationsToggle,
        onOpenNotificationSettings = viewModel::onOpenNotificationSettings,
        onBiometricUnlockToggle = { enabled ->
            viewModel.onBiometricUnlockToggle(enabled, biometricPromptStrings)
        },
        onEditUsername = onEditUsername,
        onChangePassword = onChangePassword,
        onChangeEmail = onChangeEmail,
        onOpenOssLicenses = onOpenOssLicenses,
        onDeleteAccount = onDeleteAccount,
        onSignOut = viewModel::onSignOut,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onSectionSelected: (SettingsSection) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBiometricUnlockToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (isExpanded) {
        SettingsTwoPane(
            state = state,
            onSectionSelected = onSectionSelected,
            onThemeSelected = onThemeSelected,
            onNotificationsToggle = onNotificationsToggle,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onBiometricUnlockToggle = onBiometricUnlockToggle,
            onEditUsername = onEditUsername,
            onChangePassword = onChangePassword,
            onChangeEmail = onChangeEmail,
            onOpenOssLicenses = onOpenOssLicenses,
            onDeleteAccount = onDeleteAccount,
            onSignOut = onSignOut,
            modifier = modifier,
        )
    } else {
        ProfilePhonePane(
            state = state,
            onThemeSelected = onThemeSelected,
            onNotificationsToggle = onNotificationsToggle,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onBiometricUnlockToggle = onBiometricUnlockToggle,
            onEditUsername = onEditUsername,
            onChangePassword = onChangePassword,
            onChangeEmail = onChangeEmail,
            onOpenOssLicenses = onOpenOssLicenses,
            onDeleteAccount = onDeleteAccount,
            onSignOut = onSignOut,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfilePhonePane(
    state: ProfileState,
    onThemeSelected: (ThemeMode) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBiometricUnlockToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.profile_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        VerticalSpacer(4.dp)
        ProfileHeaderCard(state)
        ProfileAccountAndAppearance(
            state = state,
            twoColumnAccount = false,
            onThemeSelected = onThemeSelected,
            onNotificationsToggle = onNotificationsToggle,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onBiometricUnlockToggle = onBiometricUnlockToggle,
            onEditUsername = onEditUsername,
            onChangePassword = onChangePassword,
            onChangeEmail = onChangeEmail,
            onOpenOssLicenses = onOpenOssLicenses,
        )
        VerticalSpacer(8.dp)
        SignOutButton(onClick = onSignOut)
        DeleteAccountButton(onClick = onDeleteAccount)
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.profile_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        VerticalSpacer(16.dp)
    }
}

@Composable
private fun SettingsTwoPane(
    state: ProfileState,
    onSectionSelected: (SettingsSection) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBiometricUnlockToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        SettingsMaster(
            state = state,
            onSectionSelected = onSectionSelected,
            onDeleteAccount = onDeleteAccount,
            onSignOut = onSignOut,
            showSecurity = state.biometricSupported,
            modifier = Modifier.width(280.dp).fillMaxSize(),
        )
        VerticalDivider()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer(4.dp)
            when (state.selectedSection) {
                SettingsSection.PROFILE -> {
                    ProfileHeaderCard(state)
                    ProfileAccountAndAppearance(
                        state = state,
                        twoColumnAccount = true,
                        onThemeSelected = onThemeSelected,
                        onNotificationsToggle = onNotificationsToggle,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onBiometricUnlockToggle = onBiometricUnlockToggle,
                        onEditUsername = onEditUsername,
                        onChangePassword = onChangePassword,
                        onChangeEmail = onChangeEmail,
                        onOpenOssLicenses = onOpenOssLicenses,
                    )
                }

                SettingsSection.APPEARANCE -> {
                    SectionLabel(stringResource(Res.string.profile_section_appearance))
                    ThemeCard(themeMode = state.themeMode, onThemeSelected = onThemeSelected)
                }

                SettingsSection.NOTIFICATIONS -> {
                    SectionLabel(stringResource(Res.string.profile_section_appearance))
                    NotificationsCard(
                        enabled = state.notificationsEnabled,
                        permissionBlocked = state.notificationsPermissionBlocked,
                        onToggle = onNotificationsToggle,
                        onOpenSettings = onOpenNotificationSettings,
                    )
                }

                SettingsSection.SECURITY -> {
                    SectionLabel(stringResource(Res.string.profile_section_security))
                    SecurityCard(
                        enabled = state.biometricUnlockEnabled,
                        available = state.biometricAvailable,
                        onToggle = onBiometricUnlockToggle,
                    )
                }

                SettingsSection.ABOUT -> {
                    SectionLabel(stringResource(Res.string.profile_section_about))
                    AccountRow(
                        iconRes = Res.drawable.ic_info,
                        title = stringResource(Res.string.profile_oss_licenses),
                        subtitle = stringResource(Res.string.profile_oss_licenses_caption),
                        onClick = onOpenOssLicenses,
                        showChevron = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.profile_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsMaster(
    state: ProfileState,
    onSectionSelected: (SettingsSection) -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    showSecurity: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(Res.string.profile_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(8.dp),
        )
        VerticalSpacer(8.dp)
        SettingsNavItem(
            iconRes = Res.drawable.ic_alternate_email,
            label = stringResource(Res.string.profile_title),
            selected = state.selectedSection == SettingsSection.PROFILE,
            onClick = { onSectionSelected(SettingsSection.PROFILE) },
        )
        SettingsNavItem(
            iconRes = Res.drawable.ic_palette,
            label = stringResource(Res.string.profile_nav_appearance),
            selected = state.selectedSection == SettingsSection.APPEARANCE,
            onClick = { onSectionSelected(SettingsSection.APPEARANCE) },
        )
        SettingsNavItem(
            iconRes = Res.drawable.ic_notifications,
            label = stringResource(Res.string.profile_nav_notifications),
            selected = state.selectedSection == SettingsSection.NOTIFICATIONS,
            onClick = { onSectionSelected(SettingsSection.NOTIFICATIONS) },
        )
        if (showSecurity) {
            SettingsNavItem(
                iconRes = Res.drawable.ic_lock,
                label = stringResource(Res.string.profile_nav_security),
                selected = state.selectedSection == SettingsSection.SECURITY,
                onClick = { onSectionSelected(SettingsSection.SECURITY) },
            )
        }
        SettingsNavItem(
            iconRes = Res.drawable.ic_info,
            label = stringResource(Res.string.profile_nav_about),
            selected = state.selectedSection == SettingsSection.ABOUT,
            onClick = { onSectionSelected(SettingsSection.ABOUT) },
        )
        Box(modifier = Modifier.weight(1f))
        SignOutButton(onClick = onSignOut)
        DeleteAccountButton(onClick = onDeleteAccount)
    }
}

@Composable
private fun SettingsNavItem(
    iconRes: DrawableResource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(12.dp)
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ProfileAccountAndAppearance(
    state: ProfileState,
    twoColumnAccount: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBiometricUnlockToggle: (Boolean) -> Unit,
    onEditUsername: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onOpenOssLicenses: () -> Unit,
) {
    SectionLabel(stringResource(Res.string.profile_section_account))
    if (twoColumnAccount) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AccountRow(
                iconRes = Res.drawable.ic_alternate_email,
                title = stringResource(Res.string.profile_account_username),
                subtitle = "@${state.username}",
                onClick = onEditUsername,
                showChevron = true,
                modifier = Modifier.weight(1f),
            )
            if (state.isRegistered) {
                AccountRow(
                    iconRes = Res.drawable.ic_mail,
                    title = stringResource(Res.string.profile_account_email),
                    subtitle = state.email,
                    onClick = onChangeEmail,
                    showChevron = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.isRegistered) {
            AccountRow(
                iconRes = Res.drawable.ic_lock,
                title = stringResource(Res.string.profile_account_password),
                subtitle = stringResource(Res.string.profile_password_subtitle),
                onClick = onChangePassword,
                showChevron = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        AccountRow(
            iconRes = Res.drawable.ic_alternate_email,
            title = stringResource(Res.string.profile_account_username),
            subtitle = "@${state.username}",
            onClick = onEditUsername,
            showChevron = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isRegistered) {
            AccountRow(
                iconRes = Res.drawable.ic_mail,
                title = stringResource(Res.string.profile_account_email),
                subtitle = state.email,
                onClick = onChangeEmail,
                showChevron = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isRegistered) {
            AccountRow(
                iconRes = Res.drawable.ic_lock,
                title = stringResource(Res.string.profile_account_password),
                subtitle = stringResource(Res.string.profile_password_subtitle),
                onClick = onChangePassword,
                showChevron = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    VerticalSpacer(4.dp)
    SectionLabel(stringResource(Res.string.profile_section_appearance))
    ThemeCard(themeMode = state.themeMode, onThemeSelected = onThemeSelected)
    NotificationsCard(
        enabled = state.notificationsEnabled,
        permissionBlocked = state.notificationsPermissionBlocked,
        onToggle = onNotificationsToggle,
        onOpenSettings = onOpenNotificationSettings,
    )
    if (state.biometricSupported) {
        VerticalSpacer(4.dp)
        SectionLabel(stringResource(Res.string.profile_section_security))
        SecurityCard(
            enabled = state.biometricUnlockEnabled,
            available = state.biometricAvailable,
            onToggle = onBiometricUnlockToggle,
        )
    }
    VerticalSpacer(4.dp)
    SectionLabel(stringResource(Res.string.profile_section_about))
    AccountRow(
        iconRes = Res.drawable.ic_info,
        title = stringResource(Res.string.profile_oss_licenses),
        subtitle = stringResource(Res.string.profile_oss_licenses_caption),
        onClick = onOpenOssLicenses,
        showChevron = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileHeaderCard(state: ProfileState) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                initials = state.initials,
                size = 56.dp,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                textStyle = MaterialTheme.typography.titleMedium,
            )
            HorizontalSpacer(16.dp)
            Column {
                Text(
                    text = state.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2.dp)
                Text(
                    text =
                        if (state.email.isBlank()) {
                            "@${state.username}"
                        } else {
                            "@${state.username} · ${state.email}"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    iconRes: DrawableResource,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    showChevron: Boolean,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.then(clickModifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                HorizontalSpacer(8.dp)
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
) {
    val caption =
        when (themeMode) {
            ThemeMode.LIGHT -> stringResource(Res.string.profile_theme_caption_light)
            ThemeMode.DARK -> stringResource(Res.string.profile_theme_caption_dark)
            ThemeMode.SYSTEM -> stringResource(Res.string.profile_theme_caption_system)
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
                        text = stringResource(Res.string.profile_theme),
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
                    label = stringResource(Res.string.profile_theme_light),
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                ThemePill(
                    iconRes = Res.drawable.ic_dark_mode,
                    label = stringResource(Res.string.profile_theme_dark),
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
                ThemePill(
                    iconRes = Res.drawable.ic_settings,
                    label = stringResource(Res.string.profile_theme_system),
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
                        text = stringResource(Res.string.profile_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.profile_notifications_caption),
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
private fun SecurityCard(
    enabled: Boolean,
    available: Boolean,
    onToggle: (Boolean) -> Unit,
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
                    imageVector = vectorResource(Res.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                HorizontalSpacer(12.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.profile_biometric_unlock),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.profile_biometric_unlock_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalSpacer(8.dp)
                Switch(
                    // No enrolled biometric/credential -> keep off and disabled; the hint explains why.
                    checked = enabled && available,
                    onCheckedChange = onToggle,
                    enabled = available,
                )
            }
        }
        if (!available) {
            Text(
                text = stringResource(Res.string.profile_biometric_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
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
                text = stringResource(Res.string.profile_notifications_blocked),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.profile_open_settings))
            }
        }
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_logout),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.profile_sign_out),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeleteAccountButton(onClick: () -> Unit) {
    // Deliberately understated next to Sign out: a plain error-tinted text button, not a filled card.
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.profile_delete_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
