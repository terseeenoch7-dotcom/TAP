package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val referralCode: String = "",
    val referredBy: String? = null,
    val accountLevel: Int = 1,
    val isSuspended: Boolean = false,
    val role: String = "user",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBalance(
    val userId: String = "",
    val availableCoins: Long = 0L,
    val totalLifetimeCoins: Long = 0L,
    val totalTaps: Long = 0L,
    val currentEnergy: Int = 1000,
    val maxEnergy: Int = 1000,
    @ServerTimestamp val lastEnergyUpdatedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBoosters(
    val userId: String = "",
    val multitapLevel: Int = 1,
    val energyBoostLevel: Int = 1,
    val energyRecoveryLevel: Int = 1,
    val turboBoostExpiresAt: Timestamp? = null,
    val autoTapExpiresAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyCheckIn(
    val userId: String = "",
    val currentStreak: Int = 0,
    val lastCheckInDate: String = "",
    @ServerTimestamp val lastClaimedAt: Timestamp? = null,
    val totalClaimed: Int = 0
)
