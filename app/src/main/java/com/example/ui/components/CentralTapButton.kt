package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.FloatingParticle
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldDark
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapNeonOrange
import com.example.ui.theme.TapOrangeAccent
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

/**
 * Central interactive 'Tap' button for mining virtual coins and incrementing
 * user-specific Firestore balances. Styled with a dark fintech aesthetic
 * featuring concentric metallic borders, glowing dynamic radial aura,
 * responsive 3D spring-physics compression, and floating coin burst particles.
 */
@Composable
fun CentralTapButton(
    modifier: Modifier = Modifier,
    coinsPerTap: Long,
    isTurboActive: Boolean = false,
    energyAvailable: Int,
    energyCost: Int = 1,
    totalTaps: Long = 0L,
    customCoinBitmap: Bitmap? = null,
    floatingParticles: List<FloatingParticle> = emptyList(),
    onTap: (x: Float, y: Float) -> Unit,
    onRemoveParticle: (id: String) -> Unit = {},
    onEnergyDepleted: () -> Unit = {},
    playAudioHaptic: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }

    // Spring animation for tactile 3D compression
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "centralTapScale"
    )

    // Ambient pulsing glow transition for the fintech aura
    val infiniteTransition = rememberInfiniteTransition(label = "auraPulse")
    val auraGlowRadius by infiniteTransition.animateFloat(
        initialValue = if (isTurboActive) 1.05f else 0.98f,
        targetValue = if (isTurboActive) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isTurboActive) 900 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    val isEnergyDepleted = energyAvailable < energyCost

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Central Button Component with Outer Aura & Concentric Rings
        Box(
            modifier = Modifier
                .size(290.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Ambient Neon Gold / Turbo Orange Radial Glow Halo
            Box(
                modifier = Modifier
                    .size(255.dp)
                    .scale(auraGlowRadius * (if (isPressed) 1.06f else 1.0f))
                    .shadow(
                        elevation = if (isTurboActive) 54.dp else 36.dp,
                        shape = CircleShape,
                        spotColor = when {
                            isEnergyDepleted -> Color(0x33FF5252)
                            isTurboActive -> TapNeonOrange
                            else -> TapGoldPrimary
                        },
                        ambientColor = when {
                            isEnergyDepleted -> Color(0x11FF5252)
                            isTurboActive -> TapOrangeAccent
                            else -> TapGoldDark
                        }
                    )
            )

            // Layer 2: Concentric Outer Ring Bevel (Dark Obsidian with Precision Border)
            Box(
                modifier = Modifier
                    .size(265.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF161C28),
                                Color(0xFF0C1017),
                                Color(0xFF080B10)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                if (isTurboActive) TapNeonOrange else TapGoldLight,
                                TapGoldDark,
                                Color(0xFF4A3B12),
                                if (isTurboActive) TapOrangeAccent else TapGoldPrimary,
                                if (isTurboActive) TapNeonOrange else TapGoldLight
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Layer 3: Inner Precision Beveled Button
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E2638),
                                    Color(0xFF111622),
                                    Color(0xFF0A0E16)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(240f, 240f)
                            )
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isTurboActive) TapNeonOrange else TapGoldLight,
                                    TapGoldDark,
                                    Color(0xFF2E2400)
                                )
                            ),
                            shape = CircleShape
                        )
                        .semantics {
                            contentDescription = "Tap to mint virtual coins. Current reward: $coinsPerTap coins per tap."
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isPressed = true
                                    playAudioHaptic()
                                    if (isEnergyDepleted) {
                                        onEnergyDepleted()
                                    } else {
                                        onTap(offset.x, offset.y)
                                    }
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        }
                        .testTag("tap_button")
                        .testTag("main_tap_coin"),
                    contentAlignment = Alignment.Center
                ) {
                    // Coin Center Emblem
                    if (customCoinBitmap != null) {
                        Image(
                            bitmap = customCoinBitmap.asImageBitmap(),
                            contentDescription = "Custom AI Coin",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.tap_coin),
                            contentDescription = "TAP Gold Coin Emblem",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    // Embossed specular glare overlay for tactile glass-finish feel
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isPressed) 0.18f else 0.08f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
            }

            // Floating +COINS Burst Indicator Particles
            floatingParticles.forEach { particle ->
                FloatingParticleItem(
                    particle = particle,
                    isTurbo = isTurboActive,
                    onAnimationEnd = { onRemoveParticle(particle.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fintech Mini Status Badge Below Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isTurboActive) Color(0xFF331A00) else TapGoldContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isTurboActive) TapNeonOrange else TapOutline
            ),
            modifier = Modifier.testTag("tap_rate_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isTurboActive) Icons.Default.RocketLaunch else Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = if (isTurboActive) TapNeonOrange else TapGoldPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTurboActive) "TURBO 2X ACTIVE: +$coinsPerTap / TAP" else "+$coinsPerTap COINS / TAP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = if (isTurboActive) TapNeonOrange else TapGoldLight
                )
                if (totalTaps > 0L) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TapTextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${NumberFormat.getNumberInstance(Locale.US).format(totalTaps)} TAPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TapTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingParticleItem(
    particle: FloatingParticle,
    isTurbo: Boolean,
    onAnimationEnd: () -> Unit
) {
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    val animScale = remember { Animatable(0.8f) }

    LaunchedEffect(particle.id) {
        animOffsetY.animateTo(
            targetValue = -140f,
            animationSpec = tween(durationMillis = 850, easing = LinearEasing)
        )
    }
    LaunchedEffect(particle.id) {
        animScale.animateTo(
            targetValue = 1.25f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(particle.id) {
        delay(400)
        animAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 450)
        )
        onAnimationEnd()
    }

    Text(
        text = particle.text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        color = if (isTurbo) TapNeonOrange else TapGoldPrimary,
        modifier = Modifier
            .offset { IntOffset(0, animOffsetY.value.toInt()) }
            .scale(animScale.value)
            .alpha(animAlpha.value)
    )
}
