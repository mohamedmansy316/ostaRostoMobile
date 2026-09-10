package com.ostarosto.app.feature.menu

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ostarosto.app.core.designsystem.ErrorBox
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.Category
import com.ostarosto.app.domain.model.Product
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onProduct: (String) -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    viewModel: MenuViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var branchSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    // Category is now a pure client-side filter — snap back to the top when the
    // tab or search changes so it reads like a toggle, not a scrolled list.
    LaunchedEffect(state.selectedCategoryId, state.search) { gridState.scrollToItem(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { branchSheet = true }) {
                        Text(state.selectedBranch?.name ?: Ar.chooseBranch)
                    }
                },
                actions = {
                    IconButton(onClick = onOrders) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = Ar.myOrders)
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.Person, contentDescription = Ar.profile)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::onSearch,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(Ar.search) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val products = state.products
                when {
                    state.loadingShell || (state.loadingCatalog && state.allProducts.isEmpty()) -> SkeletonList()
                    state.error != null && state.allProducts.isEmpty() -> ErrorBox(state.error!!, onRetry = viewModel::load)
                    products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Ar.noProducts, style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> ProductGrid(products, gridState, onProduct)
                }
            }
        }
    }

    if (branchSheet) {
        ModalBottomSheet(
            onDismissRequest = { branchSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                    Ar.chooseBranch,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OstaColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    Ar.currentBranch + ": " + (state.selectedBranch?.name ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.branches, key = { it.id }) { branch ->
                    BranchRow(
                        branch = branch,
                        selected = state.selectedBranch?.id == branch.id,
                        onClick = {
                            viewModel.selectBranch(branch)
                            branchSheet = false
                        },
                    )
                }
            }
        }
    }
}

/** One selectable branch: location badge, name + address, open/closed status, tick when active. */
@Composable
private fun BranchRow(branch: Branch, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) OstaColors.MaroonTint else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else OstaColors.MaroonTint,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    branch.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OstaColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!branch.address.isNullOrBlank()) {
                    Text(
                        branch.address!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(7.dp).background(
                            if (branch.isOpen) OstaColors.Success else MaterialTheme.colorScheme.error,
                            CircleShape,
                        ),
                    )
                    val hours = branch.hoursToday
                        ?.takeIf { branch.isOpen && !it.from.isNullOrBlank() && !it.to.isNullOrBlank() }
                        ?.let { "${it.from} - ${it.to}" }
                    Text(
                        hours ?: if (branch.isOpen) Ar.openNow else Ar.closedNow,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (branch.isOpen) OstaColors.Success else MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Ar.currentBranch,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Rockets-style tile: bold name on top, product image filling the body. */
@Composable
private fun CategoryCard(category: Category, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(104.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) OstaColors.MaroonTint else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 2.dp),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else OstaColors.Ink,
                maxLines = 2,
                minLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (!category.image.isNullOrBlank()) {
                AsyncImage(
                    model = category.image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                )
            } else {
                Spacer(Modifier.height(52.dp))
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    gridState: LazyGridState,
    onProduct: (String) -> Unit,
) {
    LazyVerticalGrid(
        // Adaptive so tablets / landscape get 3+ columns instead of two huge cards.
        columns = GridCells.Adaptive(minSize = 170.dp),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(products, key = { it.id }) { product ->
            ProductCard(product, onClick = { onProduct(product.foodicsId ?: product.id.toString()) })
        }
    }
}

/** Vertical card matching the Rockets menu: image on top, price, full-width add button. */
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val discountPercent = product.originalPrice
        ?.takeIf { product.hasDiscount && it > product.price }
        ?.let { (((it - product.price) / it) * 100).toInt() }
        ?.takeIf { it > 0 }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(128.dp)) {
                AsyncImage(
                    model = product.image,
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (product.isOutOfStock) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                        .then(if (product.isOutOfStock) Modifier.alpha(0.7f) else Modifier),
                )
                if (product.isOutOfStock) {
                    Pill(
                        text = Ar.outOfStock,
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                } else if (discountPercent != null) {
                    Pill(
                        text = "-$discountPercent%",
                        container = MaterialTheme.colorScheme.tertiary,
                        content = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                }
            }

            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OstaColors.Ink,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    product.description?.takeIf { it.isNotBlank() }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        money(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (discountPercent != null && product.originalPrice != null) {
                        Text(
                            money(product.originalPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClick,
                    enabled = !product.isOutOfStock,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (product.isOutOfStock) MaterialTheme.colorScheme.outlineVariant
                        else MaterialTheme.colorScheme.primary,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(
                        Ar.addToCart,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        shadowElevation = 1.dp,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SkeletonList() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Column(Modifier.fillMaxSize().padding(16.dp).alpha(alpha)) {
        repeat(3) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
                    Column(
                        Modifier.weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(10.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(110.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(0.85f).height(13.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.55f).height(11.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(0.35f).height(16.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(38.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)))
                    }
                }
            }
        }
    }
}
