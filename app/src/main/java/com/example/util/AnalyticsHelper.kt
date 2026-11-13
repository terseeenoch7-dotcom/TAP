package com.example.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(private val context: Context) {
    private val analytics: FirebaseAnalytics? by lazy {
        try {
            FirebaseAnalytics.getInstance(context.applicationContext)
        } catch (_: Exception) {
            null
        }
    }

    private fun safeLog(event: String, bundle: Bundle) {
        try {
            analytics?.logEvent(event, bundle)
        } catch (_: Exception) {
            // Ignore analytics logging failures
        }
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        safeLog(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logLogin(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        safeLog(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        safeLog(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    fun logTapBatch(tapsCount: Long, coinsEarned: Long) {
        val bundle = Bundle().apply {
            putLong("tap_count", tapsCount)
            putLong("coins_earned", coinsEarned)
        }
        safeLog("tap_batch_synced", bundle)
    }

    fun logBoosterPurchased(boosterKey: String, level: Int, cost: Long) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, boosterKey)
            putString(FirebaseAnalytics.Param.ITEM_NAME, boosterKey)
            putLong(FirebaseAnalytics.Param.VALUE, cost)
            putInt("booster_level", level)
        }
        safeLog("booster_purchased", bundle)
    }

    fun logDailyStreakClaimed(streak: Int, rewardCoins: Long) {
        val bundle = Bundle().apply {
            putInt("streak_day", streak)
            putLong("reward_coins", rewardCoins)
        }
        safeLog("daily_streak_claimed", bundle)
    }

    fun logTaskCompleted(taskId: String, rewardCoins: Long) {
        val bundle = Bundle().apply {
            putString("task_id", taskId)
            putLong("reward_coins", rewardCoins)
        }
        safeLog("task_completed", bundle)
    }

    fun logWithdrawalRequested(method: String, coinAmount: Long, fiatAmount: Double) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.PAYMENT_TYPE, method)
            putLong("coins_withdrawn", coinAmount)
            putDouble(FirebaseAnalytics.Param.VALUE, fiatAmount)
            putString(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
        safeLog("withdrawal_requested", bundle)
    }

    fun logAdminConfigUpdate() {
        safeLog("admin_config_updated", Bundle())
    }
}
