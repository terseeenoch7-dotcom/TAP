package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppConfiguration
import com.example.data.model.DailyCheckIn
import com.example.data.model.TaskItem
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.UserBalance
import com.example.data.model.UserBoosters
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.AiRepository
import com.example.data.repository.TapRepository
import com.example.util.AnalyticsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "TapViewModel"

data class FloatingParticle(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    val y: Float
)

data class TapUiState(
    val profile: UserProfile? = null,
    val balance: UserBalance? = null,
    val boosters: UserBoosters? = null,
    val dailyCheckIn: DailyCheckIn? = null,
    val tasks: List<TaskItem> = emptyList(),
    val submissions: List<TaskSubmission> = emptyList(),
    val withdrawals: List<WithdrawalRequest> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val leaderboard: List<UserBalance> = emptyList(),
    val allUsers: List<UserProfile> = emptyList(),
    val allWithdrawals: List<WithdrawalRequest> = emptyList(),
    val config: AppConfiguration = AppConfiguration(),
    val optimisticCoins: Long = 0L,
    val optimisticEnergy: Int = 1000,
    val floatingParticles: List<FloatingParticle> = emptyList(),
    val isTurboActive: Boolean = false,
    val isOnline: Boolean = true,
    val customCoinBitmap: Bitmap? = null,
    val isGeneratingAiCoin: Boolean = false,
    val aiCoinError: String? = null,
    val aiStrategyReport: String? = null,
    val isGeneratingStrategy: Boolean = false,
    val userFeedbackMessage: String? = null
)

