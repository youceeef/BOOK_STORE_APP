package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ValidatedTextField
import com.example.ui.components.formatUsdPrice
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.CartCheckoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CartCheckoutViewModel,
    onBack: () -> Unit,
    onOrderConfirmed: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.appConfig.collectAsStateWithLifecycle()

    val prefilledAddress = remember(user) {
        listOfNotNull(
            user?.addressLine1?.takeIf { it.isNotBlank() },
            user?.addressLine2?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }

    var name by remember(user?.name) { mutableStateOf(user?.name.orEmpty()) }
    var address by remember(prefilledAddress) { mutableStateOf(prefilledAddress) }
    var city by remember(user?.city) { mutableStateOf(user?.city.orEmpty()) }
    var postalCode by remember(user?.postalCode) { mutableStateOf(user?.postalCode.orEmpty()) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetCheckoutState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.checkout_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val completedOrderId = state.completedOrderId
        if (completedOrderId != null) {
            // Celebratory Order Confirmation State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(110.dp)
                    ) {}
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.24f),
                        shape = CircleShape,
                        modifier = Modifier.size(82.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Order Placed!",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Order #$completedOrderId is confirmed and your volumes have been allocated for dispatch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp)
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { onOrderConfirmed(completedOrderId) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("checkout_view_order_button")
                ) {
                    Text("View Order #$completedOrderId")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2-Step Checkout Progress Header
                    CheckoutStepIndicator(
                        activeStep = if (state.activePendingCheckout != null) 2 else 1
                    )

                    // Stock reservation notice (Section 4.1 & Section 6)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.checkout_reservation_note,
                                    config.reservationMinutes
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Shipping Address Form (exact fields: name, address, city, postal_code)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        border = BorderStroke(
                            0.75.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.checkout_shipping_header),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            ValidatedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.checkout_full_name),
                                errorText = state.checkoutFieldErrors["name"]?.firstOrNull(),
                                testTag = "checkout_name_input"
                            )

                            ValidatedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = stringResource(R.string.checkout_address),
                                errorText = state.checkoutFieldErrors["address"]?.firstOrNull(),
                                testTag = "checkout_address_input"
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ValidatedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = stringResource(R.string.checkout_city),
                                    errorText = state.checkoutFieldErrors["city"]?.firstOrNull(),
                                    testTag = "checkout_city_input",
                                    modifier = Modifier.weight(1f)
                                )
                                ValidatedTextField(
                                    value = postalCode,
                                    onValueChange = { postalCode = it },
                                    label = stringResource(R.string.checkout_postal_code),
                                    errorText = state.checkoutFieldErrors["postal_code"]?.firstOrNull(),
                                    testTag = "checkout_postal_code_input",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Order Summary Receipt Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(
                            0.75.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Order Summary (${cart.itemsCount} items)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            cart.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity}× ${item.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = formatUsdPrice(item.subtotal),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryAmountRow(
                                label = stringResource(R.string.cart_subtotal),
                                amount = formatUsdPrice(cart.subtotal)
                            )
                            if (cart.discount > 0.0) {
                                SummaryAmountRow(
                                    label = "Coupon (${cart.couponCode ?: ""})",
                                    amount = "-${formatUsdPrice(cart.discount)}",
                                    amountColor = SuccessGreen
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(R.string.cart_total)} (${config.currency})",
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
                        }
                    }

                    if (!state.checkoutError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.checkoutError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    val isFreeOrder = cart.total <= 0.0 && cart.items.isNotEmpty()
                    Button(
                        onClick = {
                            viewModel.initiateCheckout(
                                name = name,
                                address = address,
                                city = city,
                                postalCode = postalCode
                            )
                        },
                        enabled = !state.isCheckoutBusy && cart.items.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                            .testTag("checkout_pay_button")
                    ) {
                        if (state.isCheckoutBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Processing…")
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isFreeOrder) {
                                    stringResource(R.string.checkout_place_free_order)
                                } else {
                                    "${stringResource(R.string.checkout_pay_now)} · ${formatUsdPrice(cart.total)}"
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckoutStepIndicator(activeStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepBadge(number = 1, label = "Shipping", isActive = activeStep >= 1, isCompleted = activeStep > 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(2.dp)
                .background(
                    if (activeStep > 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
        StepBadge(number = 2, label = "Payment", isActive = activeStep >= 2, isCompleted = false)
    }
}

@Composable
private fun StepBadge(
    number: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
