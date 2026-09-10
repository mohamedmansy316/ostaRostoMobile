package com.ostarosto.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ostarosto.app.core.auth.AuthState
import com.ostarosto.app.core.auth.SessionManager
import com.ostarosto.app.core.designsystem.OstaRostoLogo
import com.ostarosto.app.core.designsystem.OstaRostoTheme
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.repository.AuthRepository
import com.ostarosto.app.feature.auth.AuthFlow
import com.ostarosto.app.feature.cart.CartScreen
import com.ostarosto.app.feature.cart.CartStore
import com.ostarosto.app.feature.checkout.CheckoutScreen
import com.ostarosto.app.feature.checkout.OrderConfirmationScreen
import com.ostarosto.app.feature.menu.MenuScreen
import com.ostarosto.app.feature.orders.OrderDetailScreen
import com.ostarosto.app.feature.orders.OrdersScreen
import com.ostarosto.app.feature.payment.PaymentWaitingScreen
import com.ostarosto.app.feature.productdetail.ProductDetailScreen
import com.ostarosto.app.feature.profile.ProfileScreen
import com.ostarosto.app.navigation.DeepLinkBus
import com.ostarosto.app.navigation.Route
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    OstaRostoTheme {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .crossfade(true)
                .build()
        }

        KoinContext {
            val session = koinInject<SessionManager>()
            val authRepo = koinInject<AuthRepository>()
            val state by session.state.collectAsState()

            LaunchedEffect(Unit) {
                if (state is AuthState.Unknown) {
                    val customer = (authRepo.me() as? ApiResult.Success)?.value
                    session.bootstrap(customer)
                }
            }

            when (state) {
                is AuthState.Unknown -> Splash()
                is AuthState.LoggedOut -> AuthFlow(onAuthenticated = {})
                is AuthState.LoggedIn -> MainGraph(onLogout = session::onSignedOut)
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainGraph(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val selection = koinInject<SelectionStore>()
    val cartStore = koinInject<CartStore>()
    val cart by cartStore.cart.collectAsState()

    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val showCartBar = currentRoute == Route.Menu || currentRoute == Route.Profile

    // Deep links (ostarosto://order/{id}) — cold start and warm start both land here.
    LaunchedEffect(Unit) {
        DeepLinkBus.uri.collect { uri ->
            if (uri != null) {
                DeepLinkBus.orderIdOf(uri)?.let { id ->
                    nav.navigate(Route.orderDetail(id)) { launchSingleTop = true }
                }
                DeepLinkBus.consume()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showCartBar) {
                Button(
                    onClick = { nav.navigate(Route.Cart) { launchSingleTop = true } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (cart.itemCount > 0) "${Ar.cart} (${cart.itemCount})" else Ar.cart)
                }
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = nav,
            startDestination = Route.Menu,
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            composable(Route.Menu) {
                MenuScreen(
                    onProduct = { ref -> nav.navigate(Route.productDetail(ref)) },
                    onOrders = { nav.navigate(Route.Orders) },
                    onProfile = { nav.navigate(Route.Profile) },
                )
            }

            composable(Route.Profile) {
                ProfileScreen(
                    onOrders = { nav.navigate(Route.Orders) },
                    onLogout = onLogout,
                )
            }

            composable(
                Route.ProductDetail,
                arguments = listOf(navArgument("productRef") { type = NavType.StringType }),
            ) { entry ->
                ProductDetailScreen(
                    productRef = entry.arguments?.getString("productRef").orEmpty(),
                    branchRef = selection.branchRef,
                    onBack = { nav.popBackStack() },
                    onAdded = { nav.popBackStack() },
                )
            }

            composable(Route.Cart) {
                CartScreen(
                    onBack = { nav.switchTab(Route.Menu) },
                    onCheckout = { nav.navigate(Route.Checkout) },
                )
            }

            composable(Route.Checkout) {
                CheckoutScreen(
                    onBack = { nav.popBackStack() },
                    onPlaced = { orderId ->
                        nav.navigate(Route.orderConfirmation(orderId)) { popUpTo(Route.Menu) }
                    },
                    onPaymentRequired = { reference ->
                        nav.navigate(Route.paymentWaiting(reference)) { popUpTo(Route.Menu) }
                    },
                )
            }

            composable(
                Route.PaymentWaiting,
                arguments = listOf(navArgument("reference") { type = NavType.StringType }),
            ) { entry ->
                val reference = entry.arguments?.getString("reference").orEmpty()
                PaymentWaitingScreen(
                    reference = reference,
                    onPaid = { orderId ->
                        nav.navigate(Route.orderConfirmation(orderId)) {
                            popUpTo(Route.Menu)
                        }
                    },
                    onFailed = { nav.navigate(Route.Cart) { popUpTo(Route.Menu) } },
                )
            }

            composable(
                Route.OrderConfirmation,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            ) { entry ->
                val orderId = entry.arguments?.getLong("orderId") ?: 0L
                OrderConfirmationScreen(
                    orderId = orderId,
                    onTrack = { id -> nav.navigate(Route.orderDetail(id)) { popUpTo(Route.Menu) } },
                    onBackToMenu = { nav.popBackStack(Route.Menu, inclusive = false) },
                )
            }

            composable(Route.Orders) {
                OrdersScreen(
                    onBack = { nav.popBackStack() },
                    onOrder = { id -> nav.navigate(Route.orderDetail(id)) },
                )
            }

            composable(
                Route.OrderDetail,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            ) { entry ->
                OrderDetailScreen(
                    orderId = entry.arguments?.getLong("orderId") ?: 0L,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun Splash() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OstaRostoLogo(modifier = Modifier.fillMaxWidth(0.6f).height(96.dp))
        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
    }
}
