package com.ostarosto.app.feature.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.PriceBreakdown
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.SectionLabel
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.core.platform.UrlOpener
import com.ostarosto.app.domain.model.OrderType
import com.ostarosto.app.feature.cart.CartViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPlaced: (Long) -> Unit,
    onPaymentRequired: (String) -> Unit,
    viewModel: CartViewModel = koinViewModel(),
    urlOpener: UrlOpener = koinInject(),
) {
    val state by viewModel.checkout.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.prepareCheckout() }
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
            urlOpener.open(url)
            onPaymentRequired(ref)
            viewModel.consumePaymentRedirect()
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
                PrimaryButton(
                    text = Ar.placeOrder,
                    onClick = viewModel::placeOrder,
                    enabled = state.canPlace,
                    loading = state.placing,
                )
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
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::setAddress,
                    label = { Text(Ar.deliveryAddress) },
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
                            selected = state.selectedPaymentId == method.id,
                            onClick = { viewModel.selectPayment(method.id) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.selectedPaymentId == method.id,
                        onClick = { viewModel.selectPayment(method.id) },
                    )
                    Text(method.name)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
