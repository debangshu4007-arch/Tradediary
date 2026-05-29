package com.example.data

import java.util.Calendar
import kotlin.math.abs

/**
 * Offline rule-based mistake detection. No AI/network — pure if/else over the
 * trade plus the chronological history of prior trades by the same user.
 *
 * Returns a list of human-readable tags (e.g. "Early Exit", "Revenge Trade").
 * The set of recognised tags is exposed via [AllTags] so downstream analytics
 * can iterate over them.
 */
object MistakeDetector {

    const val EARLY_EXIT = "Early Exit"
    const val REVENGE_TRADE = "Revenge Trade"
    const val FOMO_ENTRY = "FOMO Entry"
    const val OVERTRADING = "Overtrading"
    const val EMOTIONAL_TRADE = "Emotional Trade"
    const val WEAK_CONFIRMATION = "Weak Confirmation"
    const val POOR_SL_PLACEMENT = "Poor SL Placement"
    const val NO_TARGET = "No Planned Target"

    val AllTags: List<String> = listOf(
        EARLY_EXIT, REVENGE_TRADE, FOMO_ENTRY, OVERTRADING,
        EMOTIONAL_TRADE, WEAK_CONFIRMATION, POOR_SL_PLACEMENT, NO_TARGET
    )

    private val emotionalTriggers = setOf("fear", "fearful", "greed", "greedy", "revenge", "excited", "anxious")

    private const val REVENGE_MIN_MINUTES = 5
    private const val REVENGE_MAX_MINUTES = 15
    private const val OVERTRADING_DAILY_LIMIT = 5
    private const val WEAK_CONFIRMATION_MIN = 2
    private const val MIN_RR_FOR_FOMO = 1.0
    private const val POOR_SL_LOSS_THRESHOLD = 0.05

    /**
     * Detect tags for [trade] given the history of [priorTrades] (any order).
     * Pass the candidate trade itself in [trade]; do not include it in [priorTrades].
     */
    fun detect(trade: TradeEntity, priorTrades: List<TradeEntity>): List<String> {
        val tags = mutableListOf<String>()

        if (isEarlyExit(trade)) tags += EARLY_EXIT
        if (isFomoEntry(trade)) tags += FOMO_ENTRY
        if (isPoorSl(trade)) tags += POOR_SL_PLACEMENT
        if (trade.plannedTarget <= 0.0) tags += NO_TARGET
        if (isWeakConfirmation(trade)) tags += WEAK_CONFIRMATION
        if (isEmotionalTrade(trade)) tags += EMOTIONAL_TRADE

        val sortedPrior = priorTrades.sortedBy { it.effectiveEntryTime() }
        if (isRevengeTrade(trade, sortedPrior)) tags += REVENGE_TRADE
        if (isOvertrading(trade, sortedPrior)) tags += OVERTRADING

        return tags
    }

    private fun TradeEntity.effectiveEntryTime(): Long =
        if (entryTimeMillis > 0L) entryTimeMillis else tradeDateMillis

    private fun isEarlyExit(trade: TradeEntity): Boolean {
        if (trade.plannedTarget <= 0.0) return false
        val direction = if (trade.tradeAction.equals("SELL", ignoreCase = true)) -1 else 1
        val exit = trade.effectiveExitPrice()
        val mfe = trade.mfePrice
        if (mfe <= 0.0) return false
        val targetReached = if (direction > 0) mfe >= trade.plannedTarget else mfe <= trade.plannedTarget
        val exitBeforeTarget = if (direction > 0) exit < trade.plannedTarget else exit > trade.plannedTarget
        return targetReached && exitBeforeTarget
    }

    private fun isFomoEntry(trade: TradeEntity): Boolean {
        val risk = abs(trade.entryPrice - trade.stopLoss)
        if (risk <= 0.0 || trade.entryPrice <= 0.0) return false
        val riskPct = risk / trade.entryPrice
        if (riskPct < POOR_SL_LOSS_THRESHOLD) return false
        if (trade.plannedTarget <= 0.0) return true
        val reward = abs(trade.plannedTarget - trade.entryPrice)
        val rr = reward / risk
        return rr < MIN_RR_FOR_FOMO
    }

    private fun isPoorSl(trade: TradeEntity): Boolean {
        val risk = abs(trade.entryPrice - trade.stopLoss)
        if (risk <= 0.0 || trade.entryPrice <= 0.0) return true
        return risk / trade.entryPrice >= POOR_SL_LOSS_THRESHOLD
    }

    private fun isWeakConfirmation(trade: TradeEntity): Boolean =
        trade.parsedConfirmations().size < WEAK_CONFIRMATION_MIN

    private fun isEmotionalTrade(trade: TradeEntity): Boolean {
        val before = trade.emotionBefore.trim().lowercase()
        return before in emotionalTriggers
    }

    private fun isRevengeTrade(trade: TradeEntity, sortedPrior: List<TradeEntity>): Boolean {
        val current = trade.effectiveEntryTime()
        if (current <= 0L) return false
        val previous = sortedPrior.lastOrNull { it.effectiveEntryTime() in 1 until current } ?: return false
        if (previous.isWin()) return false
        val gapMinutes = (current - previous.effectiveEntryTime()) / 60_000L
        return gapMinutes in REVENGE_MIN_MINUTES.toLong()..REVENGE_MAX_MINUTES.toLong()
    }

    private fun isOvertrading(trade: TradeEntity, sortedPrior: List<TradeEntity>): Boolean {
        val current = trade.effectiveEntryTime().takeIf { it > 0L } ?: return false
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val sameDayCount = sortedPrior.count {
            val t = it.effectiveEntryTime()
            if (t <= 0L) return@count false
            cal.timeInMillis = t
            cal.get(Calendar.DAY_OF_YEAR) == day && cal.get(Calendar.YEAR) == year
        }
        return sameDayCount + 1 > OVERTRADING_DAILY_LIMIT
    }
}
