package de.tabmates.features.tabgroup.presentation.navigation.grouppeople

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.share.LinkShareResult
import de.tabmates.core.presentation.share.rememberLinkSharer
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.AddPlaceholderButton
import de.tabmates.features.tabgroup.presentation.components.PersonRow
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.buildInviteUrl
import de.tabmates.features.tabgroup.presentation.navigation.groupdetail.shortInviteUrl
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.activity_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_confirm_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_done_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_name_hint
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_name_placeholder
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_add_placeholders
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_placeholders_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_section_members
import tabmatesapp.features.tabgroup.presentation.generated.resources.group_people_section_placeholders
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_copied
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_copy
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_link_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_rotate_cd
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_invite_share
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_owner_badge
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_pending_badge
import tabmatesapp.features.tabgroup.presentation.generated.resources.groups_detail_pending_not_claimed
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_check
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_close
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_content_copy
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_link
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_refresh

private val ContentMaxWidth = 600.dp

@Composable
fun GroupPeopleRoot(
    groupId: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: GroupPeopleViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is GroupPeopleEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }
    GroupPeopleScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun GroupPeopleScreen(
    state: GroupPeopleState,
    onAction: (GroupPeopleAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val scrollState = rememberScrollState()
    // Keeps the input and its hint clear of the keyboard. maxValue is a key on purpose: the shell
    // ime-pads itself (see App.kt), so opening the keyboard shrinks this viewport and grows the
    // scroll range a frame *after* the row appears — without re-running on that, the first scroll
    // lands short and the hint stays hidden. Each added name grows it again for the same reason.
    LaunchedEffect(state.isAddRowVisible, state.placeholders.size, scrollState.maxValue) {
        if (state.isAddRowVisible) scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PeopleSection(
            title = stringResource(Res.string.group_people_section_members),
            count = state.members.size,
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
        ) {
            state.members.forEach { person ->
                PersonRow(
                    name = person.displayName(),
                    initials = person.initials,
                    badge = person.badge?.label(),
                )
            }
        }
        PeopleSection(
            title = stringResource(Res.string.group_people_section_placeholders),
            count = state.placeholders.size,
            caption = stringResource(Res.string.group_people_placeholders_caption),
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
        ) {
            state.placeholders.forEach { person ->
                PersonRow(
                    name = person.name,
                    initials = person.initials,
                    badge = person.badge?.label(),
                    supporting = stringResource(Res.string.groups_detail_pending_not_claimed),
                )
            }
            if (state.isAddRowVisible) {
                NewPlaceholderRow(
                    textState = state.newNameTextState,
                    onSubmit = { onAction(GroupPeopleAction.SubmitName) },
                    onCancel = { onAction(GroupPeopleAction.CancelAdd) },
                )
            } else {
                AddPlaceholderButton(
                    label = stringResource(Res.string.group_people_add_placeholders),
                    onClick = { onAction(GroupPeopleAction.AddPlaceholderClick) },
                )
            }
        }
        if (state.inviteToken.isNotBlank()) {
            InviteLinkCard(
                inviteToken = state.inviteToken,
                snackbarHostState = snackbarHostState,
                onRotate = { onAction(GroupPeopleAction.RotateInvite) },
                modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
            )
        }
        VerticalSpacer(8.dp)
    }
}

/**
 * Sits where the next placeholder will appear. Enter commits and leaves the field open, so the row
 * that pops into the list above is the cue that another name can follow.
 */
@Composable
private fun NewPlaceholderRow(
    textState: TextFieldState,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TabMatesTextField(
            state = textState,
            placeholder = stringResource(Res.string.group_people_add_name_placeholder),
            supportingText = stringResource(Res.string.group_people_add_name_hint),
            // Deliberately never disabled while a name is in flight: dropping enabled takes the
            // focus with it, the keyboard closes, and the run of names is broken. The ViewModel
            // already rejects a second submit while one is running.
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            onKeyboardAction = { onSubmit() },
            trailingIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.group_people_add_done_cd),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
        )
        // Same job as the keyboard's enter key, for anyone not reaching for it.
        FilledIconButton(onClick = onSubmit) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.group_people_add_confirm_cd),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PeopleSection(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$title · $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun InviteLinkCard(
    inviteToken: String,
    snackbarHostState: SnackbarHostState,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkSharer = rememberLinkSharer()
    val inviteUrl = remember(inviteToken) { buildInviteUrl(inviteToken) }
    val copiedMessage = stringResource(Res.string.groups_detail_invite_copied)
    val scope = rememberCoroutineScope()
    // share() falls back to the clipboard on platforms without a share sheet, so both paths can
    // report a copy.
    val announce: (LinkShareResult) -> Unit = { result ->
        if (result == LinkShareResult.Copied) {
            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
        }
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_link),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                HorizontalSpacer(8.dp)
                Text(
                    text = stringResource(Res.string.groups_detail_invite_link_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRotate) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_refresh),
                        contentDescription = stringResource(Res.string.groups_detail_invite_rotate_cd),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            VerticalSpacer(8.dp)
            Text(
                text = shortInviteUrl(inviteToken),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalSpacer(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { announce(linkSharer.copy(inviteUrl)) }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_content_copy),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    HorizontalSpacer(6.dp)
                    Text(stringResource(Res.string.groups_detail_invite_copy))
                }
                FilledTonalButton(onClick = { announce(linkSharer.share(inviteUrl)) }) {
                    Text(stringResource(Res.string.groups_detail_invite_share))
                }
            }
        }
    }
}

@Composable
private fun GroupPerson.displayName(): String =
    if (isCurrentUser) stringResource(Res.string.activity_you) else name

@Composable
private fun PersonBadge.label(): String =
    when (this) {
        PersonBadge.OWNER -> stringResource(Res.string.groups_detail_owner_badge)
        PersonBadge.PENDING -> stringResource(Res.string.groups_detail_pending_badge)
    }
