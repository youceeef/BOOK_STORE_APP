package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.remote.dto.BookDto
import com.example.ui.components.*
import com.example.ui.viewmodel.AuthProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: AuthProfileViewModel,
    isLoggedIn: Boolean,
    placeholderUrl: String?,
    onBookClick: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val state by viewModel.wishlistState.collectAsStateWithLifecycle()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.refreshWishlist()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.wishlist_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isLoggedIn && wishlist.isNotEmpty()) {
                    Text(
                        text = "${wishlist.size} saved volume(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoggedIn) {
                FilledTonalIconButton(
                    onClick = { viewModel.refreshWishlist() },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("wishlist_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        when {
            !isLoggedIn -> {
                EmptyStateView(
                    icon = Icons.Outlined.Lock,
                    title = "Sign in to view saved books",
                    subtitle = "Your wishlist is synced securely with your customer account.",
                    actionLabel = stringResource(R.string.action_sign_in),
                    onAction = onNavigateToLogin,
                    modifier = Modifier.fillMaxSize()
                )
            }
            state.isLoading && wishlist.isEmpty() -> {
                CardListSkeleton(count = 4, testTag = "wishlist_loading")
            }
            state.error != null && wishlist.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateBanner(
                        message = state.error!!,
                        onRetry = { viewModel.refreshWishlist() }
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading && wishlist.isNotEmpty(),
                    onRefresh = { viewModel.refreshWishlist() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (wishlist.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Filled.FavoriteBorder,
                            title = stringResource(R.string.wishlist_empty_title),
                            subtitle = stringResource(R.string.wishlist_empty_subtitle),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("wishlist_list")
                        ) {
                            items(wishlist, key = { it.id }) { book ->
                                WishlistBookRow(
                                    book = book,
                                    placeholderUrl = placeholderUrl,
                                    isBusy = state.busyBookId == book.id,
                                    onClick = { onBookClick(book.slug) },
                                    onMoveToBag = { viewModel.moveToCart(book.id) },
                                    onRemove = { viewModel.removeFromWishlist(book.id) }
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
private fun WishlistBookRow(
    book: BookDto,
    placeholderUrl: String?,
    isBusy: Boolean,
    onClick: () -> Unit,
    onMoveToBag: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("wishlist_item_${book.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BookCoverImage(
                url = book.coverUrl,
                placeholderUrl = placeholderUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(78.dp)
                    .aspectRatio(0.70f),
                cornerRadius = 9.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatUsdPrice(book.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StockBadge(inStock = book.inStock, stockCount = book.stock)
                }

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMoveToBag,
                        enabled = book.inStock && !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("wishlist_move_to_bag_${book.id}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.wishlist_move_to_bag))
                        }
                    }

                    OutlinedIconButton(
                        onClick = onRemove,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("wishlist_remove_${book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.action_remove)
                        )
                    }
                }
            }
        }
    }
}
