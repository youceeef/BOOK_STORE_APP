package com.example.ui.navigation

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.components.EmailVerificationDialog
import com.example.ui.components.WelcomeUserPopupOverlay
import com.example.ui.screens.*
import com.example.ui.viewmodel.*
import java.io.File
import java.util.Locale

object Routes {
    const val CATALOG = "catalog"
    const val WISHLIST = "wishlist"
    const val CART = "cart"
    const val ACCOUNT = "account"
    const val BOOK_DETAIL = "book_detail/{slug}"
    const val CHECKOUT = "checkout"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password?email={email}"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val DELETE_ACCOUNT = "delete_account"

    fun bookDetail(slug: String): String = "book_detail/${Uri.encode(slug)}"
    fun orderDetail(orderId: Long): String = "order_detail/$orderId"
    fun resetPassword(email: String): String = "reset_password?email=${Uri.encode(email)}"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(Routes.CATALOG, R.string.nav_catalog, Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_catalog"),
    TopLevelTab(Routes.WISHLIST, R.string.nav_wishlist, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_wishlist"),
    TopLevelTab(Routes.CART, R.string.nav_bag, Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "nav_bag"),
    TopLevelTab(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_account")
)

@Composable
fun BookStoreApp(
    appViewModel: AppViewModel,
    catalogViewModel: CatalogViewModel,
    bookDetailViewModel: BookDetailViewModel,
    cartCheckoutViewModel: CartCheckoutViewModel,
    ordersViewModel: OrdersViewModel,
    authProfileViewModel: AuthProfileViewModel
) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val appConfig by appViewModel.appConfig.collectAsStateWithLifecycle()
    val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()
    val cart by appViewModel.cart.collectAsStateWithLifecycle()
    val authToken by appViewModel.authToken.collectAsStateWithLifecycle()
    val currentLocale by appViewModel.locale.collectAsStateWithLifecycle()

    val isLoggedIn = !authToken.isNullOrBlank()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val baseContext = LocalContext.current

    // Apply locale configuration & RTL layout direction (Arabic -> RTL per Section 10)
    val localizedContext = remember(baseContext, currentLocale) {
        val localeObj = Locale(currentLocale)
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(localeObj)
            setLayoutDirection(localeObj)
        }
        baseContext.createConfigurationContext(config)
    }
    val layoutDirection = if (currentLocale.equals("ar", ignoreCase = true)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    // Observe global snackbars
    LaunchedEffect(Unit) {
        appViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // Observe 401 Unauthorized -> clear credentials & route to Login (Section 2 & 8)
    LaunchedEffect(Unit) {
        appViewModel.navigateToLoginEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            navController.navigate(Routes.LOGIN) {
                launchSingleTop = true
            }
        }
    }

    // Observe downloaded PDF invoices and launch external PDF viewer via FileProvider
    LaunchedEffect(Unit) {
        ordersViewModel.invoiceFileReady.collect { pdfFile ->
            openPdfInvoice(baseContext, pdfFile) { errorMsg ->
                appViewModel.repository.emitSnackbar(errorMsg)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = remember { TOP_LEVEL_TABS.map { it.route }.toSet() }
    val showNavigationChrome = currentRoute in topLevelRoutes

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDirection
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(14.dp),
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                },
                bottomBar = {
                    if (showNavigationChrome && !useNavigationRail) {
                        Column {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                TOP_LEVEL_TABS.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        icon = {
                                            if (tab.route == Routes.CART && cart.itemsCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.secondary,
                                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                                        ) {
                                                            Text("${cart.itemsCount}")
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = stringResource(tab.labelRes)
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showNavigationChrome && useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            windowInsets = WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Vertical + WindowInsetsSides.Start
                            )
                        ) {
                            Spacer(Modifier.height(14.dp))
                            TOP_LEVEL_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {
                                        if (tab.route == Routes.CART && cart.itemsCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                                    ) {
                                                        Text("${cart.itemsCount}")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                        VerticalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.CATALOG,
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Routes.CATALOG) {
                            CatalogScreen(
                                viewModel = catalogViewModel,
                                appName = appConfig.appName,
                                onBookClick = { slug -> navController.navigate(Routes.bookDetail(slug)) },
                                onRequireAuth = { navController.navigate(Routes.LOGIN) }
                            )
                        }

                        composable(Routes.WISHLIST) {
                            WishlistScreen(
                                viewModel = authProfileViewModel,
                                isLoggedIn = isLoggedIn,
                                placeholderUrl = appConfig.coverPlaceholderUrl,
                                onBookClick = { slug -> navController.navigate(Routes.bookDetail(slug)) },
                                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
                            )
                        }

                        composable(Routes.CART) {
                            CartScreen(
                                viewModel = cartCheckoutViewModel,
                                isLoggedIn = isLoggedIn,
                                placeholderUrl = appConfig.coverPlaceholderUrl,
                                onBookClick = { slug -> navController.navigate(Routes.bookDetail(slug)) },
                                onProceedToCheckout = { navController.navigate(Routes.CHECKOUT) },
                                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
                            )
                        }

                        composable(Routes.ACCOUNT) {
                            AccountScreen(
                                user = currentUser,
                                isLoggedIn = isLoggedIn,
                                appConfig = appConfig,
                                currentLocale = currentLocale,
                                onRefreshProfile = { authProfileViewModel.refreshProfile() },
                                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                                onNavigateToOrders = { navController.navigate(Routes.ORDERS) },
                                onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                                onNavigateToDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
                                onOpenVerificationDialog = { appViewModel.openEmailVerificationDialog() },
                                onSelectLocale = { loc -> appViewModel.selectLocale(loc) },
                                onLogout = {
                                    authProfileViewModel.logout {
                                        navigateToTab(Routes.CATALOG)
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.BOOK_DETAIL,
                            arguments = listOf(navArgument("slug") { type = NavType.StringType })
                        ) { entry ->
                            val slug = entry.arguments?.getString("slug").orEmpty()
                            BookDetailScreen(
                                slugOrId = slug,
                                viewModel = bookDetailViewModel,
                                onBack = { navController.popBackStack() },
                                onRelatedBookClick = { relatedSlug ->
                                    navController.navigate(Routes.bookDetail(relatedSlug))
                                },
                                onRequireAuth = { navController.navigate(Routes.LOGIN) }
                            )
                        }

                        composable(Routes.CHECKOUT) {
                            CheckoutScreen(
                                viewModel = cartCheckoutViewModel,
                                onBack = { navController.popBackStack() },
                                onOrderConfirmed = { orderId ->
                                    navController.navigate(Routes.orderDetail(orderId)) {
                                        popUpTo(Routes.CART) { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable(Routes.ORDERS) {
                            OrdersListScreen(
                                viewModel = ordersViewModel,
                                onBack = { navController.popBackStack() },
                                onOrderClick = { id -> navController.navigate(Routes.orderDetail(id)) }
                            )
                        }

                        composable(
                            route = Routes.ORDER_DETAIL,
                            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
                        ) { entry ->
                            val orderId = entry.arguments?.getLong("orderId") ?: 0L
                            OrderDetailScreen(
                                orderId = orderId,
                                viewModel = ordersViewModel,
                                placeholderUrl = appConfig.coverPlaceholderUrl,
                                onBack = { navController.popBackStack() },
                                onBookClick = { slug -> navController.navigate(Routes.bookDetail(slug)) }
                            )
                        }

                        composable(Routes.LOGIN) {
                            LoginScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToRegister = {
                                    navController.navigate(Routes.REGISTER) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToForgotPassword = {
                                    navController.navigate(Routes.FORGOT_PASSWORD)
                                },
                                onLoginSuccess = {
                                    if (!navController.popBackStack()) {
                                        navigateToTab(Routes.CATALOG)
                                    }
                                }
                            )
                        }

                        composable(Routes.REGISTER) {
                            RegisterScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                },
                                onRegisterSuccess = {
                                    navController.popBackStack(Routes.CATALOG, inclusive = false)
                                }
                            )
                        }

                        composable(Routes.FORGOT_PASSWORD) {
                            ForgotPasswordScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToResetPassword = { email ->
                                    navController.navigate(Routes.resetPassword(email))
                                }
                            )
                        }

                        composable(
                            route = Routes.RESET_PASSWORD,
                            arguments = listOf(
                                navArgument("email") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { entry ->
                            val initialEmail = entry.arguments?.getString("email").orEmpty()
                            ResetPasswordScreen(
                                initialEmail = initialEmail,
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() },
                                onResetComplete = {
                                    navController.popBackStack(Routes.LOGIN, inclusive = false)
                                }
                            )
                        }

                        composable(Routes.EDIT_PROFILE) {
                            EditProfileScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.CHANGE_PASSWORD) {
                            ChangePasswordScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.DELETE_ACCOUNT) {
                            DeleteAccountScreen(
                                viewModel = authProfileViewModel,
                                onBack = { navController.popBackStack() },
                                onAccountDeleted = {
                                    navigateToTab(Routes.CATALOG)
                                }
                            )
                        }
                    }
                }
            }

            // Animated Welcome / Goodbye Pop-up shown upon login or logout
            WelcomeUserPopupOverlay(
                userName = appState.welcomePopup?.userName,
                avatarUrl = appState.welcomePopup?.avatarUrl,
                isLogout = appState.welcomePopup?.isLogout ?: false,
                onDismiss = { appViewModel.dismissWelcomePopup() }
            )
        }

        // Global 403 Email Not Verified Dialog (Section 2, 4.4, 8)
        if (appState.showEmailVerificationDialog) {
            EmailVerificationDialog(
                message = appState.emailVerificationMessage,
                feedback = appState.verificationFeedback,
                isResending = appState.isResendingVerification,
                onResend = { appViewModel.resendVerificationEmail() },
                onRefreshStatus = { appViewModel.refreshUserVerificationStatus() },
                onDismiss = { appViewModel.dismissEmailVerificationDialog() }
            )
        }

        // Global 503 Payment Gateway Maintenance Dialog (Section 1 & 8)
        if (appState.showPaymentMaintenanceDialog) {
            AlertDialog(
                onDismissRequest = { appViewModel.dismissPaymentMaintenanceDialog() },
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                title = { Text("Payment Gateway Unavailable") },
                text = {
                    Text(
                        appState.paymentMaintenanceMessage
                            ?: "Payment gateway is undergoing maintenance. Please try again later."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { appViewModel.dismissPaymentMaintenanceDialog() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }
    }
}

private fun openPdfInvoice(context: Context, pdfFile: File, onError: (String) -> Unit) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(viewIntent, "Open Invoice PDF").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(shareIntent))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        onError("Invoice saved to ${pdfFile.name}, but no PDF viewer was found.")
    }
}
