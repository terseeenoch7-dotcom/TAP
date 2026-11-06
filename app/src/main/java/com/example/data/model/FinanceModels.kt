package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentMethod(val key: String, val displayName: String, val hint: String) {
    USDT_TON("USDT_TON", "USDT (TON / TRC20)", "Enter your USDT wallet address"),
    PAYPAL("PAYPAL", "PayPal Transfer", "Enter your PayPal email address"),
    BANK("BANK", "Bank Wire", "Enter Account Name, IBAN / Number & Bank")
}

enum class WithdrawalStatus(val label: String) {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    COMPLETED("Completed"),
    REJECTED("Rejected")
}

@IgnoreExtraProperties
data class WithdrawalRequest(
    val withdrawalId: String = "",
    val userId: String = "",
    val username: String = "",
    val coinAmount: Long = 0L,
    val fiatAmount: Double = 0.0,
    val paymentMethod: String = "USDT_TON",
    val paymentDetails: String = "",
    val status: String = "PENDING",
    val adminNote: String? = null,
    val transactionReference: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionRecord(
    val txId: String = "",
    val userId: String = "",
    val type: String = "TAP_EARNING", // TAP_EARNING, BOOSTER_PURCHASE, DAILY_REWARD, TASK_REWARD, REFERRAL_BONUS, WITHDRAWAL, ADMIN_ADJUSTMENT
    val amount: Long = 0L, // Positive for gains, negative for spent
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
