package com.example.data.remote

import com.example.data.remote.dto.ApiErrorBody
import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException

data class ApiError(
    val statusCode: Int,
    val message: String,
    val code: String? = null,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val retryAfterSeconds: Long? = null,
    val isNetworkError: Boolean = false
) {
    val isUnauthorized: Boolean get() = statusCode == 401
    val isEmailNotVerified: Boolean get() = statusCode == 403 && code == "email_not_verified"
    val isForbidden: Boolean get() = statusCode == 403 && code != "email_not_verified"
    val isNotFound: Boolean get() = statusCode == 404
    val isAlreadyAuthenticated: Boolean get() = statusCode == 400
    val isValidationError: Boolean get() = statusCode == 422
    val isRateLimited: Boolean get() = statusCode == 429
    val isPaymentGatewayUnavailable: Boolean get() = statusCode == 503

    fun firstFieldError(field: String): String? = fieldErrors[field]?.firstOrNull()
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : ApiResult<T>()
    data class Error(val error: ApiError) : ApiResult<Nothing>()
}

class ApiErrorParser(private val moshi: Moshi) {
    private val errorAdapter = moshi.adapter(ApiErrorBody::class.java)

    fun parseError(response: Response<*>): ApiError {
        val statusCode = response.code()
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()
        val rawBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsed = if (!rawBody.isNullOrBlank()) {
            try {
                errorAdapter.fromJson(rawBody)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val fallbackMessage = when (statusCode) {
            400 -> "Already authenticated."
            401 -> "Unauthenticated."
            403 -> "Access denied."
            404 -> "Resource not found."
            422 -> "Please check your input and try again."
            429 -> if (retryAfter != null) {
                "Too many requests. Please wait ${retryAfter}s and try again."
            } else {
                "Too many requests. Please try again shortly."
            }
            503 -> "Payment gateway is undergoing maintenance. Please try again later."
            else -> "Request failed (HTTP $statusCode)."
        }

        return ApiError(
            statusCode = statusCode,
            message = parsed?.message?.takeIf { it.isNotBlank() } ?: fallbackMessage,
            code = parsed?.code,
            fieldErrors = parsed?.errors ?: emptyMap(),
            retryAfterSeconds = retryAfter,
            isNetworkError = false
        )
    }

    fun networkError(throwable: Throwable): ApiError {
        val message = when (throwable) {
            is IOException -> "Unable to reach the server. Please check your internet connection and try again."
            else -> throwable.localizedMessage ?: "Unexpected network error."
        }
        return ApiError(
            statusCode = 0,
            message = message,
            isNetworkError = true
        )
    }
}
