package de.tabmates.features.tabgroup.presentation.navigation.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.format.LocalNumberSymbols
import de.tabmates.core.presentation.format.amountEpsilon
import de.tabmates.features.tabgroup.domain.models.GroupParticipant
import de.tabmates.features.tabgroup.domain.models.SplitType
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.components.formatPercent
import de.tabmates.features.tabgroup.presentation.components.parseAmount
import de.tabmates.features.tabgroup.presentation.components.rememberAmountInputTransformation
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.add_entry_paid_by_you
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_balanced
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_left_to_assign
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_over_by
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_percent_total_must_equal_100
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_running_total_must_equal
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_shares_must_be_positive
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_tab_equal
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_tab_exact
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_tab_percentage
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_tab_shares
import tabmatesapp.features.tabgroup.presentation.generated.resources.split_screen_total
import kotlin.math.abs

@Composable
internal fun SplitEditorScreen(
    state: AddEntryState,
    onTypeChange: (SplitType) -> Unit,
    onToggleParticipant: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = parseAmount(state.amountTextState.text.toString()) ?: 0.0
    val tabs = remember { SplitType.entries.toList() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
    ) {
        VerticalSpacer(8.dp)
        SplitHero(
            total = total,
            state = state,
        )
        VerticalSpacer(16.dp)
        PrimaryTabRow(
            selectedTabIndex = tabs.indexOf(state.splitType).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            tabs.forEach { type ->
                Tab(
                    selected = type == state.splitType,
                    onClick = { onTypeChange(type) },
                    text = { Text(type.tabLabel()) },
                )
            }
        }
        VerticalSpacer(12.dp)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.members.forEach { member ->
                val input =
                    state.splitInputs.firstOrNull { it.participantId == member.userId } ?: return@forEach
                SplitMemberRow(
                    participant = member,
                    isCurrentUser = member.userId == state.currentUserId,
                    included = input.included,
                    splitType = state.splitType,
                    currencySymbol = state.entryCurrencySymbol,
                    currencyDecimals = state.entryCurrencyDecimalDigits,
                    equalShare =
                        if (state.splitType == SplitType.EQUAL) {
                            equalShareFor(input, state, total)
                        } else {
                            0.0
                        },
                    exactState = input.exactAmountState,
                    percentageState = input.percentageState,
                    sharesState = input.sharesState,
                    onToggle = { onToggleParticipant(member.userId) },
                )
            }
        }
        VerticalSpacer(16.dp)
        ValidationBanner(state = state, total = total)
        VerticalSpacer(24.dp)
    }
}

@Composable
private fun SplitHero(
    total: Double,
    state: AddEntryState,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                stringResource(
                    Res.string.split_screen_total,
                    formatMoney(state.entryCurrencySymbol, total, state.entryCurrencyDecimalDigits),
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(4.dp)
        SplitRemainderText(state = state, total = total)
    }
}

