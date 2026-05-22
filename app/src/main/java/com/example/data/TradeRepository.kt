package com.example.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(private val tradeDao: TradeDao) {
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val allDailyAnalyses: Flow<List<DailyAnalysisEntity>> = tradeDao.getAllDailyAnalyses()

    suspend fun insertTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun deleteTrade(trade: TradeEntity) {
        tradeDao.deleteTrade(trade)
    }

    suspend fun insertDailyAnalysis(analysis: DailyAnalysisEntity) {
        tradeDao.insertDailyAnalysis(analysis)
    }
}
