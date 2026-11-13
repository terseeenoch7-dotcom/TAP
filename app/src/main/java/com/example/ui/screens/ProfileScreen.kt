package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.TapUiState
import com.example.ui.theme.TapBlack
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldDark
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapGreenSuccess
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapRedAlert
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import com.example.util.AudioHapticHelper

@Composable
fun ProfileScreen(
    uiState: TapUiState,
    audioHapticHelper: AudioHapticHelper,
    onOpenAiCoinStudio: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    var soundEnabled by remember { mutableStateOf(audioHapticHelper.isSoundEnabled) }
    var hapticEnabled by remember { mutableStateOf(audioHapticHelper.isHapticEnabled) }

    val userProfile = uiState.profile
    val referralCode = userProfile?.referralCode ?: "TAP-EARN"
    val isAdmin = userProfile?.role == "admin" || userProfile?.email.equals("terseeenoch7@gmail.com", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, TapGoldPrimary, CircleShape)
                        .background(TapSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userProfile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = (userProfile?.username?.take(1) ?: "T").uppercase(),
                            color = TapGoldPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userProfile?.username ?: "Tapper",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TapTextPrimary
                )

                Text(
                    text = userProfile?.email ?: "",
                    fontSize = 12.sp,
                    color = TapTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TapGoldContainer
                    ) {
                        Text(
                            text = "Level ${userProfile?.accountLevel ?: 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TapGoldLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0E3821),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TapGreenSuccess)
                        ) {
                            Text(
                                text = "ADMINISTRATOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TapGreenSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Referral Program Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TapGoldDark, TapOutline))),
            modifier = Modifier.fillMaxWidth().testTag("referral_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INVITE FRIENDS & EARN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TapGoldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share your unique code to earn 5,000 bonus coins when a friend joins TAP!",
                    fontSize = 13.sp,
                    color = TapTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TapSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Your Referral Code", fontSize = 10.sp, color = TapTextMuted)
                            Text(
                                text = referralCode,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TapGoldLight
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("TAP Code", referralCode))
                                    Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = TapGoldPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join TAP and start earning real coins! Use my referral code: $referralCode"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share TAP Referral"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Referral",
                                    tint = TapGoldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Collector Coin Studio Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141328)),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF673AB7)),
            modifier = Modifier.fillMaxWidth().testTag("ai_coin_studio_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Generative Coin Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFEDE7F6)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Synthesize custom high-detail 3D coin skins using gemini-3-pro-image with 1K, 2K, or 4K resolution options.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onOpenAiCoinStudio,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C4DFF),
                        contentColor = Color.White
                    )
                ) {
                    Text("Design", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Preferences
        Card(
            colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GAME PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TapTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = TapGoldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Tapping Sound Effects", fontSize = 14.sp, color = TapTextPrimary)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            audioHapticHelper.isSoundEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TapGoldPrimary,
                            checkedTrackColor = TapGoldContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = TapGoldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Vibration & Haptic Feedback", fontSize = 14.sp, color = TapTextPrimary)
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            audioHapticHelper.isHapticEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TapGoldPrimary,
                            checkedTrackColor = TapGoldContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Access Button if authorized
        if (isAdmin) {
            Button(
                onClick = onOpenAdminDashboard,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2838),
                    contentColor = TapGoldPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TapGoldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_dashboard_button")
            ) {
                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Administrator Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOutClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TapRedAlert),
            border = androidx.compose.foundation.BorderStroke(1.dp, TapRedAlert.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Sign Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
