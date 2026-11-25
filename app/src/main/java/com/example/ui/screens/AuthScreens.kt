package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ValidatedTextField
import com.example.ui.viewmodel.AuthProfileViewModel

@Composable
private fun AuthEditorialHeader(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String
) {
    val primary = MaterialTheme.colorScheme.primary
    val deepEmerald = Color(0xFF0F3228)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, deepEmerald)
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.92f, size.height * 0.15f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.maxDimension * 0.3f,
                    center = Offset(size.width * 0.12f, size.height * 0.95f)
                )
            }
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun AuthErrorBanner(message: String) {
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
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val formState by viewModel.authFormState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearAuthFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_sign_in)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("login_back_button")
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AuthEditorialHeader(
                    icon = Icons.Filled.MenuBook,
                    eyebrow = stringResource(R.string.app_name),
                    title = stringResource(R.string.auth_welcome),
                    subtitle = "Sign in with your email and password to sync your shopping bag, saved wishlist, and orders."
                )

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
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            errorText = formState.fieldError("email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "login_email_input"
                        )

                        ValidatedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.auth_password),
                            errorText = formState.fieldError("password"),
                            isPassword = true,
                            testTag = "login_password_input"
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = onNavigateToForgotPassword,
                                modifier = Modifier.testTag("login_forgot_password_button")
                            ) {
                                Text(stringResource(R.string.auth_forgot_password))
                            }
                        }

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            AuthErrorBanner(formState.error!!)
                        }

                        Button(
                            onClick = { viewModel.login(email, password, onLoginSuccess) },
                            enabled = !formState.isSubmitting && email.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("login_submit_button")
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
                                text = stringResource(R.string.action_sign_in),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New to BookStore?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.testTag("login_go_register_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_sign_up),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val formState by viewModel.authFormState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearAuthFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_create_account)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("register_back_button")
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AuthEditorialHeader(
                    icon = Icons.Filled.PersonAdd,
                    eyebrow = "Reader Membership",
                    title = stringResource(R.string.auth_create_account),
                    subtitle = "Join our bookshop to curate your wishlist, review titles, and track deliveries."
                )

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
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.auth_full_name),
                            errorText = formState.fieldError("name"),
                            testTag = "register_name_input"
                        )

                        ValidatedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            errorText = formState.fieldError("email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "register_email_input"
                        )

                        ValidatedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.auth_password),
                            errorText = formState.fieldError("password"),
                            isPassword = true,
                            testTag = "register_password_input"
                        )

                        Text(
                            text = "Use at least 8 characters for a secure password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ValidatedTextField(
                            value = passwordConfirmation,
                            onValueChange = { passwordConfirmation = it },
                            label = stringResource(R.string.auth_confirm_password),
                            errorText = formState.fieldError("password_confirmation"),
                            isPassword = true,
                            testTag = "register_password_confirm_input"
                        )

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            AuthErrorBanner(formState.error!!)
                        }

                        Button(
                            onClick = {
                                viewModel.register(
                                    name,
                                    email,
                                    password,
                                    passwordConfirmation,
                                    onRegisterSuccess
                                )
                            },
                            enabled = !formState.isSubmitting && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("register_submit_button")
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
                                text = stringResource(R.string.action_sign_up),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.testTag("register_go_login_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_sign_in),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit,
    onNavigateToResetPassword: (String) -> Unit
) {
    val formState by viewModel.authFormState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var sentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.clearAuthFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_forgot_password)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("forgot_back_button")
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AuthEditorialHeader(
                    icon = Icons.Filled.LockReset,
                    eyebrow = "Account Recovery",
                    title = "Reset Your Password",
                    subtitle = "Enter your registered email address and we will send you a password reset token."
                )

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
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            errorText = formState.fieldError("email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "forgot_email_input"
                        )

                        if (!sentMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = sentMessage!!,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            AuthErrorBanner(formState.error!!)
                        }

                        Button(
                            onClick = {
                                viewModel.forgotPassword(email) { msg -> sentMessage = msg }
                            },
                            enabled = !formState.isSubmitting && email.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("forgot_submit_button")
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
                                text = stringResource(R.string.auth_send_reset_link),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigateToResetPassword(email) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("forgot_enter_token_button")
                        ) {
                            Text("Have a reset token? Reset Password")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    initialEmail: String,
    viewModel: AuthProfileViewModel,
    onBack: () -> Unit,
    onResetComplete: () -> Unit
) {
    val formState by viewModel.authFormState.collectAsStateWithLifecycle()
    var token by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearAuthFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_reset_password)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reset_back_button")
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AuthEditorialHeader(
                    icon = Icons.Filled.Key,
                    eyebrow = "New Credentials",
                    title = stringResource(R.string.auth_reset_password),
                    subtitle = "Enter the reset token from your email along with a new password."
                )

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
                            value = token,
                            onValueChange = { token = it },
                            label = stringResource(R.string.auth_reset_token),
                            errorText = formState.fieldError("token"),
                            testTag = "reset_token_input"
                        )
                        ValidatedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            errorText = formState.fieldError("email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "reset_email_input"
                        )
                        ValidatedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.profile_new_password),
                            errorText = formState.fieldError("password"),
                            isPassword = true,
                            testTag = "reset_password_input"
                        )
                        ValidatedTextField(
                            value = passwordConfirmation,
                            onValueChange = { passwordConfirmation = it },
                            label = stringResource(R.string.auth_confirm_password),
                            errorText = formState.fieldError("password_confirmation"),
                            isPassword = true,
                            testTag = "reset_password_confirm_input"
                        )

                        if (!formState.error.isNullOrBlank() && formState.fieldErrors.isEmpty()) {
                            AuthErrorBanner(formState.error!!)
                        }

                        Button(
                            onClick = {
                                viewModel.resetPassword(
                                    token,
                                    email,
                                    password,
                                    passwordConfirmation,
                                    onResetComplete
                                )
                            },
                            enabled = !formState.isSubmitting && token.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("reset_submit_button")
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
                                text = stringResource(R.string.auth_reset_password),
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
