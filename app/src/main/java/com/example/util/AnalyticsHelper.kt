package com.example.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(context: Context) {
    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logLogin(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    fun logTapBatch(tapsCount: Long, coinsEarned: Long) {
        val bundle = Bundle().apply {
            putLong("tap_count", tapsCount)
            putLong("coins_earned", coinsEarned)
        }
        analytics.logEvent("tap_batch_synced", bundle)
    }

    fun logBoosterPurchased(boosterKey: String, level: Int, cost: Long) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, boosterKey)
            putString(FirebaseAnalytics.Param.ITEM_NAME, boosterKey)
            putLong(FirebaseAnalytics.Param.VALUE, cost)
            putInt("booster_level", level)
        }
        analytics.logEvent("booster_purchased", bundle)
    }

    fun logDailyStreakClaimed(streak: Int, rewardCoins: Long) {
        val bundle = Bundle().apply {
            putInt("streak_day", streak)
            putLong("reward_coins", rewardCoins)
        }
        analytics.logEvent("daily_streak_claimed", bundle)
    }

    fun logTaskCompleted(taskId: String, rewardCoins: Long) {
        val bundle = Bundle().apply {
            putString("task_id", taskId)
            putLong("reward_coins", rewardCoins)
        }
        analytics.logEvent("task_completed", bundle)
    }

    fun logWithdrawalRequested(method: String, coinAmount: Long, fiatAmount: Double) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.PAYMENT_TYPE, method)
            putLong("coins_withdrawn", coinAmount)
            putDouble(FirebaseAnalytics.Param.VALUE, fiatAmount)
            putString(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
        analytics.logEvent("withdrawal_requested", bundle)
    }

    fun logAdminConfigUpdate() {
        analytics.logEvent("admin_config_updated", Bundle())
    }
}
