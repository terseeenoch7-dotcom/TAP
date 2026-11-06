package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserBalance
import com.example.ui.TapUiState
import com.example.ui.theme.TapBlack
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldDark
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapGreenSuccess
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    uiState: TapUiState,
    currentUserId: String
) {
    val leaderboard = uiState.leaderboard

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = TapGoldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Global Tappers Leaderboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TapTextPrimary
                    )
                    Text(
                        text = "Ranked by total lifetime coin earnings",
                        fontSize = 12.sp,
                        color = TapTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Top 3 Podium Cards (if enough players)
        if (leaderboard.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Rank 2 (Silver)
                    if (leaderboard.size >= 2) {
                        PodiumPlayerCard(
                            balance = leaderboard[1],
                            rank = 2,
                            badgeColor = Color(0xFFCFD8DC),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Rank 1 (Gold Center, taller)
                    PodiumPlayerCard(
                        balance = leaderboard[0],
                        rank = 1,
                        badgeColor = TapGoldPrimary,
                        modifier = Modifier.weight(1.15f)
                    )

                    // Rank 3 (Bronze)
                    if (leaderboard.size >= 3) {
                        PodiumPlayerCard(
                            balance = leaderboard[2],
                            rank = 3,
                            badgeColor = Color(0xFFCD7F32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Leaderboard List
        itemsIndexed(leaderboard, key = { _, b -> b.userId }) { index, item ->
            val rank = index + 1
            val isCurrentUser = item.userId == currentUserId

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) TapGoldContainer else TapDarkCharcoal
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCurrentUser) TapGoldPrimary else TapOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("leaderboard_item_$rank")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (rank) {
                                1 -> TapGoldPrimary
                                2 -> Color(0xFFCFD8DC)
                                3 -> Color(0xFFCD7F32)
                                else -> TapSurfaceElevated
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$rank",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (rank <= 3) Color.Black else TapTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isCurrentUser) "You (${item.userId.take(6)})" else "Tapper_${item.userId.take(6)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCurrentUser) TapGoldLight else TapTextPrimary
                            )
                            Text(
                                text = "${NumberFormat.getNumberInstance(Locale.US).format(item.totalTaps)} total taps",
                                fontSize = 11.sp,
                                color = TapTextMuted
                            )
                        }
                    }

                    Text(
                        text = "${NumberFormat.getNumberInstance(Locale.US).format(item.totalLifetimeCoins)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = TapGoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumPlayerCard(
    balance: UserBalance,
    rank: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tapper_${balance.userId.take(4)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TapTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${NumberFormat.getNumberInstance(Locale.US).format(balance.totalLifetimeCoins)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TapGoldPrimary
            )
        }
    }
}
