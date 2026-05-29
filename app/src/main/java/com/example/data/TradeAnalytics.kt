package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekdayPerformance(
    val dayName: String,
    val wins: Int,
    val total: Int,
    val winRate: Int,
    val averagePL: Double
) {
    val hasTrades: Boolean = total > 0
}

data class HourBucket(
    val hour: Int,
    val total: Int,
    val wins: Int,
    val netPL: Double
) {
    val winRate: Int = if (total > 0) ((wins.toDouble() / total) * 100).toInt() else 0
    val hasTrades: Boolean = total > 0
    val label: String = String.format(Locale.US, "%02d", hour)
}

data class EmotionPerformance(
    val emotion: String,
    val total: Int,
    val wins: Int,
    val netPL: Double
) {
    val winRate: Int = if (total > 0) ((wins.toDouble() / total) * 100).toInt() else 0
}

data class TraderScores(
    val discipline: Int,
    val emotionalControl: Int,
    val patience: Int,
    val consistency: Int,
    val sampleSize: Int
) {
    val overall: Int = ((discipline + emotionalControl + patience + consistency) / 4)
}

data class WeeklyReport(
    val rangeStart: Long,
    val rangeEnd: Long,
    val tradesInRange: Int,
    val winRate: Int,
    val netPL: Double,
    val mostCommonMistake: String,
    val scores: TraderScores
)

data class DashboardSummary(
    val winRate: Int,
    val totalTrades: Int,
    val totalPL: Double,
    val avgRR: Double,
    val bestSetup: String,
    val worstSetup: String,
    val bestTradingTime: String,
    val worstTradingTime: String,
    val mostCommonMistake: String,
    val emotionalAccuracy: Int
)

enum class HeatLevel { LOSS_STRONG, LOSS, BREAKEVEN, PROFIT, PROFIT_STRONG, EMPTY }

data class DayStats(
    val dayStartMillis: Long,
    val netPL: Double,
    val tradeCount: Int,
    val wins: Int
) {
    val winRate: Int = if (tradeCount > 0) ((wins.toDouble() / tradeCount) * 100).toInt() else 0
}

data class EquityPoint(
    val index: Int,
    val dayStartMillis: Long,
    val cumulativePL: Double
)

data class PnlDistribution(
    val profitable: Int,
    val losing: Int,
    val breakeven: Int
) {
    val total: Int = profitable + losing + breakeven
    val profitablePct: Int = if (total > 0) ((profitable.toDouble() / total) * 100).toInt() else 0
    val losingPct: Int = if (total > 0) ((losing.toDouble() / total) * 100).toInt() else 0
    val breakevenPct: Int = if (total > 0) ((breakeven.toDouble() / total) * 100).toInt() else 0
}

data class PerformanceStats(
    val netPL: Double,
    val totalTrades: Int,
    val winRate: Int,
    val profitFactor: Double,
    val avgWinner: Double,
    val avgLoser: Double,
    val expectancy: Double,
    val bestDay: DayStats?,
    val worstDay: DayStats?,
    val winningDays: Int,
    val losingDays: Int,
    val breakevenDays: Int,
    val distribution: PnlDistribution
)

data class Medal(
    val id: String,
    val title: String,
    val description: String,
    val recordValue: String,
    val achieved: Boolean
)

