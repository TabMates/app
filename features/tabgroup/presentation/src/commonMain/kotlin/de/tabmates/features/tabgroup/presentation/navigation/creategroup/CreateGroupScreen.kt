package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.designsystem.buttons.TabMatesButton
import de.tabmates.core.designsystem.preview.PreviewScreenSizes
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.presentation.share.rememberLinkSharer
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.ParticipantType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_add_members_section
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_copy_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_default_currency
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_description_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_invite_email_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_invite_sent
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_link_copied
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_member_selected_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_name_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_pick_icon
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_placeholder_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_send_invite_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_submit_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_check
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chevron_right
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_content_copy
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_flight
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_link
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_palette
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_send

@Composable
fun CreateGroupRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inviteSentMessage = stringResource(Res.string.create_group_invite_sent)
    val linkCopiedMessage = stringResource(Res.string.create_group_link_copied)
    val linkSharer = rememberLinkSharer()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateGroupEvent.GroupCreated -> backStack.removeLastOrNull()
            CreateGroupEvent.InviteSent -> snackbarHostState.showSnackbar(inviteSentMessage)
            CreateGroupEvent.LinkCopied -> snackbarHostState.showSnackbar(linkCopiedMessage)
            CreateGroupEvent.LinkShared -> Unit
            is CreateGroupEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    CreateGroupScreen(
        state = state,
        onPickIconClick = viewModel::onPickIconClick,
        onCurrencyClick = viewModel::onCurrencyClick,
        onInviteByEmail = viewModel::onInviteByEmail,
        onCopyLink = {
            state.inviteLink?.let { link ->
                viewModel.onLinkShared(linkSharer.share(link))
            }
        },
        onToggleMember = viewModel::onToggleMember,
        onCreateClick = viewModel::onCreateClick,
        onCurrencySelected = viewModel::onCurrencySelected,
        onCurrencyPickerDismiss = viewModel::onCurrencyPickerDismiss,
        modifier = modifier,
    )
}

@Composable
private fun CreateGroupScreen(
    state: CreateGroupState,
    onPickIconClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onInviteByEmail: () -> Unit,
    onCopyLink: () -> Unit,
    onToggleMember: (String) -> Unit,
    onCreateClick: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onCurrencyPickerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isCurrencyPickerVisible) {
        CurrencyPickerBottomSheet(
            queryState = state.currencyQueryState,
            currencies = state.supportedCurrencies,
            selectedCode = state.defaultCurrencyCode,
            onCurrencySelected = onCurrencySelected,
            onDismiss = onCurrencyPickerDismiss,
        )
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
    ) {
        VerticalSpacer(8.dp)
        IconPickerRow(
            iconKey = state.iconKey,
            onPickIconClick = onPickIconClick,
        )
        VerticalSpacer(24.dp)
        TabMatesTextField(
            state = state.nameTextState,
            title = stringResource(Res.string.create_group_name_label),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        VerticalSpacer(16.dp)
        TabMatesTextField(
            state = state.descriptionTextState,
            title = stringResource(Res.string.create_group_description_placeholder),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        VerticalSpacer(16.dp)
        CurrencyRow(
            code = state.defaultCurrencyCode,
            symbol = state.supportedCurrencies.firstOrNull { it.code == state.defaultCurrencyCode }?.nativeSymbol,
            onClick = onCurrencyClick,
        )
        VerticalSpacer(24.dp)
        SectionHeader(text = stringResource(Res.string.create_group_add_members_section))
        VerticalSpacer(8.dp)
        EmailInviteRow(
            state = state.emailTextState,
            onSendClick = onInviteByEmail,
        )
        state.inviteLink?.let {
            VerticalSpacer(12.dp)
            InviteLinkCard(
                link = it,
                onCopyClick = onCopyLink,
            )
        }
        VerticalSpacer(12.dp)
        Text(
            text = stringResource(Res.string.create_group_placeholder_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(12.dp)
        MemberRow(
            members = state.members,
            onToggleMember = onToggleMember,
        )
        VerticalSpacer(24.dp)
        TabMatesButton(
            text = stringResource(Res.string.create_group_submit_action),
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isSubmitting,
        )
        VerticalSpacer(16.dp)
    }
}

@Composable
private fun IconPickerRow(
    iconKey: String?,
    onPickIconClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            iconKey?.let { key ->
                Icon(
                    imageVector = vectorResource(iconKeyToDrawable(key)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        PickIconButton(onClick = onPickIconClick)
    }
}

@Composable
private fun PickIconButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_palette),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.create_group_pick_icon),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    symbol: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        HorizontalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.create_group_default_currency),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = code,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = vectorResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmailInviteRow(
    state: TextFieldState,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabMatesTextField(
            state = state,
            title = stringResource(Res.string.create_group_invite_email_placeholder),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSendClick) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_send),
                contentDescription = stringResource(Res.string.create_group_send_invite_cd),
            )
        }
    }
}

@Composable
private fun InviteLinkCard(
    link: String,
    onCopyClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_link),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            HorizontalSpacer(12.dp)
            Text(
                text = link,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier =
                    Modifier
                        .clickable(onClick = onCopyClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_content_copy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(Res.string.create_group_copy_action),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    members: List<CreateGroupMember>,
    onToggleMember: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        members.forEach { member ->
            MemberItem(
                member = member,
                onToggle = { onToggleMember(member.id) },
            )
        }
    }
}

@Composable
private fun MemberItem(
    member: CreateGroupMember,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        HorizontalSpacer(12.dp)
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (member.isSelected) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.create_group_member_selected_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun iconKeyToDrawable(key: String): DrawableResource =
    when (key) {
        "airplane" -> Res.drawable.ic_flight
        else -> Res.drawable.ic_flight
    }

private fun previewState(): CreateGroupState =
    CreateGroupState(
        nameTextState = TextFieldState("Weekend in Lisbon"),
        defaultCurrencyCode = "EUR",
        iconKey = "airplane",
        inviteLink = "tabmates.app/g/XyzQ12",
        members =
            listOf(
                CreateGroupMember(
                    participant =
                        GroupParticipant(
                            userId = "alice",
                            username = "Alice",
                            participantType = ParticipantType.REGISTERED,
                        ),
                    isSelected = true,
                ),
                CreateGroupMember(
                    participant =
                        GroupParticipant(
                            userId = "ben",
                            username = "Ben",
                            participantType = ParticipantType.REGISTERED,
                        ),
                    isSelected = true,
                ),
            ),
    )

@PreviewThemes
@Composable
private fun CreateGroupScreenThemesPreview() {
    TabMatesTheme {
        Surface {
            CreateGroupScreen(
                state = previewState(),
                onPickIconClick = {},
                onCurrencyClick = {},
                onInviteByEmail = {},
                onCopyLink = {},
                onToggleMember = {},
                onCreateClick = {},
                onCurrencySelected = {},
                onCurrencyPickerDismiss = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CreateGroupScreenSizesPreview() {
    TabMatesTheme {
        Surface {
            CreateGroupScreen(
                state = previewState(),
                onPickIconClick = {},
                onCurrencyClick = {},
                onInviteByEmail = {},
                onCopyLink = {},
                onToggleMember = {},
                onCreateClick = {},
                onCurrencySelected = {},
                onCurrencyPickerDismiss = {},
            )
        }
    }
}
