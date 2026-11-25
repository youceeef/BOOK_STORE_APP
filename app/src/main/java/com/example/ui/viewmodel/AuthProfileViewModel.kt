package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.BookDto
import com.example.data.remote.dto.UserDto
import com.example.data.repository.BookStoreRepository
import com.example.data.repository.ProfileUpdateInput
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FormState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val successMessage: String? = null
) {
    fun fieldError(key: String): String? = fieldErrors[key]?.firstOrNull()
}

data class WishlistUiState(
    val isLoading: Boolean = false,
    val busyBookId: Long? = null,
    val error: String? = null
)

class AuthProfileViewModel(
    private val repository: BookStoreRepository
) : ViewModel() {

    val currentUser: StateFlow<UserDto?> = repository.currentUser
    val wishlist: StateFlow<List<BookDto>> = repository.wishlist

    private val _authFormState = MutableStateFlow(FormState())
    val authFormState: StateFlow<FormState> = _authFormState.asStateFlow()

    private val _profileFormState = MutableStateFlow(FormState())
    val profileFormState: StateFlow<FormState> = _profileFormState.asStateFlow()

    private val _wishlistState = MutableStateFlow(WishlistUiState())
    val wishlistState: StateFlow<WishlistUiState> = _wishlistState.asStateFlow()

    fun clearAuthFormState() {
        _authFormState.value = FormState()
    }

    fun clearProfileFormState() {
        _profileFormState.value = FormState()
    }

    // =========================================================================
    // Authentication (Email & Password strictly per API spec)
    // =========================================================================

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authFormState.value = FormState(isSubmitting = true)
            when (val res = repository.login(email, password)) {
                is ApiResult.Success -> {
                    _authFormState.value = FormState(successMessage = res.message)
                    val displayName = res.data.user.name.ifBlank { "Reader" }
                    repository.emitWelcomeUser(displayName, res.data.user.avatarUrl)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    if (res.error.isAlreadyAuthenticated) {
                        onSuccess()
                    } else {
                        _authFormState.value = FormState(
                            error = res.error.message,
                            fieldErrors = res.error.fieldErrors
                        )
                    }
                }
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authFormState.value = FormState(isSubmitting = true)
            when (val res = repository.register(name, email, password, passwordConfirmation)) {
                is ApiResult.Success -> {
                    _authFormState.value = FormState(successMessage = res.message)
                    repository.emitSnackbar(res.message ?: "Registered successfully.")
                    onSuccess()
                }
                is ApiResult.Error -> {
                    if (res.error.isAlreadyAuthenticated) {
                        onSuccess()
                    } else {
                        _authFormState.value = FormState(
                            error = res.error.message,
                            fieldErrors = res.error.fieldErrors
                        )
                    }
                }
            }
        }
    }

    fun forgotPassword(email: String, onSent: (String) -> Unit) {
        viewModelScope.launch {
            _authFormState.value = FormState(isSubmitting = true)
            when (val res = repository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    _authFormState.value = FormState(successMessage = res.data)
                    onSent(res.data)
                }
                is ApiResult.Error -> {
                    _authFormState.value = FormState(
                        error = res.error.message,
                        fieldErrors = res.error.fieldErrors
                    )
                }
            }
        }
    }

    fun resetPassword(
        token: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        onResetComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _authFormState.value = FormState(isSubmitting = true)
            when (val res = repository.resetPassword(token, email, password, passwordConfirmation)) {
                is ApiResult.Success -> {
                    _authFormState.value = FormState(successMessage = res.data)
                    repository.emitSnackbar(res.data)
                    onResetComplete()
                }
                is ApiResult.Error -> {
                    _authFormState.value = FormState(
                        error = res.error.message,
                        fieldErrors = res.error.fieldErrors
                    )
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            val userBeforeLogout = repository.currentUser.value
            val displayName = userBeforeLogout?.name?.takeIf { it.isNotBlank() } ?: "Reader"
            val avatarUrl = userBeforeLogout?.avatarUrl
            repository.logout()
            repository.emitGoodbyeUser(displayName, avatarUrl)
            onComplete()
        }
    }

    // =========================================================================
    // Profile Management
    // =========================================================================

    fun refreshProfile() {
        if (!repository.isLoggedIn()) return
        viewModelScope.launch {
            repository.fetchProfile()
        }
    }

    fun updateProfile(input: ProfileUpdateInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _profileFormState.value = FormState(isSubmitting = true)
            when (val res = repository.updateProfile(input)) {
                is ApiResult.Success -> {
                    _profileFormState.value = FormState(successMessage = res.message)
                    repository.emitSnackbar(res.message ?: "Profile updated successfully.")
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _profileFormState.value = FormState(
                        error = res.error.message,
                        fieldErrors = res.error.fieldErrors
                    )
                }
            }
        }
    }

    fun updatePassword(
        currentPassword: String,
        newPassword: String,
        passwordConfirmation: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _profileFormState.value = FormState(isSubmitting = true)
            when (val res = repository.updatePassword(currentPassword, newPassword, passwordConfirmation)) {
                is ApiResult.Success -> {
                    _profileFormState.value = FormState(successMessage = res.data)
                    repository.emitSnackbar(res.data)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _profileFormState.value = FormState(
                        error = if (res.error.isEmailNotVerified) null else res.error.message,
                        fieldErrors = res.error.fieldErrors
                    )
                }
            }
        }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _profileFormState.value = FormState(isSubmitting = true)
            when (val res = repository.deleteAccount(password)) {
                is ApiResult.Success -> {
                    _profileFormState.value = FormState(successMessage = res.data)
                    repository.emitSnackbar(res.data)
                    onDeleted()
                }
                is ApiResult.Error -> {
                    _profileFormState.value = FormState(
                        error = res.error.message,
                        fieldErrors = res.error.fieldErrors
                    )
                }
            }
        }
    }

    // =========================================================================
    // Wishlist
    // =========================================================================

    fun refreshWishlist() {
        viewModelScope.launch {
            _wishlistState.update { it.copy(isLoading = true, error = null) }
            when (val res = repository.fetchWishlist()) {
                is ApiResult.Success -> _wishlistState.update { it.copy(isLoading = false) }
                is ApiResult.Error -> _wishlistState.update {
                    it.copy(isLoading = false, error = res.error.message)
                }
            }
        }
    }

    fun removeFromWishlist(bookId: Long) {
        viewModelScope.launch {
            _wishlistState.update { it.copy(busyBookId = bookId) }
            when (val res = repository.removeFromWishlist(bookId)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Volume removed from your saved items.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _wishlistState.update { it.copy(busyBookId = null) }
        }
    }

    fun moveToCart(bookId: Long) {
        viewModelScope.launch {
            _wishlistState.update { it.copy(busyBookId = bookId) }
            when (val res = repository.moveWishlistItemToCart(bookId)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Volume moved to shopping bag.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _wishlistState.update { it.copy(busyBookId = null) }
        }
    }
}