class TapViewModel(
    private val repository: TapRepository,
    private val aiRepository: AiRepository = AiRepository(),
    private val analyticsHelper: AnalyticsHelper? = null,
    private val currentUserId: String,
    userEmail: String,
    displayName: String,
    photoUrl: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TapUiState())
    val uiState: StateFlow<TapUiState> = _uiState.asStateFlow()

    // Unsaved local taps buffer
    private var pendingCoinsToSync = 0L
    private var pendingTapsToSync = 0L
    private var syncJob: Job? = null
    private var energyRegenJob: Job? = null

    init {
        // Initialize or fetch profile
        viewModelScope.launch {
            try {
                val profile = repository.getOrCreateUserProfile(currentUserId, userEmail, displayName, photoUrl)
                _uiState.value = _uiState.value.copy(profile = profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing user profile", e)
            }
        }

        // Observe profile updates
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId).collect { p ->
                if (p != null) _uiState.value = _uiState.value.copy(profile = p)
            }
        }

        // Observe user balance
        viewModelScope.launch {
            repository.observeUserBalance(currentUserId).collect { b ->
                if (b != null) {
                    val currentOptimisticCoins = _uiState.value.optimisticCoins
                    val currentOptimisticEnergy = _uiState.value.optimisticEnergy

                    val baseCoins = if (pendingCoinsToSync > 0) {
                        (b.availableCoins + pendingCoinsToSync).coerceAtLeast(currentOptimisticCoins)
                    } else {
                        b.availableCoins
                    }

                    val baseEnergy = if (pendingCoinsToSync > 0) {
                        currentOptimisticEnergy.coerceIn(0, b.maxEnergy)
                    } else {
                        b.currentEnergy
                    }

                    _uiState.value = _uiState.value.copy(
                        balance = b,
                        optimisticCoins = baseCoins,
                        optimisticEnergy = baseEnergy
                    )
                }
            }
        }

        // Observe boosters
        viewModelScope.launch {
            repository.observeUserBoosters(currentUserId).collect { boost ->
                if (boost != null) {
                    val isTurbo = boost.turboBoostExpiresAt != null && boost.turboBoostExpiresAt > System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(boosters = boost, isTurboActive = isTurbo)
                }
            }
        }

        // Observe daily check-in
        viewModelScope.launch {
            repository.observeDailyCheckIn(currentUserId).collect { d ->
                _uiState.value = _uiState.value.copy(dailyCheckIn = d)
            }
        }

        // Observe tasks & submissions
        viewModelScope.launch {
            repository.observeActiveTasks().collect { t ->
                _uiState.value = _uiState.value.copy(tasks = t)
            }
        }
        viewModelScope.launch {
            repository.observeUserTaskSubmissions(currentUserId).collect { s ->
                _uiState.value = _uiState.value.copy(submissions = s)
            }
        }

        // Observe wallet withdrawals & transactions
        viewModelScope.launch {
            repository.observeUserWithdrawals(currentUserId).collect { w ->
                _uiState.value = _uiState.value.copy(withdrawals = w)
            }
        }
        viewModelScope.launch {
            repository.observeTransactions(currentUserId).collect { tx ->
                _uiState.value = _uiState.value.copy(transactions = tx)
            }
        }

        // Observe remote application configuration
        viewModelScope.launch {
            repository.observeAppConfiguration().collect { c ->
                _uiState.value = _uiState.value.copy(config = c)
            }
        }

        // Observe leaderboard
        viewModelScope.launch {
            repository.observeLeaderboard().collect { lb ->
                _uiState.value = _uiState.value.copy(leaderboard = lb)
            }
        }

        // Observe admin all users and withdrawals
        viewModelScope.launch {
            repository.observeAllUsers().collect { u ->
                _uiState.value = _uiState.value.copy(allUsers = u)
            }
        }
        viewModelScope.launch {
            repository.observeAllWithdrawals().collect { aw ->
                _uiState.value = _uiState.value.copy(allWithdrawals = aw)
            }
        }

        // Start background energy regeneration loop
        startEnergyRegenerationLoop()
    }

    fun setOnlineStatus(online: Boolean) {
        _uiState.value = _uiState.value.copy(isOnline = online)
    }

    private fun startEnergyRegenerationLoop() {
        energyRegenJob?.cancel()
        energyRegenJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val state = _uiState.value
                val maxEnergy = state.balance?.maxEnergy ?: 1000
                val recoveryLvl = state.boosters?.energyRecoveryLevel ?: 1
                val regenRate = (state.config.energyRegenPerSec * recoveryLvl).coerceAtLeast(1)

                if (state.optimisticEnergy < maxEnergy) {
                    val regenerated = (state.optimisticEnergy + regenRate).coerceAtMost(maxEnergy)
                    _uiState.value = state.copy(optimisticEnergy = regenerated)
                }

                // Check turbo expiration
                if (state.isTurboActive) {
                    val expires = state.boosters?.turboBoostExpiresAt ?: 0L
                    if (System.currentTimeMillis() > expires) {
                        _uiState.value = _uiState.value.copy(isTurboActive = false)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Tapping Engine (Online-First Checked)
    // ------------------------------------------------------------------------
    fun onCoinTapped(tapX: Float, tapY: Float) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Network required. Reconnecting to TAP server...")
            return
        }

        val config = state.config
        if (config.maintenanceMode) {
            _uiState.value = state.copy(userFeedbackMessage = "System is in maintenance mode. Taps are temporarily paused.")
            return
        }

        val energyCost = config.energyPerTap
        if (state.optimisticEnergy < energyCost) {
            _uiState.value = state.copy(userFeedbackMessage = "Energy depleted! Wait for regeneration or use an Ad Refill.")
            return
        }

        val multitapBonus = (state.boosters?.multitapLevel ?: 1) - 1
        val multiplier = if (state.isTurboActive) 2 else 1
        val coinReward = (config.coinsPerTap + multitapBonus) * multiplier

        val newCoins = state.optimisticCoins + coinReward
        val newEnergy = (state.optimisticEnergy - energyCost).coerceAtLeast(0)

        pendingCoinsToSync += coinReward
        pendingTapsToSync += 1

        val newParticle = FloatingParticle(
            text = "+$coinReward",
            x = tapX,
            y = tapY
        )
        val updatedParticles = (state.floatingParticles + newParticle).takeLast(12)

        _uiState.value = state.copy(
            optimisticCoins = newCoins,
            optimisticEnergy = newEnergy,
            floatingParticles = updatedParticles
        )

        scheduleSync()
    }

    fun removeParticle(id: String) {
        _uiState.value = _uiState.value.copy(
            floatingParticles = _uiState.value.floatingParticles.filter { it.id != id }
        )
    }

    private fun scheduleSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(1500L) // Debounce 1.5s
            flushPendingTaps()
        }
    }

    fun flushPendingTaps() {
        val coins = pendingCoinsToSync
        val taps = pendingTapsToSync
        val energy = _uiState.value.optimisticEnergy
        if (coins <= 0 && taps <= 0) return

        pendingCoinsToSync = 0L
        pendingTapsToSync = 0L

        viewModelScope.launch {
            try {
                repository.syncTapsBatch(
                    userId = currentUserId,
                    coinsEarned = coins,
                    energyConsumed = 0,
                    newEnergy = energy,
                    tapsCount = taps
                )
                analyticsHelper?.logTapBatch(taps, coins)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync taps batch", e)
                pendingCoinsToSync += coins
                pendingTapsToSync += taps
            }
        }
    }

    // ------------------------------------------------------------------------
    // Boosters & Upgrades
    // ------------------------------------------------------------------------
    fun buyBooster(type: String, cost: Long) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Active internet connection required to purchase boosters.")
            return
        }

        viewModelScope.launch {
            try {
                flushPendingTaps()
                repository.upgradeBooster(currentUserId, type, cost)
                val lvl = when (type) {
                    "multitap" -> (state.boosters?.multitapLevel ?: 1) + 1
                    "energy_boost" -> (state.boosters?.energyBoostLevel ?: 1) + 1
                    "recovery" -> (state.boosters?.energyRecoveryLevel ?: 1) + 1
                    else -> 1
                }
                analyticsHelper?.logBoosterPurchased(type, lvl, cost)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Upgraded successfully!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message ?: "Upgrade failed")
            }
        }
    }

    fun activateTurbo() {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Active internet connection required.")
            return
        }

        viewModelScope.launch {
            try {
                repository.activateTurboBoost(currentUserId, 60L)
                analyticsHelper?.logBoosterPurchased("turbo_boost", 1, 0L)
                _uiState.value = _uiState.value.copy(isTurboActive = true, userFeedbackMessage = "Turbo Boost 2x Active for 60s!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message ?: "Turbo failed")
            }
        }
    }

    fun refillEnergyAdReward() {
        viewModelScope.launch {
            try {
                repository.refillEnergy(currentUserId, 500)
                _uiState.value = _uiState.value.copy(
                    optimisticEnergy = (_uiState.value.optimisticEnergy + 500).coerceAtMost(_uiState.value.balance?.maxEnergy ?: 1000),
                    userFeedbackMessage = "+500 Energy Refilled via Ad!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    // ------------------------------------------------------------------------
    // Daily Reward
    // ------------------------------------------------------------------------
    fun claimDailyReward() {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Active internet connection required.")
            return
        }

        val streak = (state.dailyCheckIn?.currentStreak ?: 0) + 1
        val rewardTable = listOf(500L, 1000L, 2000L, 3500L, 5000L, 7500L, 15000L)
        val amount = rewardTable.getOrElse((streak - 1) % 7) { 1000L }

        viewModelScope.launch {
            try {
                flushPendingTaps()
                val claimedStreak = repository.claimDailyReward(currentUserId, amount)
                analyticsHelper?.logDailyStreakClaimed(claimedStreak, amount)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Day $claimedStreak reward of $amount coins claimed!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message ?: "Claim failed")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Tasks
    // ------------------------------------------------------------------------
    fun claimTask(task: TaskItem) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Active internet connection required.")
            return
        }

        viewModelScope.launch {
            try {
                flushPendingTaps()
                repository.completeAndClaimTask(currentUserId, task)
                analyticsHelper?.logTaskCompleted(task.taskId, task.rewardCoins)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "+${task.rewardCoins} coins claimed for task: ${task.title}!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message ?: "Task claim failed")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Withdrawals
    // ------------------------------------------------------------------------
    fun requestWithdrawal(coinAmount: Long, method: String, paymentDetails: String) {
        val state = _uiState.value
        if (!state.isOnline) {
            _uiState.value = state.copy(userFeedbackMessage = "Active internet connection required.")
            return
        }

        val fiat = coinAmount * state.config.coinToUsdRate
        viewModelScope.launch {
            try {
                flushPendingTaps()
                val req = repository.submitWithdrawalRequest(
                    userId = currentUserId,
                    username = state.profile?.username ?: "User",
                    coinAmount = coinAmount,
                    paymentMethod = method,
                    paymentDetails = paymentDetails
                )
                analyticsHelper?.logWithdrawalRequested(method, coinAmount, fiat)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Withdrawal ${req.withdrawalId} submitted successfully!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message ?: "Withdrawal request failed")
            }
        }
    }

    // ------------------------------------------------------------------------
    // AI Generative Coin Studio
    // ------------------------------------------------------------------------
    fun generateCustomCoin(prompt: String, resolution: String) {
        _uiState.value = _uiState.value.copy(isGeneratingAiCoin = true, aiCoinError = null)
        viewModelScope.launch {
            val result = aiRepository.generateCollectorCoinImage(prompt, resolution)
            result.onSuccess { bitmap ->
                _uiState.value = _uiState.value.copy(
                    customCoinBitmap = bitmap,
                    isGeneratingAiCoin = false,
                    userFeedbackMessage = "Custom $resolution AI Collector Coin generated!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isGeneratingAiCoin = false,
                    aiCoinError = err.message ?: "Generation failed"
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // High Thinking AI Strategy Optimization (gemini-4-argon)
    // ------------------------------------------------------------------------
    fun fetchStrategyReport() {
        _uiState.value = _uiState.value.copy(isGeneratingStrategy = true)
        val state = _uiState.value
        viewModelScope.launch {
            val result = aiRepository.getTapStrategyAdvice(
                coins = state.optimisticCoins,
                totalTaps = state.balance?.totalTaps ?: 0L,
                multitapLevel = state.boosters?.multitapLevel ?: 1,
                energyBoostLevel = state.boosters?.energyBoostLevel ?: 1,
                energyRecoveryLevel = state.boosters?.energyRecoveryLevel ?: 1,
                maxEnergy = state.balance?.maxEnergy ?: 1000
            )
            result.onSuccess { report ->
                _uiState.value = _uiState.value.copy(
                    aiStrategyReport = report,
                    isGeneratingStrategy = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    aiStrategyReport = "Error calculating strategy: ${err.message}",
                    isGeneratingStrategy = false
                )
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = null)
    }

    // ------------------------------------------------------------------------
    // Admin Operations
    // ------------------------------------------------------------------------
    fun adminUpdateConfig(newConfig: AppConfiguration) {
        viewModelScope.launch {
            try {
                repository.saveAppConfiguration(newConfig)
                analyticsHelper?.logAdminConfigUpdate()
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Remote config published to Realtime Database!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    fun adminReviewWithdrawal(id: String, status: String, note: String?) {
        viewModelScope.launch {
            try {
                repository.reviewWithdrawal(id, status, note)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Withdrawal marked as $status")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    fun adminAdjustBalance(userId: String, amount: Long, reason: String) {
        viewModelScope.launch {
            try {
                repository.adminAdjustUserBalance(userId, amount, reason)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Adjusted balance by $amount")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    fun adminToggleSuspend(userId: String, isSuspended: Boolean) {
        viewModelScope.launch {
            try {
                repository.adminToggleUserSuspension(userId, isSuspended)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "User suspension set to $isSuspended")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    fun adminCreateTask(task: TaskItem) {
        viewModelScope.launch {
            try {
                repository.adminCreateOrUpdateTask(task)
                _uiState.value = _uiState.value.copy(userFeedbackMessage = "Task saved!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userFeedbackMessage = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        flushPendingTaps()
        energyRegenJob?.cancel()
    }
}
