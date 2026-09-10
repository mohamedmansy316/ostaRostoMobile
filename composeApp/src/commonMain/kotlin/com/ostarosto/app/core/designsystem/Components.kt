package com.ostarosto.app.core.designsystem

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.OrderProgress
import kotlin.math.round

fun money(amount: Double): String {
    val negative = amount < 0
    val abs = kotlin.math.abs(amount)
    val rounded = round(abs * 100) / 100.0
    val whole = rounded.toLong()
    val cents = round((rounded - whole) * 100).toInt()
    val sign = if (negative && rounded != 0.0) "-" else ""
    val number = if (cents == 0) "$whole" else "$whole.${cents.toString().padStart(2, '0')}"
    return "$sign$number ${Ar.currency}"
}

@Composable
fun PriceText(amount: Double, modifier: Modifier = Modifier, strikethrough: Boolean = false) {
    Text(
        text = money(amount),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (strikethrough) FontWeight.Normal else FontWeight.SemiBold,
        color = if (strikethrough) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Standard top bar with a back chevron. Shared by every pushed screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Ar.back)
            }
        },
        actions = actions,
    )
}

/** A small bold section header with a bit of top spacing. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

/** One "label ........ amount" line in a price breakdown. */
@Composable
fun PriceRow(label: String, amount: Double, emphasize: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            money(amount),
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** subtotal / discount / VAT / delivery / total block. */
@Composable
fun PriceBreakdown(
    subtotal: Double,
    discount: Double,
    tax: Double,
    deliveryFee: Double,
    total: Double,
    showDelivery: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        PriceRow(Ar.subtotal, subtotal)
        if (discount > 0) PriceRow(Ar.discount, -discount)
        PriceRow(Ar.tax, tax)
        if (showDelivery) PriceRow(Ar.deliveryFee, deliveryFee)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        PriceRow(Ar.total, total, emphasize = true)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text(Ar.retry) }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = modifier) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - 1) }) { Icon(Icons.Default.Remove, contentDescription = "−") }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onChange(value + 1) }) { Icon(Icons.Default.Add, contentDescription = "+") }
    }
}

/** Horizontal 4-step tracker driven by [OrderProgress.step]. */
@Composable
fun OrderProgressStepper(progress: OrderProgress, isPickup: Boolean, modifier: Modifier = Modifier) {
    val labels = listOf(
        Ar.stepReceived,
        Ar.stepPreparing,
        if (isPickup) Ar.stepReady else Ar.stepOnTheWay,
        Ar.stepCompleted,
    )
    val active = progress.step

    if (progress == OrderProgress.Cancelled) {
        Text(Ar.stepCancelled, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium, modifier = modifier)
        return
    }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val step = index + 1
            val done = step <= active
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        else Text("$step", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (step == active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
