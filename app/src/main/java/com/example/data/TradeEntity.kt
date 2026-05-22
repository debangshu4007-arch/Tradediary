package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double,
    val quantity: Int,
    val setupLogic: String,
    val tradeThesis: String,
    val mistakeTag: String, // Tagged mistakes (e.g. "Overtrading", "FOMO entry", "Wrong RR")
    val emotionBefore: String,
    val emotionAfter: String,
    val lessonsLearned: String,
    val beforeChartUri: String? = null,
    val afterChartUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val instrument: String = "Nifty"
) {
    // Computed Properties for Trade Entry RR Analyzer
    fun getPL(): Double {
        return (exitPrice - entryPrice) * quantity
    }

    fun isWin(): Boolean = exitPrice > entryPrice

    fun getRiskAmount(): Double {
        return Math.abs(entryPrice - stopLoss) * quantity
    }

    fun getRewardAmount(): Double {
        return Math.abs(exitPrice - entryPrice) * quantity
    }

    fun getRiskRewardRatioString(): String {
        val riskDiff = Math.abs(entryPrice - stopLoss)
        val rewardDiff = Math.abs(exitPrice - entryPrice)
        if (riskDiff == 0.0) return "1:1"
        val ratio = rewardDiff / riskDiff
        return "1:${String.format("%.1f", ratio)}"
    }
}
