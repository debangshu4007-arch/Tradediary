package com.example.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(
    private val tradeDao: TradeDao,
    private val challengeDao: ChallengeDao
) {
    val allTrades: Flow<List<TradeEntity>> = tradeDao.getAllTrades()
    val allDailyAnalyses: Flow<List<DailyAnalysisEntity>> = tradeDao.getAllDailyAnalyses()
    val allFeedback: Flow<List<FeedbackEntity>> = tradeDao.getAllFeedback()
    val allChallenges: Flow<List<ChallengeEntity>> = challengeDao.getAllChallenges()

    suspend fun insertTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun updateTrade(trade: TradeEntity) {
        tradeDao.updateTrade(trade)
    }

    suspend fun deleteTrade(trade: TradeEntity) {
        tradeDao.deleteTrade(trade)
    }

    suspend fun insertTrades(trades: List<TradeEntity>) {
        tradeDao.insertTrades(trades)
    }

    suspend fun deleteAllTrades() {
        tradeDao.deleteAllTrades()
    }

    suspend fun getAllTradesSnapshot(): List<TradeEntity> =
        tradeDao.getAllTradesSnapshot()

    suspend fun insertDailyAnalysis(analysis: DailyAnalysisEntity) {
        tradeDao.insertDailyAnalysis(analysis)
    }

    suspend fun insertFeedback(feedback: FeedbackEntity) {
        tradeDao.insertFeedback(feedback)
    }

    fun getTradesByChallengeId(challengeId: Long): Flow<List<TradeEntity>> =
        tradeDao.getTradesByChallengeId(challengeId)

    suspend fun insertChallenge(challenge: ChallengeEntity): Long =
        challengeDao.insertChallenge(challenge)

    suspend fun updateChallenge(challenge: ChallengeEntity) {
        challengeDao.updateChallenge(challenge)
    }

    suspend fun deleteChallenge(challenge: ChallengeEntity) {
        challengeDao.deleteChallenge(challenge)
    }

    suspend fun getChallengeById(id: Long): ChallengeEntity? =
        challengeDao.getChallengeById(id)
}
