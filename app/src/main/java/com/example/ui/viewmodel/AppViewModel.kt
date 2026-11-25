package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.AppConfigDto
import com.example.data.remote.dto.CartDto
import com.example.data.remote.dto.UserDto
import com.example.data.repository.BookStoreRepository
import com.example.data.repository.SessionEvent
import com.stripe.android.PaymentConfiguration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WelcomePopupData(
    val userName: String,
    val avatarUrl: String? = null,
    val isLogout: Boolean = false
)

data class AppUiState(
    val isInitialized: Boolean = false,
    val configLoaded: Boolean = false,
    val configError: String? = null,
    val showEmailVerificationDialog: Boolean = false,
    val emailVerificationMessage: String? = null,
    val isResendingVerification: Boolean = false,
    val verificationFeedback: String? = null,
    val showPaymentMaintenanceDialog: Boolean = false,
    val paymentMaintenanceMessage: String? = null,
    val welcomePopup: WelcomePopupData? = null
)

class AppViewModel(
    private val appContext: Context,
    val repository: BookStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val appConfig: StateFlow<AppConfigDto> = repository.appConfig
    val currentUser: StateFlow<UserDto?> = repository.currentUser
    val cart: StateFlow<CartDto> = repository.cart
    val locale: StateFlow<String> = repository.locale
    val authToken: StateFlow<String?> = repository.authToken

    private val _navigateToLoginEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val navigateToLoginEvents: SharedFlow<String> = _navigateToLoginEvents.asSharedFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        observeSessionEvents()
        bootstrap()
    }

    private fun observeSessionEvents() {
        viewModelScope.launch {
            repository.sessionEvents.collect { event ->
                when (event) {
                    is SessionEvent.Unauthorized -> {
                        _navigateToLoginEvents.tryEmit(event.message)
                    }
                    is SessionEvent.EmailNotVerified -> {
                        _uiState.update {
                            it.copy(
                                showEmailVerificationDialog = true,
                                emailVerificationMessage = event.message,
                                verificationFeedback = null
                            )
                        }
                    }
                    is SessionEvent.PaymentGatewayUnavailable -> {
                        _uiState.update {
                            it.copy(
                                showPaymentMaintenanceDialog = true,
                                paymentMaintenanceMessage = event.message
                            )
                        }
                    }
                    is SessionEvent.SnackbarMessage -> {
                        _snackbarEvents.tryEmit(event.message)
                    }
                    is SessionEvent.WelcomeUser -> {
                        _uiState.update {
                            it.copy(
                                welcomePopup = WelcomePopupData(
                                    userName = event.userName,
                                    avatarUrl = event.avatarUrl,
                                    isLogout = false
                                )
                            )
                        }
                    }
                    is SessionEvent.GoodbyeUser -> {
                        _uiState.update {
                            it.copy(
                                welcomePopup = WelcomePopupData(
                                    userName = event.userName,
                                    avatarUrl = event.avatarUrl,
                                    isLogout = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun bootstrap() {
        viewModelScope.launch {
            repository.awaitSessionReady()
            when (val configResult = repository.fetchConfig()) {
                is ApiResult.Success -> {
                    val key = configResult.data.stripePublishableKey
                    if (!key.isNullOrBlank()) {
                        PaymentConfiguration.init(appContext, key)
                    }
                    _uiState.update {
                        it.copy(isInitialized = true, configLoaded = true, configError = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isInitialized = true,
                            configLoaded = false,
                            configError = configResult.error.message
                        )
                    }
                }
            }

            if (repository.isLoggedIn()) {
                val userResult = repository.fetchAuthUser(silent = true)
                if (userResult is ApiResult.Success) {
                    repository.refreshAuthenticatedData()
                }
            }
        }
    }

    fun dismissEmailVerificationDialog() {
        _uiState.update {
            it.copy(showEmailVerificationDialog = false, verificationFeedback = null)
        }
    }

    fun openEmailVerificationDialog() {
        _uiState.update {
            it.copy(showEmailVerificationDialog = true, verificationFeedback = null)
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResendingVerification = true, verificationFeedback = null) }
            when (val res = repository.resendVerificationEmail()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isResendingVerification = false, verificationFeedback = res.data)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isResendingVerification = false, verificationFeedback = res.error.message)
                    }
                }
            }
        }
    }

    fun refreshUserVerificationStatus() {
        viewModelScope.launch {
            when (val res = repository.fetchAuthUser(silent = true)) {
                is ApiResult.Success -> {
                    if (res.data.emailVerified) {
                        _uiState.update {
                            it.copy(showEmailVerificationDialog = false, verificationFeedback = null)
                        }
                        _snackbarEvents.tryEmit("Email verified!")
                    } else {
                        _uiState.update {
                            it.copy(verificationFeedback = "Your email is still not verified yet.")
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(verificationFeedback = res.error.message) }
                }
            }
        }
    }

    fun dismissPaymentMaintenanceDialog() {
        _uiState.update { it.copy(showPaymentMaintenanceDialog = false) }
    }

    fun dismissWelcomePopup() {
        _uiState.update { it.copy(welcomePopup = null) }
    }

    fun selectLocale(newLocale: String) {
        viewModelScope.launch {
            repository.setLocale(newLocale)
            _snackbarEvents.tryEmit("Language set to ${newLocale.uppercase()}")
        }
    }
}
