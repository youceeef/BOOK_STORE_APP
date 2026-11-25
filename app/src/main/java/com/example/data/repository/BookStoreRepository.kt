package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.example.data.local.SessionStore
import com.example.data.remote.ApiError
import com.example.data.remote.ApiErrorParser
import com.example.data.remote.ApiResult
import com.example.data.remote.BookStoreApiService
import com.example.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

sealed class SessionEvent {
    data class Unauthorized(val message: String) : SessionEvent()
    data class EmailNotVerified(val message: String) : SessionEvent()
    data class PaymentGatewayUnavailable(val message: String) : SessionEvent()
    data class SnackbarMessage(val message: String) : SessionEvent()
    data class WelcomeUser(val userName: String, val avatarUrl: String? = null) : SessionEvent()
    data class GoodbyeUser(val userName: String, val avatarUrl: String? = null) : SessionEvent()
}

data class ProfileUpdateInput(
    val name: String,
    val email: String,
    val phone: String?,
    val bio: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val removeAvatar: Boolean = false,
    val avatarUri: Uri? = null
)

class BookStoreRepository(
    private val appContext: Context,
    private val api: BookStoreApiService,
    private val sessionStore: SessionStore,
    private val errorParser: ApiErrorParser
) {

    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    private val _appConfig = MutableStateFlow(AppConfigDto())
    val appConfig: StateFlow<AppConfigDto> = _appConfig.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _cart = MutableStateFlow(CartDto())
    val cart: StateFlow<CartDto> = _cart.asStateFlow()

    private val _wishlist = MutableStateFlow<List<BookDto>>(emptyList())
    val wishlist: StateFlow<List<BookDto>> = _wishlist.asStateFlow()

    val wishlistBookIds: StateFlow<Set<Long>> = _wishlist
        .map { books -> books.map { it.id }.toSet() }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    val authToken: StateFlow<String?> = sessionStore.token
    val locale: StateFlow<String> = sessionStore.locale

    fun isLoggedIn(): Boolean = !sessionStore.token.value.isNullOrBlank()

    suspend fun awaitSessionReady() {
        sessionStore.awaitHydration()
    }

    suspend fun setLocale(locale: String) {
        sessionStore.setLocale(locale)
    }

    fun emitSnackbar(message: String) {
        _sessionEvents.tryEmit(SessionEvent.SnackbarMessage(message))
    }

    fun emitWelcomeUser(userName: String, avatarUrl: String? = null) {
        _sessionEvents.tryEmit(SessionEvent.WelcomeUser(userName, avatarUrl))
    }

    fun emitGoodbyeUser(userName: String, avatarUrl: String? = null) {
        _sessionEvents.tryEmit(SessionEvent.GoodbyeUser(userName, avatarUrl))
    }

    // =========================================================================
    // 4.1 System & Catalog (Public)
    // =========================================================================

    suspend fun fetchConfig(): ApiResult<AppConfigDto> {
        val result = executeDataCall(emitGlobalEvents = false) { api.getConfig() }
        if (result is ApiResult.Success) {
            _appConfig.value = result.data
        }
        return result
    }

    suspend fun fetchCategories(): ApiResult<List<CategoryDto>> =
        executeDataCall(emitGlobalEvents = false) { api.getCategories() }

    suspend fun fetchBooks(
        page: Int = 1,
        category: String? = null,
        search: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        inStock: Boolean? = null,
        sort: String? = null
    ): ApiResult<PaginatedResponse<BookDto>> =
        executeRawCall(emitGlobalEvents = false) {
            api.getBooks(
                page = page,
                category = category?.takeIf { it.isNotBlank() },
                search = search?.takeIf { it.isNotBlank() },
                minPrice = minPrice,
                maxPrice = maxPrice,
                inStock = if (inStock == true) 1 else null,
                sort = sort?.takeIf { it.isNotBlank() }
            )
        }

    suspend fun fetchBookDetail(slugOrId: String): ApiResult<BookDetailDto> =
        executeDataCall(emitGlobalEvents = false) { api.getBookDetail(slugOrId) }

    suspend fun fetchBookReviews(slugOrId: String, page: Int = 1): ApiResult<PaginatedResponse<ReviewDto>> =
        executeRawCall(emitGlobalEvents = false) { api.getBookReviews(slugOrId, page) }

    // =========================================================================
    // 4.2 Authentication
    // =========================================================================

    private fun currentDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val combined = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
        return "$combined (Android ${Build.VERSION.RELEASE})".trim()
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): ApiResult<AuthDataDto> {
        val result = executeDataCall(emitGlobalEvents = false) {
            api.register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    password = password,
                    passwordConfirmation = passwordConfirmation,
                    deviceName = currentDeviceName()
                )
            )
        }
        if (result is ApiResult.Success) {
            sessionStore.saveToken(result.data.token)
            _currentUser.value = result.data.user
            refreshAuthenticatedData()
        }
        return result
    }

    suspend fun login(email: String, password: String): ApiResult<AuthDataDto> {
        val result = executeDataCall(emitGlobalEvents = false) {
            api.login(
                LoginRequest(
                    email = email.trim().lowercase(),
                    password = password,
                    deviceName = currentDeviceName()
                )
            )
        }
        if (result is ApiResult.Success) {
            sessionStore.saveToken(result.data.token)
            _currentUser.value = result.data.user
            refreshAuthenticatedData()
        }
        return result
    }

    suspend fun forgotPassword(email: String): ApiResult<String> =
        executeMessageCall(emitGlobalEvents = false) {
            api.forgotPassword(ForgotPasswordRequest(email = email.trim().lowercase()))
        }

    suspend fun resetPassword(
        token: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): ApiResult<String> =
        executeMessageCall(emitGlobalEvents = false) {
            api.resetPassword(
                ResetPasswordRequest(
                    token = token.trim(),
                    email = email.trim().lowercase(),
                    password = password,
                    passwordConfirmation = passwordConfirmation
                )
            )
        }

    suspend fun logout(): ApiResult<String> {
        val result = executeMessageCall(emitGlobalEvents = false) { api.logout() }
        clearLocalSession()
        return result
    }

    suspend fun fetchAuthUser(silent: Boolean = false): ApiResult<UserDto> {
        if (!isLoggedIn()) {
            return ApiResult.Error(ApiError(401, "Unauthenticated."))
        }
        val result = executeDataCall(emitGlobalEvents = !silent) { api.getAuthUser() }
        if (result is ApiResult.Success) {
            _currentUser.value = result.data
        } else if (result is ApiResult.Error && result.error.isUnauthorized) {
            clearLocalSession()
        }
        return result
    }

    suspend fun resendVerificationEmail(): ApiResult<String> =
        executeMessageCall(emitGlobalEvents = true) { api.resendVerificationEmail() }

    suspend fun refreshAuthenticatedData() {
        if (!isLoggedIn()) return
        fetchCart(silent = true)
        fetchWishlist(silent = true)
    }

    suspend fun clearLocalSession() {
        sessionStore.clearToken()
        _currentUser.value = null
        _cart.value = CartDto()
        _wishlist.value = emptyList()
    }

    // =========================================================================
    // 4.3 Database-Backed Cart
    // =========================================================================

    suspend fun fetchCart(silent: Boolean = false): ApiResult<CartDto> {
        if (!isLoggedIn()) {
            _cart.value = CartDto()
            return ApiResult.Success(CartDto())
        }
        val result = executeDataCall(emitGlobalEvents = !silent) { api.getCart() }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun addToCart(bookId: Long, quantity: Int = 1): ApiResult<CartDto> {
        val result = executeDataCall { api.addCartItem(AddCartItemRequest(bookId, quantity)) }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun updateCartItemQuantity(bookId: Long, quantity: Int): ApiResult<CartDto> {
        val result = executeDataCall { api.updateCartItem(bookId, UpdateCartItemRequest(quantity)) }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun removeCartItem(bookId: Long): ApiResult<CartDto> {
        val result = executeDataCall { api.removeCartItem(bookId) }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun clearCart(): ApiResult<CartDto> {
        val result = executeDataCall { api.clearCart() }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun applyCoupon(couponCode: String): ApiResult<CartDto> {
        val result = executeDataCall { api.applyCoupon(ApplyCouponRequest(couponCode.trim())) }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    suspend fun removeCoupon(): ApiResult<CartDto> {
        val result = executeDataCall { api.removeCoupon() }
        if (result is ApiResult.Success) {
            _cart.value = result.data
        }
        return result
    }

    // =========================================================================
    // 4.4 Mobile Checkout & Orders
    // =========================================================================

    suspend fun createPaymentIntent(
        name: String,
        address: String,
        city: String,
        postalCode: String
    ): ApiResult<PaymentIntentDataDto> {
        val result = executeDataCall {
            api.createPaymentIntent(
                CreatePaymentIntentRequest(
                    name = name.trim(),
                    address = address.trim(),
                    city = city.trim(),
                    postalCode = postalCode.trim()
                )
            )
        }
        if (result is ApiResult.Success && result.data.freeOrder) {
            // Backend clears cart on 100% free coupon orders
            _cart.value = CartDto()
        }
        return result
    }

    suspend fun confirmCheckout(
        orderId: Long,
        paymentIntentId: String? = null
    ): ApiResult<OrderDetailDto> {
        val result = executeDataCall {
            api.confirmCheckout(ConfirmOrderRequest(orderId = orderId, paymentIntentId = paymentIntentId))
        }
        if (result is ApiResult.Success) {
            // Backend clears cart on confirmed payment
            _cart.value = CartDto()
        }
        return result
    }

    suspend fun fetchOrders(page: Int = 1): ApiResult<PaginatedResponse<OrderSummaryDto>> =
        executeRawCall { api.getOrders(page) }

    suspend fun fetchOrderDetail(orderId: Long): ApiResult<OrderDetailDto> =
        executeDataCall { api.getOrderDetail(orderId) }

    /**
     * Downloads the official PDF tax invoice (GET /api/v1/orders/{id}/invoice).
     * Strictly checks response.isSuccessful before streaming bytes to app cache.
     * Parses JSON error body on 4xx errors per Section 4.4.
     */
    suspend fun downloadOrderInvoice(orderId: Long): ApiResult<File> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadOrderInvoice(orderId)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return@withContext ApiResult.Error(ApiError(500, "Empty invoice response."))
                val invoicesDir = File(appContext.cacheDir, "invoices").apply { mkdirs() }
                val padded = orderId.toString().padStart(5, '0')
                val targetFile = File(invoicesDir, "invoice-ORD-$padded.pdf")
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                ApiResult.Success(targetFile, "Invoice saved.")
            } else {
                val error = errorParser.parseError(response)
                handleGlobalError(error)
                ApiResult.Error(error)
            }
        } catch (t: Throwable) {
            ApiResult.Error(errorParser.networkError(t))
        }
    }

    // =========================================================================
    // 4.5 Wishlist
    // =========================================================================

    suspend fun fetchWishlist(silent: Boolean = false): ApiResult<List<BookDto>> {
        if (!isLoggedIn()) {
            _wishlist.value = emptyList()
            return ApiResult.Success(emptyList())
        }
        val result = executeDataCall(emitGlobalEvents = !silent) { api.getWishlist() }
        if (result is ApiResult.Success) {
            _wishlist.value = result.data
        }
        return result
    }

    suspend fun toggleWishlist(bookId: Long): ApiResult<WishlistToggleDataDto> {
        val result = executeDataCall { api.toggleWishlist(WishlistToggleRequest(bookId)) }
        if (result is ApiResult.Success) {
            fetchWishlist(silent = true)
        }
        return result
    }

    suspend fun removeFromWishlist(bookId: Long): ApiResult<WishlistRemoveDataDto> {
        val result = executeDataCall { api.removeFromWishlist(bookId) }
        if (result is ApiResult.Success) {
            _wishlist.value = _wishlist.value.filterNot { it.id == bookId }
        }
        return result
    }

    suspend fun moveWishlistItemToCart(bookId: Long): ApiResult<CartDto> {
        val result = executeDataCall { api.moveWishlistItemToCart(bookId) }
        if (result is ApiResult.Success) {
            _cart.value = result.data
            _wishlist.value = _wishlist.value.filterNot { it.id == bookId }
        }
        return result
    }

    // =========================================================================
    // 4.6 Reviews
    // =========================================================================

    suspend fun submitReview(
        bookSlugOrId: String,
        rating: Int,
        title: String?,
        comment: String
    ): ApiResult<ReviewDto> =
        executeDataCall {
            api.submitReview(
                bookSlugOrId = bookSlugOrId,
                body = SubmitReviewRequest(
                    rating = rating,
                    title = title?.trim()?.takeIf { it.isNotEmpty() },
                    comment = comment.trim()
                )
            )
        }

    suspend fun markReviewHelpful(reviewId: Long): ApiResult<HelpfulVoteDataDto> =
        executeDataCall { api.markReviewHelpful(reviewId) }

    // =========================================================================
    // 4.7 Profile Management
    // =========================================================================

    suspend fun fetchProfile(): ApiResult<UserDto> {
        val result = executeDataCall { api.getProfile() }
        if (result is ApiResult.Success) {
            _currentUser.value = result.data
        }
        return result
    }

    suspend fun updateProfile(input: ProfileUpdateInput): ApiResult<UserDto> {
        val textPlain = "text/plain".toMediaType()
        val fields = mutableMapOf<String, RequestBody>()
        fields["name"] = input.name.trim().toRequestBody(textPlain)
        fields["email"] = input.email.trim().lowercase().toRequestBody(textPlain)

        fun putOptional(key: String, value: String?) {
            fields[key] = (value?.trim() ?: "").toRequestBody(textPlain)
        }

        putOptional("phone", input.phone)
        putOptional("bio", input.bio)
        putOptional("address_line1", input.addressLine1)
        putOptional("address_line2", input.addressLine2)
        putOptional("city", input.city)
        putOptional("state", input.state)
        putOptional("postal_code", input.postalCode)
        putOptional("country", input.country)

        if (input.removeAvatar) {
            fields["remove_avatar"] = "1".toRequestBody(textPlain)
        }

        val avatarPart: MultipartBody.Part? = if (input.avatarUri != null && !input.removeAvatar) {
            val built = buildAvatarPart(input.avatarUri)
            if (built.isFailure) {
                return ApiResult.Error(
                    ApiError(
                        statusCode = 422,
                        message = built.exceptionOrNull()?.message ?: "Invalid avatar file.",
                        fieldErrors = mapOf("avatar" to listOf(built.exceptionOrNull()?.message ?: "Invalid avatar file."))
                    )
                )
            }
            built.getOrNull()
        } else {
            null
        }

        val result = executeDataCall {
            api.updateProfileMultipart(fields = fields, avatar = avatarPart)
        }
        if (result is ApiResult.Success) {
            _currentUser.value = result.data
        }
        return result
    }

    private suspend fun buildAvatarPart(uri: Uri): Result<MultipartBody.Part> = withContext(Dispatchers.IO) {
        try {
            val resolver = appContext.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val allowedMimes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
            if (mimeType.lowercase() !in allowedMimes) {
                return@withContext Result.failure(
                    IllegalArgumentException("Avatar must be a JPEG, PNG, or WebP image.")
                )
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Could not read selected image."))
            if (bytes.size > MAX_AVATAR_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Avatar image must be 2 MB or smaller.")
                )
            }
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            Result.success(MultipartBody.Part.createFormData("avatar", "avatar.$ext", requestBody))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updatePassword(
        currentPassword: String,
        newPassword: String,
        passwordConfirmation: String
    ): ApiResult<String> =
        executeMessageCall {
            api.updatePassword(
                UpdatePasswordRequest(
                    currentPassword = currentPassword,
                    password = newPassword,
                    passwordConfirmation = passwordConfirmation
                )
            )
        }

    suspend fun deleteAccount(password: String): ApiResult<String> {
        val result = executeMessageCall {
            api.deleteAccount(DeleteAccountRequest(password = password))
        }
        if (result is ApiResult.Success) {
            clearLocalSession()
        }
        return result
    }

    // =========================================================================
    // Internal Execution & Spec Error Mapping
    // =========================================================================

    private suspend fun <T> executeDataCall(
        emitGlobalEvents: Boolean = true,
        call: suspend () -> Response<DataResponse<T>>
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body.data, body.message)
                } else {
                    ApiResult.Error(ApiError(response.code(), "Empty response from server."))
                }
            } else {
                val error = errorParser.parseError(response)
                if (emitGlobalEvents) handleGlobalError(error)
                ApiResult.Error(error)
            }
        } catch (t: Throwable) {
            ApiResult.Error(errorParser.networkError(t))
        }
    }

    private suspend fun <T> executeRawCall(
        emitGlobalEvents: Boolean = true,
        call: suspend () -> Response<T>
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body, null)
                } else {
                    ApiResult.Error(ApiError(response.code(), "Empty response from server."))
                }
            } else {
                val error = errorParser.parseError(response)
                if (emitGlobalEvents) handleGlobalError(error)
                ApiResult.Error(error)
            }
        } catch (t: Throwable) {
            ApiResult.Error(errorParser.networkError(t))
        }
    }

    private suspend fun executeMessageCall(
        emitGlobalEvents: Boolean = true,
        call: suspend () -> Response<MessageResponse>
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val msg = response.body()?.message ?: "Success."
                ApiResult.Success(msg, msg)
            } else {
                val error = errorParser.parseError(response)
                if (emitGlobalEvents) handleGlobalError(error)
                ApiResult.Error(error)
            }
        } catch (t: Throwable) {
            ApiResult.Error(errorParser.networkError(t))
        }
    }

    private suspend fun handleGlobalError(error: ApiError) {
        when {
            error.isUnauthorized -> {
                clearLocalSession()
                _sessionEvents.tryEmit(SessionEvent.Unauthorized(error.message))
            }
            error.isEmailNotVerified -> {
                _sessionEvents.tryEmit(SessionEvent.EmailNotVerified(error.message))
            }
            error.isPaymentGatewayUnavailable -> {
                _sessionEvents.tryEmit(SessionEvent.PaymentGatewayUnavailable(error.message))
            }
        }
    }

    companion object {
        private const val MAX_AVATAR_BYTES = 2 * 1024 * 1024 // 2 MB max per spec Section 4.7
    }
}
