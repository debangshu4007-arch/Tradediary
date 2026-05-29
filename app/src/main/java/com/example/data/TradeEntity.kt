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
    val instrument: String = "Nifty",
    val optionType: String = "CE",
    val tradeAction: String = "BUY",
    val strikePrice: Double = 0.0,
    val tradeDateMillis: Long = timestamp,
    val marketSegment: String = "F&O",
    val partialExits: String = "",
    val behaviorTags: String = "",
    val challengeId: Long? = null,
    val plannedTarget: Double = 0.0,
    val entryTimeMillis: Long = 0L,
    val confirmations: String = "",
    val mfePrice: Double = 0.0,
    val autoTags: String = ""
) {
    fun parsedConfirmations(): List<String> =
        confirmations.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun parsedAutoTags(): List<String> =
        autoTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun exitLegs(): List<Pair<Int, Double>> {
        return partialExits.split(",", ";", "\n")
            .mapNotNull { raw ->
                val parts = raw.trim().replace(" ", "").split("@", "x", "X")
                if (parts.size != 2) return@mapNotNull null
                val qty = parts[0].toIntOrNull() ?: return@mapNotNull null
                val price = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                if (qty > 0 && price > 0.0) qty to price else null
            }
    }

    fun effectiveExitPrice(): Double {
        val legs = exitLegs()
        val exitedQty = legs.sumOf { it.first }
        if (legs.isEmpty() || exitedQty <= 0) return exitPrice
        val cappedQty = exitedQty.coerceAtMost(quantity)
        val weightedTotal = legs.takeWhileInclusiveQuantity(quantity).sumOf { it.first * it.second }
        return weightedTotal / cappedQty
    }

    // Computed Properties for Trade Entry RR Analyzer
    fun getPL(): Double {
        val direction = if (tradeAction.equals("SELL", ignoreCase = true)) -1 else 1
        val legs = exitLegs()
        if (legs.isEmpty()) return (exitPrice - entryPrice) * quantity * direction
        var remaining = quantity
        var total = 0.0
        legs.forEach { (legQty, legPrice) ->
            if (remaining <= 0) return@forEach
            val appliedQty = legQty.coerceAtMost(remaining)
            total += (legPrice - entryPrice) * appliedQty * direction
            remaining -= appliedQty
        }
        if (remaining > 0) total += (exitPrice - entryPrice) * remaining * direction
        return total
    }

    fun isWin(): Boolean = getPL() > 0.0

    fun getRiskAmount(): Double {
        return Math.abs(entryPrice - stopLoss) * quantity
    }

    fun getRewardAmount(): Double {
        return Math.abs(effectiveExitPrice() - entryPrice) * quantity
    }

    fun getRiskRewardRatioString(): String {
        val riskDiff = Math.abs(entryPrice - stopLoss)
        val rewardDiff = Math.abs(effectiveExitPrice() - entryPrice)
        if (riskDiff == 0.0) return "1:1"
        val ratio = rewardDiff / riskDiff
        return "1:${String.format(java.util.Locale.US, "%.1f", ratio)}"
    }

    fun parsedBehaviorTags(): List<String> =
        behaviorTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun List<Pair<Int, Double>>.takeWhileInclusiveQuantity(maxQty: Int): List<Pair<Int, Double>> {
        var remaining = maxQty
        val result = mutableListOf<Pair<Int, Double>>()
        for ((qty, price) in this) {
            if (remaining <= 0) break
            val appliedQty = qty.coerceAtMost(remaining)
            result += appliedQty to price
            remaining -= appliedQty
        }
        return result
    }

    fun contractLabel(): String {
        val inst = instrument.uppercase(java.util.Locale.US)
        if (optionType.uppercase() == "EQ") {
            return "$inst ${tradeAction.uppercase(java.util.Locale.US)}".trim()
        }
        val strikeText = if (strikePrice > 0.0) {
            String.format(java.util.Locale.US, "%.0f", strikePrice)
        } else {
            ""
        }
        return "$inst $strikeText ${optionType.uppercase(java.util.Locale.US)} ${tradeAction.uppercase(java.util.Locale.US)}".trim().replace("\\s+".toRegex(), " ")
    }
}
