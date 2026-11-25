package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.local.SessionStore
import com.example.data.remote.ApiErrorParser
import com.example.data.remote.NetworkModule
import com.example.data.repository.BookStoreRepository
import com.example.ui.navigation.BookStoreApp
import com.example.ui.theme.BookStoreTheme
import com.example.ui.viewmodel.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.launch

private object BookStoreContainer {
    @Volatile
    private var repositoryInstance: BookStoreRepository? = null

    fun getRepository(appContext: Context): BookStoreRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: run {
                val sessionStore = SessionStore(appContext)
                val moshi = NetworkModule.createMoshi()
                val okHttpClient = NetworkModule.createOkHttpClient(sessionStore)
                val apiService = NetworkModule.createApiService(moshi, okHttpClient)
                val errorParser = ApiErrorParser(moshi)
                BookStoreRepository(appContext, apiService, sessionStore, errorParser).also {
                    repositoryInstance = it
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var cartCheckoutViewModel: CartCheckoutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val repository = BookStoreContainer.getRepository(appContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    AppViewModel::class.java -> AppViewModel(appContext, repository) as T
                    CatalogViewModel::class.java -> CatalogViewModel(repository) as T
                    BookDetailViewModel::class.java -> BookDetailViewModel(repository) as T
                    CartCheckoutViewModel::class.java -> CartCheckoutViewModel(repository) as T
                    OrdersViewModel::class.java -> OrdersViewModel(repository) as T
                    AuthProfileViewModel::class.java -> AuthProfileViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        val appViewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]
        val catalogViewModel = ViewModelProvider(this, factory)[CatalogViewModel::class.java]
        val bookDetailViewModel = ViewModelProvider(this, factory)[BookDetailViewModel::class.java]
        cartCheckoutViewModel = ViewModelProvider(this, factory)[CartCheckoutViewModel::class.java]
        val ordersViewModel = ViewModelProvider(this, factory)[OrdersViewModel::class.java]
        val authProfileViewModel = ViewModelProvider(this, factory)[AuthProfileViewModel::class.java]

        // Register Stripe PaymentSheet in ComponentActivity.onCreate per Section 5
        paymentSheet = PaymentSheet(this) { result ->
            cartCheckoutViewModel.onStripePaymentSheetResult(result)
        }

        lifecycleScope.launch {
            cartCheckoutViewModel.launchStripeSheetEvents.collect { pending ->
                val config = appViewModel.appConfig.value
                val publishableKey = config.stripePublishableKey
                if (!publishableKey.isNullOrBlank()) {
                    PaymentConfiguration.init(appContext, publishableKey)
                }
                val sheetConfig = PaymentSheet.Configuration(
                    merchantDisplayName = config.appName.ifBlank { "BookStore" },
                    allowsDelayedPaymentMethods = false
                )
                paymentSheet.presentWithPaymentIntent(pending.clientSecret, sheetConfig)
            }
        }

        setContent {
            BookStoreTheme {
                BookStoreApp(
                    appViewModel = appViewModel,
                    catalogViewModel = catalogViewModel,
                    bookDetailViewModel = bookDetailViewModel,
                    cartCheckoutViewModel = cartCheckoutViewModel,
                    ordersViewModel = ordersViewModel,
                    authProfileViewModel = authProfileViewModel
                )
            }
        }
    }
}
