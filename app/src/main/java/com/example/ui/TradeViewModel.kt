package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TradeRepository
    
    val allTrades: StateFlow<List<TradeEntity>>
    val allDailyAnalyses: StateFlow<List<DailyAnalysisEntity>>
    
    private val _aiAnalysis = MutableStateFlow<String>("")
    val aiAnalysis: StateFlow<String> = _aiAnalysis.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    init {
        val tradeDao = AppDatabase.getDatabase(application).tradeDao()
        repository = TradeRepository(tradeDao)
        
        allTrades = repository.allTrades
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        allDailyAnalyses = repository.allDailyAnalyses
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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
        afterChartUri: String?
    ) {
        viewModelScope.launch {
            val trade = TradeEntity(
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
                afterChartUri = afterChartUri
            )
            repository.insertTrade(trade)
        }
    }

    fun deleteTrade(trade: TradeEntity) {
        viewModelScope.launch {
            repository.deleteTrade(trade)
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
            val analysis = DailyAnalysisEntity(
                dateStr = dateStr,
                whatWentRight = whatWentRight,
                whatWentWrong = whatWentWrong,
                marketCondition = marketCondition,
                followedSetup = followedSetup
            )
            repository.insertDailyAnalysis(analysis)
        }
    }

    fun triggerAIAnalysis() {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _aiAnalysis.value = "Analyzing trading terminal logs..."
        
        viewModelScope.launch {
            val trades = allTrades.value
            val diaries = allDailyAnalyses.value
            
            if (trades.isEmpty()) {
                _aiAnalysis.value = "Your trading log is currently empty. Enter a few Nifty trades first on the Trade Entry tab to let the AI analyze your setup accuracy and mistakes!"
                _isAnalyzing.value = false
                return@launch
            }
            
            val promptBuilder = StringBuilder()
            promptBuilder.append("Analyze this NIFTY trading journal for performance optimization:\n\n")
            
            trades.take(15).forEachIndexed { index, trade ->
                promptBuilder.append("Trade #${index + 1}:\n")
                promptBuilder.append("- Entry: ${trade.entryPrice}, Exit: ${trade.exitPrice}, SL: ${trade.stopLoss}, Qty: ${trade.quantity}\n")
                promptBuilder.append("- Setup: ${trade.setupLogic}, Mistake: ${trade.mistakeTag}\n")
                promptBuilder.append("- Emotions: Before=${trade.emotionBefore}, After=${trade.emotionAfter}\n")
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
            
            val result = GeminiService.getTradeAnalysis(promptBuilder.toString())
            _aiAnalysis.value = result
            _isAnalyzing.value = false
        }
    }
}
