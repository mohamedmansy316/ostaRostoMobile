package com.ostarosto.app.feature.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.designsystem.PriceText
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.QuantityStepper
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.CartLine
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
        topBar = {
            BackTopBar(
                if (cart.isEmpty) Ar.cart else "${Ar.cart} • ${cart.itemCount} ${Ar.items}",
                onBack,
            )
        },
        bottomBar = {
            if (!cart.isEmpty) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Ar.subtotal, fontWeight = FontWeight.SemiBold)
                            PriceText(cart.subtotal)
                        }
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(
                            text = Ar.checkout,
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (cart.isEmpty) {
            EmptyCart(Modifier.fillMaxSize().padding(padding), onBrowseMenu = onBack)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cart.lines, key = { it.lineId }) { line ->
                    CartLineCard(
                        line = line,
                        onQuantityChange = { viewModel.setQuantity(line.lineId, it) },
                        onRemove = { viewModel.remove(line.lineId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(OstaColors.MaroonTint),
            ) {
                if (!line.image.isNullOrBlank()) {
                    AsyncImage(
                        model = line.image,
                        contentDescription = line.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        line.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OstaColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (line.selectedOptions.isNotEmpty()) {
                    Text(
                        line.selectedOptions.joinToString(" • ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!line.notes.isNullOrBlank()) {
                    Text(
                        line.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PriceText(line.lineTotal)
                    QuantityStepper(line.quantity, onQuantityChange)
                }
            }
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier, onBrowseMenu: () -> Unit) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).background(OstaColors.MaroonTint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = OstaColors.Maroon,
                modifier = Modifier.size(42.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            Ar.emptyCart,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OstaColors.Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            Ar.emptyCartHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = Ar.browseMenu,
            onClick = onBrowseMenu,
            modifier = Modifier.fillMaxWidth(0.7f),
        )
    }
}
