package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.FloatingParticle
import com.example.ui.TapUiState
import com.example.ui.TapViewModel
import com.example.ui.theme.TapBlack
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldDark
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapGreenSuccess
import com.example.ui.theme.TapNeonOrange
import com.example.ui.theme.TapOrangeAccent
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import com.example.util.AudioHapticHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: TapUiState,
    viewModel: TapViewModel,
    audioHapticHelper: AudioHapticHelper,
    onNavigateToTasks: () -> Unit,
    onNavigateToBoosters: () -> Unit,
    onOpenAiStrategist: () -> Unit
) {
    val context = LocalContext.current
    var isCoinPressed by remember { mutableStateOf(false) }
    val coinScale by animateFloatAsState(
        targetValue = if (isCoinPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "coinScale"
    )

    var showAdRewardDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(audioHapticHelper.isSoundEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: User info & Sound Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, TapGoldPrimary, CircleShape)
                        .background(TapSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = uiState.profile?.photoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = (uiState.profile?.username?.take(1) ?: "T").uppercase(),
                            color = TapGoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hello, ${uiState.profile?.username ?: "Tapper"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TapTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TapGoldContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "LVL ${uiState.profile?.accountLevel ?: 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TapGoldLight,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (uiState.profile?.role == "admin") "ADMIN" else "MEMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.profile?.role == "admin") TapGreenSuccess else TapTextMuted
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        soundEnabled = !soundEnabled
                        audioHapticHelper.isSoundEnabled = soundEnabled
                    }
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Audio",
                        tint = if (soundEnabled) TapGoldPrimary else TapTextMuted
                    )
                }

                // AI Strategist Quick Access Badge
                Surface(
                    onClick = onOpenAiStrategist,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E163B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF)),
                    modifier = Modifier.testTag("ai_strategist_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Advisor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEDE7F6)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Remote Banner Notice (authoritative from Firestore config)
        if (uiState.config.bannerNotice.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TapSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TapGoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.config.bannerNotice,
                        fontSize = 12.sp,
                        color = TapTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Total Coin Balance Display
        val formattedCoins = NumberFormat.getNumberInstance(Locale.US).format(uiState.optimisticCoins)
        Card(
            colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TapGoldDark, TapOutline))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = TapTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.tap_coin),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formattedCoins,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = TapGoldPrimary,
                        modifier = Modifier.testTag("coin_balance_text")
                    )
                }
                Text(
                    text = "≈ $${String.format(Locale.US, "%.2f", uiState.optimisticCoins * uiState.config.coinToUsdRate)} USD",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TapGreenSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Coin Tapping Area
        Box(
            modifier = Modifier
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radiant Golden Background Aura
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .scale(if (isCoinPressed) 1.05f else 1.0f)
                    .shadow(
                        elevation = if (uiState.isTurboActive) 48.dp else 32.dp,
                        shape = CircleShape,
                        spotColor = if (uiState.isTurboActive) TapNeonOrange else TapGoldPrimary
                    )
            )

            // The Coin Button
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .scale(coinScale)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        brush = Brush.radialGradient(
                            listOf(
                                if (uiState.isTurboActive) TapNeonOrange else TapGoldLight,
                                TapGoldDark
                            )
                        ),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                isCoinPressed = true
                                audioHapticHelper.playTapFeedback()
                                viewModel.onCoinTapped(offset.x, offset.y)
                                tryAwaitRelease()
                                isCoinPressed = false
                            }
                        )
                    }
                    .testTag("main_tap_coin"),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.customCoinBitmap != null) {
                    Image(
                        bitmap = uiState.customCoinBitmap.asImageBitmap(),
                        contentDescription = "Custom AI Coin",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.tap_coin),
                        contentDescription = "TAP Gold Coin",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Floating +COINS particle effects
            uiState.floatingParticles.forEach { particle ->
                FloatingParticleView(
                    particle = particle,
                    onAnimationEnd = { viewModel.removeParticle(particle.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Energy Progress Bar
        val maxEnergy = uiState.balance?.maxEnergy ?: uiState.config.maxEnergy
        val currentEnergy = uiState.optimisticEnergy
        val energyFraction = (currentEnergy.toFloat() / maxEnergy.toFloat()).coerceIn(0f, 1f)

        Card(
            colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Energy",
                            tint = if (energyFraction < 0.2f) TapNeonOrange else TapGoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ENERGY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TapTextSecondary
                        )
                    }
                    Text(
                        text = "$currentEnergy / $maxEnergy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TapTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { energyFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (uiState.isTurboActive) TapNeonOrange else TapGoldPrimary,
                    trackColor = TapSurfaceElevated
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "+${uiState.config.energyRegenPerSec + ((uiState.boosters?.energyRecoveryLevel ?: 1) - 1)} / sec",
                        fontSize = 11.sp,
                        color = TapGreenSuccess
                    )
                    Text(
                        text = if (uiState.isTurboActive) "TURBO 2X ACTIVE" else "TAP COST: ${uiState.config.energyPerTap}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isTurboActive) TapNeonOrange else TapTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Booster & Ad Refill Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Turbo 2x Button
            Button(
                onClick = { viewModel.activateTurbo() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isTurboActive) TapNeonOrange else TapSurfaceVariant,
                    contentColor = TapTextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isTurboActive) TapNeonOrange else TapOutline),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("turbo_boost_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = if (uiState.isTurboActive) Color.Black else TapNeonOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isTurboActive) "Turbo On" else "Turbo 2x",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Ad Reward Energy Refill Button
            Button(
                onClick = { showAdRewardDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapGoldContainer,
                    contentColor = TapGoldLight
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapGoldDark),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ad_refill_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = TapGoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+500 Energy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Banner
        if (uiState.userFeedbackMessage != null) {
            Surface(
                color = TapSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapGoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.userFeedbackMessage,
                        fontSize = 12.sp,
                        color = TapTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "DISMISS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TapGoldPrimary,
                        modifier = Modifier
                            .clickable { viewModel.clearFeedbackMessage() }
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }

    // Rewarded Ad Dialog Simulation (conforms to Google Play AdMob policy)
    if (showAdRewardDialog) {
        AlertDialog(
            onDismissRequest = { showAdRewardDialog = false },
            title = {
                Text(
                    text = "Google AdMob Rewarded Ad",
                    fontWeight = FontWeight.Bold,
                    color = TapTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Watch this sponsor message to instantly refill +500 Energy points! Rewards are credited upon completion.",
                        fontSize = 13.sp,
                        color = TapTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TapSurfaceElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AdUnit: ca-app-pub-3940256099942544/5224354917 (Test Unit)",
                            fontSize = 11.sp,
                            color = TapTextMuted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdRewardDialog = false
                        viewModel.refillEnergyAdReward()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Watch & Claim", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdRewardDialog = false }) {
                    Text("Cancel", color = TapTextSecondary)
                }
            },
            containerColor = TapDarkCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun FloatingParticleView(
    particle: FloatingParticle,
    onAnimationEnd: () -> Unit
) {
    val animOffsetY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(particle.id) {
        animOffsetY.animateTo(
            targetValue = -120f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
    }
    LaunchedEffect(particle.id) {
        delay(400)
        animAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        onAnimationEnd()
    }

    Text(
        text = particle.text,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        color = TapGoldPrimary,
        modifier = Modifier
            .offset { IntOffset(0, animOffsetY.value.toInt()) }
            .alpha(animAlpha.value)
    )
}
