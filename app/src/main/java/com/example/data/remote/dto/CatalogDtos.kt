package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppConfigDto(
    @Json(name = "app_name") val appName: String = "BookStore",
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "locales") val locales: List<String> = listOf("en", "fr", "ar"),
    @Json(name = "default_locale") val defaultLocale: String = "en",
    @Json(name = "stripe_publishable_key") val stripePublishableKey: String? = null,
    @Json(name = "reservation_minutes") val reservationMinutes: Int = 15,
    @Json(name = "cover_placeholder_url") val coverPlaceholderUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "books_count") val booksCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class BookCategoryRefDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String
)

@JsonClass(generateAdapter = true)
data class BookDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "author") val author: String,
    @Json(name = "isbn") val isbn: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "stock") val stock: Int,
    @Json(name = "in_stock") val inStock: Boolean,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "has_sample") val hasSample: Boolean = false,
    @Json(name = "category") val category: BookCategoryRefDto? = null,
    @Json(name = "average_rating") val averageRating: Double = 0.0,
    @Json(name = "reviews_count") val reviewsCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RatingBreakdownItemDto(
    @Json(name = "count") val count: Int = 0,
    @Json(name = "percentage") val percentage: Int = 0
)

@JsonClass(generateAdapter = true)
data class RelatedBookDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "price") val price: Double,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "in_stock") val inStock: Boolean
)

@JsonClass(generateAdapter = true)
data class BookDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "author") val author: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "sample_excerpt") val sampleExcerpt: String? = null,
    @Json(name = "reading_time_minutes") val readingTimeMinutes: Int? = null,
    @Json(name = "isbn") val isbn: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "stock") val stock: Int,
    @Json(name = "in_stock") val inStock: Boolean,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "has_sample") val hasSample: Boolean = false,
    @Json(name = "category") val category: BookCategoryRefDto? = null,
    @Json(name = "average_rating") val averageRating: Double = 0.0,
    @Json(name = "reviews_count") val reviewsCount: Int = 0,
    @Json(name = "rating_breakdown") val ratingBreakdown: List<RatingBreakdownItemDto> = emptyList(),
    @Json(name = "related_books") val relatedBooks: List<RelatedBookDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ReviewUserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    @Json(name = "id") val id: Long,
    @Json(name = "user") val user: ReviewUserDto,
    @Json(name = "rating") val rating: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "comment") val comment: String,
    @Json(name = "is_verified_purchase") val isVerifiedPurchase: Boolean = false,
    @Json(name = "helpful_count") val helpfulCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitReviewRequest(
    @Json(name = "rating") val rating: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "comment") val comment: String
)

@JsonClass(generateAdapter = true)
data class HelpfulVoteDataDto(
    @Json(name = "helpful_count") val helpfulCount: Int
)
