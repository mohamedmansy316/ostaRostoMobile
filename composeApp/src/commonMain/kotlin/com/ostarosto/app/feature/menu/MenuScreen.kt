package com.ostarosto.app.feature.menu

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
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
import com.ostarosto.app.domain.model.HeroSlide
import com.ostarosto.app.domain.model.OrderType
import com.ostarosto.app.domain.model.Product
import com.ostarosto.app.domain.model.PromotionalBanner
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onProduct: (String) -> Unit,
    onOrders: () -> Unit,
    onProfile: () -> Unit,
    onSelectDeliveryLocation: () -> Unit,
    viewModel: MenuViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deliveryDestination by viewModel.deliveryDestination.collectAsStateWithLifecycle()
    var branchSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        TextButton(onClick = { branchSheet = true }, contentPadding = PaddingValues(0.dp)) {
                            Text(state.selectedBranch?.name ?: Ar.chooseBranch)
                        }
                        if (state.orderType == OrderType.Delivery) {
                            Text(
                                deliveryDestination?.let { "${Ar.deliveringTo}: ${it.displayText}" }
                                    ?: Ar.noDeliveryLocationYet,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOrders) {
                        Box(
                            Modifier.size(36.dp).background(OstaColors.Yellow, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = Ar.myOrders,
                                tint = OstaColors.Maroon,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    IconButton(onClick = onProfile) {
                        Box(
                            Modifier.size(36.dp).background(OstaColors.Yellow, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = Ar.profile,
                                tint = OstaColors.Maroon,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::onSearch,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                placeholder = { Text(Ar.search, style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(52.dp),
            )

            if (state.categories.isNotEmpty() && state.search.isBlank()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "home") {
                        HomeCard(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                        )
                    }
                }
            }

            val isHome = state.search.isBlank() && state.selectedCategoryId == null
            val showHeader = isHome && (state.heroSlides.isNotEmpty() || state.promotionalBanners.isNotEmpty())

            // headerHeightPx: measured height of the fixed hero+banner backdrop.
            // revealPx: how much of it is currently showing above the white sheet —
            // headerHeightPx = fully revealed, 0 = the sheet has slid all the way up
            // over it. Driven by nested scroll so it tracks the drag 1:1, then snaps
            // fully open or closed once the user lets go.
            var headerHeightPx by remember { mutableFloatStateOf(0f) }
            var revealPx by remember { mutableFloatStateOf(0f) }
            var headerMeasured by remember { mutableStateOf(false) }

            LaunchedEffect(state.selectedCategoryId, state.search) {
                gridState.scrollToItem(0)
                if (showHeader) revealPx = headerHeightPx
            }

            val nestedScrollConnection = remember(showHeader) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (!showHeader) return Offset.Zero
                        // Only intercept downward scroll to hide the header before the grid scrolls.
                        // Upward reveal is handled in onPostScroll so the header only shows
                        // once the grid is fully scrolled back to the top.
                        if (available.y < 0) {
                            val previous = revealPx
                            revealPx = (revealPx + available.y).coerceIn(0f, headerHeightPx)
                            return Offset(0f, revealPx - previous)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        if (!showHeader) return Offset.Zero
                        // Reveal the header only after the grid has consumed all it can scrolling up
                        // (available.y > 0 means the grid is already at the top).
                        if (available.y > 0) {
                            val previous = revealPx
                            revealPx = (revealPx + available.y).coerceIn(0f, headerHeightPx)
                            return Offset(0f, revealPx - previous)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (showHeader && revealPx != 0f && revealPx != headerHeightPx) {
                            val target = if (revealPx > headerHeightPx / 2f) headerHeightPx else 0f
                            animate(initialValue = revealPx, targetValue = target, animationSpec = tween(220)) { value, _ ->
                                revealPx = value
                            }
                        }
                        return super.onPostFling(consumed, available)
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                // The hero/banners never move — they sit fixed behind everything else,
                // like CSS `background-attachment: fixed`.
                if (showHeader) {
                    Column(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .onGloballyPositioned {
                                headerHeightPx = it.size.height.toFloat()
                                if (!headerMeasured) {
                                    revealPx = headerHeightPx
                                    headerMeasured = true
                                }
                            },
                    ) {
                        if (state.promotionalBanners.isNotEmpty()) {
                            PromotionalBannersRow(state.promotionalBanners, Modifier.padding(top = 4.dp, bottom = 12.dp, start = 16.dp, end = 16.dp))
                        } else if (state.heroSlides.isNotEmpty()) {
                            HeroSection(state.heroSlides, Modifier.padding(top = 4.dp, bottom = 12.dp, start = 16.dp, end = 16.dp))
                        }
                    }
                }

                // The white sheet: slides up over the fixed header as the user scrolls.
                Box(
                    Modifier
                        .fillMaxSize()
                        .offset { if (showHeader) IntOffset(0, revealPx.roundToInt()) else IntOffset.Zero }
                        .then(if (showHeader) Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) else Modifier)
                        .background(MaterialTheme.colorScheme.background)
                        .nestedScroll(nestedScrollConnection),
                ) {
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
                            else -> ProductGrid(
                                products = products,
                                gridState = gridState,
                                onProduct = onProduct,
                                onAddToCart = viewModel::addToCart,
                            )
                        }
                    }
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
                Spacer(Modifier.height(16.dp))
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        deliveryDestination?.let { "${Ar.deliveringTo}: ${it.displayText}" }
                            ?: Ar.noDeliveryLocationYet,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (deliveryDestination != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (deliveryDestination != null) OstaColors.Maroon else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                            // Pickup needs nothing further; delivery needs a pin dropped
                            // and matched against this branch's zones before checkout.
                            if (state.orderType == OrderType.Delivery) onSelectDeliveryLocation()
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

/** Leading tab that clears the category filter and brings back the hero/promo home view. */
@Composable
private fun HomeCard(selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(84.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) OstaColors.Maroon else OstaColors.Yellow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 2.dp),
        border = if (selected) BorderStroke(1.5.dp, OstaColors.MaroonDark) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Ar.home,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else OstaColors.Maroon,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(3.dp))
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = if (selected) Color.White else OstaColors.Maroon,
                    modifier = Modifier.size(26.dp),
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
        modifier = Modifier.width(84.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) OstaColors.Maroon else OstaColors.Yellow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 2.dp),
        border = if (selected) BorderStroke(1.5.dp, OstaColors.MaroonDark) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else OstaColors.Maroon,
                maxLines = 1,
                minLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(3.dp))
            if (!category.image.isNullOrBlank()) {
                AsyncImage(
                    model = category.image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                )
            } else {
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    gridState: LazyGridState,
    onProduct: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    LazyVerticalGrid(
        // Adaptive so tablets / landscape get 3+ columns instead of two huge cards.
        columns = GridCells.Adaptive(minSize = 170.dp),
        state = gridState,
        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(products, key = { it.id }) { product ->
            ProductCard(
                product,
                onClick = { onProduct(product.foodicsId ?: product.id.toString()) },
                onAddToCart = { onAddToCart(product) },
            )
        }
    }
}

/** Auto-advancing full-bleed slideshow mirroring the website's home-page hero. */
@Composable
private fun HeroSection(slides: List<HeroSlide>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)),
        ) { page ->
            val slide = slides[page]
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = slide.imageUrl,
                    contentDescription = slide.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 60f,
                        ),
                    ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    if (!slide.badgeText.isNullOrBlank()) {
                        Pill(
                            text = slide.badgeText,
                            container = OstaColors.Yellow,
                            content = OstaColors.Maroon,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (!slide.title.isNullOrBlank()) {
                        Text(
                            slide.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!slide.description.isNullOrBlank()) {
                        Text(
                            slide.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (slides.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                slides.indices.forEach { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        Modifier.padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .background(
                                if (active) OstaColors.Maroon else OstaColors.MaroonTint,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** Auto-advancing full-bleed promo slideshow, full width like the hero section. */
@Composable
private fun PromotionalBannersRow(banners: List<PromotionalBanner>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(banners.size) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)),
        ) { page ->
            val banner = banners[page]
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (banners.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                banners.indices.forEach { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        Modifier.padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .background(
                                if (active) OstaColors.Maroon else OstaColors.MaroonTint,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** Vertical card matching the Rockets menu: image on top, price, full-width add button. */
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit, onAddToCart: () -> Unit) {
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
            Box(Modifier.fillMaxWidth().height(96.dp)) {
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

            Column(Modifier.fillMaxWidth().padding(8.dp)) {
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
                Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { if (product.hasModifiers) onClick() else onAddToCart() },
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
                    contentPadding = PaddingValues(vertical = 6.dp),
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
