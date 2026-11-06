package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class UserBalance(
    val userId: String = "",
    val availableCoins: Long = 0L,
    val totalLifetimeCoins: Long = 0L,
    val totalTaps: Long = 0L,
    val currentEnergy: Int = 1000,
    val maxEnergy: Int = 1000,
    val lastEnergyUpdatedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class UserBoosters(
    val userId: String = "",
    val multitapLevel: Int = 1,
    val energyBoostLevel: Int = 1,
    val energyRecoveryLevel: Int = 1,
    val turboBoostExpiresAt: Long? = null,
    val autoTapExpiresAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class DailyCheckIn(
    val userId: String = "",
    val currentStreak: Int = 0,
    val lastCheckInDate: String = "",
    val lastClaimedAt: Long = 0L,
    val totalClaimed: Int = 0
)

