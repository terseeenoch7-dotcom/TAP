package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskCategory(val key: String, val label: String) {
    DAILY("daily", "Daily"),
    SOCIAL("social", "Social"),
    PROMO("promotional", "Promos"),
    SPECIAL("special", "Special"),
    REFERRAL("referral", "Referral")
}

@IgnoreExtraProperties
data class TaskItem(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "daily",
    val rewardCoins: Long = 100L,
    val actionUrl: String? = null,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskSubmission(
    val submissionId: String = "",
    val userId: String = "",
    val taskId: String = "",
    val status: String = "pending", // pending, completed, claimed
    val rewardClaimed: Boolean = false,
    @ServerTimestamp val completedAt: Timestamp? = null
)
