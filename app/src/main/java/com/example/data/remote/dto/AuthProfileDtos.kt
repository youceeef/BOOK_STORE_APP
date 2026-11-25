package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "email_verified") val emailVerified: Boolean = false,
    @Json(name = "email_verified_at") val emailVerifiedAt: String? = null,
    @Json(name = "is_admin") val isAdmin: Boolean = false,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "address_line1") val addressLine1: String? = null,
    @Json(name = "address_line2") val addressLine2: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postal_code") val postalCode: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthDataDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String,
    @Json(name = "device_name") val deviceName: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_name") val deviceName: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "token") val token: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class UpdatePasswordRequest(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "password") val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class DeleteAccountRequest(
    @Json(name = "password") val password: String
)
