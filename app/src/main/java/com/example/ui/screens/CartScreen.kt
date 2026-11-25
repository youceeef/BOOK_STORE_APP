package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.remote.dto.CartItemDto
import com.example.ui.components.*
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.CartCheckoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartCheckoutViewModel,
    isLoggedIn: Boolean,
    placeholderUrl: String?,
    onBookClick: (String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var couponInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.refreshCart()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Editorial Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isLoggedIn && cart.itemsCount > 0) {
                    Text(
                        text = "${cart.itemsCount} volume(s) in your bag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoggedIn) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cart.items.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("cart_clear_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_clear),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { viewModel.refreshCart() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("cart_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        when {
            !isLoggedIn -> {
                EmptyStateView(
                    icon = Icons.Outlined.Lock,
                    title = "Sign in to access your shopping bag",
                    subtitle = "Your bag is persisted server-side across sessions.",
                    actionLabel = stringResource(R.string.action_sign_in),
                    onAction = onNavigateToLogin,
                    modifier = Modifier.fillMaxSize()
                )
            }
            state.isLoadingCart && cart.items.isEmpty() -> {
                CardListSkeleton(count = 3, testTag = "cart_loading")
            }
            cart.items.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoadingCart,
                    onRefresh = { viewModel.refreshCart() },
                    modifier = Modifier.weight(1f)
                ) {
                    EmptyStateView(
                        icon = Icons.Filled.ShoppingBag,
                        title = stringResource(R.string.cart_empty_title),
                        subtitle = stringResource(R.string.cart_empty_subtitle),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoadingCart,
                    onRefresh = { viewModel.refreshCart() },
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("cart_items_list")
                    ) {
                        items(cart.items, key = { it.bookId }) { item ->
                            CartItemRow(
                                item = item,
                                placeholderUrl = placeholderUrl,
                                isBusy = state.busyItemBookId == item.bookId,
                                onClick = { onBookClick(item.slug) },
                                onUpdateQty = { newQty -> viewModel.updateQuantity(item.bookId, newQty) },
                                onRemove = { viewModel.removeItem(item.bookId) }
                            )
                        }

                        // Promo / Coupon Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(
                                    0.75.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocalOffer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Discount Coupon",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    if (!cart.couponCode.isNullOrBlank()) {
                                        InputChip(
                                            selected = true,
                                            onClick = { viewModel.removeCoupon() },
                                            label = {
                                                Text(
                                                    text = "${cart.couponCode}  ·  -${formatUsdPrice(cart.discount)}",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove coupon",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier.testTag("cart_remove_coupon_chip")
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = couponInput,
                                            onValueChange = { couponInput = it },
                                            placeholder = { Text(stringResource(R.string.cart_coupon_hint)) },
                                            isError = !state.couponError.isNullOrBlank(),
                                            supportingText = if (!state.couponError.isNullOrBlank()) {
                                                { Text(state.couponError!!, color = MaterialTheme.colorScheme.error) }
                                            } else {
                                                null
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("cart_coupon_input")
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.applyCoupon(couponInput)
                                                couponInput = ""
                                            },
                                            enabled = !state.isCouponBusy && couponInput.isNotBlank(),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 56.dp)
                                                .testTag("cart_apply_coupon_button")
                                        ) {
                                            if (state.isCouponBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(stringResource(R.string.action_apply))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }

                // Sticky Bottom Order Summary & Checkout Dock
                val hasOutOfStockItem = cart.items.any { !it.inStock }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryAmountRow(
                                label = stringResource(R.string.cart_subtotal),
                                amount = formatUsdPrice(cart.subtotal)
                            )
                            if (cart.discount > 0.0) {
                                SummaryAmountRow(
                                    label = stringResource(R.string.cart_discount),
                                    amount = "-${formatUsdPrice(cart.discount)}",
                                    amountColor = SuccessGreen
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.cart_total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatUsdPrice(cart.total),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasOutOfStockItem) {
                                Text(
                                    text = "One or more items exceed available stock. Adjust quantities before checkout.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Button(
                                onClick = onProceedToCheckout,
                                enabled = cart.items.isNotEmpty() && !hasOutOfStockItem,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("cart_proceed_checkout_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.cart_proceed_checkout),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.cart_clear_bag)) },
            text = { Text("Remove all items from your shopping bag?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearCart()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("cart_confirm_clear_button")
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItemDto,
    placeholderUrl: String?,
    isBusy: Boolean,
    onClick: () -> Unit,
    onUpdateQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cart_item_${item.bookId}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BookCoverImage(
                url = item.coverUrl,
                placeholderUrl = placeholderUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(74.dp)
                    .aspectRatio(0.70f),
                cornerRadius = 9.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRemove,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cart_item_remove_${item.bookId}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.action_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${formatUsdPrice(item.price)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.inStock) {
                    StockBadge(inStock = false, stockCount = item.stock)
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateQty(item.quantity - 1) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("cart_item_minus_${item.bookId}")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                            IconButton(
                                onClick = { onUpdateQty(item.quantity + 1) },
                                enabled = !isBusy && item.quantity < item.stock,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("cart_item_plus_${item.bookId}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }

                    Text(
                        text = formatUsdPrice(item.subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryAmountRow(
    label: String,
    amount: String,
    amountColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}
