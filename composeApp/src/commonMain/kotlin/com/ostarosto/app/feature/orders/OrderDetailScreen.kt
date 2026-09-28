package com.ostarosto.app.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.LoadingBox
import com.ostarosto.app.core.designsystem.OrderProgressStepper
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.designsystem.PriceBreakdown
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.OrderItem
import com.ostarosto.app.domain.model.OrderProgress
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.selected?.takeIf { it.id == orderId }
    var confirmCancel by remember { mutableStateOf(false) }

    LaunchedEffect(state.reorderResult) {
        val result = state.reorderResult
        if (result != null && result.added > 0 && result.skipped == 0) {
            viewModel.consumeReorderResult()
            onOpenCart()
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Hero Status Card ──────────────────────────────────────────────
            val (heroContainer, heroContent) = when (order.progress) {
                OrderProgress.Cancelled ->
                    MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                OrderProgress.Completed ->
                    OstaColors.Success.copy(alpha = 0.12f) to OstaColors.Success
                else ->
                    OstaColors.Maroon to Color.White
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = heroContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(heroContent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (order.isPickup) Icons.Default.Storefront else Icons.Default.DeliveryDining,
                                contentDescription = null,
                                tint = heroContent,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                order.displayStatus,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = heroContent,
                            )
                            val meta = listOfNotNull(order.branch?.name, order.createdAt?.take(10))
                            if (meta.isNotEmpty()) {
                                Text(
                                    meta.joinToString("  ·  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = heroContent.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }

                    order.estimatedArrival?.let {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(heroContent.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = heroContent, modifier = Modifier.size(16.dp))
                            Text(
                                "${Ar.estimatedTimeLabel}: ${it.take(16).replace('T', ' ')}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = heroContent,
                            )
                        }
                    }
                }
            }

            // ── Progress Tracker Card ─────────────────────────────────────────
            if (order.progress != OrderProgress.Cancelled) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                        OrderProgressStepper(order.progress, order.isPickup)
                    }
                }
            }

            // ── Items Card ────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeaderRow(Ar.orderItems)
                    Spacer(Modifier.height(10.dp))
                    order.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = OstaColors.Hairline)
                        OrderItemRow(item)
                    }
                }
            }

            // ── Summary Card ──────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeaderRow(Ar.orderSummary)
                    Spacer(Modifier.height(10.dp))
                    PriceBreakdown(
                        subtotal = order.subtotal,
                        discount = order.discount,
                        tax = order.tax,
                        deliveryFee = order.deliveryFee,
                        total = order.total,
                        showDelivery = !order.isPickup,
                    )
                    order.paymentMethod?.let {
                        Spacer(Modifier.height(14.dp))
                        IconRow(Icons.Default.Payments, "${Ar.paymentMethod}: $it")
                    }
                    order.address?.let {
                        Spacer(Modifier.height(8.dp))
                        IconRow(Icons.Default.LocationOn, it)
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────────────────
            if (order.items.isNotEmpty()) {
                Button(
                    onClick = { viewModel.reorder(order) },
                    enabled = state.reorderingId != order.id,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OstaColors.Maroon,
                        contentColor = Color.White,
                    ),
                ) {
                    if (state.reorderingId == order.id) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Ar.reorder, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (order.progress == OrderProgress.Received) {
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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

    state.reorderResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::consumeReorderResult,
            title = { Text(Ar.reorder) },
            text = { Text(if (result.added == 0) Ar.reorderUnavailable else Ar.reorderPartial) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.consumeReorderResult()
                        if (result.added > 0) onOpenCart()
                    },
                ) { Text(Ar.ok) }
            },
        )
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
private fun SectionHeaderRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .background(OstaColors.Maroon, RoundedCornerShape(2.dp))
        )
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(64.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OstaColors.MaroonTint),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.image.isNullOrBlank()) {
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = OstaColors.Maroon,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(20.dp)
                    .background(OstaColors.Maroon, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.nameAr.ifBlank { item.name },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (item.modifiers.isNotEmpty()) {
                Text(
                    item.modifiers.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            money(item.lineTotal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = OstaColors.Maroon,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun IconRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(OstaColors.MaroonTint, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = OstaColors.Maroon,
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
