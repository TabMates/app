package de.tabmates.features.tabgroup.presentation.navigation.profile

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.text.SectionLabel
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import de.tabmates.features.tabgroup.presentation.navigation.settings.SettingsRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_alternate_email
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_lock
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_logout
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_mail
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_person_add
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_email
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_password
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_account_username
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_delete_account
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_password_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_section_account
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_create_account
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_dismiss
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_text_one
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_text_other
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_guest_dialog_text
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_guest_dialog_text_pending_writes
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_guest_dialog_text_pending_writes_other
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_sign_out_guest_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.profile_upgrade_account_title

/** Widest the column ever gets; beyond it the content centres instead of stretching. */
private val ContentMaxWidth = 560.dp

@Composable
fun ProfileRoot(
    onEditUsername: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // An upgrade is confirmed in a mail client, i.e. off this screen — so the only way this device
    // learns the account is no longer a guest one is by asking again on the way back in.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccount()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // Session is cleared on sign-out; the app shell observes it and returns to Welcome.
            ProfileEvent.SignedOut -> Unit
        }
    }

    ProfileScreen(
        state = state,
        onEditUsername = onEditUsername,
        onChangeEmail = onChangeEmail,
        onChangePassword = onChangePassword,
        // Also reached from the sign-out dialog, which has to close behind the navigation.
        onUpgradeAccount = {
            viewModel.onDismissSignOutDialog()
            onUpgradeAccount()
        },
        onDeleteAccount = onDeleteAccount,
        onSignOut = viewModel::onSignOutClick,
        onDismissSignOutDialog = viewModel::onDismissSignOutDialog,
        onConfirmSignOut = viewModel::onConfirmSignOut,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onEditUsername: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDismissSignOutDialog: () -> Unit = {},
    onConfirmSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // The initial state claims a registered account, so rendering it would show a guest the
    // email/password rows for a frame and then swap them for the upgrade row.
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
            ProfileIdentity(state)
            VerticalSpacer(8.dp)
            SectionLabel(stringResource(Res.string.profile_section_account))
            // Guests have no email or password rows to show; this takes their place and is the only
            // entry point to the upgrade from here, so it stays at the top of the section.
            if (!state.isRegistered) {
                val pendingMigrationEmail = state.pendingMigrationEmail
                if (pendingMigrationEmail != null) {
                    PendingMigrationBanner(
                        email = pendingMigrationEmail,
                        onOpenUpgrade = onUpgradeAccount,
                    )
                } else {
                    SettingsRow(
                        iconRes = Res.drawable.ic_person_add,
                        title = stringResource(Res.string.profile_upgrade_account_title),
                        subtitle = stringResource(Res.string.profile_upgrade_account_subtitle),
                        onClick = onUpgradeAccount,
                        showChevron = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SettingsRow(
                iconRes = Res.drawable.ic_alternate_email,
                title = stringResource(Res.string.profile_account_username),
                subtitle = state.username,
                onClick = onEditUsername,
                showChevron = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isRegistered) {
                SettingsRow(
                    iconRes = Res.drawable.ic_mail,
                    title = stringResource(Res.string.profile_account_email),
                    subtitle = state.email,
                    onClick = onChangeEmail,
                    showChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsRow(
                    iconRes = Res.drawable.ic_lock,
                    title = stringResource(Res.string.profile_account_password),
                    subtitle = stringResource(Res.string.profile_password_subtitle),
                    onClick = onChangePassword,
                    showChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            VerticalSpacer(8.dp)
            SignOutButton(onClick = onSignOut)
            DeleteAccountButton(onClick = onDeleteAccount)
            VerticalSpacer(16.dp)
        }
    }

    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = onDismissSignOutDialog,
            title = {
                Text(
                    text =
                        if (state.isRegistered) {
                            stringResource(Res.string.profile_sign_out_dialog_title)
                        } else {
                            stringResource(Res.string.profile_sign_out_guest_dialog_title)
                        },
                )
            },
            text = { Text(text = signOutDialogText(state)) },
            confirmButton = {
                // Guests are steered to the upgrade instead: signing out destroys the only way back
                // into the account, so the safe action gets the emphasis position.
                if (state.isRegistered) {
                    TextButton(onClick = onConfirmSignOut) {
                        Text(text = stringResource(Res.string.profile_sign_out_dialog_confirm))
                    }
                } else {
                    TextButton(onClick = onUpgradeAccount) {
                        Text(text = stringResource(Res.string.profile_sign_out_dialog_create_account))
                    }
                }
            },
            dismissButton = {
                if (state.isRegistered) {
                    TextButton(onClick = onDismissSignOutDialog) {
                        Text(text = stringResource(Res.string.profile_sign_out_dialog_dismiss))
                    }
                } else {
                    TextButton(onClick = onConfirmSignOut) {
                        Text(text = stringResource(Res.string.profile_sign_out_dialog_confirm))
                    }
                }
            },
        )
    }
}

@Composable
private fun signOutDialogText(state: ProfileState): String {
    return when {
        state.isRegistered && state.pendingWriteCount == 1 -> {
            stringResource(Res.string.profile_sign_out_dialog_text_one)
        }

        state.isRegistered -> {
            stringResource(Res.string.profile_sign_out_dialog_text_other, state.pendingWriteCount)
        }

        state.pendingWriteCount == 0 -> {
            stringResource(Res.string.profile_sign_out_guest_dialog_text)
        }

        state.pendingWriteCount == 1 -> {
            stringResource(
                Res.string.profile_sign_out_guest_dialog_text_pending_writes,
                state.pendingWriteCount,
            )
        }

        else -> {
            stringResource(
                Res.string.profile_sign_out_guest_dialog_text_pending_writes_other,
                state.pendingWriteCount,
            )
        }
    }
}

/**
 * Who you are, stated once at the top.
 *
 * Unboxed on purpose: the top bar already says "Profile", so a second framed card here would only
 * repeat the card that was tapped to get in.
 */
@Composable
private fun ProfileIdentity(state: ProfileState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(
            initials = state.initials,
            size = 72.dp,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            textStyle = MaterialTheme.typography.titleLarge,
        )
        VerticalSpacer(12.dp)
        Text(
            text = state.username,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.email.isNotBlank()) {
            VerticalSpacer(2.dp)
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

@PreviewThemes
@Composable
private fun ProfileScreenPreview() {
    TabMatesTheme {
        Surface {
            ProfileScreen(
                state =
                    ProfileState(
                        isLoading = false,
                        username = "Dennis Bauer",
                        email = "dennis@tabmates.de",
                        initials = "DE",
                    ),
                onEditUsername = {},
                onChangeEmail = {},
                onChangePassword = {},
                onUpgradeAccount = {},
                onDeleteAccount = {},
                onSignOut = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun ProfileScreenGuestPreview() {
    TabMatesTheme {
        Surface {
            ProfileScreen(
                state =
                    ProfileState(
                        isLoading = false,
                        username = "Dennis",
                        initials = "DE",
                        isRegistered = false,
                    ),
                onEditUsername = {},
                onChangeEmail = {},
                onChangePassword = {},
                onUpgradeAccount = {},
                onDeleteAccount = {},
                onSignOut = {},
            )
        }
    }
}
