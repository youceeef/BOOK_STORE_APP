package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.remote.dto.AppConfigDto
import com.example.data.remote.dto.UserDto
import com.example.data.repository.ProfileUpdateInput
import com.example.ui.components.ValidatedTextField
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AuthProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    user: UserDto?,
    isLoggedIn: Boolean,
    appConfig: AppConfigDto,
    currentLocale: String,
    onRefreshProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onOpenVerificationDialog: () -> Unit,
    onSelectLocale: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onRefreshProfile()
    }

    val primary = MaterialTheme.colorScheme.primary
    val deepEmerald = Color(0xFF0F3228)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (!isLoggedIn || user == null) {
                    // Editorial Guest Welcome Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primary, deepEmerald)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.06f),
                                    radius = size.maxDimension * 0.48f,
                                    center = Offset(size.width * 0.9f, size.height * 0.12f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.04f),
                                    radius = size.maxDimension * 0.34f,
                                    center = Offset(size.width * 0.1f, size.height * 0.92f)
                                )
                            }
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Your Reader Account",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Sign in or create an account to manage your shopping bag, saved wishlist, reviews, and order tracking.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onNavigateToLogin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = deepEmerald
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 50.dp)
                                        .testTag("account_sign_in_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_sign_in),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onNavigateToRegister,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 50.dp)
                                        .testTag("account_sign_up_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_sign_up),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Atmospheric Authenticated Profile Hero Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primary, deepEmerald)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.06f),
                                    radius = size.maxDimension * 0.45f,
                                    center = Offset(size.width * 0.92f, size.height * 0.14f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.04f),
                                    radius = size.maxDimension * 0.30f,
                                    center = Offset(size.width * 0.08f, size.height * 0.95f)
                                )
                            }
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    if (!user.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = user.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Surface(
                                        color = if (user.emailVerified) {
                                            Color.White.copy(alpha = 0.18f)
                                        } else {
                                            WarningAmber.copy(alpha = 0.28f)
                                        },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (user.emailVerified) Icons.Filled.Verified else Icons.Filled.MarkEmailUnread,
                                                contentDescription = null,
                                                tint = if (user.emailVerified) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (user.emailVerified) "Email Verified" else "Verification Needed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            if (!user.bio.isNullOrBlank()) {
                                Text(
                                    text = user.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.86f)
                                )
                            }

                            if (!user.emailVerified) {
                                Button(
                                    onClick = onOpenVerificationDialog,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = deepEmerald
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 46.dp)
                                        .testTag("account_verify_email_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MarkEmailUnread,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_resend_verification),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Customer Account Actions Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AccountMenuRow(
                                icon = Icons.Filled.ReceiptLong,
                                title = stringResource(R.string.orders_title),
                                subtitle = "View history, shipment tracking & PDF invoices",
                                testTag = "account_orders_row",
                                onClick = onNavigateToOrders
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            AccountMenuRow(
                                icon = Icons.Filled.Edit,
                                title = stringResource(R.string.profile_edit),
                                subtitle = "Name, email, avatar, phone, bio & address",
                                testTag = "account_edit_profile_row",
                                onClick = onNavigateToEditProfile
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            AccountMenuRow(
                                icon = Icons.Filled.Key,
                                title = stringResource(R.string.profile_change_password),
                                subtitle = "Update your account password",
                                testTag = "account_change_password_row",
                                onClick = onNavigateToChangePassword
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            AccountMenuRow(
                                icon = Icons.Filled.DeleteForever,
                                title = stringResource(R.string.profile_delete_account),
                                subtitle = "Permanently delete or anonymize account",
                                isDestructive = true,
                                testTag = "account_delete_account_row",
                                onClick = onNavigateToDeleteAccount
                            )
                        }
                    }
                }

                // Language Preferences Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Storefront language & automatic RTL layout for Arabic",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val availableLocales = appConfig.locales.ifEmpty { listOf("en", "fr", "ar") }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableLocales.forEach { loc ->
                                val label = when (loc.lowercase()) {
                                    "en" -> "English"
                                    "fr" -> "Français"
                                    "ar" -> "العربية"
                                    else -> loc.uppercase()
                                }
                                val isSelected = currentLocale.equals(loc, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectLocale(loc) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("locale_chip_$loc")
                                )
                            }
                        }
                    }
                }

                if (isLoggedIn) {
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("account_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_sign_out),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (isDestructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            },
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val formState by viewModel.profileFormState.collectAsStateWithLifecycle()

    var name by remember(user) { mutableStateOf(user?.name.orEmpty()) }
    var email by remember(user) { mutableStateOf(user?.email.orEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var bio by remember(user) { mutableStateOf(user?.bio.orEmpty()) }
    var addressLine1 by remember(user) { mutableStateOf(user?.addressLine1.orEmpty()) }
    var addressLine2 by remember(user) { mutableStateOf(user?.addressLine2.orEmpty()) }
    var city by remember(user) { mutableStateOf(user?.city.orEmpty()) }
    var stateField by remember(user) { mutableStateOf(user?.state.orEmpty()) }
    var postalCode by remember(user) { mutableStateOf(user?.postalCode.orEmpty()) }
    var country by remember(user) { mutableStateOf(user?.country.orEmpty()) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var removeAvatar by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker per Google Play Policy
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            removeAvatar = false
        }
    }

    LaunchedEffect(Unit) { viewModel.clearProfileFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_edit)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("edit_profile_back_button")
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
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar & Personal Details Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(78.dp)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                                    .clip(CircleShape)
                            ) {
                                val previewModel: Any? = when {
                                    removeAvatar -> null
                                    selectedAvatarUri != null -> selectedAvatarUri
                                    !user?.avatarUrl.isNullOrBlank() -> user?.avatarUrl
                                    else -> null
                                }
                                if (previewModel != null) {
                                    AsyncImage(
                                        model = previewModel,
                                        contentDescription = "Profile avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("edit_profile_pick_avatar_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Choose Photo (Max 2 MB)")
                                }
                                if (!user?.avatarUrl.isNullOrBlank() || selectedAvatarUri != null) {
                                    TextButton(
                                        onClick = {
                                            selectedAvatarUri = null
                                            removeAvatar = true
                                        },
                                        modifier = Modifier.testTag("edit_profile_remove_avatar_button")
                                    ) {
                                        Text(
                                            text = "Remove Avatar",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                formState.fieldError("avatar")?.let { err ->
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )

                        ValidatedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.auth_full_name),
                            errorText = formState.fieldError("name"),
                            testTag = "edit_profile_name_input"
                        )

                        ValidatedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            errorText = formState.fieldError("email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "edit_profile_email_input"
                        )
                        if (user != null && email.trim().lowercase() != user!!.email.lowercase()) {
                            Surface(
                                color = WarningAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Changing your email resets verification status until the new address is verified.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        ValidatedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = stringResource(R.string.profile_phone),
                            errorText = formState.fieldError("phone"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            testTag = "edit_profile_phone_input"
                        )

                        ValidatedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = stringResource(R.string.profile_bio),
                            errorText = formState.fieldError("bio"),
                            singleLine = false,
                            minLines = 3,
                            testTag = "edit_profile_bio_input"
                        )
                    }
                }

                // Shipping Address Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
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
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
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
                                text = "Shipping Address",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        ValidatedTextField(
                            value = addressLine1,
                            onValueChange = { addressLine1 = it },
                            label = stringResource(R.string.profile_address_line1),
                            errorText = formState.fieldError("address_line1"),
                            testTag = "edit_profile_address1_input"
                        )

                        ValidatedTextField(
                            value = addressLine2,
                            onValueChange = { addressLine2 = it },
                            label = stringResource(R.string.profile_address_line2),
                            errorText = formState.fieldError("address_line2"),
                            testTag = "edit_profile_address2_input"
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ValidatedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = stringResource(R.string.checkout_city),
                                errorText = formState.fieldError("city"),
                                testTag = "edit_profile_city_input",
                                modifier = Modifier.weight(1f)
                            )
                            ValidatedTextField(
                                value = stateField,
                                onValueChange = { stateField = it },
                                label = stringResource(R.string.profile_state),
                                errorText = formState.fieldError("state"),
                                testTag = "edit_profile_state_input",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ValidatedTextField(
                                value = postalCode,
                                onValueChange = { postalCode = it },
                                label = stringResource(R.string.checkout_postal_code),
                                errorText = formState.fieldError("postal_code"),
                                testTag = "edit_profile_postal_input",
                                modifier = Modifier.weight(1f)
                            )
                            ValidatedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = stringResource(R.string.profile_country),
                                errorText = formState.fieldError("country"),
                                testTag = "edit_profile_country_input",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = formState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            ProfileUpdateInput(
                                name = name,
                                email = email,
                                phone = phone,
                                bio = bio,
                                addressLine1 = addressLine1,
                                addressLine2 = addressLine2,
                                city = city,
                                state = stateField,
                                postalCode = postalCode,
                                country = country,
                                removeAvatar = removeAvatar,
                                avatarUri = selectedAvatarUri
                            ),
                            onSuccess = onBack
                        )
                    },
                    enabled = !formState.isSubmitting && name.isNotBlank() && email.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                        .testTag("edit_profile_save_button")
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit
) {
    val formState by viewModel.profileFormState.collectAsStateWithLifecycle()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearProfileFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_change_password)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("change_password_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Requires a verified email address. New password must be at least 8 characters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ValidatedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = stringResource(R.string.profile_current_password),
                            errorText = formState.fieldError("current_password"),
                            isPassword = true,
                            testTag = "change_password_current_input"
                        )
                        ValidatedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = stringResource(R.string.profile_new_password),
                            errorText = formState.fieldError("password"),
                            isPassword = true,
                            testTag = "change_password_new_input"
                        )
                        ValidatedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = stringResource(R.string.auth_confirm_password),
                            errorText = formState.fieldError("password_confirmation"),
                            isPassword = true,
                            testTag = "change_password_confirm_input"
                        )

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formState.error!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.updatePassword(
                                    currentPassword,
                                    newPassword,
                                    confirmPassword,
                                    onBack
                                )
                            },
                            enabled = !formState.isSubmitting && currentPassword.isNotBlank() && newPassword.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("change_password_submit_button")
                        ) {
                            if (formState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                text = stringResource(R.string.action_save),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val formState by viewModel.profileFormState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearProfileFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_delete_account)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("delete_account_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Permanent Action",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "If you have existing orders, your personal data will be permanently anonymized and all tokens revoked while retaining accounting records. If you have no orders, your account will be permanently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ValidatedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.profile_current_password),
                            errorText = formState.fieldError("password"),
                            isPassword = true,
                            testTag = "delete_account_password_input"
                        )

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            Text(
                                text = formState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { viewModel.deleteAccount(password, onAccountDeleted) },
                            enabled = !formState.isSubmitting && password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("delete_account_confirm_button")
                        ) {
                            if (formState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                text = stringResource(R.string.profile_delete_account),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
