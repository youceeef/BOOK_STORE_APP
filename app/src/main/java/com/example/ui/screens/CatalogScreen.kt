package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.remote.dto.BookDto
import com.example.ui.components.*
import com.example.ui.viewmodel.CatalogFilters
import com.example.ui.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    appName: String,
    onBookClick: (String) -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val wishlistIds by viewModel.wishlistBookIds.collectAsStateWithLifecycle()
    val placeholderUrl by viewModel.coverPlaceholderUrl.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    val isDefaultBrowse = state.filters == CatalogFilters()
    val featuredBook = remember(state.books, isDefaultBrowse) {
        if (isDefaultBrowse && state.books.size >= 2) state.books.first() else null
    }
    val gridBooks = remember(state.books, featuredBook) {
        if (featuredBook != null) state.books.drop(1) else state.books
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Boutique Editorial Masthead (Server URL button removed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = appName.ifBlank { stringResource(R.string.app_name) },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (state.totalBooks > 0) {
                            "${state.totalBooks} curated editions"
                        } else {
                            "Curated literary editions"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { viewModel.refreshAll() },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("catalog_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Elevated Search Pill + Filter Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.filters.search,
                onValueChange = viewModel::onSearchChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.catalog_search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (state.filters.search.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchChange("") },
                            modifier = Modifier.testTag("catalog_clear_search_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("catalog_search_input")
            )

            BadgedBox(
                badge = {
                    if (state.filters.activeFilterCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text("${state.filters.activeFilterCount}")
                        }
                    }
                }
            ) {
                FilledTonalIconButton(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (state.filters.activeFilterCount > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("catalog_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = stringResource(R.string.catalog_filters),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Category Filter Ribbon
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.filters.categorySlug == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text(stringResource(R.string.catalog_all_categories)) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("category_chip_all")
                )
            }
            items(state.categories, key = { it.id }) { category ->
                val selected = state.filters.categorySlug == category.slug
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onCategorySelected(category.slug) },
                    label = { Text("${category.name} · ${category.booksCount}") },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("category_chip_${category.slug}")
                )
            }
        }

        // Active Advanced Filter Summary Bar
        AnimatedVisibility(visible = state.filters.activeFilterCount > 0) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.filters.sort != "latest") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = {
                                viewModel.applyAdvancedFilters(
                                    state.filters.minPrice,
                                    state.filters.maxPrice,
                                    state.filters.inStockOnly,
                                    "latest"
                                )
                            },
                            label = { Text("Sort: ${state.filters.sort.replace("_", " ")}") },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Reset sort", modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
                if (state.filters.inStockOnly) {
                    item {
                        InputChip(
                            selected = true,
                            onClick = {
                                viewModel.applyAdvancedFilters(
                                    state.filters.minPrice,
                                    state.filters.maxPrice,
                                    false,
                                    state.filters.sort
                                )
                            },
                            label = { Text(stringResource(R.string.catalog_in_stock_only)) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Remove in-stock filter", modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
                if (state.filters.minPrice.isNotBlank() || state.filters.maxPrice.isNotBlank()) {
                    item {
                        val rangeLabel = buildString {
                            append("Price: ")
                            append(if (state.filters.minPrice.isNotBlank()) "$${state.filters.minPrice}" else "$0")
                            append("–")
                            append(if (state.filters.maxPrice.isNotBlank()) "$${state.filters.maxPrice}" else "∞")
                        }
                        InputChip(
                            selected = true,
                            onClick = {
                                viewModel.applyAdvancedFilters("", "", state.filters.inStockOnly, state.filters.sort)
                            },
                            label = { Text(rangeLabel) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Clear price filter", modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }

        // Main Pull-to-Refresh Content Area
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.books.isNotEmpty(),
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.isLoading && state.books.isEmpty() -> {
                    CatalogGridSkeleton()
                }
                state.error != null && state.books.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorStateBanner(
                            message = state.error ?: "Failed to load catalog.",
                            onRetry = { viewModel.refreshAll() }
                        )
                    }
                }
                state.books.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Outlined.AutoStories,
                        title = stringResource(R.string.catalog_empty_title),
                        subtitle = stringResource(R.string.catalog_empty_subtitle),
                        actionLabel = if (state.filters != CatalogFilters()) {
                            stringResource(R.string.action_clear)
                        } else {
                            stringResource(R.string.action_refresh)
                        },
                        onAction = {
                            if (state.filters != CatalogFilters()) {
                                viewModel.clearFilters()
                            } else {
                                viewModel.refreshAll()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("catalog_books_grid")
                    ) {
                        if (state.error != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ErrorStateBanner(
                                    message = state.error!!,
                                    onRetry = { viewModel.loadBooks(page = 1, reset = true) }
                                )
                            }
                        }

                        // Editorial Featured Volume Spotlight on default browse
                        if (featuredBook != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FeaturedVolumeSpotlightCard(
                                    book = featuredBook,
                                    placeholderUrl = placeholderUrl,
                                    isWishlisted = featuredBook.id in wishlistIds,
                                    isBusy = state.busyBookId == featuredBook.id,
                                    onClick = { onBookClick(featuredBook.slug) },
                                    onToggleWishlist = { viewModel.toggleWishlist(featuredBook, onRequireAuth) },
                                    onQuickAdd = { viewModel.quickAddToBag(featuredBook, onRequireAuth) }
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EditorialSectionHeader(
                                    title = "All Volumes",
                                    subtitle = "Explore our latest titles",
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }
                        }

                        items(gridBooks, key = { it.id }) { book ->
                            BookCatalogCard(
                                book = book,
                                placeholderUrl = placeholderUrl,
                                isWishlisted = book.id in wishlistIds,
                                isBusy = state.busyBookId == book.id,
                                onClick = { onBookClick(book.slug) },
                                onToggleWishlist = { viewModel.toggleWishlist(book, onRequireAuth) },
                                onQuickAdd = { viewModel.quickAddToBag(book, onRequireAuth) }
                            )
                        }

                        if (state.canLoadMore || state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.loadNextPage() },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("catalog_load_more_button")
                                        ) {
                                            Text(stringResource(R.string.catalog_load_more))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        CatalogFilterBottomSheet(
            currentFilters = state.filters,
            onApply = { minPrice, maxPrice, inStockOnly, sort ->
                viewModel.applyAdvancedFilters(minPrice, maxPrice, inStockOnly, sort)
                showFilterSheet = false
            },
            onReset = {
                viewModel.clearFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun FeaturedVolumeSpotlightCard(
    book: BookDto,
    placeholderUrl: String?,
    isWishlisted: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF174437),
        Color(0xFF0E2C23)
    )
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("book_card_${book.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.height * 0.85f,
                        center = Offset(size.width * 0.92f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color(0xFFE08E45).copy(alpha = 0.10f),
                        radius = size.height * 0.42f,
                        center = Offset(size.width * 0.10f, size.height * 0.95f)
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCoverImage(
                    url = book.coverUrl,
                    placeholderUrl = placeholderUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(108.dp)
                        .aspectRatio(0.68f),
                    cornerRadius = 10.dp,
                    elevation = 10.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE08E45).copy(alpha = 0.22f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFF6C67A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "FEATURED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF6C67A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AnimatedWishlistButton(
                            isWishlisted = isWishlisted,
                            onClick = onToggleWishlist,
                            testTag = "book_wishlist_button_${book.id}",
                            containerColor = Color.White.copy(alpha = 0.16f)
                        )
                    }

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFAF7F2),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD7E4DF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    RatingStarsRow(
                        rating = book.averageRating,
                        reviewsCount = book.reviewsCount,
                        starSize = 14.dp,
                        textColor = Color(0xFFD7E4DF)
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatUsdPrice(book.price),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFF6C67A),
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = onQuickAdd,
                            enabled = book.inStock && !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFAF7F2),
                                contentColor = Color(0xFF174437)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("book_quick_add_${book.id}")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF174437)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (book.inStock) "Add" else "Sold Out",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCatalogCard(
    book: BookDto,
    placeholderUrl: String?,
    isWishlisted: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onQuickAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("book_card_${book.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
            ) {
                BookCoverImage(
                    url = book.coverUrl,
                    placeholderUrl = placeholderUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 12.dp,
                    elevation = 4.dp
                )

                AnimatedWishlistButton(
                    isWishlisted = isWishlisted,
                    onClick = onToggleWishlist,
                    testTag = "book_wishlist_button_${book.id}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )

                if (book.hasSample) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                    ) {
                        Text(
                            text = "Sample",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            book.category?.name?.let { cat ->
                Text(
                    text = cat.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
            }

            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            RatingStarsRow(
                rating = book.averageRating,
                reviewsCount = book.reviewsCount,
                starSize = 13.dp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = formatUsdPrice(book.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StockBadge(inStock = book.inStock, stockCount = book.stock)
                }

                FilledIconButton(
                    onClick = onQuickAdd,
                    enabled = book.inStock && !isBusy,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("book_quick_add_${book.id}")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AddShoppingCart,
                            contentDescription = stringResource(R.string.book_add_to_bag),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogFilterBottomSheet(
    currentFilters: CatalogFilters,
    onApply: (minPrice: String, maxPrice: String, inStockOnly: Boolean, sort: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var minPrice by remember { mutableStateOf(currentFilters.minPrice) }
    var maxPrice by remember { mutableStateOf(currentFilters.maxPrice) }
    var inStockOnly by remember { mutableStateOf(currentFilters.inStockOnly) }
    var selectedSort by remember { mutableStateOf(currentFilters.sort) }

    val sortOptions = listOf(
        "latest" to stringResource(R.string.catalog_sort_latest),
        "price_asc" to stringResource(R.string.catalog_sort_price_asc),
        "price_desc" to stringResource(R.string.catalog_sort_price_desc),
        "title_asc" to stringResource(R.string.catalog_sort_title_asc),
        "title_desc" to stringResource(R.string.catalog_sort_title_desc)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.catalog_filters),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("filter_reset_button")
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            }

            Text(
                text = "Sort Order",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    sortOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSort = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("sort_option_$key"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSort == key,
                                onClick = { selectedSort = key }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedSort == key) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    label = { Text(stringResource(R.string.catalog_min_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_min_price_input")
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    label = { Text(stringResource(R.string.catalog_max_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_max_price_input")
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.catalog_in_stock_only),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = inStockOnly,
                        onCheckedChange = { inStockOnly = it },
                        modifier = Modifier.testTag("filter_in_stock_switch")
                    )
                }
            }

            Button(
                onClick = { onApply(minPrice, maxPrice, inStockOnly, selectedSort) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("filter_apply_button")
            ) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }
}
