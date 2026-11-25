package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.CartDto
import com.example.data.remote.dto.OrderDetailDto
import com.example.data.remote.dto.PaymentIntentDataDto
import com.example.data.repository.BookStoreRepository
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingStripeCheckout(
    val orderId: Long,
    val clientSecret: String,
    val paymentIntentId: String?
)

data class CartCheckoutUiState(
    val isLoadingCart: Boolean = false,
    val busyItemBookId: Long? = null,
    val isCouponBusy: Boolean = false,
    val couponError: String? = null,
    val isCheckoutBusy: Boolean = false,
    val checkoutError: String? = null,
    val checkoutFieldErrors: Map<String, List<String>> = emptyMap(),
    val activePendingCheckout: PendingStripeCheckout? = null,
    val completedOrderId: Long? = null,
    val confirmedOrderDetail: OrderDetailDto? = null
)

class CartCheckoutViewModel(
    private val repository: BookStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartCheckoutUiState())
    val uiState: StateFlow<CartCheckoutUiState> = _uiState.asStateFlow()

    val cart: StateFlow<CartDto> = repository.cart
    val currentUser = repository.currentUser
    val appConfig = repository.appConfig

    private val _launchStripeSheetEvents = MutableSharedFlow<PendingStripeCheckout>(extraBufferCapacity = 2)
    val launchStripeSheetEvents: SharedFlow<PendingStripeCheckout> = _launchStripeSheetEvents.asSharedFlow()

    fun refreshCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCart = true) }
            repository.fetchCart()
            _uiState.update { it.copy(isLoadingCart = false) }
        }
    }

    fun updateQuantity(bookId: Long, newQuantity: Int) {
        if (newQuantity < 1) {
            removeItem(bookId)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyItemBookId = bookId) }
            when (val res = repository.updateCartItemQuantity(bookId, newQuantity)) {
                is ApiResult.Success -> {
                    res.message?.let { repository.emitSnackbar(it) }
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _uiState.update { it.copy(busyItemBookId = null) }
        }
    }

    fun removeItem(bookId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyItemBookId = bookId) }
            when (val res = repository.removeCartItem(bookId)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Volume removed from shopping bag.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _uiState.update { it.copy(busyItemBookId = null) }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCart = true) }
            when (val res = repository.clearCart()) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Shopping bag cleared.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _uiState.update { it.copy(isLoadingCart = false) }
        }
    }

    fun applyCoupon(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(couponError = "Please enter a coupon code.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCouponBusy = true, couponError = null) }
            when (val res = repository.applyCoupon(code)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isCouponBusy = false, couponError = null) }
                    repository.emitSnackbar(res.message ?: "Coupon applied successfully.")
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isCouponBusy = false, couponError = res.error.message) }
                }
            }
        }
    }

    fun removeCoupon() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCouponBusy = true, couponError = null) }
            when (val res = repository.removeCoupon()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isCouponBusy = false) }
                    repository.emitSnackbar(res.message ?: "Coupon removed.")
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isCouponBusy = false, couponError = res.error.message) }
                }
            }
        }
    }

    fun resetCheckoutState() {
        _uiState.update {
            it.copy(
                isCheckoutBusy = false,
                checkoutError = null,
                checkoutFieldErrors = emptyMap(),
                activePendingCheckout = null,
                completedOrderId = null,
                confirmedOrderDetail = null
            )
        }
    }

    /**
     * Step 1 of Mobile Checkout: POST /api/v1/checkout/create-payment-intent
     * Handles both 100% free coupon orders (free_order == true) and Stripe PaymentSheet flow.
     * On retries, discards any previous order_id and uses the freshly returned order_id & client_secret.
     */
    fun initiateCheckout(
        name: String,
        address: String,
        city: String,
        postalCode: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckoutBusy = true,
                    checkoutError = null,
                    checkoutFieldErrors = emptyMap(),
                    completedOrderId = null
                )
            }
            when (val res = repository.createPaymentIntent(name, address, city, postalCode)) {
                is ApiResult.Success -> {
                    handlePaymentIntentCreated(res.data, res.message)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckoutBusy = false,
                            checkoutError = if (res.error.isEmailNotVerified || res.error.isPaymentGatewayUnavailable) {
                                null
                            } else {
                                res.error.message
                            },
                            checkoutFieldErrors = res.error.fieldErrors
                        )
                    }
                }
            }
        }
    }

    private fun handlePaymentIntentCreated(data: PaymentIntentDataDto, message: String?) {
        // Case 1: 100% Free Coupon Order — Stripe skipped, order is immediately paid
        if (data.freeOrder) {
            _uiState.update {
                it.copy(
                    isCheckoutBusy = false,
                    activePendingCheckout = null,
                    completedOrderId = data.orderId
                )
            }
            repository.emitSnackbar(message ?: "Order placed successfully.")
            return
        }

        // Case 2: Standard Paid Order -> launch Stripe PaymentSheet
        val clientSecret = data.clientSecret
        if (clientSecret.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isCheckoutBusy = false,
                    checkoutError = "Payment client secret was missing from server response."
                )
            }
            return
        }

        val paymentIntentId = clientSecret.substringBefore("_secret_").takeIf { it.startsWith("pi_") }
        val pending = PendingStripeCheckout(
            orderId = data.orderId,
            clientSecret = clientSecret,
            paymentIntentId = paymentIntentId
        )
        _uiState.update {
            it.copy(
                isCheckoutBusy = false,
                activePendingCheckout = pending
            )
        }
        _launchStripeSheetEvents.tryEmit(pending)
    }

    /**
     * Step 2 of Mobile Checkout: handles Stripe PaymentSheetResult callback.
     * On Completed -> calls POST /api/v1/checkout/confirm with order_id and optional payment_intent_id.
     * On confirm failure/network drop -> verifies order status via GET /api/v1/orders/{id} checking payment_status == "paid".
     */
    fun onStripePaymentSheetResult(result: PaymentSheetResult) {
        val pending = _uiState.value.activePendingCheckout ?: return
        when (result) {
            is PaymentSheetResult.Completed -> {
                confirmOrderWithFallback(pending)
            }
            is PaymentSheetResult.Canceled -> {
                repository.emitSnackbar("Payment cancelled. Reserved stock expires after ${appConfig.value.reservationMinutes} minutes.")
            }
            is PaymentSheetResult.Failed -> {
                val msg = result.error.localizedMessage ?: "Payment failed."
                _uiState.update { it.copy(checkoutError = "Payment failed: $msg") }
            }
        }
    }

    private fun confirmOrderWithFallback(pending: PendingStripeCheckout) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true, checkoutError = null) }
            when (val res = repository.confirmCheckout(pending.orderId, pending.paymentIntentId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckoutBusy = false,
                            activePendingCheckout = null,
                            completedOrderId = res.data.id,
                            confirmedOrderDetail = res.data
                        )
                    }
                    repository.emitSnackbar(res.message ?: "Payment confirmed! Your order has been placed.")
                }
                is ApiResult.Error -> {
                    // Per Section 5: verify actual order status via GET /orders/{id} before declaring error
                    verifyOrderStatusAfterConfirmFailure(pending.orderId, res.error.message)
                }
            }
        }
    }

    private suspend fun verifyOrderStatusAfterConfirmFailure(orderId: Long, originalError: String) {
        when (val verify = repository.fetchOrderDetail(orderId)) {
            is ApiResult.Success -> {
                if (verify.data.paymentStatus == "paid") {
                    repository.fetchCart(silent = true)
                    _uiState.update {
                        it.copy(
                            isCheckoutBusy = false,
                            activePendingCheckout = null,
                            completedOrderId = verify.data.id,
                            confirmedOrderDetail = verify.data
                        )
                    }
                    repository.emitSnackbar("Payment confirmed! Your order has been placed.")
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckoutBusy = false,
                            checkoutError = "Payment verification incomplete ($originalError). Please check My Orders."
                        )
                    }
                }
            }
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        isCheckoutBusy = false,
                        checkoutError = "Network error while verifying order. Please check My Orders to verify status."
                    )
                }
            }
        }
    }
}
