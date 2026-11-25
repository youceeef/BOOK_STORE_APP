package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CartItemDto(
    @Json(name = "book_id") val bookId: Long,
    @Json(name = "title") val title: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "price") val price: Double,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "subtotal") val subtotal: Double,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "stock") val stock: Int,
    @Json(name = "in_stock") val inStock: Boolean
)

@JsonClass(generateAdapter = true)
data class CartDto(
    @Json(name = "items") val items: List<CartItemDto> = emptyList(),
    @Json(name = "subtotal") val subtotal: Double = 0.0,
    @Json(name = "discount") val discount: Double = 0.0,
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "total") val total: Double = 0.0,
    @Json(name = "items_count") val itemsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AddCartItemRequest(
    @Json(name = "book_id") val bookId: Long,
    @Json(name = "quantity") val quantity: Int
)

@JsonClass(generateAdapter = true)
data class UpdateCartItemRequest(
    @Json(name = "quantity") val quantity: Int
)

@JsonClass(generateAdapter = true)
data class ApplyCouponRequest(
    @Json(name = "coupon_code") val couponCode: String
)

@JsonClass(generateAdapter = true)
data class CreatePaymentIntentRequest(
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "city") val city: String,
    @Json(name = "postal_code") val postalCode: String
)

@JsonClass(generateAdapter = true)
data class PaymentIntentDataDto(
    @Json(name = "order_id") val orderId: Long,
    @Json(name = "client_secret") val clientSecret: String? = null,
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "free_order") val freeOrder: Boolean = false,
    @Json(name = "status") val status: String = "pending"
)

@JsonClass(generateAdapter = true)
data class ConfirmOrderRequest(
    @Json(name = "order_id") val orderId: Long,
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderSummaryDto(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "fulfillment_status") val fulfillmentStatus: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "items_count") val itemsCount: Int = 0,
    @Json(name = "tracking_number") val trackingNumber: String? = null,
    @Json(name = "carrier") val carrier: String? = null,
    @Json(name = "tracking_url") val trackingUrl: String? = null,
    @Json(name = "step_index") val stepIndex: Int = 1,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderItemBookRefDto(
    @Json(name = "slug") val slug: String,
    @Json(name = "cover_url") val coverUrl: String
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    @Json(name = "id") val id: Long,
    @Json(name = "book_id") val bookId: Long,
    @Json(name = "title") val title: String,
    @Json(name = "author") val author: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "price") val price: Double,
    @Json(name = "book") val book: OrderItemBookRefDto? = null
)

@JsonClass(generateAdapter = true)
data class OrderDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "fulfillment_status") val fulfillmentStatus: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_email") val customerEmail: String,
    @Json(name = "shipping_address") val shippingAddress: String,
    @Json(name = "carrier") val carrier: String? = null,
    @Json(name = "tracking_number") val trackingNumber: String? = null,
    @Json(name = "tracking_url") val trackingUrl: String? = null,
    @Json(name = "estimated_delivery_at") val estimatedDeliveryAt: String? = null,
    @Json(name = "shipped_at") val shippedAt: String? = null,
    @Json(name = "delivered_at") val deliveredAt: String? = null,
    @Json(name = "step_index") val stepIndex: Int = 1,
    @Json(name = "items") val items: List<OrderItemDto> = emptyList(),
    @Json(name = "invoice_url") val invoiceUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WishlistToggleRequest(
    @Json(name = "book_id") val bookId: Long
)

@JsonClass(generateAdapter = true)
data class WishlistToggleDataDto(
    @Json(name = "book_id") val bookId: Long,
    @Json(name = "is_wishlisted") val isWishlisted: Boolean,
    @Json(name = "wishlist_count") val wishlistCount: Int
)

@JsonClass(generateAdapter = true)
data class WishlistRemoveDataDto(
    @Json(name = "wishlist_count") val wishlistCount: Int
)
