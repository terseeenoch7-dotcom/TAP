package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class AppConfiguration(
    val configId: String = "main",
    val coinsPerTap: Int = 1,
    val maxEnergy: Int = 1000,
    val energyPerTap: Int = 1,
    val energyRegenPerSec: Int = 2,
    val minWithdrawalCoins: Long = 50000L,
    val coinToUsdRate: Double = 0.0001, // 10,000 coins = $1.00 USD
    val maintenanceMode: Boolean = false,
    val bannerNotice: String = "Welcome to TAP! Tap the gold coin, invite friends, and earn real rewards.",
    val announcementActive: Boolean = false,
    val announcementTitle: String = "",
    val announcementBody: String = "",
    val announcementLink: String = "",
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class NotificationItem(
    val notifId: String = "",
    val recipientUserId: String = "ALL",
    val title: String = "",
    val message: String = "",
    val type: String = "SYSTEM", // SYSTEM, TASK, WITHDRAWAL, REWARD, PROMO
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
