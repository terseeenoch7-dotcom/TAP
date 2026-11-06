package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BoostersScreen(
    uiState: TapUiState,
    viewModel: TapViewModel
) {
    val balance = uiState.optimisticCoins
    val boosters = uiState.boosters

    val multitapLevel = boosters?.multitapLevel ?: 1
    val energyBoostLevel = boosters?.energyBoostLevel ?: 1
    val recoveryLevel = boosters?.energyRecoveryLevel ?: 1

    val multitapCost = multitapLevel * 1000L
    val energyBoostCost = energyBoostLevel * 1500L
    val recoveryCost = recoveryLevel * 2000L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header Balance Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Your Balance", fontSize = 11.sp, color = TapTextMuted)
                        Text(
                            text = "${NumberFormat.getNumberInstance(Locale.US).format(balance)} Coins",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = TapGoldPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TapGoldContainer
                    ) {
                        Text(
                            text = if (uiState.isTurboActive) "Turbo 2X ACTIVE" else "Standard Multipliers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isTurboActive) TapNeonOrange else TapGoldLight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Performance Upgrades",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TapTextPrimary
            )
        }

        // Multitap Upgrade
        item {
            BoosterCard(
                icon = Icons.Default.Bolt,
                title = "Multitap Level $multitapLevel",
                description = "Earn +1 additional coin for every single screen tap. Current: +${multitapLevel} coins/tap.",
                price = multitapCost,
                canAfford = balance >= multitapCost,
                onUpgrade = { viewModel.buyBooster("multitapLevel", multitapCost) },
                testTag = "upgrade_multitap_button"
            )
        }

        // Energy Boost Upgrade
        item {
            BoosterCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Energy Tank Level $energyBoostLevel",
                description = "Expands total energy reserve by +500 points. Never run empty during big tapping streaks.",
                price = energyBoostCost,
                canAfford = balance >= energyBoostCost,
                onUpgrade = { viewModel.buyBooster("energyBoostLevel", energyBoostCost) },
                testTag = "upgrade_energy_button"
            )
        }

        // Energy Recovery Upgrade
        item {
            BoosterCard(
                icon = Icons.Default.Speed,
                title = "Recharge Speed Level $recoveryLevel",
                description = "Accelerates passive energy regeneration by +1 energy point every second.",
                price = recoveryCost,
                canAfford = balance >= recoveryCost,
                onUpgrade = { viewModel.buyBooster("energyRecoveryLevel", recoveryCost) },
                testTag = "upgrade_recovery_button"
            )
        }

        item {
            Text(
                text = "Special Accelerators",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TapTextPrimary
            )
        }

        // Turbo Boost 2x
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isTurboActive) TapNeonOrange else TapOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF331B05), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = TapNeonOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Turbo 2X Multiplier",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TapTextPrimary
                            )
                            Text(
                                text = "Doubles all coin rewards for 60 seconds",
                                fontSize = 12.sp,
                                color = TapTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { viewModel.activateTurbo() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapNeonOrange,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (uiState.isTurboActive) "Active" else "Activate",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterCard(
    icon: ImageVector,
    title: String,
    description: String,
    price: Long,
    canAfford: Boolean,
    onUpgrade: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(TapGoldContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TapGoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TapTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = TapTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cost: ${NumberFormat.getNumberInstance(Locale.US).format(price)} coins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TapGoldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapGoldPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = TapSurfaceElevated,
                    disabledContentColor = TapTextMuted
                ),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = "Upgrade",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
