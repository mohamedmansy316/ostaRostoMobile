package com.ostarosto.app.di

import com.ostarosto.app.core.auth.SessionManager
import com.ostarosto.app.core.auth.TokenStore
import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.HttpClientFactory
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.repository.AuthRepository
import com.ostarosto.app.data.repository.CartRepository
import com.ostarosto.app.data.repository.CatalogRepository
import com.ostarosto.app.data.repository.DeviceRepository
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.data.repository.PaymentRepository
import com.ostarosto.app.feature.auth.AuthViewModel
import com.ostarosto.app.feature.cart.CartStore
import com.ostarosto.app.feature.cart.CartViewModel
import com.ostarosto.app.feature.menu.MenuViewModel
import com.ostarosto.app.feature.orders.OrdersViewModel
import com.ostarosto.app.feature.payment.PaymentViewModel
import com.ostarosto.app.feature.productdetail.ProductDetailViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Provided by each platform: a secure `Settings` for the [TokenStore]. */
expect val platformModule: Module

val coreModule: Module = module {
    single { TokenStore(get()) }
    single { SessionManager(get()) }
    single { HttpClientFactory.create(get()) }
    single { ApiClient(get(), onUnauthorized = get<SessionManager>()::onSignedOut) }
}

val dataModule: Module = module {
    single { CartStore() }
    single { SelectionStore() }
    single { AuthRepository(get(), get()) }
    single { CatalogRepository(get()) }
    single { CartRepository(get()) }
    single { OrderRepository(get()) }
    single { PaymentRepository(get()) }
    single { DeviceRepository(get()) }
}

val viewModelModule: Module = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::MenuViewModel)
    viewModelOf(::ProductDetailViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::OrdersViewModel)
    viewModelOf(::PaymentViewModel)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, coreModule, dataModule, viewModelModule)
    }
}
