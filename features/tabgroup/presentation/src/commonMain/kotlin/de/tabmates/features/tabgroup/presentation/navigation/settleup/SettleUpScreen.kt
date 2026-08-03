package de.tabmates.features.tabgroup.presentation.navigation.settleup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tabmates.core.designsystem.spacer.HorizontalSpacer
import de.tabmates.core.designsystem.spacer.VerticalSpacer
import de.tabmates.core.designsystem.textfields.TabMatesTextField
import de.tabmates.core.presentation.format.amountEpsilon
import de.tabmates.core.presentation.util.ObserveAsEvents
import de.tabmates.features.tabgroup.presentation.components.formatMoney
import de.tabmates.features.tabgroup.presentation.components.parseAmount
import de.tabmates.features.tabgroup.presentation.components.rememberAmountInputTransformation
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.UserAvatar
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_all_settled_caption
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_all_settled_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_dialog_cancel
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_dialog_confirm
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_dialog_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_dialog_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_error_required
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_error_too_high
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_amount_label
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_default_title
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_other_members_header
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_owes
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_pay_to
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_record_action
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_recorded
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_recorded_other
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_subtitle
import tabmatesapp.features.tabgroup.presentation.generated.resources.settle_up_your_payments_header

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
    val pendingSettlement by viewModel.pendingSettlement.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettleUpEvent.PaymentRecorded -> {
                val message =
                    if (event.isFromCurrentUser) {
                        getString(Res.string.settle_up_recorded, event.toName)
                    } else {
                        getString(Res.string.settle_up_recorded_other, event.fromName, event.toName)
                    }
                snackbarHostState.showSnackbar(message)
            }

            is SettleUpEvent.Error -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    SettleUpScreen(
        state = state,
        onPaymentRowClick = viewModel::onPaymentRowClick,
        modifier = modifier,
    )

    pendingSettlement?.let { payment ->
        val defaultTitle = stringResource(Res.string.settle_up_default_title)
        SettleAmountBottomSheet(
            payment = payment,
            amountState = viewModel.settleAmountTextState,
            currencySymbol = state.currencySymbol,
            currencyDecimals = state.currencyDecimalDigits,
            onConfirm = { viewModel.onSettleConfirm(defaultTitle) },
            onDismiss = viewModel::onSettleDismiss,
        )
    }
}

@Composable
internal fun SettleUpScreen(
    state: SettleUpState,
    onPaymentRowClick: (SettleUpPayment) -> Unit,
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
            val (yourPayments, otherPayments) =
                state.payments.partition { it.fromUserId == state.currentUserId }
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
                if (yourPayments.isNotEmpty()) {
                    VerticalSpacer(16.dp)
                    SectionHeader(stringResource(Res.string.settle_up_your_payments_header))
                    VerticalSpacer(8.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        yourPayments.forEach { payment ->
                            PaymentRow(
                                payment = payment,
                                isFromCurrentUser = true,
                                currencySymbol = state.currencySymbol,
                                currencyDecimals = state.currencyDecimalDigits,
                                onSettleClick = { onPaymentRowClick(payment) },
                            )
                        }
                    }
                }
                if (otherPayments.isNotEmpty()) {
                    VerticalSpacer(16.dp)
                    SectionHeader(stringResource(Res.string.settle_up_other_members_header))
                    VerticalSpacer(8.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        otherPayments.forEach { payment ->
                            PaymentRow(
                                payment = payment,
                                isFromCurrentUser = false,
                                currencySymbol = state.currencySymbol,
                                currencyDecimals = state.currencyDecimalDigits,
                                onSettleClick = { onPaymentRowClick(payment) },
                            )
                        }
                    }
                }
                VerticalSpacer(24.dp)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PaymentRow(
    payment: SettleUpPayment,
    isFromCurrentUser: Boolean,
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
            UserAvatar(initials = if (isFromCurrentUser) payment.toInitials else payment.fromInitials)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        if (isFromCurrentUser) {
                            stringResource(Res.string.settle_up_pay_to, payment.toName)
                        } else {
                            stringResource(Res.string.settle_up_owes, payment.fromName, payment.toName)
                        },
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SettleAmountBottomSheet(
    payment: SettleUpPayment,
    amountState: TextFieldState,
    currencySymbol: String,
    currencyDecimals: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val epsilon = amountEpsilon(currencyDecimals)
    val parsed = parseAmount(amountState.text.toString())
    val isTooHigh = parsed != null && parsed > payment.amount + epsilon
    val isInvalid = parsed == null || parsed <= 0.0
    val supportingText =
        when {
            isInvalid -> {
                stringResource(Res.string.settle_up_amount_error_required)
            }

            isTooHigh -> {
                stringResource(
                    Res.string.settle_up_amount_error_too_high,
                    formatMoney(currencySymbol, payment.amount, currencyDecimals),
                )
            }

            else -> {
                null
            }
        }
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.settle_up_amount_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(4.dp)
            Text(
                text =
                    stringResource(
                        Res.string.settle_up_amount_dialog_subtitle,
                        payment.fromName,
                        payment.toName,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer(16.dp)
            TabMatesTextField(
                state = amountState,
                title = stringResource(Res.string.settle_up_amount_label),
                singleLine = true,
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                inputTransformation = rememberAmountInputTransformation(currencyDecimals),
                onKeyboardAction = { if (supportingText == null) onConfirm() },
                isError = supportingText != null,
                supportingText = supportingText,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
            Button(
                onClick = onConfirm,
                enabled = supportingText == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settle_up_amount_dialog_confirm))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settle_up_amount_dialog_cancel))
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
