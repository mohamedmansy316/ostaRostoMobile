package com.ostarosto.app.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.PriceBreakdown
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.SectionLabel
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.core.platform.UrlOpener
import com.ostarosto.app.domain.model.OrderType
import com.ostarosto.app.domain.model.PaymentChannel
import com.ostarosto.app.feature.cart.CartViewModel
import com.ostarosto.app.navigation.LocationPickBus
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPlaced: (Long) -> Unit,
    onPaymentRequired: (String) -> Unit,
    onPickLocation: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
    urlOpener: UrlOpener = koinInject(),
) {
    val state by viewModel.checkout.collectAsStateWithLifecycle()
    val pickedLocation by LocationPickBus.result.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.prepareCheckout() }
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let {
            viewModel.setLocation(it.latitude, it.longitude, it.availability)
            LocationPickBus.consume()
        }
    }
    LaunchedEffect(state.placedOrder) {
        state.placedOrder?.let {
            onPlaced(it.id)
            viewModel.consumePlacedOrder()
        }
    }
    LaunchedEffect(state.paymentUrl) {
        val url = state.paymentUrl
        val ref = state.paymentReference
        if (url != null && ref != null) {
            if (urlOpener.open(url)) {
                onPaymentRequired(ref)
                viewModel.consumePaymentRedirect()
            } else {
                // Order is already placed; keep the user here with a way to retry
                // opening the payment page instead of stranding them.
                viewModel.reportCheckoutError(Ar.couldNotOpenPayment)
            }
        }
    }

    Scaffold(
        topBar = { BackTopBar(Ar.checkout, onBack) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                state.totals?.let { t ->
                    PriceBreakdown(
                        subtotal = t.subtotal,
                        discount = t.discount,
                        tax = t.tax,
                        deliveryFee = t.deliveryFee,
                        total = t.total,
                        showDelivery = state.orderType == OrderType.Delivery,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                val pendingPaymentUrl = state.paymentUrl
                val pendingPaymentRef = state.paymentReference
                if (pendingPaymentUrl != null) {
                    // The order is placed but the browser could not be opened —
                    // offer a retry rather than re-submitting the order.
                    PrimaryButton(
                        text = Ar.reopenPayment,
                        onClick = {
                            if (urlOpener.open(pendingPaymentUrl)) {
                                pendingPaymentRef?.let(onPaymentRequired)
                                viewModel.consumePaymentRedirect()
                            } else {
                                viewModel.reportCheckoutError(Ar.couldNotOpenPayment)
                            }
                        },
                    )
                } else {
                    PrimaryButton(
                        text = Ar.placeOrder,
                        onClick = viewModel::placeOrder,
                        enabled = state.canPlace,
                        loading = state.placing,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text("${Ar.chooseBranch}: ${state.branchName}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.orderType == OrderType.Pickup,
                    onClick = { viewModel.setOrderType(OrderType.Pickup) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(Ar.pickup) }
                SegmentedButton(
                    selected = state.orderType == OrderType.Delivery,
                    onClick = { viewModel.setOrderType(OrderType.Delivery) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(Ar.delivery) }
            }

            if (state.orderType == OrderType.Delivery) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onPickLocation)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.deliveryZoneName ?: Ar.selectLocationOnMap,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.latitude != null) {
                            Text(
                                "${Ar.deliveryFee}: ${money(state.deliveryFee ?: 0.0)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (state.latitude != null) {
                        Text(
                            Ar.changeLocation,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::setAddress,
                    label = { Text(Ar.addressDetailsOptional) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text(Ar.orderNotes) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel(Ar.paymentMethod)
            state.paymentMethods.forEach { method ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(
                            selected = state.selectedPaymentId == method.id && state.selectedChannel == PaymentChannel.Card,
                            onClick = { viewModel.selectPayment(method.id, PaymentChannel.Card) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.selectedPaymentId == method.id && state.selectedChannel == PaymentChannel.Card,
                        onClick = { viewModel.selectPayment(method.id, PaymentChannel.Card) },
                    )
                    Text(method.name)
                }
                if (method.isWalletEnabled) {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = state.selectedPaymentId == method.id && state.selectedChannel == PaymentChannel.Wallet,
                                onClick = { viewModel.selectPayment(method.id, PaymentChannel.Wallet) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.selectedPaymentId == method.id && state.selectedChannel == PaymentChannel.Wallet,
                            onClick = { viewModel.selectPayment(method.id, PaymentChannel.Wallet) },
                        )
                        Text(Ar.walletPayment)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
