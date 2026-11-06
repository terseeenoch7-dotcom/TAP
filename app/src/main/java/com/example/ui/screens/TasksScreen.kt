package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.ui.TapUiState
import com.example.ui.TapViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    uiState: TapUiState,
    viewModel: TapViewModel
) {
    var selectedCategory by remember { mutableStateOf("all") }
    val categories = listOf("all", "daily", "social", "promotional", "special", "referral")

    val streakRewards = listOf(500L, 1000L, 2000L, 3500L, 5000L, 7500L, 15000L)
    val currentStreak = uiState.dailyCheckIn?.currentStreak ?: 0
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isClaimedToday = uiState.dailyCheckIn?.lastCheckInDate == todayStr

    // Fallback tasks if catalog is freshly initialized
    val effectiveTasks = if (uiState.tasks.isNotEmpty()) {
        uiState.tasks
    } else {
        listOf(
            TaskItem(taskId = "default_tap_100", title = "Tap 100 Coins", description = "Perform 100 manual coin taps today", category = "daily", rewardCoins = 250L),
            TaskItem(taskId = "default_follow_x", title = "Follow Official Announcement Channel", description = "Stay up to date with updates & bonus codes", category = "social", rewardCoins = 1000L),
            TaskItem(taskId = "default_invite_1", title = "Invite 1 Friend to TAP", description = "Share your unique referral code with a friend", category = "referral", rewardCoins = 5000L),
            TaskItem(taskId = "default_upgrade_booster", title = "Upgrade Multitap Booster", description = "Invest in any booster upgrade in the store", category = "special", rewardCoins = 1500L)
        )
    }

    val filteredTasks = if (selectedCategory == "all") {
        effectiveTasks
    } else {
        effectiveTasks.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Daily Check-in Streak Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TapGoldDark, TapOutline))),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_rewards_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = TapGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Check-in Streak",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TapTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TapGoldContainer
                        ) {
                            Text(
                                text = "Streak: $currentStreak / 7",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TapGoldLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7-day calendar row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(7) { index ->
                            val dayNum = index + 1
                            val isCompleted = dayNum <= currentStreak
                            val isCurrent = dayNum == ((currentStreak % 7) + 1) && !isClaimedToday
                            val reward = streakRewards.getOrElse(index) { 1000L }

                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) TapGoldContainer
                                        else if (isCompleted) TapSurfaceElevated
                                        else TapSurfaceVariant
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) TapGoldPrimary else TapOutline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Day $dayNum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent) TapGoldLight else TapTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isCompleted) TapGreenSuccess else if (isCurrent) TapGoldPrimary else TapTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "+$reward",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) TapGoldPrimary else TapTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.claimDailyReward() },
                        enabled = !isClaimedToday,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapGoldPrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = TapSurfaceElevated,
                            disabledContentColor = TapTextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("claim_daily_button")
                    ) {
                        Text(
                            text = if (isClaimedToday) "Claimed Today ✓" else "Claim Day ${(currentStreak % 7) + 1} Reward",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section Title: Tasks & Challenges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = TapGoldPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Earning Tasks & Challenges",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TapTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Category Filter Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {}
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) TapGoldPrimary else TapSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TapGoldLight else TapOutline)
                        ) {
                            Text(
                                text = cat.replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TapTextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Tasks List
        items(filteredTasks, key = { it.taskId }) { task ->
            val submission = uiState.submissions.find { it.taskId == task.taskId }
            val isClaimed = submission?.rewardClaimed == true

            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("task_card_${task.taskId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TapSurfaceElevated
                            ) {
                                Text(
                                    text = task.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TapGoldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+${task.rewardCoins} coins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TapGreenSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TapTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = TapTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.claimTask(task) },
                        enabled = !isClaimed,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapGoldPrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = TapSurfaceElevated,
                            disabledContentColor = TapTextMuted
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = if (isClaimed) "Claimed ✓" else "Claim",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
