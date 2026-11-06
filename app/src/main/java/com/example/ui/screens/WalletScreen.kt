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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    uiState: TapUiState,
    viewModel: TapViewModel
) {
    var showWithdrawModal by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Withdrawals, 1: Transaction Ledger

    val availableCoins = uiState.optimisticCoins
    val usdValue = availableCoins * uiState.config.coinToUsdRate
    val totalLifetime = uiState.balance?.totalLifetimeCoins ?: 0L

    val completedWithdrawalsAmount = uiState.withdrawals
        .filter { it.status == "COMPLETED" }
        .sumOf { it.coinAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Balance Overview Cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TapGoldDark, TapOutline))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AVAILABLE WITHDRAWABLE BALANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TapTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(availableCoins) + " COINS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TapGoldPrimary
                    )
                    Text(
                        text = "≈ $${String.format(Locale.US, "%.2f", usdValue)} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TapGreenSuccess
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Lifetime Earnings",
                            value = "${NumberFormat.getNumberInstance(Locale.US).format(totalLifetime)}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Total Withdrawn",
                            value = "${NumberFormat.getNumberInstance(Locale.US).format(completedWithdrawalsAmount)}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showWithdrawModal = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapGoldPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("withdraw_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Request Withdrawal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tabs: Withdrawal History vs Transactions
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TapSurfaceVariant,
                contentColor = TapGoldPrimary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Withdrawals (${uiState.withdrawals.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Transactions (${uiState.transactions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Content for selected tab
        if (selectedTab == 0) {
            if (uiState.withdrawals.isEmpty()) {
                item {
                    EmptyStateCard(message = "No withdrawal requests submitted yet. Earn coins and cash out via Crypto, PayPal, or Bank.")
                }
            } else {
                items(uiState.withdrawals, key = { it.withdrawalId }) { withdrawal ->
                    WithdrawalItemCard(withdrawal = withdrawal)
                }
            }
        } else {
            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyStateCard(message = "No transaction records found yet.")
                }
            } else {
                items(uiState.transactions, key = { it.txId }) { tx ->
                    TransactionItemCard(tx = tx)
                }
            }
        }
    }

    // Modal Withdrawal Request Dialog
    if (showWithdrawModal) {
        WithdrawalRequestModal(
            availableCoins = availableCoins,
            minWithdrawalCoins = uiState.config.minWithdrawalCoins,
            coinToUsdRate = uiState.config.coinToUsdRate,
            onDismiss = { showWithdrawModal = false },
            onSubmit = { amount, method, details ->
                viewModel.requestWithdrawal(amount, method, details)
                showWithdrawModal = false
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = TapSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 10.sp, color = TapTextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TapTextPrimary)
        }
    }
}

@Composable
private fun WithdrawalItemCard(withdrawal: WithdrawalRequest) {
    val statusColor = when (withdrawal.status) {
        "COMPLETED" -> TapGreenSuccess
        "APPROVED" -> Color(0xFF29B6F6)
        "UNDER_REVIEW" -> Color(0xFFAB47BC)
        "REJECTED" -> TapRedAlert
        else -> TapGoldPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("withdrawal_item_${withdrawal.withdrawalId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = withdrawal.withdrawalId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TapTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = withdrawal.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.US).format(withdrawal.coinAmount)} Coins ($${String.format(Locale.US, "%.2f", withdrawal.fiatAmount)})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TapGoldPrimary
                )
                Text(
                    text = withdrawal.paymentMethod,
                    fontSize = 12.sp,
                    color = TapTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Destination: ${withdrawal.paymentDetails}",
                fontSize = 11.sp,
                color = TapTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!withdrawal.adminNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Admin Note: ${withdrawal.adminNote}",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB74D)
                )
            }
        }
    }
}

@Composable
private fun TransactionItemCard(tx: TransactionRecord) {
    val isPositive = tx.amount >= 0
    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (isPositive) Color(0xFF0F291E) else Color(0xFF331414), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isPositive) TapGreenSuccess else TapRedAlert,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tx.description,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TapTextPrimary
                    )
                    Text(
                        text = tx.type,
                        fontSize = 10.sp,
                        color = TapTextMuted
                    )
                }
            }

            Text(
                text = "${if (isPositive) "+" else ""}${tx.amount} coins",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isPositive) TapGreenSuccess else TapRedAlert
            )
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TapDarkCharcoal),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TapTextMuted,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TapTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalRequestModal(
    availableCoins: Long,
    minWithdrawalCoins: Long,
    coinToUsdRate: Double,
    onDismiss: () -> Unit,
    onSubmit: (amount: Long, method: String, details: String) -> Unit
) {
    var amountInput by remember { mutableStateOf(minWithdrawalCoins.toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.USDT_TON) }
    var detailsInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountLong = amountInput.toLongOrNull() ?: 0L
    val usdEstimate = amountLong * coinToUsdRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Request Cash-Out",
                fontWeight = FontWeight.Bold,
                color = TapTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Select your preferred payment method and enter payout details:",
                    fontSize = 13.sp,
                    color = TapTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Selector
                PaymentMethod.values().forEach { method ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedMethod == method) TapGoldContainer else TapSurfaceElevated,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (selectedMethod == method) TapGoldPrimary else TapOutline,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = method.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TapTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Coin Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Coins Amount (Min: $minWithdrawalCoins)") },
                    supportingText = { Text("Estimated Payout: $${String.format(Locale.US, "%.2f", usdEstimate)} USD") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TapGoldPrimary,
                        unfocusedBorderColor = TapOutline,
                        focusedLabelColor = TapGoldPrimary,
                        focusedTextColor = TapTextPrimary,
                        unfocusedTextColor = TapTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("withdrawal_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Destination details
                OutlinedTextField(
                    value = detailsInput,
                    onValueChange = { detailsInput = it },
                    label = { Text(selectedMethod.hint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TapGoldPrimary,
                        unfocusedBorderColor = TapOutline,
                        focusedLabelColor = TapGoldPrimary,
                        focusedTextColor = TapTextPrimary,
                        unfocusedTextColor = TapTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("withdrawal_details_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = TapRedAlert,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountLong < minWithdrawalCoins) {
                        errorMessage = "Minimum withdrawal is $minWithdrawalCoins coins."
                    } else if (amountLong > availableCoins) {
                        errorMessage = "Amount exceeds available balance ($availableCoins)."
                    } else if (detailsInput.trim().length < 4) {
                        errorMessage = "Please enter valid payment details."
                    } else {
                        onSubmit(amountLong, selectedMethod.name, detailsInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TapGoldPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_withdrawal_button")
            ) {
                Text("Confirm Request", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TapTextSecondary)
            }
        },
        containerColor = TapDarkCharcoal,
        shape = RoundedCornerShape(18.dp)
    )
}
