package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiResult
import com.example.data.remote.dto.OrderDetailDto
import com.example.data.remote.dto.OrderSummaryDto
import com.example.data.repository.BookStoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class OrdersListUiState(
    val orders: List<OrderSummaryDto> = emptyList(),
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
) {
    val canLoadMore: Boolean get() = currentPage < lastPage && !isLoading && !isLoadingMore
}

data class OrderDetailUiState(
    val order: OrderDetailDto? = null,
    val isLoading: Boolean = false,
    val isDownloadingInvoice: Boolean = false,
    val error: String? = null,
    val isNotFound: Boolean = false
)

class OrdersViewModel(
    private val repository: BookStoreRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(OrdersListUiState())
    val listState: StateFlow<OrdersListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(OrderDetailUiState())
    val detailState: StateFlow<OrderDetailUiState> = _detailState.asStateFlow()

    private val _invoiceFileReady = MutableSharedFlow<File>(extraBufferCapacity = 2)
    val invoiceFileReady: SharedFlow<File> = _invoiceFileReady.asSharedFlow()

    fun loadOrders(page: Int = 1, reset: Boolean = true) {
        viewModelScope.launch {
            _listState.update {
                if (reset) it.copy(isLoading = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }
            when (val res = repository.fetchOrders(page)) {
                is ApiResult.Success -> {
                    val paginated = res.data
                    _listState.update { state ->
                        val merged = if (reset || page == 1) {
                            paginated.data
                        } else {
                            (state.orders + paginated.data).distinctBy { it.id }
                        }
                        state.copy(
                            orders = merged,
                            currentPage = paginated.meta?.currentPage ?: page,
                            lastPage = paginated.meta?.lastPage ?: page,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _listState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = res.error.message
                        )
                    }
                }
            }
        }
    }

    fun loadMoreOrders() {
        val state = _listState.value
        if (state.canLoadMore) {
            loadOrders(page = state.currentPage + 1, reset = false)
        }
    }

    fun loadOrderDetail(orderId: Long) {
        viewModelScope.launch {
            _detailState.update {
                it.copy(isLoading = true, error = null, isNotFound = false)
            }
            when (val res = repository.fetchOrderDetail(orderId)) {
                is ApiResult.Success -> {
                    _detailState.update { it.copy(order = res.data, isLoading = false) }
                }
                is ApiResult.Error -> {
                    _detailState.update {
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

    fun downloadInvoice(orderId: Long) {
        viewModelScope.launch {
            _detailState.update { it.copy(isDownloadingInvoice = true) }
            when (val res = repository.downloadOrderInvoice(orderId)) {
                is ApiResult.Success -> {
                    _detailState.update { it.copy(isDownloadingInvoice = false) }
                    _invoiceFileReady.tryEmit(res.data)
                }
                is ApiResult.Error -> {
                    _detailState.update { it.copy(isDownloadingInvoice = false) }
                    repository.emitSnackbar(res.error.message)
                }
            }
        }
    }
}
