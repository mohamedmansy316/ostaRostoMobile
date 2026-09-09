package com.ostarosto.app.feature.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.PriceText
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.QuantityStepper
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BackTopBar(Ar.cart, onBack) },
        bottomBar = {
            if (!cart.isEmpty) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Ar.subtotal, fontWeight = FontWeight.SemiBold)
                        PriceText(cart.subtotal)
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(text = Ar.checkout, onClick = onCheckout)
                }
            }
        },
    ) { padding ->
        if (cart.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Ar.emptyCart, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(cart.lines, key = { it.lineId }) { line ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(line.name, style = MaterialTheme.typography.titleSmall)
                            if (line.selectedOptions.isNotEmpty()) {
                                Text(
                                    line.selectedOptions.joinToString(" • ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(money(line.lineTotal), style = MaterialTheme.typography.bodyMedium)
                        }
                        QuantityStepper(line.quantity, { viewModel.setQuantity(line.lineId, it) })
                        IconButton(onClick = { viewModel.remove(line.lineId) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
