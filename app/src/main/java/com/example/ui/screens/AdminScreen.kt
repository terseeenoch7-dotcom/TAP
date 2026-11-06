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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfiguration
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
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
import com.example.ui.theme.TapRedAlert
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminScreen(
    uiState: TapUiState,
    viewModel: TapViewModel,
    onBackClick: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) } // 0: Overview, 1: Config, 2: Withdrawals, 3: Users, 4: Tasks
    val sections = listOf("Overview", "Remote Config", "Withdrawals", "Users", "Tasks")

    // State for Remote Config Form
    var coinsPerTapInput by remember(uiState.config) { mutableStateOf(uiState.config.coinsPerTap.toString()) }
    var maxEnergyInput by remember(uiState.config) { mutableStateOf(uiState.config.maxEnergy.toString()) }
    var regenPerSecInput by remember(uiState.config) { mutableStateOf(uiState.config.energyRegenPerSec.toString()) }
    var minWithdrawalInput by remember(uiState.config) { mutableStateOf(uiState.config.minWithdrawalCoins.toString()) }
    var bannerNoticeInput by remember(uiState.config) { mutableStateOf(uiState.config.bannerNotice) }
    var maintenanceModeState by remember(uiState.config) { mutableStateOf(uiState.config.maintenanceMode) }

    // Dialog state for withdrawal action
    var reviewWithdrawalItem by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var reviewStatus by remember { mutableStateOf("APPROVED") }
    var reviewNote by remember { mutableStateOf("") }

    // Dialog state for user balance adjust
    var adjustUserItem by remember { mutableStateOf<UserProfile?>(null) }
    var adjustAmountInput by remember { mutableStateOf("1000") }
    var adjustReasonInput by remember { mutableStateOf("Promotional Bonus") }

    // Dialog state for adding task
    var showAddTaskModal by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }
    var newTaskReward by remember { mutableStateOf("1000") }
    var newTaskCategory by remember { mutableStateOf("daily") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
    ) {
        // Top App Bar
        Surface(
            color = TapDarkCharcoal,
            border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TapGoldPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = TapGoldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Administrator Control Center",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TapTextPrimary
                    )
                    Text(
                        text = "Authoritative Live Game Operations",
                        fontSize = 11.sp,
                        color = TapTextMuted
                    )
                }
            }
        }

        // Section Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            containerColor = TapDarkCharcoal,
            contentColor = TapGoldPrimary,
            edgePadding = 12.dp
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Body Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedSection) {
                0 -> AdminOverviewTab(uiState = uiState)
                1 -> AdminConfigTab(
                    coinsPerTap = coinsPerTapInput,
                    onCoinsPerTapChange = { coinsPerTapInput = it },
                    maxEnergy = maxEnergyInput,
                    onMaxEnergyChange = { maxEnergyInput = it },
                    regenPerSec = regenPerSecInput,
                    onRegenPerSecChange = { regenPerSecInput = it },
                    minWithdrawal = minWithdrawalInput,
                    onMinWithdrawalChange = { minWithdrawalInput = it },
                    bannerNotice = bannerNoticeInput,
                    onBannerNoticeChange = { bannerNoticeInput = it },
                    maintenanceMode = maintenanceModeState,
                    onMaintenanceModeChange = { maintenanceModeState = it },
                    configVersion = uiState.config.version,
                    onPublish = {
                        val newConfig = uiState.config.copy(
                            coinsPerTap = coinsPerTapInput.toIntOrNull() ?: 1,
                            maxEnergy = maxEnergyInput.toIntOrNull() ?: 1000,
                            energyRegenPerSec = regenPerSecInput.toIntOrNull() ?: 2,
                            minWithdrawalCoins = minWithdrawalInput.toLongOrNull() ?: 50000L,
                            bannerNotice = bannerNoticeInput,
                            maintenanceMode = maintenanceModeState
                        )
                        viewModel.adminUpdateConfig(newConfig)
                    }
                )
                2 -> AdminWithdrawalsTab(
                    withdrawals = uiState.allWithdrawals,
                    onReview = { wd, status ->
                        reviewWithdrawalItem = wd
                        reviewStatus = status
                        reviewNote = ""
                    }
                )
                3 -> AdminUsersTab(
                    users = uiState.allUsers,
                    onAdjustBalance = { u ->
                        adjustUserItem = u
                        adjustAmountInput = "1000"
                        adjustReasonInput = "Loyalty Bonus"
                    },
                    onToggleSuspend = { u, isSuspended ->
                        viewModel.adminToggleSuspend(u.userId, isSuspended)
                    }
                )
                4 -> AdminTasksTab(
                    tasks = uiState.tasks,
                    onAddNewTaskClick = { showAddTaskModal = true }
                )
            }
        }
    }

    // Review Withdrawal Dialog
    if (reviewWithdrawalItem != null) {
        val wd = reviewWithdrawalItem!!
        AlertDialog(
            onDismissRequest = { reviewWithdrawalItem = null },
            title = { Text("Confirm $reviewStatus Withdrawal", fontWeight = FontWeight.Bold, color = TapTextPrimary) },
            text = {
                Column {
                    Text(
                        text = "User: ${wd.username} (${wd.userId.take(6)})\nAmount: ${wd.coinAmount} Coins ($${String.format(Locale.US, "%.2f", wd.fiatAmount)} USD)\nMethod: ${wd.paymentMethod}\nDetails: ${wd.paymentDetails}",
                        fontSize = 13.sp,
                        color = TapTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Administrative Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminReviewWithdrawal(wd.withdrawalId, reviewStatus, reviewNote)
                        reviewWithdrawalItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reviewStatus == "APPROVED" || reviewStatus == "COMPLETED") TapGreenSuccess else TapRedAlert,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Apply Status")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reviewWithdrawalItem = null }) {
                    Text("Cancel", color = TapTextSecondary)
                }
            },
            containerColor = TapDarkCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Adjust User Balance Dialog
    if (adjustUserItem != null) {
        val u = adjustUserItem!!
        AlertDialog(
            onDismissRequest = { adjustUserItem = null },
            title = { Text("Audited Balance Adjustment", fontWeight = FontWeight.Bold, color = TapTextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Adjust coin balance for ${u.username} (${u.email}):",
                        fontSize = 13.sp,
                        color = TapTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustAmountInput,
                        onValueChange = { adjustAmountInput = it },
                        label = { Text("Coin Amount (can be negative)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustReasonInput,
                        onValueChange = { adjustReasonInput = it },
                        label = { Text("Audit Reason / Memo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = adjustAmountInput.toLongOrNull() ?: 0L
                        if (amount != 0L) {
                            viewModel.adminAdjustBalance(u.userId, amount, adjustReasonInput)
                        }
                        adjustUserItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Commit Adjustment")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { adjustUserItem = null }) {
                    Text("Cancel", color = TapTextSecondary)
                }
            },
            containerColor = TapDarkCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add New Task Modal
    if (showAddTaskModal) {
        AlertDialog(
            onDismissRequest = { showAddTaskModal = false },
            title = { Text("Create New Earning Task", fontWeight = FontWeight.Bold, color = TapTextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskReward,
                        onValueChange = { newTaskReward = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Reward Coins") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskCategory,
                        onValueChange = { newTaskCategory = it },
                        label = { Text("Category (daily, social, promo, special)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val task = TaskItem(
                                title = newTaskTitle.trim(),
                                description = newTaskDesc.trim(),
                                rewardCoins = newTaskReward.toLongOrNull() ?: 500L,
                                category = newTaskCategory.trim().lowercase(),
                                isActive = true
                            )
                            viewModel.adminCreateTask(task)
                            showAddTaskModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Save Task")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskModal = false }) {
                    Text("Cancel", color = TapTextSecondary)
                }
            },
            containerColor = TapDarkCharcoal,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AdminOverviewTab(uiState: TapUiState) {
    val totalRegistered = uiState.allUsers.size
    val pendingWds = uiState.allWithdrawals.count { it.status == "PENDING" || it.status == "UNDER_REVIEW" }
    val completedWds = uiState.allWithdrawals.count { it.status == "COMPLETED" }
    val activeTasks = uiState.tasks.count { it.isActive }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Key System Metrics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TapTextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Registered Users",
                    value = "$totalRegistered",
                    subtitle = "Online accounts",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pending Cash-outs",
                    value = "$pendingWds",
                    subtitle = "Needs admin review",
                    accentColor = if (pendingWds > 0) TapGoldPrimary else TapGreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Completed Payouts",
                    value = "$completedWds",
                    subtitle = "Processed transactions",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Tasks",
                    value = "$activeTasks",
                    subtitle = "Live in rewards pool",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Cloudflare & Firestore Multi-Region Architecture",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TapGoldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Authoritative zero-trust backend running on Firestore Enterprise. Remote configurations are published in real-time without recompiling the APK.",
                        fontSize = 12.sp,
                        color = TapTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color = TapTextPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, color = TapTextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TapTextSecondary)
        }
    }
}

@Composable
private fun AdminConfigTab(
    coinsPerTap: String,
    onCoinsPerTapChange: (String) -> Unit,
    maxEnergy: String,
    onMaxEnergyChange: (String) -> Unit,
    regenPerSec: String,
    onRegenPerSecChange: (String) -> Unit,
    minWithdrawal: String,
    onMinWithdrawalChange: (String) -> Unit,
    bannerNotice: String,
    onBannerNoticeChange: (String) -> Unit,
    maintenanceMode: Boolean,
    onMaintenanceModeChange: (Boolean) -> Unit,
    configVersion: Int,
    onPublish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dynamic Remote Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TapTextPrimary
                )
                Text(
                    text = "Version $configVersion • Synchronizes globally across all users",
                    fontSize = 11.sp,
                    color = TapGoldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Maintenance Mode Switch
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (maintenanceMode) Color(0xFF3B1515) else TapDarkCharcoal
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (maintenanceMode) TapRedAlert else TapOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Maintenance Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (maintenanceMode) TapRedAlert else TapTextPrimary
                    )
                    Text(
                        text = "Pauses coin tapping & payouts for all connected clients",
                        fontSize = 11.sp,
                        color = TapTextSecondary
                    )
                }
                Switch(
                    checked = maintenanceMode,
                    onCheckedChange = onMaintenanceModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TapRedAlert,
                        checkedTrackColor = Color(0xFF5E1B1B)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = coinsPerTap,
            onValueChange = onCoinsPerTapChange,
            label = { Text("Base Coins per Tap") },
            modifier = Modifier.fillMaxWidth().testTag("admin_coins_per_tap_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = maxEnergy,
            onValueChange = onMaxEnergyChange,
            label = { Text("Base Maximum Energy Capacity") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = regenPerSec,
            onValueChange = onRegenPerSecChange,
            label = { Text("Energy Regeneration Rate (pts/sec)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = minWithdrawal,
            onValueChange = onMinWithdrawalChange,
            label = { Text("Minimum Coins for Withdrawal") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bannerNotice,
            onValueChange = onBannerNoticeChange,
            label = { Text("Home Screen Broadcast Banner") },
            modifier = Modifier.fillMaxWidth().height(90.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onPublish,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_publish_config_button")
        ) {
            Icon(imageVector = Icons.Default.Publish, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Publish Changes to All Users Live",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AdminWithdrawalsTab(
    withdrawals: List<WithdrawalRequest>,
    onReview: (WithdrawalRequest, String) -> Unit
) {
    if (withdrawals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No withdrawal requests found.", color = TapTextSecondary)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(withdrawals, key = { it.withdrawalId }) { wd ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = wd.withdrawalId, fontWeight = FontWeight.Bold, color = TapGoldPrimary)
                            Text(text = wd.status, fontWeight = FontWeight.Bold, color = TapTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${wd.username} • ${wd.coinAmount} Coins ($${String.format(Locale.US, "%.2f", wd.fiatAmount)})",
                            fontSize = 13.sp,
                            color = TapTextPrimary
                        )
                        Text(
                            text = "${wd.paymentMethod}: ${wd.paymentDetails}",
                            fontSize = 11.sp,
                            color = TapTextMuted
                        )

                        if (wd.status == "PENDING" || wd.status == "UNDER_REVIEW") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onReview(wd, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TapGreenSuccess, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onReview(wd, "COMPLETED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Complete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onReview(wd, "REJECTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TapRedAlert, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<UserProfile>,
    onAdjustBalance: (UserProfile) -> Unit,
    onToggleSuspend: (UserProfile, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = users.filter {
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.userId.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TapGoldPrimary) },
            label = { Text("Search users by name, email, or ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.userId }) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (user.isSuspended) TapRedAlert else TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = user.username, fontWeight = FontWeight.Bold, color = TapTextPrimary)
                                if (user.isSuspended) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "[SUSPENDED]", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TapRedAlert)
                                }
                            }
                            Text(text = user.email, fontSize = 11.sp, color = TapTextSecondary)
                            Text(text = "ID: ${user.userId}", fontSize = 10.sp, color = TapTextMuted)
                        }

                        Row {
                            IconButton(onClick = { onAdjustBalance(user) }) {
                                Icon(Icons.Default.Tune, contentDescription = "Adjust Balance", tint = TapGoldPrimary)
                            }
                            IconButton(onClick = { onToggleSuspend(user, !user.isSuspended) }) {
                                Icon(
                                    imageVector = if (user.isSuspended) Icons.Default.LockOpen else Icons.Default.Block,
                                    contentDescription = "Toggle Suspend",
                                    tint = if (user.isSuspended) TapGreenSuccess else TapRedAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTasksTab(
    tasks: List<TaskItem>,
    onAddNewTaskClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddNewTaskClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Earning Task", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.taskId }) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = task.title, fontWeight = FontWeight.Bold, color = TapTextPrimary)
                            Text(text = "+${task.rewardCoins} coins", fontWeight = FontWeight.Bold, color = TapGoldPrimary)
                        }
                        Text(text = "${task.category} • ${task.description}", fontSize = 11.sp, color = TapTextSecondary)
                    }
                }
            }
        }
    }
}
