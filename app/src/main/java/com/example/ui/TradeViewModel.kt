package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GroqService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    data class Success(val message: String) : ConnectionTestState()
    data class Failure(val message: String) : ConnectionTestState()
}

sealed class DataOpState {
    object Idle : DataOpState()
    object Running : DataOpState()
    data class Success(val message: String) : DataOpState()
    data class Failure(val message: String) : DataOpState()
}

data class StrategyAnalytics(
    val strategy: String,
    val totalTrades: Int,
    val wins: Int,
    val winRate: Double,
    val totalPL: Double,
    val avgRR: Double
)

data class ChallengeStats(
    val totalTrades: Int,
    val wins: Int,
    val winRate: Double,
    val totalPL: Double,
    val bestTradePL: Double,
    val worstTradePL: Double,
    val avgRR: Double
)

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TradeRepository
    private val feedbackRepository = FeedbackRepository()
    private val userIdProvider = UserIdProvider(application)
    private val settingsStore = SettingsStore(application)
    private val secureKeyStore = SecureKeyStore(application)
    private val backupManager = BackupManager(application)

    val userId: String

    private val _groqApiKey = MutableStateFlow(secureKeyStore.getGroqApiKey())
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    val appSettings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _dataOpState = MutableStateFlow<DataOpState>(DataOpState.Idle)
    val dataOpState: StateFlow<DataOpState> = _dataOpState.asStateFlow()

    val allTrades: StateFlow<List<TradeEntity>>
    val allDailyAnalyses: StateFlow<List<DailyAnalysisEntity>>
    val allFeedback: StateFlow<List<FeedbackEntity>>
    val allChallenges: StateFlow<List<ChallengeEntity>>

    private val _aiAnalysis = MutableStateFlow<String>("")
    val aiAnalysis: StateFlow<String> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _feedbackSubmitState = MutableStateFlow<FeedbackSubmitState>(FeedbackSubmitState.Idle)
    val feedbackSubmitState: StateFlow<FeedbackSubmitState> = _feedbackSubmitState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        val tradeDao = db.tradeDao()
        val challengeDao = db.challengeDao()
        repository = TradeRepository(tradeDao, challengeDao)

        val id = runBlockingOnInit { userIdProvider.getUserId() }
        userId = id
        AnalyticsHelper.setUserId(id)

        allTrades = repository.allTrades
            .catch { t ->
                android.util.Log.e("TradeViewModel", "Error streaming trades log", t)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allDailyAnalyses = repository.allDailyAnalyses
            .catch { t ->
                android.util.Log.e("TradeViewModel", "Error streaming daily analyses", t)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allFeedback = repository.allFeedback
            .catch { t ->
                android.util.Log.e("TradeViewModel", "Error streaming feedback", t)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allChallenges = repository.allChallenges
            .catch { t ->
                android.util.Log.e("TradeViewModel", "Error streaming challenges", t)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    private fun <T> runBlockingOnInit(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            block()
        }
    }

    fun addTrade(
        entryPrice: Double,
        exitPrice: Double,
        stopLoss: Double,
        quantity: Int,
        setupLogic: String,
        tradeThesis: String,
        mistakeTag: String,
        emotionBefore: String,
        emotionAfter: String,
        lessonsLearned: String,
        beforeChartUri: String?,
        afterChartUri: String?,
        optionType: String = "CE",
        tradeAction: String = "BUY",
        strikePrice: Double = 0.0,
        tradeDateMillis: Long = System.currentTimeMillis(),
        instrument: String = "Nifty",
        marketSegment: String = "F&O",
        partialExits: String = "",
        behaviorTags: String = "",
        challengeId: Long? = null,
        plannedTarget: Double = 0.0,
        entryTimeMillis: Long = 0L,
        confirmations: String = "",
        mfePrice: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                val candidate = TradeEntity(
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    stopLoss = stopLoss,
                    quantity = quantity,
                    setupLogic = setupLogic,
                    tradeThesis = tradeThesis,
                    mistakeTag = mistakeTag,
                    emotionBefore = emotionBefore,
                    emotionAfter = emotionAfter,
                    lessonsLearned = lessonsLearned,
                    beforeChartUri = beforeChartUri,
                    afterChartUri = afterChartUri,
                    optionType = optionType,
                    tradeAction = tradeAction,
                    strikePrice = strikePrice,
                    tradeDateMillis = tradeDateMillis,
                    instrument = instrument,
                    marketSegment = marketSegment,
                    partialExits = partialExits,
                    behaviorTags = behaviorTags,
                    challengeId = challengeId,
                    plannedTarget = plannedTarget,
                    entryTimeMillis = entryTimeMillis,
                    confirmations = confirmations,
                    mfePrice = mfePrice
                )
                val autoTags = MistakeDetector.detect(candidate, allTrades.value).joinToString(",")
                val trade = candidate.copy(autoTags = autoTags)
                repository.insertTrade(trade)
                AnalyticsHelper.logAddTrade(
                    mapOf(
                        "marketSegment" to marketSegment,
                        "tradeAction" to tradeAction,
                        "optionType" to optionType,
                        "hasScreenshot" to if (beforeChartUri != null || afterChartUri != null) "1" else "0"
                    )
                )
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error adding trade", t)
            }
        }
    }

    fun updateTrade(
        trade: TradeEntity,
        entryPrice: Double,
        exitPrice: Double,
        stopLoss: Double,
        quantity: Int,
        setupLogic: String,
        tradeThesis: String,
        mistakeTag: String,
        emotionBefore: String,
        emotionAfter: String,
        lessonsLearned: String,
        beforeChartUri: String?,
        afterChartUri: String?,
        optionType: String,
        tradeAction: String,
        strikePrice: Double,
        tradeDateMillis: Long,
        instrument: String,
        marketSegment: String,
        partialExits: String,
        behaviorTags: String,
        plannedTarget: Double = trade.plannedTarget,
        entryTimeMillis: Long = trade.entryTimeMillis,
        confirmations: String = trade.confirmations,
        mfePrice: Double = trade.mfePrice
    ) {
        viewModelScope.launch {
            try {
                val candidate = trade.copy(
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    stopLoss = stopLoss,
                    quantity = quantity,
                    setupLogic = setupLogic,
                    tradeThesis = tradeThesis,
                    mistakeTag = mistakeTag,
                    emotionBefore = emotionBefore,
                    emotionAfter = emotionAfter,
                    lessonsLearned = lessonsLearned,
                    beforeChartUri = if (beforeChartUri != null && !beforeChartUri.startsWith("content://") && beforeChartUri.isNotEmpty()) beforeChartUri else trade.beforeChartUri,
                    afterChartUri = if (afterChartUri != null && !afterChartUri.startsWith("content://") && afterChartUri.isNotEmpty()) afterChartUri else trade.afterChartUri,
                    optionType = optionType,
                    tradeAction = tradeAction,
                    strikePrice = strikePrice,
                    tradeDateMillis = tradeDateMillis,
                    instrument = instrument,
                    marketSegment = marketSegment,
                    partialExits = partialExits,
                    behaviorTags = behaviorTags,
                    plannedTarget = plannedTarget,
                    entryTimeMillis = entryTimeMillis,
                    confirmations = confirmations,
                    mfePrice = mfePrice
                )
                val priorTrades = allTrades.value.filter { it.id != trade.id }
                val autoTags = MistakeDetector.detect(candidate, priorTrades).joinToString(",")
                val updated = candidate.copy(autoTags = autoTags)
                repository.updateTrade(updated)
                AnalyticsHelper.logEditTrade(
                    mapOf(
                        "marketSegment" to marketSegment,
                        "tradeAction" to tradeAction,
                        "optionType" to optionType,
                        "hasScreenshot" to if (beforeChartUri != null || afterChartUri != null) "1" else "0"
                    )
                )
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error updating trade", t)
            }
        }
    }

    fun submitFeedback(rating: Int, message: String) {
        viewModelScope.launch {
            _feedbackSubmitState.value = FeedbackSubmitState.Loading
            try {
                repository.insertFeedback(FeedbackEntity(rating = rating, message = message))
                val result = feedbackRepository.submitFeedback(rating, message, userId)
                result.fold(
                    onSuccess = {
                        _feedbackSubmitState.value = FeedbackSubmitState.Success
                        _snackbarMessage.value = "Feedback submitted successfully"
                        AnalyticsHelper.logFeedbackSubmit(success = true)
                    },
                    onFailure = { e ->
                        _feedbackSubmitState.value = FeedbackSubmitState.Error(e.message ?: "Unknown error")
                        _snackbarMessage.value = "Failed to submit feedback: ${e.localizedMessage}"
                        AnalyticsHelper.logFeedbackSubmit(success = false)
                    }
                )
            } catch (t: Throwable) {
                _feedbackSubmitState.value = FeedbackSubmitState.Error(t.message ?: "Unknown error")
                _snackbarMessage.value = "Failed to submit feedback: ${t.localizedMessage}"
                AnalyticsHelper.logFeedbackSubmit(success = false)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun deleteTrade(trade: TradeEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTrade(trade)
                AnalyticsHelper.logDeleteTrade()
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error deleting trade", t)
            }
        }
    }

    fun addDailyAnalysis(
        dateStr: String,
        whatWentRight: String,
        whatWentWrong: String,
        marketCondition: String,
        followedSetup: Boolean
    ) {
        viewModelScope.launch {
            try {
                val analysis = DailyAnalysisEntity(
                    dateStr = dateStr,
                    whatWentRight = whatWentRight,
                    whatWentWrong = whatWentWrong,
                    marketCondition = marketCondition,
                    followedSetup = followedSetup
                )
                repository.insertDailyAnalysis(analysis)
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error adding daily analysis", t)
            }
        }
    }

    fun saveGroqApiKey(key: String) {
        secureKeyStore.setGroqApiKey(key.trim())
        _groqApiKey.value = key.trim()
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun deleteGroqApiKey() {
        secureKeyStore.clearGroqApiKey()
        _groqApiKey.value = ""
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun testGroqConnection() {
        val key = _groqApiKey.value
        if (key.isBlank()) {
            _connectionTestState.value = ConnectionTestState.Failure("No API key saved.")
            return
        }
        _connectionTestState.value = ConnectionTestState.Testing
        viewModelScope.launch {
            try {
                val reply = GroqService.getTradeAnalysis(
                    "Reply with the single word OK to confirm connectivity.",
                    key
                )
                if (reply.startsWith("Analysis Error", ignoreCase = true) ||
                    reply.startsWith("Please add", ignoreCase = true)
                ) {
                    _connectionTestState.value = ConnectionTestState.Failure(reply)
                } else {
                    _connectionTestState.value = ConnectionTestState.Success("Connected. Model responded successfully.")
                }
            } catch (t: Throwable) {
                _connectionTestState.value =
                    ConnectionTestState.Failure(t.localizedMessage ?: "Connection failed.")
            }
        }
    }

    fun clearConnectionTest() {
        _connectionTestState.value = ConnectionTestState.Idle
    }

    // ---- Preferences ----
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    // ---- Data & Backup ----
    fun clearDataOpState() { _dataOpState.value = DataOpState.Idle }

    fun exportTrades() {
        runDataOp("Exported") {
            val trades = repository.getAllTradesSnapshot()
            if (trades.isEmpty()) throw IllegalStateException("No trades to export.")
            val file = backupManager.writeBackup(trades, prefix = "trade_export")
            "Exported ${trades.size} trades to ${file.name}"
        }
    }

    fun backupNow() {
        runDataOp("Backed up") {
            val trades = repository.getAllTradesSnapshot()
            val file = backupManager.writeBackup(trades, prefix = "backup")
            settingsStore.update { it.copy(lastBackupMillis = System.currentTimeMillis()) }
            "Backup saved: ${file.name} (${trades.size} trades)"
        }
    }

    fun importTrades(uri: android.net.Uri) {
        runDataOp("Imported") {
            val imported = backupManager.readFromUri(uri)
            repository.insertTrades(imported)
            "Imported ${imported.size} trades"
        }
    }

    fun deleteAllTrades() {
        runDataOp("Deleted") {
            repository.deleteAllTrades()
            "All trades deleted"
        }
    }

    fun clearCache() {
        runDataOp("Cleared") {
            val freed = backupManager.clearCache()
            "Cache cleared (${freed / 1024} KB freed)"
        }
    }

    private fun runDataOp(verb: String, block: suspend () -> String) {
        _dataOpState.value = DataOpState.Running
        viewModelScope.launch {
            try {
                val message = withContext(Dispatchers.IO) { block() }
                _dataOpState.value = DataOpState.Success(message)
            } catch (t: Throwable) {
                _dataOpState.value = DataOpState.Failure(t.localizedMessage ?: "$verb failed.")
            }
        }
    }

    fun triggerAIAnalysis() {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _aiAnalysis.value = "Analyzing trading terminal logs..."

        viewModelScope.launch {
            var success = false
            try {
                val trades = allTrades.value
                val diaries = allDailyAnalyses.value

                if (trades.isEmpty()) {
                    _aiAnalysis.value = "Your trading log is currently empty. Enter a few trades first on the Trade Entry tab to let the AI analyze your setup accuracy and mistakes!"
                    _isAnalyzing.value = false
                    return@launch
                }

                val promptBuilder = StringBuilder()
                val weekdayStats = TradeAnalytics.weekdayPerformance(trades)
                val bestDay = TradeAnalytics.bestWeekday(weekdayStats)
                val worstDay = TradeAnalytics.worstWeekday(weekdayStats)
                val avgRR = trades.map {
                    val risk = kotlin.math.abs(it.entryPrice - it.stopLoss)
                    val reward = kotlin.math.abs(it.effectiveExitPrice() - it.entryPrice)
                    if (risk > 0) reward / risk else 0.0
                }.average()
                val avgPositionSize = trades.map { it.entryPrice * it.quantity }.average()
                val biggestLoss = trades.minByOrNull { it.getPL() }
                val mistakeSummary = trades
                    .filter { it.mistakeTag.isNotEmpty() && it.mistakeTag != "None" }
                    .groupBy { it.mistakeTag }
                    .map { (mistake, items) -> "$mistake: ${items.size} trades, net P&L ${items.sumOf { it.getPL() }}" }
                    .joinToString("\n")
                val behaviorSummary = trades
                    .flatMap { trade -> trade.parsedBehaviorTags().map { it to trade.getPL() } }
                    .groupBy { it.first }
                    .map { (tag, items) -> "$tag: ${items.size} occurrences, net P&L ${items.sumOf { it.second }}" }
                    .joinToString("\n")
                val segmentSummary = trades
                    .groupBy { it.marketSegment }
                    .map { (segment, items) -> "$segment: ${items.size} trades, win rate ${(items.count { it.isWin() }.toDouble() / items.size * 100).toInt()}%, net P&L ${items.sumOf { it.getPL() }}" }
                    .joinToString("\n")

                promptBuilder.append("Analyze this options trading journal for loopholes and performance optimization.\n")
                promptBuilder.append("Focus on risk-reward issues, partial exits, money management, best/worst trading day, repeated mistakes, stock vs F&O behavior, CE vs PE, and Buy vs Sell behavior.\n\n")
                promptBuilder.append("Portfolio summary:\n")
                promptBuilder.append("- Total trades: ${trades.size}\n")
                promptBuilder.append("- Overall win rate: ${(trades.count { it.isWin() }.toDouble() / trades.size * 100).toInt()}%\n")
                promptBuilder.append("- Net P&L: ${trades.sumOf { it.getPL() }}\n")
                promptBuilder.append("- Average RR: ${String.format(java.util.Locale.US, "%.2f", avgRR)}\n")
                promptBuilder.append("- Average position size: ${String.format(java.util.Locale.US, "%.2f", avgPositionSize)}\n")
                promptBuilder.append("- Best weekday: ${bestDay?.dayName ?: "No data"} ${bestDay?.winRate ?: 0}%\n")
                promptBuilder.append("- Worst weekday: ${worstDay?.dayName ?: "No data"} ${worstDay?.winRate ?: 0}%\n")
                promptBuilder.append("- Biggest loss: ${biggestLoss?.contractLabel() ?: "No data"} P&L ${biggestLoss?.getPL() ?: 0.0}\n\n")

                promptBuilder.append("Weekday performance:\n")
                weekdayStats.forEach { day ->
                    promptBuilder.append("- ${day.dayName}: ${day.winRate}% (${day.wins}/${day.total}), Avg P&L ${String.format(java.util.Locale.US, "%.1f", day.averagePL)}\n")
                }
                promptBuilder.append("\nMistake summary:\n${if (mistakeSummary.isEmpty()) "No tagged mistakes." else mistakeSummary}\n\n")
                promptBuilder.append("Behavior summary:\n${if (behaviorSummary.isEmpty()) "No behavior tags." else behaviorSummary}\n\n")
                promptBuilder.append("Segment summary:\n${if (segmentSummary.isEmpty()) "No segment data." else segmentSummary}\n\n")

                trades.take(15).forEachIndexed { index, trade ->
                    promptBuilder.append("Trade #${index + 1}:\n")
                    promptBuilder.append("- Segment: ${trade.marketSegment}, Contract: ${trade.contractLabel()}, Date: ${TradeAnalytics.formatTradeDate(trade.tradeDateMillis)}\n")
                    promptBuilder.append("- Entry: ${trade.entryPrice}, Exit: ${trade.exitPrice}, SL: ${trade.stopLoss}, Qty: ${trade.quantity}\n")
                    if (trade.partialExits.isNotBlank()) promptBuilder.append("- Partial exits: ${trade.partialExits}\n")
                    promptBuilder.append("- Setup: ${trade.setupLogic}, Mistake: ${trade.mistakeTag}\n")
                    if (trade.behaviorTags.isNotBlank()) promptBuilder.append("- Behavior tags: ${trade.behaviorTags}\n")
                    promptBuilder.append("- Emotions: Before=${trade.emotionBefore}, After=${trade.emotionAfter}\n")
                    promptBuilder.append("- RR: ${trade.getRiskRewardRatioString()}\n")
                    promptBuilder.append("- Realized P&L: ${trade.getPL()}\n\n")
                }

                if (diaries.isNotEmpty()) {
                    promptBuilder.append("EOD Daily Diaries:\n")
                    diaries.take(5).forEach { diary ->
                        promptBuilder.append("- Date: ${diary.dateStr} | Regime: ${diary.marketCondition} | Followed Setup: ${diary.followedSetup}\n")
                        promptBuilder.append("  Successes: ${diary.whatWentRight}\n")
                        promptBuilder.append("  Errors: ${diary.whatWentWrong}\n\n")
                    }
                }

                val result = GroqService.getTradeAnalysis(promptBuilder.toString(), groqApiKey.value)
                _aiAnalysis.value = result
                success = true
            } catch (t: Throwable) {
                _aiAnalysis.value = "Analysis Error: ${t.localizedMessage ?: "Unknown error"}"
            } finally {
                _isAnalyzing.value = false
                AnalyticsHelper.logAiCoachRun(success)
            }
        }
    }

    fun computeStrategyAnalytics(): List<StrategyAnalytics> {
        val trades = allTrades.value
        return trades.groupBy { it.setupLogic.ifEmpty { "Unspecified" } }
            .map { (strategy, items) ->
                val wins = items.count { it.isWin() }
                val total = items.size
                val totalPL = items.sumOf { it.getPL() }
                val avgRR = items.mapNotNull { t ->
                    val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
                    val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
                    if (risk > 0) reward / risk else null
                }.average().let { if (it.isNaN()) 0.0 else it }
                StrategyAnalytics(
                    strategy = strategy,
                    totalTrades = total,
                    wins = wins,
                    winRate = if (total > 0) wins.toDouble() / total * 100.0 else 0.0,
                    totalPL = totalPL,
                    avgRR = avgRR
                )
            }
            .sortedByDescending { it.totalTrades }
    }

    fun computeChallengeStats(challengeId: Long): ChallengeStats {
        val trades = allTrades.value.filter { it.challengeId == challengeId }
        val wins = trades.count { it.isWin() }
        val total = trades.size
        val totalPL = trades.sumOf { it.getPL() }
        val bestTrade = trades.maxByOrNull { it.getPL() }
        val worstTrade = trades.minByOrNull { it.getPL() }
        val avgRR = trades.mapNotNull { t ->
            val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
            val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
            if (risk > 0) reward / risk else null
        }.average().let { if (it.isNaN()) 0.0 else it }
        return ChallengeStats(
            totalTrades = total,
            wins = wins,
            winRate = if (total > 0) wins.toDouble() / total * 100.0 else 0.0,
            totalPL = totalPL,
            bestTradePL = bestTrade?.getPL() ?: 0.0,
            worstTradePL = worstTrade?.getPL() ?: 0.0,
            avgRR = avgRR
        )
    }

    fun createChallenge(
        name: String,
        capital: Double,
        duration: Int,
        strategyType: String,
        instrument: String,
        targetProfit: Double
    ) {
        viewModelScope.launch {
            try {
                val challenge = ChallengeEntity(
                    name = name,
                    capital = capital,
                    duration = duration,
                    strategyType = strategyType,
                    instrument = instrument,
                    targetProfit = targetProfit
                )
                repository.insertChallenge(challenge)
                _snackbarMessage.value = "Challenge created!"
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error creating challenge", t)
                _snackbarMessage.value = "Failed to create challenge"
            }
        }
    }

    fun deleteChallenge(challenge: ChallengeEntity) {
        viewModelScope.launch {
            try {
                repository.deleteChallenge(challenge)
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error deleting challenge", t)
            }
        }
    }

    fun toggleChallengeActive(challenge: ChallengeEntity) {
        viewModelScope.launch {
            try {
                repository.updateChallenge(challenge.copy(isActive = !challenge.isActive))
            } catch (t: Throwable) {
                android.util.Log.e("TradeViewModel", "Error toggling challenge", t)
            }
        }
    }
}
