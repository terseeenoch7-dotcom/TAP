package com.example.data.repository

import android.content.Context
import android.util.Log
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TapRepository"
const val RTDB_URL = "https://gen-lang-client-0113954346-default-rtdb.firebaseio.com/"

class TapRepository(
    private val rtdb: FirebaseDatabase,
    private val firestore: FirebaseFirestore? = null
) {

    constructor(context: Context) : this(
        FirebaseDatabase.getInstance(RTDB_URL).apply {
            try {
                setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Ignore if already initialized
            }
        },
        try { Firebase.firestore } catch (_: Exception) { null }
    )

    constructor() : this(
        FirebaseDatabase.getInstance(RTDB_URL).apply {
            try {
                setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Ignore if already initialized
            }
        },
        try { Firebase.firestore } catch (_: Exception) { null }
    )

    constructor(firestore: FirebaseFirestore) : this(
        FirebaseDatabase.getInstance(RTDB_URL).apply {
            try {
                setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Ignore if already initialized
            }
        },
        firestore
    )

    val database: FirebaseDatabase get() = rtdb
    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing TAP database.")
    }

    // ------------------------------------------------------------------------
    // Helper: Flow Builders for Realtime Database
    // ------------------------------------------------------------------------
    private fun <T : Any> DatabaseReference.toObjectFlow(clazz: Class<T>): Flow<T?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(clazz)
                trySend(value)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Database error at $path: ${error.message}")
                trySend(null)
                close()
            }
        }
        addValueEventListener(listener)
        awaitClose { removeEventListener(listener) }
    }

    private fun <T : Any> DatabaseReference.toListFlow(clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<T>()
                for (child in snapshot.children) {
                    child.getValue(clazz)?.let { list.add(it) }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Database error at $path: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        addValueEventListener(listener)
        awaitClose { removeEventListener(listener) }
    }

    // ------------------------------------------------------------------------
    // 1. User Profile Management
    // ------------------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (firestore != null) {
            val docRef = firestore.collection("users").document(userId)
            return docRef.snapshots().map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
            }
        }
        val ref = rtdb.getReference("users").child(userId)
        return ref.toObjectFlow(UserProfile::class.java).catch { e ->
            Log.e(TAG, "Failed to observe profile for $userId", e)
            emit(null)
        }
    }

    suspend fun getOrCreateUserProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String?
    ): UserProfile {
        val isAdminUser = email.equals("terseeenoch7@gmail.com", ignoreCase = true)
        val generatedRefCode = "TAP-" + userId.take(5).uppercase()
        val username = displayName.ifBlank { "Tapper_${userId.take(4)}" }

        val defaultProfile = UserProfile(
            userId = userId,
            username = username,
            email = email,
            photoUrl = photoUrl,
            referralCode = generatedRefCode,
            role = if (isAdminUser) "admin" else "user",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        if (firestore != null) {
            try {
                val docRef = firestore.collection("users").document(userId)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(UserProfile::class.java)
                    if (existing != null) {
                        if (isAdminUser && existing.role != "admin") {
                            val updated = existing.copy(role = "admin")
                            docRef.set(updated).await()
                            return updated
                        }
                        return existing
                    }
                }
                docRef.set(defaultProfile).await()
                val balRef = firestore.collection("user_balances").document(userId)
                if (!balRef.get().await().exists()) {
                    balRef.set(UserBalance(userId = userId)).await()
                }
                return defaultProfile
            } catch (e: Exception) {
                Log.w(TAG, "Firestore getOrCreateUserProfile note (e.g. offline): ${e.message}")
            }
        }

        try {
            val userRef = rtdb.getReference("users").child(userId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val existing = snapshot.getValue(UserProfile::class.java)
                if (existing != null) {
                    if (isAdminUser && existing.role != "admin") {
                        val updated = existing.copy(role = "admin")
                        userRef.setValue(updated).await()
                        return updated
                    }
                    return existing
                }
            }

            userRef.setValue(defaultProfile).await()

            if (isAdminUser) {
                rtdb.getReference("admins").child(userId).setValue(true).await()
            }

            initializeUserBalanceIfMissing(userId)
            initializeUserBoostersIfMissing(userId)
            initializeDailyCheckInIfMissing(userId)

            return defaultProfile
        } catch (e: Exception) {
            Log.w(TAG, "RTDB profile init note: ${e.message}")
            return defaultProfile
        }
    }

    // ------------------------------------------------------------------------
    // 2. User Balance & Energy Engine
    // ------------------------------------------------------------------------
    fun observeUserBalance(userId: String): Flow<UserBalance?> {
        if (firestore != null) {
            val docRef = firestore.collection("user_balances").document(userId)
            return docRef.snapshots().map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserBalance::class.java) else null
            }
        }
        val ref = rtdb.getReference("user_balances").child(userId)
        return ref.toObjectFlow(UserBalance::class.java).catch { e ->
            Log.e(TAG, "Failed to observe balance for $userId", e)
            emit(null)
        }
    }

    private suspend fun initializeUserBalanceIfMissing(userId: String) {
        val ref = rtdb.getReference("user_balances").child(userId)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            val balance = UserBalance(
                userId = userId,
                availableCoins = 0L,
                totalLifetimeCoins = 0L,
                totalTaps = 0L,
                currentEnergy = 1000,
                maxEnergy = 1000,
                lastEnergyUpdatedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            ref.setValue(balance).await()
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

        if (firestore != null) {
            val balRef = firestore.collection("user_balances").document(userId)
            firestore.runTransaction { tx ->
                val snap = tx.get(balRef)
                if (!snap.exists()) {
                    val initial = UserBalance(
                        userId = userId,
                        availableCoins = coinsEarned,
                        totalLifetimeCoins = coinsEarned,
                        totalTaps = tapsCount,
                        currentEnergy = newEnergy,
                        maxEnergy = 1000,
                        lastEnergyUpdatedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    tx.set(balRef, initial)
                } else {
                    val curCoins = snap.getLong("availableCoins") ?: 0L
                    val curTotal = snap.getLong("totalLifetimeCoins") ?: 0L
                    val curTaps = snap.getLong("totalTaps") ?: 0L
                    tx.update(
                        balRef,
                        mapOf(
                            "availableCoins" to curCoins + coinsEarned,
                            "totalLifetimeCoins" to curTotal + coinsEarned,
                            "totalTaps" to curTaps + tapsCount,
                            "currentEnergy" to newEnergy,
                            "lastEnergyUpdatedAt" to System.currentTimeMillis(),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }
            }.await()
        }

        try {
            val ref = rtdb.getReference("user_balances").child(userId)
            if (firestore != null) {
                ref.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                        val updated = balance.copy(
                            availableCoins = balance.availableCoins + coinsEarned,
                            totalLifetimeCoins = balance.totalLifetimeCoins + coinsEarned,
                            totalTaps = balance.totalTaps + tapsCount,
                            currentEnergy = newEnergy.coerceIn(0, balance.maxEnergy),
                            lastEnergyUpdatedAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        currentData.value = updated
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                })
            } else {
                suspendCancellableCoroutine<Unit> { continuation ->
                    ref.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                            val updated = balance.copy(
                                availableCoins = balance.availableCoins + coinsEarned,
                                totalLifetimeCoins = balance.totalLifetimeCoins + coinsEarned,
                                totalTaps = balance.totalTaps + tapsCount,
                                currentEnergy = newEnergy.coerceIn(0, balance.maxEnergy),
                                lastEnergyUpdatedAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            currentData.value = updated
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            if (error != null) {
                                continuation.resumeWithException(error.toException())
                            } else {
                                continuation.resume(Unit)
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "RTDB transaction: ${e.message}")
        }
    }

    suspend fun refillEnergy(userId: String, amount: Int) {
        val ref = rtdb.getReference("user_balances").child(userId)
        suspendCancellableCoroutine<Unit> { continuation ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                    val updated = balance.copy(
                        currentEnergy = (balance.currentEnergy + amount).coerceAtMost(balance.maxEnergy),
                        lastEnergyUpdatedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }

    // ------------------------------------------------------------------------
    // 3. Boosters & Upgrades
    // ------------------------------------------------------------------------
    fun observeUserBoosters(userId: String): Flow<UserBoosters?> {
        val ref = rtdb.getReference("user_boosters").child(userId)
        return ref.toObjectFlow(UserBoosters::class.java).catch { e ->
            Log.e(TAG, "Failed to observe boosters for $userId", e)
            emit(null)
        }
    }

    private suspend fun initializeUserBoostersIfMissing(userId: String) {
        val ref = rtdb.getReference("user_boosters").child(userId)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            val boosters = UserBoosters(userId = userId)
            ref.setValue(boosters).await()
        }
    }

    suspend fun upgradeBooster(userId: String, boosterType: String, cost: Long) {
        val balanceRef = rtdb.getReference("user_balances").child(userId)
        val boostersRef = rtdb.getReference("user_boosters").child(userId)

        val balanceDeducted = suspendCancellableCoroutine<Boolean> { continuation ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: return Transaction.abort()
                    if (balance.availableCoins < cost) {
                        return Transaction.abort()
                    }
                    val newMaxEnergy = if (boosterType == "energy_boost") balance.maxEnergy + 500 else balance.maxEnergy
                    val updated = balance.copy(
                        availableCoins = balance.availableCoins - cost,
                        maxEnergy = newMaxEnergy,
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(committed)
                    }
                }
            })
        }

        if (!balanceDeducted) {
            throw IllegalStateException("Insufficient coins to purchase booster.")
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            boostersRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val boosters = currentData.getValue(UserBoosters::class.java) ?: UserBoosters(userId = userId)
                    val updated = when (boosterType) {
                        "multitap" -> boosters.copy(multitapLevel = boosters.multitapLevel + 1, updatedAt = System.currentTimeMillis())
                        "energy_boost" -> boosters.copy(energyBoostLevel = boosters.energyBoostLevel + 1, updatedAt = System.currentTimeMillis())
                        "recovery" -> boosters.copy(energyRecoveryLevel = boosters.energyRecoveryLevel + 1, updatedAt = System.currentTimeMillis())
                        else -> boosters
                    }
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }

        addTransactionRecord(userId, "BOOSTER_PURCHASE", -cost, "Upgraded $boosterType booster")
    }

    suspend fun activateTurboBoost(userId: String, durationSeconds: Long = 60L) {
        val expiresAt = System.currentTimeMillis() + durationSeconds * 1000L
        val boostersRef = rtdb.getReference("user_boosters").child(userId)
        boostersRef.updateChildren(
            mapOf(
                "turboBoostExpiresAt" to expiresAt,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    // ------------------------------------------------------------------------
    // 4. Daily Check-In & Streak Rewards
    // ------------------------------------------------------------------------
    fun observeDailyCheckIn(userId: String): Flow<DailyCheckIn?> {
        val ref = rtdb.getReference("daily_check_ins").child(userId)
        return ref.toObjectFlow(DailyCheckIn::class.java).catch { e ->
            Log.e(TAG, "Failed to observe daily check-in for $userId", e)
            emit(null)
        }
    }

    private suspend fun initializeDailyCheckInIfMissing(userId: String) {
        val ref = rtdb.getReference("daily_check_ins").child(userId)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            val checkIn = DailyCheckIn(userId = userId)
            ref.setValue(checkIn).await()
        }
    }

    suspend fun claimDailyReward(userId: String, rewardAmount: Long): Int {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val checkInRef = rtdb.getReference("daily_check_ins").child(userId)
        val snapshot = checkInRef.get().await()
        val currentCheckIn = snapshot.getValue(DailyCheckIn::class.java) ?: DailyCheckIn(userId = userId)

        if (currentCheckIn.lastCheckInDate == todayStr) {
            throw IllegalStateException("Daily reward already claimed for today.")
        }

        val yesterdayCal = java.util.Calendar.getInstance()
        yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterdayCal.time)

        val newStreak = if (currentCheckIn.lastCheckInDate == yesterdayStr) {
            if (currentCheckIn.currentStreak >= 7) 1 else currentCheckIn.currentStreak + 1
        } else {
            1
        }

        val updatedCheckIn = currentCheckIn.copy(
            currentStreak = newStreak,
            lastCheckInDate = todayStr,
            lastClaimedAt = System.currentTimeMillis(),
            totalClaimed = currentCheckIn.totalClaimed + 1
        )
        checkInRef.setValue(updatedCheckIn).await()

        val balanceRef = rtdb.getReference("user_balances").child(userId)
        suspendCancellableCoroutine<Unit> { continuation ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                    val updated = balance.copy(
                        availableCoins = balance.availableCoins + rewardAmount,
                        totalLifetimeCoins = balance.totalLifetimeCoins + rewardAmount,
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }

        addTransactionRecord(userId, "DAILY_REWARD", rewardAmount, "Claimed Day $newStreak daily reward")
        return newStreak
    }

    // ------------------------------------------------------------------------
    // 5. Tasks & Challenges
    // ------------------------------------------------------------------------
    fun observeActiveTasks(): Flow<List<TaskItem>> = callbackFlow {
        val tasksRef = rtdb.getReference("tasks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    seedDefaultTasks()
                    return
                }
                val list = mutableListOf<TaskItem>()
                for (child in snapshot.children) {
                    val task = child.getValue(TaskItem::class.java)
                    if (task != null && task.isActive) {
                        list.add(task)
                    }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Tasks observe cancelled: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        tasksRef.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    private fun seedDefaultTasks() {
        val defaults = listOf(
            TaskItem(taskId = "TASK-01", title = "Daily Check-In", description = "Claim your daily reward streak in the app", category = "daily", rewardCoins = 500L),
            TaskItem(taskId = "TASK-02", title = "Follow TAP on X (Twitter)", description = "Stay tuned with official announcements and drop alerts", category = "social", rewardCoins = 1500L, actionUrl = "https://x.com"),
            TaskItem(taskId = "TASK-03", title = "Join Official Telegram Group", description = "Connect with global tappers and earn bonus coins", category = "social", rewardCoins = 2500L, actionUrl = "https://t.me"),
            TaskItem(taskId = "TASK-04", title = "Subscribe to YouTube Channel", description = "Watch strategy guides and tutorials", category = "promotional", rewardCoins = 3000L, actionUrl = "https://youtube.com"),
            TaskItem(taskId = "TASK-05", title = "Invite 3 Friends to TAP", description = "Share your referral code to unlock massive rewards", category = "referral", rewardCoins = 7500L),
            TaskItem(taskId = "TASK-06", title = "Reach 5,000 Total Taps", description = "Prove your dedication as a Master Tapper", category = "special", rewardCoins = 5000L)
        )
        val tasksRef = rtdb.getReference("tasks")
        for (item in defaults) {
            tasksRef.child(item.taskId).setValue(item)
        }
    }

    fun observeUserTaskSubmissions(userId: String): Flow<List<TaskSubmission>> {
        val ref = rtdb.getReference("task_submissions").child(userId)
        return ref.toListFlow(TaskSubmission::class.java).catch { e ->
            Log.e(TAG, "Failed to observe submissions for $userId", e)
            emit(emptyList())
        }
    }

    suspend fun completeAndClaimTask(userId: String, task: TaskItem) {
        val submissionRef = rtdb.getReference("task_submissions").child(userId).child(task.taskId)
        val existing = submissionRef.get().await()
        if (existing.exists()) {
            val sub = existing.getValue(TaskSubmission::class.java)
            if (sub?.rewardClaimed == true) {
                throw IllegalStateException("Task reward has already been claimed.")
            }
        }

        val submission = TaskSubmission(
            submissionId = task.taskId,
            userId = userId,
            taskId = task.taskId,
            status = "claimed",
            rewardClaimed = true,
            completedAt = System.currentTimeMillis()
        )
        submissionRef.setValue(submission).await()

        val balanceRef = rtdb.getReference("user_balances").child(userId)
        suspendCancellableCoroutine<Unit> { continuation ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                    val updated = balance.copy(
                        availableCoins = balance.availableCoins + task.rewardCoins,
                        totalLifetimeCoins = balance.totalLifetimeCoins + task.rewardCoins,
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }

        addTransactionRecord(userId, "TASK_REWARD", task.rewardCoins, "Completed task: ${task.title}")
    }

    // ------------------------------------------------------------------------
    // 6. Wallet & Withdrawals
    // ------------------------------------------------------------------------
    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        val ref = rtdb.getReference("user_withdrawals").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<WithdrawalRequest>()
                for (child in snapshot.children) {
                    val req = child.getValue(WithdrawalRequest::class.java)
                    if (req != null) {
                        list.add(req)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeUserWithdrawals note: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun submitWithdrawalRequest(
        userId: String,
        username: String,
        coinAmount: Long,
        paymentMethod: String,
        paymentDetails: String
    ): WithdrawalRequest {
        val configSnap = rtdb.getReference("app_config").child("main").get().await()
        val config = configSnap.getValue(AppConfiguration::class.java) ?: AppConfiguration()

        if (coinAmount < config.minWithdrawalCoins) {
            throw IllegalArgumentException("Minimum withdrawal is ${config.minWithdrawalCoins} coins.")
        }

        val balanceRef = rtdb.getReference("user_balances").child(userId)
        val deducted = suspendCancellableCoroutine<Boolean> { continuation ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: return Transaction.abort()
                    if (balance.availableCoins < coinAmount) {
                        return Transaction.abort()
                    }
                    val updated = balance.copy(
                        availableCoins = balance.availableCoins - coinAmount,
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(committed)
                    }
                }
            })
        }

        if (!deducted) {
            throw IllegalStateException("Insufficient coin balance for withdrawal.")
        }

        val withdrawalId = "WD-" + UUID.randomUUID().toString().take(8).uppercase()
        val fiatValue = coinAmount * config.coinToUsdRate
        val request = WithdrawalRequest(
            withdrawalId = withdrawalId,
            userId = userId,
            username = username,
            coinAmount = coinAmount,
            fiatAmount = fiatValue,
            paymentMethod = paymentMethod,
            paymentDetails = paymentDetails,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        try {
            rtdb.getReference("user_withdrawals").child(userId).child(withdrawalId).setValue(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "User withdrawal write note: ${e.message}")
        }
        try {
            rtdb.getReference("withdrawals").child(withdrawalId).setValue(request).await()
        } catch (e: Exception) {
            Log.w(TAG, "Global withdrawal write note: ${e.message}")
        }
        addTransactionRecord(userId, "WITHDRAWAL", -coinAmount, "Withdrawal request via $paymentMethod")

        return request
    }

    // ------------------------------------------------------------------------
    // 7. Transactions Ledger
    // ------------------------------------------------------------------------
    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = callbackFlow {
        val ref = rtdb.getReference("transactions").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<TransactionRecord>()
                for (child in snapshot.children) {
                    child.getValue(TransactionRecord::class.java)?.let { list.add(it) }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Transactions observe note: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private suspend fun addTransactionRecord(userId: String, type: String, amount: Long, description: String) {
        val txId = "TX-" + UUID.randomUUID().toString().take(10).uppercase()
        val record = TransactionRecord(
            txId = txId,
            userId = userId,
            type = type,
            amount = amount,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        rtdb.getReference("transactions").child(userId).child(txId).setValue(record).await()
    }

    // ------------------------------------------------------------------------
    // 8. Remote App Configuration (Synced live without APK rebuilds)
    // ------------------------------------------------------------------------
    fun observeAppConfiguration(): Flow<AppConfiguration> = callbackFlow {
        val configRef = rtdb.getReference("app_config").child("main")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val defaultConfig = AppConfiguration()
                    configRef.setValue(defaultConfig)
                    trySend(defaultConfig)
                } else {
                    val config = snapshot.getValue(AppConfiguration::class.java) ?: AppConfiguration()
                    trySend(config)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Config load error: ${error.message}")
                trySend(AppConfiguration())
            }
        }
        configRef.addValueEventListener(listener)
        awaitClose { configRef.removeEventListener(listener) }
    }

    suspend fun saveAppConfiguration(config: AppConfiguration) {
        val updated = config.copy(
            version = config.version + 1,
            updatedAt = System.currentTimeMillis()
        )
        rtdb.getReference("app_config").child("main").setValue(updated).await()
    }

    // ------------------------------------------------------------------------
    // 9. Admin Dashboard Management Operations
    // ------------------------------------------------------------------------
    fun observeAllUsers(): Flow<List<UserProfile>> {
        val ref = rtdb.getReference("users")
        return ref.toListFlow(UserProfile::class.java)
    }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> = callbackFlow {
        val ref = rtdb.getReference("withdrawals")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<WithdrawalRequest>()
                for (child in snapshot.children) {
                    child.getValue(WithdrawalRequest::class.java)?.let { list.add(it) }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeAllWithdrawals note: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun reviewWithdrawal(withdrawalId: String, newStatus: String, adminNote: String?) {
        val ref = rtdb.getReference("withdrawals").child(withdrawalId)
        val snap = ref.get().await()
        val req = snap.getValue(WithdrawalRequest::class.java) ?: return

        ref.updateChildren(
            mapOf(
                "status" to newStatus,
                "adminNote" to (adminNote ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()

        if (newStatus == "REJECTED" && req.status != "REJECTED") {
            val balanceRef = rtdb.getReference("user_balances").child(req.userId)
            suspendCancellableCoroutine<Unit> { continuation ->
                balanceRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = req.userId)
                        val updated = balance.copy(
                            availableCoins = balance.availableCoins + req.coinAmount,
                            updatedAt = System.currentTimeMillis()
                        )
                        currentData.value = updated
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                            continuation.resumeWithException(error.toException())
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                })
            }
            addTransactionRecord(req.userId, "REFUND", req.coinAmount, "Withdrawal refund: ${adminNote ?: "Rejected by admin"}")
        }
    }

    suspend fun adminAdjustUserBalance(userId: String, adjustment: Long, reason: String) {
        val balanceRef = rtdb.getReference("user_balances").child(userId)
        suspendCancellableCoroutine<Unit> { continuation ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val balance = currentData.getValue(UserBalance::class.java) ?: UserBalance(userId = userId)
                    val newCoins = (balance.availableCoins + adjustment).coerceAtLeast(0L)
                    val updated = balance.copy(
                        availableCoins = newCoins,
                        updatedAt = System.currentTimeMillis()
                    )
                    currentData.value = updated
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }
        addTransactionRecord(userId, "ADMIN_ADJUSTMENT", adjustment, "Admin adjustment: $reason")
    }

    suspend fun adminToggleUserSuspension(userId: String, isSuspended: Boolean) {
        rtdb.getReference("users").child(userId).updateChildren(
            mapOf(
                "isSuspended" to isSuspended,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun adminCreateOrUpdateTask(task: TaskItem) {
        val taskId = task.taskId.ifBlank { "TASK-" + UUID.randomUUID().toString().take(6).uppercase() }
        val item = task.copy(taskId = taskId, createdAt = if (task.createdAt > 0L) task.createdAt else System.currentTimeMillis())
        rtdb.getReference("tasks").child(taskId).setValue(item).await()
    }

    // ------------------------------------------------------------------------
    // 10. Leaderboard (Live rankings)
    // ------------------------------------------------------------------------
    fun observeLeaderboard(): Flow<List<UserBalance>> = callbackFlow {
        val query = rtdb.getReference("user_balances")
            .orderByChild("totalLifetimeCoins")
            .limitToLast(50)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserBalance>()
                for (child in snapshot.children) {
                    child.getValue(UserBalance::class.java)?.let { list.add(it) }
                }
                list.sortByDescending { it.totalLifetimeCoins }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Leaderboard observe note: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
}