@Composable
private fun SplitRemainderText(
    state: AddEntryState,
    total: Double,
) {
    when (state.splitType) {
        SplitType.EXACT_AMOUNT -> {
            val assigned =
                state.splitInputs.sumOf { parseAmount(it.exactAmountState.text.toString()) ?: 0.0 }
            val remaining = total - assigned
            RemainderText(
                remaining = remaining,
                symbol = state.entryCurrencySymbol,
                decimals = state.entryCurrencyDecimalDigits,
            )
        }

        SplitType.PERCENTAGE -> {
            val assigned =
                state.splitInputs.sumOf { parseAmount(it.percentageState.text.toString()) ?: 0.0 }
            val remaining = 100.0 - assigned
            PercentageRemainderText(remaining = remaining)
        }

        SplitType.EQUAL, SplitType.SHARES -> {
            Text(
                text = stringResource(Res.string.split_screen_balanced),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemainderText(
    remaining: Double,
    symbol: String,
    decimals: Int,
) {
    val text =
        when {
            abs(remaining) < amountEpsilon(decimals) -> {
                stringResource(Res.string.split_screen_balanced)
            }

            remaining > 0 -> {
                stringResource(
                    Res.string.split_screen_left_to_assign,
                    formatMoney(symbol, remaining, decimals),
                )
            }

            else -> {
                stringResource(
                    Res.string.split_screen_over_by,
                    formatMoney(symbol, -remaining, decimals),
                )
            }
        }
    val color =
        if (abs(remaining) < amountEpsilon(decimals)) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PercentageRemainderText(remaining: Double) {
    val text =
        when {
            abs(remaining) < 0.01 -> {
                stringResource(Res.string.split_screen_balanced)
            }

            remaining > 0 -> {
                stringResource(
                    Res.string.split_screen_left_to_assign,
                    formatPercent(remaining),
                )
            }

            else -> {
                stringResource(
                    Res.string.split_screen_over_by,
                    formatPercent(-remaining),
                )
            }
        }
    val color =
        if (abs(remaining) < 0.01) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ValidationBanner(
    state: AddEntryState,
    total: Double,
) {
    val message: String? =
        when (state.splitType) {
            SplitType.EXACT_AMOUNT -> {
                val assigned =
                    state.splitInputs.sumOf { parseAmount(it.exactAmountState.text.toString()) ?: 0.0 }
                if (abs(assigned - total) > amountEpsilon(state.entryCurrencyDecimalDigits)) {
                    stringResource(
                        Res.string.split_screen_running_total_must_equal,
                        formatMoney(state.entryCurrencySymbol, total, state.entryCurrencyDecimalDigits),
                    )
                } else {
                    null
                }
            }

            SplitType.PERCENTAGE -> {
                val assigned =
                    state.splitInputs.sumOf { parseAmount(it.percentageState.text.toString()) ?: 0.0 }
                if (abs(assigned - 100.0) > 0.01) {
                    stringResource(Res.string.split_screen_percent_total_must_equal_100)
                } else {
                    null
                }
            }

            SplitType.SHARES -> {
                val totalShares =
                    state.splitInputs.sumOf {
                        it.sharesState.text
                            .toString()
                            .toIntOrNull() ?: 0
                    }
                if (totalShares <= 0) {
                    stringResource(Res.string.split_screen_shares_must_be_positive)
                } else {
                    null
                }
            }

            SplitType.EQUAL -> {
                null
            }
        }
    if (message != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SplitMemberRow(
    participant: GroupParticipant,
    isCurrentUser: Boolean,
    included: Boolean,
    splitType: SplitType,
    currencySymbol: String,
    currencyDecimals: Int,
    equalShare: Double,
    exactState: TextFieldState,
    percentageState: TextFieldState,
    sharesState: TextFieldState,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(initials = participant.initials)
        HorizontalSpacer(12.dp)
        Text(
            text = if (isCurrentUser) stringResource(Res.string.add_entry_paid_by_you) else participant.username,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        HorizontalSpacer(8.dp)
        when (splitType) {
            SplitType.EQUAL -> {
                EqualShareChip(
                    included = included,
                    share =
                        if (included) {
                            formatMoney(currencySymbol, equalShare, currencyDecimals)
                        } else {
                            "—"
                        },
                    onToggle = onToggle,
                )
            }

            SplitType.EXACT_AMOUNT -> {
                AmountInputField(
                    state = exactState,
                    prefix = currencySymbol,
                    keyboardType = KeyboardType.Decimal,
                    inputTransformation = rememberAmountInputTransformation(currencyDecimals),
                )
            }

            SplitType.PERCENTAGE -> {
                AmountInputField(
                    state = percentageState,
                    prefix = null,
                    suffix = LocalNumberSymbols.current.percentSymbol,
                    keyboardType = KeyboardType.Decimal,
                    inputTransformation = rememberAmountInputTransformation(PERCENTAGE_DECIMALS),
                )
            }

            SplitType.SHARES -> {
                AmountInputField(
                    state = sharesState,
                    prefix = null,
                    keyboardType = KeyboardType.Number,
                )
            }
        }
    }
}

@Composable
private fun EqualShareChip(
    included: Boolean,
    share: String,
    onToggle: () -> Unit,
) {
    FilterChip(
        selected = included,
        onClick = onToggle,
        label = {
            Text(
                text = share,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
private fun AmountInputField(
    state: TextFieldState,
    keyboardType: KeyboardType,
    prefix: String? = null,
    suffix: String? = null,
    inputTransformation: InputTransformation? = null,
) {
    Row(
        modifier = Modifier.widthIn(min = 120.dp, max = 180.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabMatesTextField(
            state = state,
            singleLine = true,
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
            inputTransformation = inputTransformation,
            placeholder = "${prefix.orEmpty()}0${suffix.orEmpty()}",
            modifier = Modifier.weight(1f),
        )
    }
}

private fun equalShareFor(
    input: ParticipantSplitInput,
    state: AddEntryState,
    total: Double,
): Double {
    if (!input.included) return 0.0
    val count = state.splitInputs.count { it.included }
    if (count == 0) return 0.0
    return total / count
}

@Composable
private fun SplitType.tabLabel(): String =
    when (this) {
        SplitType.EQUAL -> stringResource(Res.string.split_screen_tab_equal)
        SplitType.EXACT_AMOUNT -> stringResource(Res.string.split_screen_tab_exact)
        SplitType.PERCENTAGE -> stringResource(Res.string.split_screen_tab_percentage)
        SplitType.SHARES -> stringResource(Res.string.split_screen_tab_shares)
    }
