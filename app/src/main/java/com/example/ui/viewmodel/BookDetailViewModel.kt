package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.BookDetailDto
import com.example.data.remote.dto.ReviewDto
import com.example.data.repository.BookStoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: BookDetailDto? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val reviewsCurrentPage: Int = 1,
    val reviewsLastPage: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMoreReviews: Boolean = false,
    val isAddingToBag: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val votingReviewId: Long? = null,
    val votedHelpfulIds: Set<Long> = emptySet(),
    val error: String? = null,
    val isNotFound: Boolean = false,
    val reviewError: String? = null,
    val reviewFieldErrors: Map<String, List<String>> = emptyMap(),
    val reviewSubmittedSuccess: Boolean = false
) {
    val canLoadMoreReviews: Boolean
        get() = reviewsCurrentPage < reviewsLastPage && !isLoadingMoreReviews
}

class BookDetailViewModel(
    private val repository: BookStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    val wishlistBookIds: StateFlow<Set<Long>> = repository.wishlistBookIds
    val coverPlaceholderUrl: StateFlow<String?> = repository.appConfig
        .map { it.coverPlaceholderUrl }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var activeSlugOrId: String? = null

    fun loadBook(slugOrId: String) {
        activeSlugOrId = slugOrId
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isNotFound = false,
                    reviewSubmittedSuccess = false
                )
            }
            when (val res = repository.fetchBookDetail(slugOrId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(book = res.data, isLoading = false) }
                    loadReviews(slugOrId, page = 1, reset = true)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = res.error.message,
                            isNotFound = res.error.isNotFound
                        )
                    }
                }
            }
        }
    }

    fun loadReviews(slugOrId: String, page: Int = 1, reset: Boolean = false) {
        viewModelScope.launch {
            if (!reset) {
                _uiState.update { it.copy(isLoadingMoreReviews = true) }
            }
            when (val res = repository.fetchBookReviews(slugOrId, page)) {
                is ApiResult.Success -> {
                    val paginated = res.data
                    _uiState.update { state ->
                        val merged = if (reset || page == 1) {
                            paginated.data
                        } else {
                            (state.reviews + paginated.data).distinctBy { it.id }
                        }
                        state.copy(
                            reviews = merged,
                            reviewsCurrentPage = paginated.meta?.currentPage ?: page,
                            reviewsLastPage = paginated.meta?.lastPage ?: page,
                            isLoadingMoreReviews = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoadingMoreReviews = false) }
                }
            }
        }
    }

    fun loadMoreReviews() {
        val slug = activeSlugOrId ?: return
        val state = _uiState.value
        if (state.canLoadMoreReviews) {
            loadReviews(slug, page = state.reviewsCurrentPage + 1, reset = false)
        }
    }

    fun addToBag(quantity: Int, onRequireAuth: () -> Unit) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToBag = true) }
            when (val res = repository.addToCart(book.id, quantity)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Book added to cart successfully.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _uiState.update { it.copy(isAddingToBag = false) }
        }
    }

    fun toggleWishlist(onRequireAuth: () -> Unit) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            when (val res = repository.toggleWishlist(book.id)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Wishlist updated.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
        }
    }

    fun submitReview(
        rating: Int,
        title: String,
        comment: String,
        onRequireAuth: () -> Unit
    ) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
        val slug = activeSlugOrId ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingReview = true,
                    reviewError = null,
                    reviewFieldErrors = emptyMap(),
                    reviewSubmittedSuccess = false
                )
            }
            when (val res = repository.submitReview(slug, rating, title, comment)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingReview = false,
                            reviewSubmittedSuccess = true
                        )
                    }
                    repository.emitSnackbar(res.message ?: "Review published.")
                    // Refresh book detail (rating breakdown + average) & reviews list
                    loadBook(slug)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingReview = false,
                            reviewError = res.error.message,
                            reviewFieldErrors = res.error.fieldErrors
                        )
                    }
                }
            }
        }
    }

    fun clearReviewFormState() {
        _uiState.update {
            it.copy(
                reviewError = null,
                reviewFieldErrors = emptyMap(),
                reviewSubmittedSuccess = false
            )
        }
    }

    fun markReviewHelpful(reviewId: Long, onRequireAuth: () -> Unit) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(votingReviewId = reviewId) }
            when (val res = repository.markReviewHelpful(reviewId)) {
                is ApiResult.Success -> {
                    val newCount = res.data.helpfulCount
                    _uiState.update { state ->
                        state.copy(
                            votingReviewId = null,
                            votedHelpfulIds = state.votedHelpfulIds + reviewId,
                            reviews = state.reviews.map { review ->
                                if (review.id == reviewId) {
                                    review.copy(helpfulCount = newCount)
                                } else {
                                    review
                                }
                            }
                        )
                    }
                    repository.emitSnackbar(res.message ?: "Thank you for your feedback.")
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(votingReviewId = null) }
                    repository.emitSnackbar(res.error.message)
                }
            }
        }
    }
}
