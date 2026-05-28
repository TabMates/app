package de.tabmates.features.tabgroup.presentation.navigation.settleup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_all_settled_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_all_settled_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_pay_to
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_record_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_recorded
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_subtitle

@Composable
fun SettleUpRoot(
    groupId: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SettleUpViewModel =
        koinViewModel(
            key = groupId,
            parameters = { parametersOf(groupId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettleUpEvent.PaymentRecorded -> {
                snackbarHostState.showSnackbar(getString(Res.string.settle_up_recorded, event.toName))
            }

            is SettleUpEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    SettleUpScreen(
        state = state,
        onSettleClick = viewModel::onSettleClick,
        modifier = modifier,
    )
}

@Composable
internal fun SettleUpScreen(
    state: SettleUpState,
    onSettleClick: (SettleUpPayment) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.payments.isEmpty() -> {
            AllSettled(modifier = modifier)
        }

        else -> {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
            ) {
                VerticalSpacer(8.dp)
                Text(
                    text = stringResource(Res.string.settle_up_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalSpacer(16.dp)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.payments.forEach { payment ->
                        PaymentRow(
                            payment = payment,
                            currencySymbol = state.currencySymbol,
                            currencyDecimals = state.currencyDecimalDigits,
                            onSettleClick = { onSettleClick(payment) },
                        )
                    }
                }
                VerticalSpacer(24.dp)
            }
        }
    }
}

@Composable
private fun PaymentRow(
    payment: SettleUpPayment,
    currencySymbol: String,
    currencyDecimals: Int,
    onSettleClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(initials = payment.toInitials)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settle_up_pay_to, payment.toName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMoney(currencySymbol, payment.amount, currencyDecimals),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            HorizontalSpacer(12.dp)
            Button(
                onClick = onSettleClick,
                enabled = !payment.isSettling,
            ) {
                if (payment.isSettling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(Res.string.settle_up_record_action))
                }
            }
        }
    }
}

@Composable
private fun AllSettled(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.settle_up_all_settled_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.settle_up_all_settled_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
