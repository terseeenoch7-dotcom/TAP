package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

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
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class TaskSubmission(
    val submissionId: String = "",
    val userId: String = "",
    val taskId: String = "",
    val status: String = "pending", // pending, completed, claimed
    val rewardClaimed: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)
