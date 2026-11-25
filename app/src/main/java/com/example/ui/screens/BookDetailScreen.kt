package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.remote.dto.BookDetailDto
import com.example.data.remote.dto.RelatedBookDto
import com.example.data.remote.dto.ReviewDto
import com.example.ui.components.*
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.StarGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.BookDetailViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    slugOrId: String,
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onRelatedBookClick: (String) -> Unit,
    onRequireAuth: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val wishlistIds by viewModel.wishlistBookIds.collectAsStateWithLifecycle()
    val placeholderUrl by viewModel.coverPlaceholderUrl.collectAsStateWithLifecycle()

    var quantity by remember(slugOrId) { mutableIntStateOf(1) }
    var showSampleSheet by remember { mutableStateOf(false) }
    var showReviewSheet by remember { mutableStateOf(false) }

    LaunchedEffect(slugOrId) {
        viewModel.loadBook(slugOrId)
    }

    LaunchedEffect(state.reviewSubmittedSuccess) {
        if (state.reviewSubmittedSuccess) {
            showReviewSheet = false
            viewModel.clearReviewFormState()
        }
    }

    val book = state.book
    val isWishlisted = book != null && book.id in wishlistIds

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = book?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("book_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (book != null) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            AnimatedWishlistButton(
                                isWishlisted = isWishlisted,
                                onClick = { viewModel.toggleWishlist(onRequireAuth) },
                                testTag = "book_detail_wishlist_button",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (book != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Pill quantity selector
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        enabled = quantity > 1 && book.inStock,
                                        modifier = Modifier.testTag("book_detail_qty_minus")
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                    }
                                    Text(
                                        text = "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { if (quantity < book.stock) quantity++ },
                                        enabled = quantity < book.stock && book.inStock,
                                        modifier = Modifier.testTag("book_detail_qty_plus")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.addToBag(quantity, onRequireAuth) },
                                enabled = book.inStock && !state.isAddingToBag,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("book_detail_add_to_bag_button")
                            ) {
                                if (state.isAddingToBag) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (book.inStock) {
                                        "${stringResource(R.string.book_add_to_bag)} · ${formatUsdPrice(book.price * quantity)}"
                                    } else {
                                        stringResource(R.string.out_of_stock)
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading && book == null -> {
                BookDetailSkeleton(modifier = Modifier.padding(innerPadding))
            }
            state.error != null && book == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateBanner(
                        message = state.error!!,
                        onRetry = { viewModel.loadBook(slugOrId) }
                    )
                }
            }
            book != null -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("book_detail_content")
                ) {
                    // Atmospheric Book Backdrop Hero
                    item {
                        BookDetailHero(
                            book = book,
                            placeholderUrl = placeholderUrl,
                            onReadSample = { showSampleSheet = true }
                        )
                    }

                    // Structured Metadata Ribbon
                    item {
                        BookMetadataRibbon(
                            book = book,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Description
                    if (!book.description.isNullOrBlank()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EditorialSectionHeader(title = "About This Volume")
                                Text(
                                    text = book.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 25.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Rating Breakdown + Write Review Button
                    item {
                        RatingSummaryCard(
                            book = book,
                            onWriteReview = {
                                viewModel.clearReviewFormState()
                                showReviewSheet = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Reader Reviews Header
                    item {
                        EditorialSectionHeader(
                            title = "${stringResource(R.string.book_reviews_section)} (${book.reviewsCount})",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (state.reviews.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "No reader reviews yet. Be the first to share your thoughts on this edition.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(state.reviews, key = { it.id }) { review ->
                            ReviewCard(
                                review = review,
                                isVoting = state.votingReviewId == review.id,
                                hasVoted = review.id in state.votedHelpfulIds,
                                onHelpfulClick = {
                                    viewModel.markReviewHelpful(review.id, onRequireAuth)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        if (state.canLoadMoreReviews || state.isLoadingMoreReviews) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoadingMoreReviews) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.loadMoreReviews() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("book_load_more_reviews_button")
                                        ) {
                                            Text("Load more reviews")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Related Books Carousel
                    if (book.relatedBooks.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditorialSectionHeader(
                                    title = stringResource(R.string.book_related_titles),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(book.relatedBooks, key = { it.id }) { related ->
                                        RelatedBookCard(
                                            book = related,
                                            placeholderUrl = placeholderUrl,
                                            onClick = { onRelatedBookClick(related.slug) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSampleSheet && book != null) {
        ModalBottomSheet(
            onDismissRequest = { showSampleSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (book.readingTimeMinutes != null) {
                            "${stringResource(R.string.book_sample_excerpt)} · ${book.readingTimeMinutes} min read"
                        } else {
                            stringResource(R.string.book_sample_excerpt)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = book.sampleExcerpt ?: "Sample excerpt unavailable.",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontSize = 17.sp,
                        lineHeight = 29.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }

    if (showReviewSheet) {
        WriteReviewBottomSheet(
            isSubmitting = state.isSubmittingReview,
            generalError = state.reviewError,
            fieldErrors = state.reviewFieldErrors,
            onSubmit = { rating, title, comment ->
                viewModel.submitReview(rating, title, comment, onRequireAuth)
            },
            onDismiss = { showReviewSheet = false }
        )
    }
}

@Composable
private fun BookDetailHero(
    book: BookDetailDto,
    placeholderUrl: String?,
    onReadSample: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            BookCoverImage(
                url = book.coverUrl,
                placeholderUrl = placeholderUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(138.dp)
                    .aspectRatio(0.68f),
                cornerRadius = 12.dp,
                elevation = 10.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                book.category?.name?.let { cat ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RatingStarsRow(
                    rating = book.averageRating,
                    reviewsCount = book.reviewsCount,
                    starSize = 16.dp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = formatUsdPrice(book.price),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                StockBadge(inStock = book.inStock, stockCount = book.stock)

                if (book.hasSample && !book.sampleExcerpt.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onReadSample,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("book_detail_read_sample_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (book.readingTimeMinutes != null) {
                                "${stringResource(R.string.book_read_sample)} (${book.readingTimeMinutes}m)"
                            } else {
                                stringResource(R.string.book_read_sample)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookMetadataRibbon(
    book: BookDetailDto,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetadataCell(
                label = "Inventory",
                value = if (book.inStock) "${book.stock} in stock" else "Out of stock",
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.height(28.dp))
            MetadataCell(
                label = "ISBN",
                value = book.isbn?.takeIf { it.isNotBlank() } ?: "—",
                modifier = Modifier.weight(1.3f)
            )
            VerticalDivider(modifier = Modifier.height(28.dp))
            MetadataCell(
                label = "Currency",
                value = "USD",
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

@Composable
private fun MetadataCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RatingSummaryCard(
    book: BookDetailDto,
    onWriteReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "%.1f", book.averageRating),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RatingStarsRow(rating = book.averageRating, starSize = 16.dp)
                    Text(
                        text = "${book.reviewsCount} reader ratings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onWriteReview,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("book_detail_write_review_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.book_write_review))
                }
            }

            if (book.ratingBreakdown.size == 5) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    book.ratingBreakdown.forEachIndexed { index, bucket ->
                        val stars = 5 - index
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$stars★",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(24.dp)
                            )
                            LinearProgressIndicator(
                                progress = { (bucket.percentage.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = StarGold,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            Text(
                                text = "${bucket.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: ReviewDto,
    isVoting: Boolean,
    hasVoted: Boolean,
    onHelpfulClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    if (!review.user.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = review.user.avatarUrl,
                            contentDescription = review.user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = review.user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = review.user.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (review.isVerifiedPurchase) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.book_verified_purchase),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = formatIsoDateShort(review.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RatingStarsRow(rating = review.rating.toDouble(), starSize = 14.dp)
            }

            if (!review.title.isNullOrBlank()) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onHelpfulClick,
                    enabled = !isVoting && !hasVoted,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("review_helpful_button_${review.id}")
                ) {
                    if (isVoting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (hasVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.book_helpful)} (${review.helpfulCount})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedBookCard(
    book: RelatedBookDto,
    placeholderUrl: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag("related_book_${book.id}")
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            BookCoverImage(
                url = book.coverUrl,
                placeholderUrl = placeholderUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f),
                cornerRadius = 9.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatUsdPrice(book.price),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteReviewBottomSheet(
    isSubmitting: Boolean,
    generalError: String?,
    fieldErrors: Map<String, List<String>>,
    onSubmit: (rating: Int, title: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var title by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.book_write_review),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "One review per book. Submitting again updates your existing review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.book_review_rating),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { rating = star },
                                modifier = Modifier.testTag("review_star_$star")
                            ) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = StarGold,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            ValidatedTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.book_review_title),
                errorText = fieldErrors["title"]?.firstOrNull(),
                testTag = "review_title_input"
            )

            ValidatedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = stringResource(R.string.book_review_comment),
                errorText = fieldErrors["comment"]?.firstOrNull(),
                singleLine = false,
                minLines = 4,
                testTag = "review_comment_input"
            )

            if (!generalError.isNullOrBlank() && fieldErrors.isEmpty()) {
                Text(
                    text = generalError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { onSubmit(rating, title, comment) },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("submit_review_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.book_submit_review))
            }
        }
    }
}
