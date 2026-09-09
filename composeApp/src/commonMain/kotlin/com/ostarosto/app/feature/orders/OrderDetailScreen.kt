package com.ostarosto.app.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.LoadingBox
import com.ostarosto.app.core.designsystem.OrderProgressStepper
import com.ostarosto.app.core.designsystem.PriceBreakdown
import com.ostarosto.app.core.designsystem.SectionLabel
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.OrderProgress
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.selected?.takeIf { it.id == orderId }
    var confirmCancel by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        viewModel.loadDetail(orderId)
        while (true) {
            delay(20_000)
            val current = viewModel.state.value.selected
            if (current == null ||
                current.progress == OrderProgress.Completed ||
                current.progress == OrderProgress.Cancelled
            ) {
                break
            }
            viewModel.loadDetail(orderId)
        }
    }

    Scaffold(
        topBar = { BackTopBar("${Ar.orderNumber} ${order?.orderNumber ?: ""}", onBack) },
    ) { padding ->
        if (order == null) {
            LoadingBox()
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        order.displayStatus,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    order.estimatedArrival?.let {
                        Text(
                            "~ ${it.take(16).replace('T', ' ')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    OrderProgressStepper(order.progress, order.isPickup)
                }
            }

            // Items
            Card {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel(Ar.orderItems)
                    order.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${item.quantity}× ${item.nameAr.ifBlank { item.name }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (item.modifiers.isNotEmpty()) {
                                    Text(
                                        item.modifiers.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(money(item.lineTotal), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Summary
            Card {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel(Ar.orderSummary)
                    PriceBreakdown(
                        subtotal = order.subtotal,
                        discount = order.discount,
                        tax = order.tax,
                        deliveryFee = order.deliveryFee,
                        total = order.total,
                        showDelivery = !order.isPickup,
                    )
                    order.paymentMethod?.let {
                        Spacer(Modifier.height(10.dp))
                        IconRow(Icons.Default.Payments, "${Ar.paymentMethod}: $it")
                    }
                    order.address?.let {
                        Spacer(Modifier.height(6.dp))
                        IconRow(Icons.Default.LocationOn, it)
                    }
                }
            }

            if (order.progress == OrderProgress.Received) {
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) { Text(Ar.cancelOrder, fontWeight = FontWeight.SemiBold) }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(Ar.cancelOrderConfirmTitle) },
            text = { Text(Ar.cancelOrderConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCancel = false
                        viewModel.cancel(orderId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(Ar.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(Ar.dismiss) }
            },
        )
    }
}

@Composable
private fun IconRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
