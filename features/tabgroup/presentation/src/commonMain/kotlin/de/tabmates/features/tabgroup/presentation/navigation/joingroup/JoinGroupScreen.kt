package de.tabmates.features.tabgroup.presentation.navigation.joingroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.features.tabgroup.domain.models.GroupInvitePreview
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_flight
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_link_off
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_amount_tracked
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_expired_back_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_expired_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_expired_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_fallback_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_invited_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_join_as_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_member_count
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_option_claim
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_option_claim_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_option_new_member
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_option_new_member_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.join_group_submit

@Composable
internal fun JoinGroupScreen(
    state: JoinGroupState,
    onAction: (JoinGroupAction) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            JoinGroupState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            JoinGroupState.Expired -> ExpiredContent(onBack = onCloseClick)

            is JoinGroupState.Ready -> ReadyContent(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ReadyContent(
    state: JoinGroupState.Ready,
    onAction: (JoinGroupAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewCard(preview = state.preview)
            Text(
                text = stringResource(Res.string.join_group_join_as_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            JoinOptionRow(
                title = stringResource(Res.string.join_group_option_new_member),
                caption = stringResource(Res.string.join_group_option_new_member_caption),
                selected = state.selectedOption is JoinOption.NewMember,
                onClick = { onAction(JoinGroupAction.SelectOption(JoinOption.NewMember)) },
                leading = { AvatarBadge(initials = "?", outlined = false) },
            )
            state.preview?.placeholders?.forEach { placeholder ->
                val claimOption = JoinOption.ClaimPlaceholder(
                    placeholderId = placeholder.userId,
                    placeholderName = placeholder.username,
                )
                val selected =
                    (state.selectedOption as? JoinOption.ClaimPlaceholder)?.placeholderId == placeholder.userId
                JoinOptionRow(
                    title = stringResource(Res.string.join_group_option_claim, placeholder.username),
                    caption = stringResource(Res.string.join_group_option_claim_caption),
                    selected = selected,
                    onClick = { onAction(JoinGroupAction.SelectOption(claimOption)) },
                    leading = { AvatarBadge(initials = placeholder.initials, outlined = true) },
                )
            }
        }
        Button(
            onClick = { onAction(JoinGroupAction.Submit) },
            enabled = !state.isJoining,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(stringResource(Res.string.join_group_submit))
        }
    }
}

@Composable
private fun PreviewCard(preview: GroupInvitePreview?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_flight),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            VerticalSpacer(12.dp)
            Text(
                text = preview?.title ?: stringResource(Res.string.join_group_fallback_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (preview != null) {
                VerticalSpacer(6.dp)
                Text(
                    text = buildSubtitle(preview),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (preview.members.isNotEmpty()) {
                    VerticalSpacer(12.dp)
                    MemberAvatarRow(members = preview.members.take(MAX_PREVIEW_AVATARS))
                }
            }
        }
    }
}

@Composable
private fun buildSubtitle(preview: GroupInvitePreview): String {
    val invitedBy = stringResource(Res.string.join_group_invited_by, preview.inviterUsername)
    val memberCount = stringResource(Res.string.join_group_member_count, preview.memberCount)
    val tracked = stringResource(
        Res.string.join_group_amount_tracked,
        formatMoney(preview.totalSpent, preview.defaultCurrencyCode),
    )
    return "$invitedBy · $memberCount · $tracked"
}

private fun formatMoney(amount: Double, currencyCode: String): String {
    val cents = kotlin.math.round(amount * 100).toLong()
    val whole = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    return "$currencyCode $whole.$frac"
}

@Composable
private fun MemberAvatarRow(members: List<GroupParticipant>) {
    Row(horizontalArrangement = Arrangement.spacedBy(-8.dp)) {
        members.forEach { participant ->
            AvatarBadge(
                initials = participant.initials,
                outlined = false,
                size = 32.dp,
            )
        }
    }
}

@Composable
private fun AvatarBadge(
    initials: String,
    outlined: Boolean,
    size: Dp = 40.dp,
) {
    Surface(
        shape = CircleShape,
        color = if (outlined) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (outlined) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials.ifBlank { "?" },
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun JoinOptionRow(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun ExpiredContent(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_link_off),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            VerticalSpacer(24.dp)
            Text(
                text = stringResource(Res.string.join_group_expired_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            VerticalSpacer(8.dp)
            Text(
                text = stringResource(Res.string.join_group_expired_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(stringResource(Res.string.join_group_expired_back_home))
        }
    }
}

private const val MAX_PREVIEW_AVATARS = 5
