package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.BookDto
import com.example.data.remote.dto.CategoryDto
import com.example.data.repository.BookStoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CatalogFilters(
    val categorySlug: String? = null,
    val search: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val inStockOnly: Boolean = false,
    val sort: String = "latest"
) {
    val activeFilterCount: Int
        get() = listOf(
            minPrice.isNotBlank(),
            maxPrice.isNotBlank(),
            inStockOnly,
            sort != "latest"
        ).count { it }
}

data class CatalogUiState(
    val categories: List<CategoryDto> = emptyList(),
    val books: List<BookDto> = emptyList(),
    val filters: CatalogFilters = CatalogFilters(),
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val totalBooks: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val busyBookId: Long? = null
) {
    val canLoadMore: Boolean get() = currentPage < lastPage && !isLoading && !isLoadingMore
}

class CatalogViewModel(
    private val repository: BookStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState(isLoading = true))
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    val wishlistBookIds: StateFlow<Set<Long>> = repository.wishlistBookIds
    val coverPlaceholderUrl: StateFlow<String?> = repository.appConfig
        .map { it.coverPlaceholderUrl }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var searchDebounceJob: Job? = null

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.awaitSessionReady()
            launch { loadCategories() }
            loadBooks(page = 1, reset = true)
        }
    }

    private suspend fun loadCategories() {
        val res = repository.fetchCategories()
        if (res is ApiResult.Success) {
            _uiState.update { it.copy(categories = res.data) }
        }
    }

    fun loadBooks(page: Int = 1, reset: Boolean = false) {
        viewModelScope.launch {
            val filters = _uiState.value.filters
            _uiState.update {
                if (reset) it.copy(isLoading = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }
            val result = repository.fetchBooks(
                page = page,
                category = filters.categorySlug,
                search = filters.search,
                minPrice = filters.minPrice.toDoubleOrNull(),
                maxPrice = filters.maxPrice.toDoubleOrNull(),
                inStock = filters.inStockOnly,
                sort = filters.sort
            )
            when (result) {
                is ApiResult.Success -> {
                    val paginated = result.data
                    _uiState.update { state ->
                        val merged = if (reset || page == 1) {
                            paginated.data
                        } else {
                            (state.books + paginated.data).distinctBy { it.id }
                        }
                        state.copy(
                            books = merged,
                            currentPage = paginated.meta?.currentPage ?: page,
                            lastPage = paginated.meta?.lastPage ?: page,
                            totalBooks = paginated.meta?.total ?: merged.size,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.canLoadMore) {
            loadBooks(page = state.currentPage + 1, reset = false)
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(filters = it.filters.copy(search = query)) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(350)
            loadBooks(page = 1, reset = true)
        }
    }

    fun onCategorySelected(slug: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(categorySlug = slug)) }
        loadBooks(page = 1, reset = true)
    }

    fun applyAdvancedFilters(
        minPrice: String,
        maxPrice: String,
        inStockOnly: Boolean,
        sort: String
    ) {
        _uiState.update {
            it.copy(
                filters = it.filters.copy(
                    minPrice = minPrice.trim(),
                    maxPrice = maxPrice.trim(),
                    inStockOnly = inStockOnly,
                    sort = sort
                )
            )
        }
        loadBooks(page = 1, reset = true)
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = CatalogFilters()) }
        loadBooks(page = 1, reset = true)
    }

    fun quickAddToBag(book: BookDto, onRequireAuth: () -> Unit) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyBookId = book.id) }
            when (val res = repository.addToCart(book.id, 1)) {
                is ApiResult.Success -> {
                    repository.emitSnackbar(res.message ?: "Book added to cart successfully.")
                }
                is ApiResult.Error -> {
                    repository.emitSnackbar(res.error.message)
                }
            }
            _uiState.update { it.copy(busyBookId = null) }
        }
    }

    fun toggleWishlist(book: BookDto, onRequireAuth: () -> Unit) {
        if (!repository.isLoggedIn()) {
            onRequireAuth()
            return
        }
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
}
