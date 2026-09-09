package com.ostarosto.app.feature.productdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.ErrorBox
import com.ostarosto.app.core.designsystem.LoadingBox
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.QuantityStepper
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.ModifierOption
import com.ostarosto.app.domain.model.Product
import org.koin.compose.viewmodel.koinViewModel
import com.ostarosto.app.domain.model.Modifier as MenuModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productRef: String,
    branchRef: String?,
    onBack: () -> Unit,
    onAdded: () -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(productRef) { viewModel.load(productRef, branchRef) }
    LaunchedEffect(state.added) { if (state.added) onAdded() }

    Scaffold(
        topBar = { BackTopBar(state.product?.name ?: Ar.menu, onBack) },
        bottomBar = {
            state.product?.let { product ->
                Surface(shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuantityStepper(state.quantity, viewModel::setQuantity)
                        PrimaryButton(
                            text = "${Ar.addToCart}  •  ${money(state.lineUnitPrice(product) * state.quantity)}",
                            onClick = viewModel::addToCart,
                            enabled = !product.isOutOfStock,
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox()
            state.error != null && state.product == null ->
                ErrorBox(state.error!!, onRetry = { viewModel.load(productRef, branchRef) })
            state.product != null -> ProductContent(
                state = state,
                product = state.product!!,
                contentPadding = padding,
                onToggle = viewModel::toggleOption,
                onNotes = viewModel::setNotes,
            )
        }
    }
}

@Composable
private fun ProductContent(
    state: ProductDetailUiState,
    product: Product,
    contentPadding: PaddingValues,
    onToggle: (MenuModifier, ModifierOption) -> Unit,
    onNotes: (String) -> Unit,
) {
    val unsatisfied = state.unsatisfied(product)

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()),
    ) {
        AsyncImage(
            model = product.image,
            contentDescription = product.name,
            modifier = Modifier.fillMaxWidth().height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Column(Modifier.padding(16.dp)) {
            Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!product.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    product.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                money(product.price),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (state.showErrors && unsatisfied.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        state.error ?: "يرجى اختيار الخيارات المطلوبة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            }

            product.modifiers.forEach { modifier ->
                Spacer(Modifier.height(18.dp))
                ModifierGroup(
                    modifier = modifier,
                    selectedIds = state.selections[modifier.id].orEmpty(),
                    showError = state.showErrors && modifier.id in unsatisfied,
                    onToggle = { option -> onToggle(modifier, option) },
                )
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotes,
                label = { Text(Ar.orderNotes) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModifierGroup(
    modifier: MenuModifier,
    selectedIds: Set<String>,
    showError: Boolean,
    onToggle: (ModifierOption) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        RequirementTag(required = modifier.isRequired(), error = showError, max = modifier.maxSelection)
    }
    if (modifier.maxSelection > 1) {
        Text(
            "اختر حتى ${modifier.maxSelection}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modifier.options.forEach { option ->
            val selected = option.foodicsId in selectedIds
            FilterChip(
                selected = selected,
                enabled = !option.isOutOfStock,
                onClick = { onToggle(option) },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            option.name,
                            textDecoration = if (option.isOutOfStock) TextDecoration.LineThrough else null,
                        )
                        if (option.price > 0) {
                            Text(
                                "  +${money(option.price).removeSuffix(" ${Ar.currency}")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun RequirementTag(required: Boolean, error: Boolean, max: Int) {
    val (text, container, content) = when {
        error -> Triple("مطلوب", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        required -> Triple("مطلوب", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
        else -> Triple("اختياري", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
