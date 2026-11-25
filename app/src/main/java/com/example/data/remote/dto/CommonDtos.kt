package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DataResponse<T>(
    @Json(name = "data") val data: T,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class PaginatedResponse<T>(
    @Json(name = "data") val data: List<T> = emptyList(),
    @Json(name = "links") val links: PaginationLinksDto? = null,
    @Json(name = "meta") val meta: PaginationMetaDto? = null
)

@JsonClass(generateAdapter = true)
data class PaginationLinksDto(
    @Json(name = "first") val first: String? = null,
    @Json(name = "last") val last: String? = null,
    @Json(name = "prev") val prev: String? = null,
    @Json(name = "next") val next: String? = null
)

@JsonClass(generateAdapter = true)
data class PaginationMetaDto(
    @Json(name = "current_page") val currentPage: Int = 1,
    @Json(name = "from") val from: Int? = null,
    @Json(name = "last_page") val lastPage: Int = 1,
    @Json(name = "path") val path: String? = null,
    @Json(name = "per_page") val perPage: Int = 15,
    @Json(name = "to") val to: Int? = null,
    @Json(name = "total") val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class ApiErrorBody(
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "errors") val errors: Map<String, List<String>>? = null
)
