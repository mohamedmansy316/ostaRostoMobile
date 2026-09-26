package com.ostarosto.app.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.ErrorBox
import com.ostarosto.app.core.designsystem.LoadingBox
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.OrderProgress
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOrder: (Long) -> Unit,
    onOpenCart: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = { BackTopBar(Ar.myOrders, onBack) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                state.orders.isEmpty() -> EmptyOrders()
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderCard(
                            order,
                            onClick = { onOrder(order.id) },
                            onReorder = { viewModel.reorder(order) },
                            reordering = state.reorderingId == order.id,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(state.reorderResult) {
        val result = state.reorderResult
        // Nothing worth reviewing for a fully-available reorder — go straight to
        // the cart to complete the order. Partial/empty results still need the
        // dialog below so the user knows which items didn't make it in.
        if (result != null && result.added > 0 && result.skipped == 0) {
            viewModel.consumeReorderResult()
            onOpenCart()
        }
    }

    state.reorderResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::consumeReorderResult,
            title = { Text(Ar.reorder) },
            text = {
                Text(
                    when {
                        result.added == 0 -> Ar.reorderUnavailable
                        else -> Ar.reorderPartial
                    },
                )
            },
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
}

@Composable
private fun EmptyOrders() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            Ar.noOrders,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One order summary: number + status up top, branch/type/date, total + chevron on the bottom row. */
@Composable
private fun OrderCard(order: Order, onClick: () -> Unit, onReorder: () -> Unit, reordering: Boolean) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, OstaColors.Hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "${Ar.orderNumber} ${order.orderNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val meta = listOfNotNull(order.branch?.name, order.createdAt?.take(10))
                    if (meta.isNotEmpty()) {
                        Text(
                            meta.joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusChip(order.displayStatus, order.progress)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = OstaColors.Hairline)
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (order.isPickup) Icons.Default.Storefront else Icons.Default.DeliveryDining,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (order.isPickup) Ar.pickup else Ar.delivery,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (order.items.isNotEmpty()) {
                        Text(
                            "•  ${order.items.size} ${Ar.items}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        money(order.total),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (order.items.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onReorder,
                    enabled = !reordering,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (reordering) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Ar.reorder, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatusChip(text: String, progress: OrderProgress, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = statusContainerColor(progress),
        contentColor = statusContentColor(progress),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun statusContainerColor(progress: OrderProgress): Color = when (progress) {
    OrderProgress.Cancelled -> MaterialTheme.colorScheme.errorContainer
    OrderProgress.Ready, OrderProgress.Completed -> OstaColors.Success.copy(alpha = 0.14f)
    OrderProgress.Received, OrderProgress.Preparing -> OstaColors.MaroonTint
}

@Composable
internal fun statusContentColor(progress: OrderProgress): Color = when (progress) {
    OrderProgress.Cancelled -> MaterialTheme.colorScheme.error
    OrderProgress.Ready, OrderProgress.Completed -> OstaColors.Success
    OrderProgress.Received, OrderProgress.Preparing -> MaterialTheme.colorScheme.primary
}
