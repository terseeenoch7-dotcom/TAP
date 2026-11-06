package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppConfiguration
import com.example.data.model.DailyCheckIn
import com.example.data.model.NotificationItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.UserBalance
import com.example.data.model.UserBoosters
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "TapRepository"

class TapRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // ------------------------------------------------------------------------
    // 1. User Profile
    // ------------------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        }.catch { e ->
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun getOrCreateUserProfile(userId: String, email: String, displayName: String, photoUrl: String?): UserProfile {
        val docRef = db.collection("users").document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = userId, email = email, username = displayName)
        }

        val generatedRefCode = "TAP-" + userId.take(5).uppercase()
        val isAdminUser = email.equals("terseeenoch7@gmail.com", ignoreCase = true)
        val profile = UserProfile(
            userId = userId,
            username = displayName.ifBlank { "Tapper_${userId.take(4)}" },
            email = email,
            photoUrl = photoUrl,
            referralCode = generatedRefCode,
            role = if (isAdminUser) "admin" else "user"
        )

        val payload = mapOf(
            "userId" to profile.userId,
            "username" to profile.username,
            "email" to profile.email,
            "photoUrl" to profile.photoUrl,
            "referralCode" to profile.referralCode,
            "accountLevel" to 1,
            "isSuspended" to false,
            "role" to profile.role,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        docRef.set(payload).await()

        // Also initialize initial balance & boosters
        initializeUserBalanceIfMissing(userId)
        initializeUserBoostersIfMissing(userId)
        initializeDailyCheckInIfMissing(userId)

        return profile
    }

    // ------------------------------------------------------------------------
    // 2. User Balance & Energy Engine
    // ------------------------------------------------------------------------
    fun observeUserBalance(userId: String): Flow<UserBalance?> {
        val docRef = db.collection("user_balances").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserBalance::class.java) else null
        }.catch { e ->
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    private suspend fun initializeUserBalanceIfMissing(userId: String) {
        val docRef = db.collection("user_balances").document(userId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val payload = mapOf(
                "userId" to userId,
                "availableCoins" to 0L,
                "totalLifetimeCoins" to 0L,
                "totalTaps" to 0L,
                "currentEnergy" to 1000,
                "maxEnergy" to 1000,
                "lastEnergyUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
    }

    suspend fun syncTapsBatch(
        userId: String,
        coinsEarned: Long,
        energyConsumed: Int,
        newEnergy: Int,
        tapsCount: Long
    ) {
        if (coinsEarned <= 0 && energyConsumed <= 0) return
        val docRef = db.collection("user_balances").document(userId)
        try {
            db.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                val currentCoins = snapshot.getLong("availableCoins") ?: 0L
                val lifetimeCoins = snapshot.getLong("totalLifetimeCoins") ?: 0L
                val totalTaps = snapshot.getLong("totalTaps") ?: 0L
                val maxEnergy = (snapshot.getLong("maxEnergy") ?: 1000L).toInt()

                val updatedCoins = (currentCoins + coinsEarned).coerceAtLeast(0L)
                val updatedLifetime = lifetimeCoins + coinsEarned
                val updatedTaps = totalTaps + tapsCount
                val boundedEnergy = newEnergy.coerceIn(0, maxEnergy)

                tx.update(docRef, mapOf(
                    "availableCoins" to updatedCoins,
                    "totalLifetimeCoins" to updatedLifetime,
                    "totalTaps" to updatedTaps,
                    "currentEnergy" to boundedEnergy,
                    "lastEnergyUpdatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun refillEnergy(userId: String, amount: Int) {
        val docRef = db.collection("user_balances").document(userId)
        db.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val maxEnergy = (snapshot.getLong("maxEnergy") ?: 1000L).toInt()
            val currentEnergy = (snapshot.getLong("currentEnergy") ?: 0L).toInt()
            val newEnergy = (currentEnergy + amount).coerceAtMost(maxEnergy)
            tx.update(docRef, mapOf(
                "currentEnergy" to newEnergy,
                "lastEnergyUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    // ------------------------------------------------------------------------
    // 3. Boosters & Upgrades
    // ------------------------------------------------------------------------
    fun observeUserBoosters(userId: String): Flow<UserBoosters?> {
        val docRef = db.collection("boosters").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserBoosters::class.java) else null
        }.catch { e ->
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    private suspend fun initializeUserBoostersIfMissing(userId: String) {
        val docRef = db.collection("boosters").document(userId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val payload = mapOf(
                "userId" to userId,
                "multitapLevel" to 1,
                "energyBoostLevel" to 1,
                "energyRecoveryLevel" to 1,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
    }

    suspend fun upgradeBooster(userId: String, boosterType: String, cost: Long) {
        val balanceRef = db.collection("user_balances").document(userId)
        val boostersRef = db.collection("boosters").document(userId)

        db.runTransaction { tx ->
            val balanceSnap = tx.get(balanceRef)
            val currentCoins = balanceSnap.getLong("availableCoins") ?: 0L
            if (currentCoins < cost) {
                throw IllegalStateException("Insufficient coins for booster upgrade. Cost: $cost, Available: $currentCoins")
            }

            val boosterSnap = tx.get(boostersRef)
            val currentLevel = (boosterSnap.getLong(boosterType) ?: 1L).toInt()

            tx.update(balanceRef, "availableCoins", currentCoins - cost)
            tx.update(boostersRef, mapOf(
                boosterType to currentLevel + 1,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // If upgrading energy boost, also increase maxEnergy in balance
            if (boosterType == "energyBoostLevel") {
                val currentMax = (balanceSnap.getLong("maxEnergy") ?: 1000L).toInt()
                tx.update(balanceRef, "maxEnergy", currentMax + 500)
            }
        }.await()

        addTransactionRecord(userId, "BOOSTER_PURCHASE", -cost, "Upgraded $boosterType to next level")
    }

    suspend fun activateTurboBoost(userId: String, durationSeconds: Long = 60L) {
        val docRef = db.collection("boosters").document(userId)
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + durationSeconds * 1000))
        docRef.update(mapOf(
            "turboBoostExpiresAt" to expiresAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }

    // ------------------------------------------------------------------------
    // 4. Daily Rewards & Streak
    // ------------------------------------------------------------------------
    fun observeDailyCheckIn(userId: String): Flow<DailyCheckIn?> {
        val docRef = db.collection("daily_checkins").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(DailyCheckIn::class.java) else null
        }.catch { e ->
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    private suspend fun initializeDailyCheckInIfMissing(userId: String) {
        val docRef = db.collection("daily_checkins").document(userId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val payload = mapOf(
                "userId" to userId,
                "currentStreak" to 0,
                "lastCheckInDate" to "",
                "totalClaimed" to 0
            )
            docRef.set(payload).await()
        }
    }

    suspend fun claimDailyReward(userId: String, rewardAmount: Long): Int {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val checkInRef = db.collection("daily_checkins").document(userId)
        val balanceRef = db.collection("user_balances").document(userId)

        var newStreak = 1
        db.runTransaction { tx ->
            val checkInSnap = tx.get(checkInRef)
            val lastDate = checkInSnap.getString("lastCheckInDate") ?: ""
            if (lastDate == todayStr) {
                throw IllegalStateException("Daily reward already claimed today.")
            }

            val currentStreak = (checkInSnap.getLong("currentStreak") ?: 0L).toInt()
            val totalClaimed = (checkInSnap.getLong("totalClaimed") ?: 0L).toInt()
            newStreak = if (currentStreak >= 7) 1 else currentStreak + 1

            tx.update(checkInRef, mapOf(
                "currentStreak" to newStreak,
                "lastCheckInDate" to todayStr,
                "lastClaimedAt" to FieldValue.serverTimestamp(),
                "totalClaimed" to totalClaimed + 1
            ))

            val currentCoins = tx.get(balanceRef).getLong("availableCoins") ?: 0L
            val currentLifetime = tx.get(balanceRef).getLong("totalLifetimeCoins") ?: 0L
            tx.update(balanceRef, mapOf(
                "availableCoins" to currentCoins + rewardAmount,
                "totalLifetimeCoins" to currentLifetime + rewardAmount,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()

        addTransactionRecord(userId, "DAILY_REWARD", rewardAmount, "Claimed Day $newStreak daily reward")
        return newStreak
    }

    // ------------------------------------------------------------------------
    // 5. Tasks and Challenges
    // ------------------------------------------------------------------------
    fun observeActiveTasks(): Flow<List<TaskItem>> {
        return db.collection("tasks")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { it.toObjects(TaskItem::class.java) }
    }

    fun observeUserTaskSubmissions(userId: String): Flow<List<TaskSubmission>> {
        return db.collection("task_submissions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(TaskSubmission::class.java) }
    }

    suspend fun completeAndClaimTask(userId: String, task: TaskItem) {
        val subId = "${userId}_${task.taskId}"
        val subRef = db.collection("task_submissions").document(subId)
        val balanceRef = db.collection("user_balances").document(userId)

        db.runTransaction { tx ->
            val subSnap = tx.get(subRef)
            if (subSnap.exists() && subSnap.getBoolean("rewardClaimed") == true) {
                throw IllegalStateException("Task reward has already been claimed.")
            }

            tx.set(subRef, mapOf(
                "submissionId" to subId,
                "userId" to userId,
                "taskId" to task.taskId,
                "status" to "claimed",
                "rewardClaimed" to true,
                "completedAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge())

            val currentCoins = tx.get(balanceRef).getLong("availableCoins") ?: 0L
            val currentLifetime = tx.get(balanceRef).getLong("totalLifetimeCoins") ?: 0L
            tx.update(balanceRef, mapOf(
                "availableCoins" to currentCoins + task.rewardCoins,
                "totalLifetimeCoins" to currentLifetime + task.rewardCoins,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()

        addTransactionRecord(userId, "TASK_REWARD", task.rewardCoins, "Completed task: ${task.title}")
    }

    // ------------------------------------------------------------------------
    // 6. Wallet & Withdrawals
    // ------------------------------------------------------------------------
    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> {
        return db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java).sortedByDescending { req -> req.createdAt } }
    }

    suspend fun submitWithdrawalRequest(
        userId: String,
        username: String,
        coinAmount: Long,
        fiatAmount: Double,
        method: String,
        details: String
    ): String {
        val balanceRef = db.collection("user_balances").document(userId)
        val withdrawalId = "WD-" + UUID.randomUUID().toString().take(8).uppercase()
        val withdrawalRef = db.collection("withdrawals").document(withdrawalId)

        db.runTransaction { tx ->
            val balanceSnap = tx.get(balanceRef)
            val currentCoins = balanceSnap.getLong("availableCoins") ?: 0L
            if (currentCoins < coinAmount) {
                throw IllegalStateException("Insufficient coin balance for withdrawal. Needed: $coinAmount, Available: $currentCoins")
            }

            tx.update(balanceRef, mapOf(
                "availableCoins" to currentCoins - coinAmount,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            tx.set(withdrawalRef, mapOf(
                "withdrawalId" to withdrawalId,
                "userId" to userId,
                "username" to username,
                "coinAmount" to coinAmount,
                "fiatAmount" to fiatAmount,
                "paymentMethod" to method,
                "paymentDetails" to details,
                "status" to "PENDING",
                "transactionReference" to "REF-${System.currentTimeMillis().toString().takeLast(6)}",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()

        addTransactionRecord(userId, "WITHDRAWAL", -coinAmount, "Submitted withdrawal $withdrawalId via $method")
        return withdrawalId
    }

    // ------------------------------------------------------------------------
    // 7. Transaction History
    // ------------------------------------------------------------------------
    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> {
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { it.toObjects(TransactionRecord::class.java).sortedByDescending { tx -> tx.createdAt } }
    }

    private suspend fun addTransactionRecord(userId: String, type: String, amount: Long, description: String) {
        val txId = "TX-" + UUID.randomUUID().toString().take(8)
        val docRef = db.collection("users").document(userId).collection("transactions").document(txId)
        docRef.set(mapOf(
            "txId" to txId,
            "userId" to userId,
            "type" to type,
            "amount" to amount,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
    }

    // ------------------------------------------------------------------------
    // 8. Remote App Configuration (Authoritative live sync)
    // ------------------------------------------------------------------------
    fun observeAppConfiguration(): Flow<AppConfiguration> {
        val docRef = db.collection("app_configuration").document("main")
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) {
                snapshot.toObject(AppConfiguration::class.java) ?: AppConfiguration()
            } else {
                AppConfiguration()
            }
        }.catch { e ->
            Log.w(TAG, "Failed to observe app configuration, falling back to default", e)
            emit(AppConfiguration())
        }
    }

    suspend fun saveAppConfiguration(config: AppConfiguration) {
        val docRef = db.collection("app_configuration").document("main")
        docRef.set(mapOf(
            "configId" to "main",
            "coinsPerTap" to config.coinsPerTap,
            "maxEnergy" to config.maxEnergy,
            "energyPerTap" to config.energyPerTap,
            "energyRegenPerSec" to config.energyRegenPerSec,
            "minWithdrawalCoins" to config.minWithdrawalCoins,
            "coinToUsdRate" to config.coinToUsdRate,
            "maintenanceMode" to config.maintenanceMode,
            "bannerNotice" to config.bannerNotice,
            "announcementActive" to config.announcementActive,
            "announcementTitle" to config.announcementTitle,
            "announcementBody" to config.announcementBody,
            "announcementLink" to config.announcementLink,
            "version" to config.version + 1,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    // ------------------------------------------------------------------------
    // 9. Admin Dashboard Management Operations
    // ------------------------------------------------------------------------
    fun observeAllUsers(): Flow<List<UserProfile>> {
        return db.collection("users")
            .snapshots()
            .map { it.toObjects(UserProfile::class.java) }
    }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> {
        return db.collection("withdrawals")
            .snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java).sortedByDescending { it.createdAt } }
    }

    suspend fun reviewWithdrawal(withdrawalId: String, newStatus: String, adminNote: String?) {
        val docRef = db.collection("withdrawals").document(withdrawalId)
        docRef.update(mapOf(
            "status" to newStatus,
            "adminNote" to (adminNote ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }

    suspend fun adminAdjustUserBalance(userId: String, adjustment: Long, reason: String) {
        val balanceRef = db.collection("user_balances").document(userId)
        db.runTransaction { tx ->
            val snap = tx.get(balanceRef)
            val currentCoins = snap.getLong("availableCoins") ?: 0L
            val newCoins = (currentCoins + adjustment).coerceAtLeast(0L)
            tx.update(balanceRef, mapOf(
                "availableCoins" to newCoins,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }.await()

        addTransactionRecord(userId, "ADMIN_ADJUSTMENT", adjustment, "Admin adjustment: $reason")
    }

    suspend fun adminToggleUserSuspension(userId: String, isSuspended: Boolean) {
        val docRef = db.collection("users").document(userId)
        docRef.update(mapOf(
            "isSuspended" to isSuspended,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }

    suspend fun adminCreateOrUpdateTask(task: TaskItem) {
        val taskId = task.taskId.ifBlank { "TASK-" + UUID.randomUUID().toString().take(6).uppercase() }
        val docRef = db.collection("tasks").document(taskId)
        docRef.set(mapOf(
            "taskId" to taskId,
            "title" to task.title,
            "description" to task.description,
            "category" to task.category,
            "rewardCoins" to task.rewardCoins,
            "actionUrl" to task.actionUrl,
            "isActive" to task.isActive,
            "createdAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    // ------------------------------------------------------------------------
    // 10. Leaderboard (Live rankings)
    // ------------------------------------------------------------------------
    fun observeLeaderboard(): Flow<List<UserBalance>> {
        return db.collection("user_balances")
            .orderBy("totalLifetimeCoins", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects(UserBalance::class.java) }
    }
}
