package com.example.data.remote

import com.example.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Exact 1:1 Retrofit contract for the Android Native Mobile REST API Specification (v1).
 * Base URL ends with `/api/v1/`.
 */
interface BookStoreApiService {

    // =========================================================================
    // 4.1 System & Catalog (Public)
    // =========================================================================

    @GET("config")
    suspend fun getConfig(): Response<DataResponse<AppConfigDto>>

    @GET("categories")
    suspend fun getCategories(): Response<DataResponse<List<CategoryDto>>>

    @GET("books")
    suspend fun getBooks(
        @Query("page") page: Int? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("in_stock") inStock: Int? = null,
        @Query("sort") sort: String? = null
    ): Response<PaginatedResponse<BookDto>>

    @GET("books/{book}")
    suspend fun getBookDetail(
        @Path("book") bookSlugOrId: String
    ): Response<DataResponse<BookDetailDto>>

    @GET("books/{book}/reviews")
    suspend fun getBookReviews(
        @Path("book") bookSlugOrId: String,
        @Query("page") page: Int? = null
    ): Response<PaginatedResponse<ReviewDto>>

    // =========================================================================
    // 4.2 Authentication (/api/v1/auth/...)
    // =========================================================================

    @Headers("X-Guest-Route: true")
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<DataResponse<AuthDataDto>>

    @Headers("X-Guest-Route: true")
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<DataResponse<AuthDataDto>>

    @Headers("X-Guest-Route: true")
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body body: ForgotPasswordRequest
    ): Response<MessageResponse>

    @Headers("X-Guest-Route: true")
    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body body: ResetPasswordRequest
    ): Response<MessageResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("auth/user")
    suspend fun getAuthUser(): Response<DataResponse<UserDto>>

    @POST("auth/email/verification-notification")
    suspend fun resendVerificationEmail(): Response<MessageResponse>

    // =========================================================================
    // 4.3 Database-Backed Cart (/api/v1/cart/...)
    // =========================================================================

    @GET("cart")
    suspend fun getCart(): Response<DataResponse<CartDto>>

    @POST("cart/items")
    suspend fun addCartItem(
        @Body body: AddCartItemRequest
    ): Response<DataResponse<CartDto>>

    @PATCH("cart/items/{book_id}")
    suspend fun updateCartItem(
        @Path("book_id") bookId: Long,
        @Body body: UpdateCartItemRequest
    ): Response<DataResponse<CartDto>>

    @DELETE("cart/items/{book_id}")
    suspend fun removeCartItem(
        @Path("book_id") bookId: Long
    ): Response<DataResponse<CartDto>>

    @POST("cart/clear")
    suspend fun clearCart(): Response<DataResponse<CartDto>>

    @POST("cart/coupon")
    suspend fun applyCoupon(
        @Body body: ApplyCouponRequest
    ): Response<DataResponse<CartDto>>

    @DELETE("cart/coupon")
    suspend fun removeCoupon(): Response<DataResponse<CartDto>>

    // =========================================================================
    // 4.4 Mobile Checkout & Orders (/api/v1/checkout/... & /api/v1/orders/...)
    // =========================================================================

    @POST("checkout/create-payment-intent")
    suspend fun createPaymentIntent(
        @Body body: CreatePaymentIntentRequest
    ): Response<DataResponse<PaymentIntentDataDto>>

    @POST("checkout/confirm")
    suspend fun confirmCheckout(
        @Body body: ConfirmOrderRequest
    ): Response<DataResponse<OrderDetailDto>>

    @GET("orders")
    suspend fun getOrders(
        @Query("page") page: Int? = null
    ): Response<PaginatedResponse<OrderSummaryDto>>

    @GET("orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") orderId: Long
    ): Response<DataResponse<OrderDetailDto>>

    @Streaming
    @Headers("Accept: application/pdf")
    @GET("orders/{id}/invoice")
    suspend fun downloadOrderInvoice(
        @Path("id") orderId: Long
    ): Response<ResponseBody>

    // =========================================================================
    // 4.5 Wishlist (/api/v1/wishlist/...)
    // =========================================================================

    @GET("wishlist")
    suspend fun getWishlist(): Response<DataResponse<List<BookDto>>>

    @POST("wishlist/toggle")
    suspend fun toggleWishlist(
        @Body body: WishlistToggleRequest
    ): Response<DataResponse<WishlistToggleDataDto>>

    @DELETE("wishlist/{book_id}")
    suspend fun removeFromWishlist(
        @Path("book_id") bookId: Long
    ): Response<DataResponse<WishlistRemoveDataDto>>

    @POST("wishlist/{book_id}/move-to-cart")
    suspend fun moveWishlistItemToCart(
        @Path("book_id") bookId: Long
    ): Response<DataResponse<CartDto>>

    // =========================================================================
    // 4.6 Reviews (/api/v1/...)
    // =========================================================================

    @POST("books/{book}/reviews")
    suspend fun submitReview(
        @Path("book") bookSlugOrId: String,
        @Body body: SubmitReviewRequest
    ): Response<DataResponse<ReviewDto>>

    @POST("reviews/{id}/helpful")
    suspend fun markReviewHelpful(
        @Path("id") reviewId: Long
    ): Response<DataResponse<HelpfulVoteDataDto>>

    // =========================================================================
    // 4.7 Profile Management (/api/v1/profile/...)
    // =========================================================================

    @GET("profile")
    suspend fun getProfile(): Response<DataResponse<UserDto>>

    @Multipart
    @POST("profile")
    suspend fun updateProfileMultipart(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part avatar: MultipartBody.Part? = null
    ): Response<DataResponse<UserDto>>

    @PUT("profile/password")
    suspend fun updatePassword(
        @Body body: UpdatePasswordRequest
    ): Response<MessageResponse>

    @HTTP(method = "DELETE", path = "profile", hasBody = true)
    suspend fun deleteAccount(
        @Body body: DeleteAccountRequest
    ): Response<MessageResponse>
}
