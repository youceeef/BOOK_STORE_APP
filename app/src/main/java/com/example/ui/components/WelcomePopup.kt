package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.StarGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WelcomeUserPopupOverlay(
    userName: String?,
    avatarUrl: String?,
    isLogout: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the last non-null popup state briefly so exit animation displays real content
    var displayedName by remember { mutableStateOf(userName.orEmpty()) }
    var displayedAvatar by remember { mutableStateOf(avatarUrl) }
    var displayedIsLogout by remember { mutableStateOf(isLogout) }
    if (!userName.isNullOrBlank()) {
        displayedName = userName
        displayedAvatar = avatarUrl
        displayedIsLogout = isLogout
    }

    val visible = !userName.isNullOrBlank()

    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    // Auto-dismiss progress countdown while visible
    val progress = remember { Animatable(1f) }
    LaunchedEffect(visible, displayedName, displayedIsLogout) {
        if (visible) {
            progress.snapTo(1f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4800, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(240)),
                exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(180)
                ) + fadeOut(tween(180))
            ) {
                AuthCelebrationPopupCard(
                    userName = displayedName,
                    avatarUrl = displayedAvatar,
                    isLogout = displayedIsLogout,
                    countdownProgress = progress.value,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun AuthCelebrationPopupCard(
    userName: String,
    avatarUrl: String?,
    isLogout: Boolean,
    countdownProgress: Float,
    onDismiss: () -> Unit
) {
    val gradientStart = if (isLogout) Color(0xFF9C4321) else MaterialTheme.colorScheme.primary
    val gradientEnd = if (isLogout) Color(0xFF3E190C) else Color(0xFF0E3026)
    val avatarGradientTop = if (isLogout) Color(0xFFB55427) else Color(0xFF286955)
    val avatarGradientBottom = if (isLogout) Color(0xFF5A2006) else Color(0xFF143C2F)
    val accentGlow = if (isLogout) Color(0xFFFFC078) else StarGold

    // Staggered entrance animations for avatar & typography
    val avatarScale = remember { Animatable(0.4f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(18f) }

    LaunchedEffect(userName, isLogout) {
        avatarScale.snapTo(0.4f)
        contentAlpha.snapTo(0f)
        contentOffsetY.snapTo(18f)
        launch {
            delay(80)
            avatarScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            delay(140)
            contentAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            delay(140)
            contentOffsetY.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }
    }

    // Gentle orbital shimmer & pulse for decorative stars & rings
    val infiniteTransition = rememberInfiniteTransition(label = "auth_popup_sparkle")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_orbit"
    )

    val initials = remember(userName) {
        userName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 18.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume taps inside card so scrim doesn't dismiss unintentionally
            )
            .testTag(if (isLogout) "goodbye_user_popup" else "welcome_user_popup")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Atmospheric Hero Header with Animated Sparkles & Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.54f

                    // Soft radial warm glow behind avatar
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentGlow.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = size.minDimension * 0.62f * pulse
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.62f * pulse
                    )

                    // Delicate concentric brass/gold rings
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = 52.dp.toPx() * pulse,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = accentGlow.copy(alpha = 0.22f),
                        radius = 68.dp.toPx() * (2f - pulse),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Orbiting star motes
                    val sparkleRadii = listOf(72.dp.toPx(), 84.dp.toPx(), 62.dp.toPx(), 78.dp.toPx(), 66.dp.toPx())
                    val sparkleAngles = listOf(20f, 110f, 200f, 290f, 340f)
                    sparkleAngles.forEachIndexed { idx, baseDeg ->
                        val rad = Math.toRadians((baseDeg + orbitAngle * if (idx % 2 == 0) 1f else -0.7f).toDouble())
                        val r = sparkleRadii[idx]
                        val sx = cx + (cos(rad) * r * 1.25f).toFloat()
                        val sy = cy + (sin(rad) * r * 0.72f).toFloat()
                        val dotRadius = if (idx % 2 == 0) 3.2.dp.toPx() else 2.2.dp.toPx()
                        drawCircle(
                            color = if (idx % 2 == 0) accentGlow.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.55f),
                            radius = dotRadius,
                            center = Offset(sx, sy)
                        )
                    }
                }

                // Close icon button in top-end
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("welcome_popup_close_button")
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss notification",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Centered spring-animated user emblem / avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 6.dp)
                        .scale(avatarScale.value),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(86.dp)
                            .border(2.5.dp, accentGlow.copy(alpha = 0.9f), CircleShape)
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(avatarGradientTop, avatarGradientBottom)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (initials != "?") {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Corner emblem badge pinned to bottom-end of avatar
                    Surface(
                        shape = CircleShape,
                        color = accentGlow,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .border(2.dp, gradientEnd, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLogout) Icons.Filled.WavingHand else Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF1A1404),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Warm Editorial Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = contentOffsetY.value.dp)
                    .alpha(contentAlpha.value)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isLogout) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isLogout) Icons.AutoMirrored.Filled.Logout else Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (isLogout) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isLogout) "SIGNED OUT" else "WELCOME BACK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLogout) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (isLogout) "See you soon, $userName!" else "Welcome, $userName!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("welcome_popup_title")
                )

                Text(
                    text = if (isLogout) {
                        "You've been safely signed out. Your saved volumes will be waiting whenever you return."
                    } else {
                        "Your personal library, saved wishlist, and shopping bag are synced and ready."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Button(
                    onClick = onDismiss,
                    colors = if (isLogout) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("welcome_popup_continue_button")
                ) {
                    Text(
                        text = if (isLogout) "Continue Browsing" else "Start Exploring",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Subtle countdown progress bar at the bottom edge
            LinearProgressIndicator(
                progress = { countdownProgress.coerceIn(0f, 1f) },
                color = if (isLogout) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}