object TradeAnalytics {
    private val weekdays = listOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday"
    )

    fun weekdayPerformance(trades: List<TradeEntity>): List<WeekdayPerformance> {
        val calendar = Calendar.getInstance()
        return weekdays.map { (day, name) ->
            val dayTrades = trades.filter {
                calendar.timeInMillis = it.tradeDateMillis
                calendar.get(Calendar.DAY_OF_WEEK) == day
            }
            val wins = dayTrades.count { it.isWin() }
            val winRate = if (dayTrades.isNotEmpty()) {
                ((wins.toDouble() / dayTrades.size) * 100).toInt()
            } else {
                0
            }
            WeekdayPerformance(
                dayName = name,
                wins = wins,
                total = dayTrades.size,
                winRate = winRate,
                averagePL = if (dayTrades.isNotEmpty()) dayTrades.map { it.getPL() }.average() else 0.0
            )
        }
    }

    fun bestWeekday(performance: List<WeekdayPerformance>): WeekdayPerformance? =
        performance.filter { it.hasTrades }.maxWithOrNull(compareBy<WeekdayPerformance> { it.winRate }.thenBy { it.averagePL })

    fun worstWeekday(performance: List<WeekdayPerformance>): WeekdayPerformance? =
        performance.filter { it.hasTrades }.minWithOrNull(compareBy<WeekdayPerformance> { it.winRate }.thenBy { it.averagePL })

    fun parseTradeDate(dateText: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(dateText)?.time
    } catch (_: Exception) {
        null
    }

    fun formatTradeDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    /** Counts of auto-detected mistake tags across [trades]. */
    fun mistakeDistribution(trades: List<TradeEntity>): Map<String, Int> {
        val counts = linkedMapOf<String, Int>()
        trades.forEach { t ->
            t.parsedAutoTags().forEach { tag ->
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        return counts
    }

    /** Setup distribution restricted to winning trades — what's actually working. */
    fun winningSetupDistribution(trades: List<TradeEntity>): Map<String, Int> {
        return trades.filter { it.isWin() }
            .groupBy { it.setupLogic.ifBlank { "Unspecified" } }
            .mapValues { it.value.size }
    }

    fun emotionPerformance(trades: List<TradeEntity>): List<EmotionPerformance> {
        return trades.filter { it.emotionBefore.isNotBlank() }
            .groupBy { it.emotionBefore.trim().replaceFirstChar { c -> c.uppercase() } }
            .map { (emotion, ts) ->
                EmotionPerformance(
                    emotion = emotion,
                    total = ts.size,
                    wins = ts.count { it.isWin() },
                    netPL = ts.sumOf { it.getPL() }
                )
            }
            .sortedByDescending { it.total }
    }

    /** Per-hour-of-day performance using entryTimeMillis when present, else tradeDateMillis. */
    fun hourPerformance(trades: List<TradeEntity>): List<HourBucket> {
        val cal = Calendar.getInstance()
        val grouped = trades.groupBy {
            val t = if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis
            cal.timeInMillis = t
            cal.get(Calendar.HOUR_OF_DAY)
        }
        return (9..15).map { hour ->
            val bucket = grouped[hour].orEmpty()
            HourBucket(
                hour = hour,
                total = bucket.size,
                wins = bucket.count { it.isWin() },
                netPL = bucket.sumOf { it.getPL() }
            )
        }
    }

    fun dashboardSummary(trades: List<TradeEntity>): DashboardSummary {
        if (trades.isEmpty()) {
            return DashboardSummary(0, 0, 0.0, 0.0, "—", "—", "—", "—", "—", 0)
        }
        val wins = trades.count { it.isWin() }
        val total = trades.size
        val winRate = ((wins.toDouble() / total) * 100).toInt()
        val totalPL = trades.sumOf { it.getPL() }
        val avgRR = trades.mapNotNull { t ->
            val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
            val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
            if (risk > 0) reward / risk else null
        }.average().let { if (it.isNaN()) 0.0 else it }

        val bySetup = trades.groupBy { it.setupLogic.ifBlank { "Unspecified" } }
            .mapValues { e -> e.value.sumOf { it.getPL() } to e.value.size }
        val bestSetup = bySetup.filter { it.value.second > 0 }
            .maxByOrNull { it.value.first }?.key ?: "—"
        val worstSetup = bySetup.filter { it.value.second > 0 }
            .minByOrNull { it.value.first }?.key ?: "—"

        val hours = hourPerformance(trades).filter { it.hasTrades }
        val bestTime = hours.maxByOrNull { it.netPL }?.let { "${it.label}:00" } ?: "—"
        val worstTime = hours.minByOrNull { it.netPL }?.let { "${it.label}:00" } ?: "—"

        val mistakes = mistakeDistribution(trades)
        val mostCommonMistake = mistakes.maxByOrNull { it.value }?.key ?: "—"

        val calmTrades = trades.filter { it.emotionBefore.equals("Calm", ignoreCase = true) }
        val emotionalAccuracy = if (calmTrades.isNotEmpty()) {
            ((calmTrades.count { it.isWin() }.toDouble() / calmTrades.size) * 100).toInt()
        } else 0

        return DashboardSummary(
            winRate = winRate,
            totalTrades = total,
            totalPL = totalPL,
            avgRR = avgRR,
            bestSetup = bestSetup,
            worstSetup = worstSetup,
            bestTradingTime = bestTime,
            worstTradingTime = worstTime,
            mostCommonMistake = mostCommonMistake,
            emotionalAccuracy = emotionalAccuracy
        )
    }

    private val calmEmotions = setOf("calm", "focused", "neutral", "confident")

    fun traderScores(trades: List<TradeEntity>): TraderScores {
        if (trades.isEmpty()) return TraderScores(0, 0, 0, 0, 0)

        val total = trades.size

        val mistakeTrades = trades.count { it.parsedAutoTags().isNotEmpty() }
        val discipline = ((1.0 - mistakeTrades.toDouble() / total) * 100).toInt().coerceIn(0, 100)

        val calmTrades = trades.count { t ->
            t.emotionBefore.trim().lowercase() in calmEmotions
        }
        val emotionalControl = ((calmTrades.toDouble() / total) * 100).toInt().coerceIn(0, 100)

        val earlyExits = trades.count { MistakeDetector.EARLY_EXIT in it.parsedAutoTags() }
        val overtrading = trades.count { MistakeDetector.OVERTRADING in it.parsedAutoTags() }
        val fomo = trades.count { MistakeDetector.FOMO_ENTRY in it.parsedAutoTags() }
        val impatiencePenalty = (earlyExits + overtrading + fomo).toDouble() / (total * 3)
        val patience = ((1.0 - impatiencePenalty) * 100).toInt().coerceIn(0, 100)

        // Consistency = inverse coefficient of variation of daily P&L.
        val cal = Calendar.getInstance()
        val dailyPL = trades.groupBy {
            cal.timeInMillis = if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.values.map { day -> day.sumOf { it.getPL() } }
        val consistency = if (dailyPL.size < 2) {
            50
        } else {
            val mean = dailyPL.average()
            val variance = dailyPL.sumOf { (it - mean) * (it - mean) } / dailyPL.size
            val std = kotlin.math.sqrt(variance)
            val cv = if (kotlin.math.abs(mean) < 1e-6) std else std / kotlin.math.abs(mean)
            ((1.0 / (1.0 + cv)) * 100).toInt().coerceIn(0, 100)
        }

        return TraderScores(
            discipline = discipline,
            emotionalControl = emotionalControl,
            patience = patience,
            consistency = consistency,
            sampleSize = total
        )
    }

    // ======================================================================
    // HEATMAP ANALYTICS
    // ======================================================================

    /** Milliseconds at local midnight for the day containing [millis]. */
    fun dayStart(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun tradeDay(t: TradeEntity): Long =
        dayStart(if (t.tradeDateMillis > 0L) t.tradeDateMillis else t.entryTimeMillis)

    /** Net P&L (and counts) for every day that has at least one trade, keyed by midnight millis. */
    fun dailyStats(trades: List<TradeEntity>): Map<Long, DayStats> {
        return trades.groupBy { tradeDay(it) }
            .map { (day, ts) ->
                day to DayStats(
                    dayStartMillis = day,
                    netPL = ts.sumOf { it.getPL() },
                    tradeCount = ts.size,
                    wins = ts.count { it.isWin() }
                )
            }
            .toMap()
    }

    /** Stats for a single calendar day. */
    fun statsForDay(trades: List<TradeEntity>, dayStartMillis: Long): DayStats {
        val ts = trades.filter { tradeDay(it) == dayStartMillis }
        return DayStats(
            dayStartMillis = dayStartMillis,
            netPL = ts.sumOf { it.getPL() },
            tradeCount = ts.size,
            wins = ts.count { it.isWin() }
        )
    }

    fun tradesForDay(trades: List<TradeEntity>, dayStartMillis: Long): List<TradeEntity> =
        trades.filter { tradeDay(it) == dayStartMillis }
            .sortedBy { if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis }

    /** Classifies a day's net P&L into a heat bucket, scaled relative to [maxAbs]. */
    fun heatLevel(netPL: Double, maxAbs: Double, hasTrades: Boolean): HeatLevel {
        if (!hasTrades) return HeatLevel.EMPTY
        val scale = if (maxAbs <= 0.0) 1.0 else maxAbs
        val r = netPL / scale
        return when {
            r <= -0.5 -> HeatLevel.LOSS_STRONG
            r < -0.02 -> HeatLevel.LOSS
            r <= 0.02 -> HeatLevel.BREAKEVEN
            r < 0.5 -> HeatLevel.PROFIT
            else -> HeatLevel.PROFIT_STRONG
        }
    }

    /** Largest absolute single-day P&L — used to scale heat intensity. */
    fun maxAbsDailyPL(daily: Map<Long, DayStats>): Double =
        daily.values.maxOfOrNull { kotlin.math.abs(it.netPL) } ?: 0.0

    /** Cumulative realized P&L ordered by trade day — the equity curve. */
    fun equityCurve(trades: List<TradeEntity>): List<EquityPoint> {
        val daily = dailyStats(trades).values.sortedBy { it.dayStartMillis }
        var running = 0.0
        return daily.mapIndexed { index, day ->
            running += day.netPL
            EquityPoint(index = index, dayStartMillis = day.dayStartMillis, cumulativePL = running)
        }
    }

    private fun isBreakeven(pl: Double): Boolean = kotlin.math.abs(pl) < 0.0001

    fun pnlDistribution(trades: List<TradeEntity>): PnlDistribution {
        var profit = 0; var loss = 0; var be = 0
        trades.forEach { t ->
            val pl = t.getPL()
            when {
                isBreakeven(pl) -> be++
                pl > 0 -> profit++
                else -> loss++
            }
        }
        return PnlDistribution(profit, loss, be)
    }

    /** Full performance statistics for the Stats tab. */
    fun performanceStats(trades: List<TradeEntity>): PerformanceStats {
        if (trades.isEmpty()) {
            return PerformanceStats(
                0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, null, null, 0, 0, 0,
                PnlDistribution(0, 0, 0)
            )
        }
        val netPL = trades.sumOf { it.getPL() }
        val wins = trades.filter { it.getPL() > 0 }
        val losses = trades.filter { it.getPL() < 0 }
        val winRate = ((wins.size.toDouble() / trades.size) * 100).toInt()

        val grossProfit = wins.sumOf { it.getPL() }
        val grossLoss = kotlin.math.abs(losses.sumOf { it.getPL() })
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else grossProfit
        val avgWinner = if (wins.isNotEmpty()) wins.sumOf { it.getPL() } / wins.size else 0.0
        val avgLoser = if (losses.isNotEmpty()) losses.sumOf { it.getPL() } / losses.size else 0.0

        // Expectancy = (winRate% * avgWin) + (lossRate% * avgLoss)
        val winProb = wins.size.toDouble() / trades.size
        val lossProb = losses.size.toDouble() / trades.size
        val expectancy = (winProb * avgWinner) + (lossProb * avgLoser)

        val daily = dailyStats(trades)
        val bestDay = daily.values.maxByOrNull { it.netPL }
        val worstDay = daily.values.minByOrNull { it.netPL }
        val winningDays = daily.values.count { it.netPL > 0 }
        val losingDays = daily.values.count { it.netPL < 0 }
        val breakevenDays = daily.values.count { isBreakeven(it.netPL) }

        return PerformanceStats(
            netPL = netPL,
            totalTrades = trades.size,
            winRate = winRate,
            profitFactor = profitFactor,
            avgWinner = avgWinner,
            avgLoser = avgLoser,
            expectancy = expectancy,
            bestDay = bestDay,
            worstDay = worstDay,
            winningDays = winningDays,
            losingDays = losingDays,
            breakevenDays = breakevenDays,
            distribution = pnlDistribution(trades)
        )
    }

    /**
     * Personal-best "records". A medal is earned whenever the trader sets a new
     * best on one of these dimensions. Each medal reports its current record value.
     */
    fun medals(trades: List<TradeEntity>): List<Medal> {
        // 1. Biggest single win (₹)
        val biggestWin = trades.filter { it.isWin() }.maxOfOrNull { it.getPL() } ?: 0.0

        // 2. Longest win streak (chronological by effective time)
        val ordered = trades.sortedBy {
            if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis
        }
        var longestStreak = 0
        var current = 0
        ordered.forEach { t ->
            if (t.isWin()) {
                current += 1
                if (current > longestStreak) longestStreak = current
            } else {
                current = 0
            }
        }

        // 3. Highest RR actually hit
        val highestRR = trades.mapNotNull { t ->
            val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
            val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
            if (risk > 0 && t.isWin()) reward / risk else null
        }.maxOrNull() ?: 0.0

        // 4. Cleanest day — most trades in a single day with zero detected mistakes
        val cal = Calendar.getInstance()
        val byDay = trades.groupBy {
            cal.timeInMillis = if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }
        val cleanestDay = byDay.values
            .filter { day -> day.isNotEmpty() && day.all { it.parsedAutoTags().isEmpty() } }
            .maxOfOrNull { it.size } ?: 0

        return listOf(
            Medal(
                id = "biggest_win",
                title = "Biggest Single Win",
                description = "Largest profit booked on one trade",
                recordValue = if (biggestWin > 0) "₹${String.format(Locale.US, "%,.0f", biggestWin)}" else "—",
                achieved = biggestWin > 0
            ),
            Medal(
                id = "win_streak",
                title = "Longest Win Streak",
                description = "Most consecutive winning trades",
                recordValue = if (longestStreak > 0) "$longestStreak in a row" else "—",
                achieved = longestStreak >= 2
            ),
            Medal(
                id = "highest_rr",
                title = "Highest RR Hit",
                description = "Best risk-to-reward ratio realised on a winner",
                recordValue = if (highestRR > 0) "${String.format(Locale.US, "%.1f", highestRR)}R" else "—",
                achieved = highestRR >= 1.0
            ),
            Medal(
                id = "cleanest_day",
                title = "Cleanest Day",
                description = "Most trades in a day with zero mistakes",
                recordValue = if (cleanestDay > 0) "$cleanestDay trade${if (cleanestDay == 1) "" else "s"}" else "—",
                achieved = cleanestDay > 0
            )
        )
    }

    fun weeklyReport(trades: List<TradeEntity>, now: Long = System.currentTimeMillis()): WeeklyReport {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val rangeStart = now - sevenDaysMs
        val window = trades.filter {
            val t = if (it.entryTimeMillis > 0L) it.entryTimeMillis else it.tradeDateMillis
            t in rangeStart..now
        }
        val wins = window.count { it.isWin() }
        val winRate = if (window.isNotEmpty()) ((wins.toDouble() / window.size) * 100).toInt() else 0
        val netPL = window.sumOf { it.getPL() }
        val mistakes = mistakeDistribution(window)
        val topMistake = mistakes.maxByOrNull { it.value }?.key ?: "—"
        return WeeklyReport(
            rangeStart = rangeStart,
            rangeEnd = now,
            tradesInRange = window.size,
            winRate = winRate,
            netPL = netPL,
            mostCommonMistake = topMistake,
            scores = traderScores(window)
        )
    }
}
