package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayStats
import com.example.data.HeatLevel
import com.example.data.PerformanceStats
import com.example.data.TradeAnalytics
import com.example.data.TradeEntity
import com.example.ui.theme.AccentColor
import com.example.ui.theme.BorderColor
import com.example.ui.theme.HeatBreakeven
import com.example.ui.theme.HeatEmpty
import com.example.ui.theme.HeatLoss
import com.example.ui.theme.HeatLossStrong
import com.example.ui.theme.HeatProfit
import com.example.ui.theme.HeatProfitStrong
import com.example.ui.theme.NeonBluePrimary
import com.example.ui.theme.NeonGreenProfit
import com.example.ui.theme.NeonRedLoss
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBackground
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun heatColor(level: HeatLevel): Color = when (level) {
    HeatLevel.LOSS_STRONG -> HeatLossStrong
    HeatLevel.LOSS -> HeatLoss
    HeatLevel.BREAKEVEN -> HeatBreakeven
    HeatLevel.PROFIT -> HeatProfit
    HeatLevel.PROFIT_STRONG -> HeatProfitStrong
    HeatLevel.EMPTY -> HeatEmpty
}

private fun money(value: Double): String {
    val sign = if (value > 0) "+" else if (value < 0) "-" else ""
    return "${sign}₹${String.format(Locale.US, "%,.0f", kotlin.math.abs(value))}"
}

private fun monthLabel(year: Int, monthZero: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year); set(Calendar.MONTH, monthZero); set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
}

// ==========================================================================
// ANALYTICS SCREEN — Heatmap · Calendar · Stats
// ==========================================================================
@Composable
fun AnalyticsScreen(
    trades: List<TradeEntity>,
    onOpenDay: (Long) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("HEATMAP", "CALENDAR", "STATS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ANALYTICS",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonBluePrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Performance consistency over time",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary
                )
            }
            Icon(Icons.Filled.Insights, contentDescription = null, tint = NeonBluePrimary)
        }

        Spacer(Modifier.height(14.dp))

        // Segmented sub-tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TerminalSurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = subTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) NeonBluePrimary else Color.Transparent)
                        .clickable { subTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) TerminalBackground else TerminalTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (subTab) {
            0 -> HeatmapTab(trades)
            1 -> CalendarTab(trades, onOpenDay)
            2 -> StatsTab(trades)
        }
    }
}

// --------------------------------------------------------------------------
// HEATMAP TAB
// --------------------------------------------------------------------------
@Composable
private fun HeatmapTab(trades: List<TradeEntity>) {
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }

    val daily = remember(trades) { TradeAnalytics.dailyStats(trades) }
    val maxAbs = remember(daily) { TradeAnalytics.maxAbsDailyPL(daily) }

    val monthDays = remember(year, month, daily) { buildMonthGrid(year, month) }
    val monthStats = remember(year, month, daily) {
        daily.values.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dayStartMillis }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AnalyticsCard {
                Text("P&L HEATMAP", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                MonthSwitcher(year, month, onPrev = {
                    if (month == 0) { month = 11; year-- } else month--
                }, onNext = {
                    if (month == 11) { month = 0; year++ } else month++
                })
                Spacer(Modifier.height(12.dp))
                WeekdayHeader()
                Spacer(Modifier.height(6.dp))
                // 6 rows x 7 cols grid
                monthDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            val stats = cell?.let { daily[it] }
                            val level = TradeAnalytics.heatLevel(
                                stats?.netPL ?: 0.0, maxAbs, stats != null
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (cell == null) Color.Transparent else heatColor(level)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != null) {
                                    val c = Calendar.getInstance().apply { timeInMillis = cell }
                                    Text(
                                        text = c.get(Calendar.DAY_OF_MONTH).toString(),
                                        fontSize = 10.sp,
                                        color = if (level == HeatLevel.EMPTY) TerminalTextSecondary else TerminalBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(10.dp))
                HeatLegend()
            }
        }

        item {
            val totalPL = monthStats.sumOf { it.netPL }
            val winningDays = monthStats.count { it.netPL > 0 }
            val losingDays = monthStats.count { it.netPL < 0 }
            val breakeven = monthStats.count { kotlin.math.abs(it.netPL) < 0.0001 }
            val tradedDays = monthStats.size
            val winRate = if (tradedDays > 0) (winningDays * 100 / tradedDays) else 0
            val bestDay = monthStats.maxByOrNull { it.netPL }

            AnalyticsCard {
                Text("${monthLabel(year, month).uppercase()} SUMMARY", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryStat("Total P&L", money(totalPL), if (totalPL >= 0) NeonGreenProfit else NeonRedLoss, Modifier.weight(1f))
                    SummaryStat("Winning Days", winningDays.toString(), NeonGreenProfit, Modifier.weight(1f))
                    SummaryStat("Losing Days", losingDays.toString(), NeonRedLoss, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryStat("Breakeven", breakeven.toString(), TerminalTextPrimary, Modifier.weight(1f))
                    SummaryStat("Win Rate", "$winRate%", AccentColor, Modifier.weight(1f))
                    SummaryStat("Best Day", bestDay?.let { money(it.netPL) } ?: "—", NeonGreenProfit, Modifier.weight(1f))
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// CALENDAR TAB
// --------------------------------------------------------------------------
@Composable
private fun CalendarTab(trades: List<TradeEntity>, onOpenDay: (Long) -> Unit) {
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }

    val daily = remember(trades) { TradeAnalytics.dailyStats(trades) }
    val maxAbs = remember(daily) { TradeAnalytics.maxAbsDailyPL(daily) }
    val monthDays = remember(year, month) { buildMonthGrid(year, month) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AnalyticsCard {
                MonthSwitcher(year, month, onPrev = {
                    if (month == 0) { month = 11; year-- } else month--
                }, onNext = {
                    if (month == 11) { month = 0; year++ } else month++
                })
                Spacer(Modifier.height(12.dp))
                WeekdayHeader()
                Spacer(Modifier.height(8.dp))
                monthDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            val stats = cell?.let { daily[it] }
                            val level = TradeAnalytics.heatLevel(stats?.netPL ?: 0.0, maxAbs, stats != null)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.82f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (cell == null) Color.Transparent
                                        else if (stats == null) TerminalSurfaceVariant
                                        else heatColor(level).copy(alpha = 0.22f)
                                    )
                                    .border(
                                        width = if (stats != null) 1.dp else 0.dp,
                                        color = if (stats != null) heatColor(level) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = cell != null) { cell?.let(onOpenDay) }
                                    .padding(3.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (cell != null) {
                                    val c = Calendar.getInstance().apply { timeInMillis = cell }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = c.get(Calendar.DAY_OF_MONTH).toString(),
                                            fontSize = 11.sp,
                                            color = TerminalTextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (stats != null) {
                                            Text(
                                                text = money(stats.netPL),
                                                fontSize = 7.sp,
                                                color = if (stats.netPL >= 0) NeonGreenProfit else NeonRedLoss,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "${stats.tradeCount}T",
                                                fontSize = 7.sp,
                                                color = TerminalTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        item {
            Text(
                "Tap any day to view its trade breakdown.",
                color = TerminalTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// --------------------------------------------------------------------------
// STATS TAB
// --------------------------------------------------------------------------
@Composable
private fun StatsTab(trades: List<TradeEntity>) {
    val stats = remember(trades) { TradeAnalytics.performanceStats(trades) }
    val equity = remember(trades) { TradeAnalytics.equityCurve(trades) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AnalyticsCard {
                Text("NET P&L", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    money(stats.netPL),
                    color = if (stats.netPL >= 0) NeonGreenProfit else NeonRedLoss,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(16.dp))
                Text("EQUITY CURVE", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                if (equity.size >= 2) {
                    val values: Array<Number> = equity.map { it.cumulativePL.toFloat() as Number }.toTypedArray()
                    val model = entryModelOf(*values)
                    Chart(
                        chart = lineChart(
                            lines = listOf(lineSpec(lineColor = NeonBluePrimary))
                        ),
                        model = model,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    EmptyChartHint("Add trades across multiple days to plot your equity curve.")
                }
            }
        }

        item {
            AnalyticsCard {
                Text("P&L DISTRIBUTION", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        profitable = stats.distribution.profitable,
                        losing = stats.distribution.losing,
                        breakeven = stats.distribution.breakeven,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendRow(NeonGreenProfit, "Profitable", "${stats.distribution.profitable} (${stats.distribution.profitablePct}%)")
                        LegendRow(NeonRedLoss, "Losing", "${stats.distribution.losing} (${stats.distribution.losingPct}%)")
                        LegendRow(HeatBreakeven, "Breakeven", "${stats.distribution.breakeven} (${stats.distribution.breakevenPct}%)")
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Win Rate", "${stats.winRate}%", AccentColor, Modifier.weight(1f))
                MetricCard("Profit Factor", String.format(Locale.US, "%.2f", stats.profitFactor), NeonBluePrimary, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Avg Winner", money(stats.avgWinner), NeonGreenProfit, Modifier.weight(1f))
                MetricCard("Avg Loser", money(stats.avgLoser), NeonRedLoss, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Expectancy", money(stats.expectancy), if (stats.expectancy >= 0) NeonGreenProfit else NeonRedLoss, Modifier.weight(1f))
                MetricCard("Total Trades", stats.totalTrades.toString(), TerminalTextPrimary, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    "Best Day",
                    stats.bestDay?.let { money(it.netPL) } ?: "—",
                    NeonGreenProfit, Modifier.weight(1f)
                )
                MetricCard(
                    "Worst Day",
                    stats.worstDay?.let { money(it.netPL) } ?: "—",
                    NeonRedLoss, Modifier.weight(1f)
                )
            }
        }
    }
}

// ==========================================================================
// DAY DETAIL SCREEN
// ==========================================================================
@Composable
fun DayDetailScreen(
    trades: List<TradeEntity>,
    dayStartMillis: Long,
    onBack: () -> Unit
) {
    val dayStats = remember(trades, dayStartMillis) { TradeAnalytics.statsForDay(trades, dayStartMillis) }
    val dayTrades = remember(trades, dayStartMillis) { TradeAnalytics.tradesForDay(trades, dayStartMillis) }
    val dateLabel = remember(dayStartMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(java.util.Date(dayStartMillis))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalSurfaceVariant)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = TerminalTextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DAY DETAILS", color = NeonBluePrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(dateLabel, color = TerminalTextSecondary, fontSize = 12.sp)
                }
            }
        }

        item {
            AnalyticsCard {
                Text("P&L", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    money(dayStats.netPL),
                    color = if (dayStats.netPL >= 0) NeonGreenProfit else NeonRedLoss,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryStat("Trades", dayStats.tradeCount.toString(), TerminalTextPrimary, Modifier.weight(1f))
                    SummaryStat("Win Rate", "${dayStats.winRate}%", AccentColor, Modifier.weight(1f))
                    SummaryStat("Wins", dayStats.wins.toString(), NeonGreenProfit, Modifier.weight(1f))
                }
            }
        }

        item {
            Text("TRADE LIST", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }

        if (dayTrades.isEmpty()) {
            item {
                AnalyticsCard {
                    Text("No trades recorded on this day.", color = TerminalTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(dayTrades.size) { i ->
                val t = dayTrades[i]
                val pl = t.getPL()
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (pl >= 0) NeonGreenProfit else NeonRedLoss)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.contractLabel(), color = TerminalTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${t.setupLogic.ifBlank { "—" }} · RR ${t.getRiskRewardRatioString()}",
                                color = TerminalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            money(pl),
                            color = if (pl >= 0) NeonGreenProfit else NeonRedLoss,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================================
// SHARED COMPONENTS
// ==========================================================================
@Composable
private fun AnalyticsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun MonthSwitcher(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(Icons.Filled.ChevronLeft, onPrev)
        Text(monthLabel(year, month), color = TerminalTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        IconChip(Icons.Filled.ChevronRight, onNext)
    }
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TerminalTextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WeekdayHeader() {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = TerminalTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeatLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Loss", color = TerminalTextSecondary, fontSize = 9.sp)
        listOf(HeatLossStrong, HeatLoss, HeatBreakeven, HeatProfit, HeatProfitStrong).forEach {
            Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(it))
        }
        Text("Profit", color = TerminalTextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun SummaryStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TerminalTextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun MetricCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = TerminalTextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TerminalTextPrimary, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = TerminalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyChartHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TerminalTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun DonutChart(profitable: Int, losing: Int, breakeven: Int, modifier: Modifier = Modifier) {
    val total = (profitable + losing + breakeven).coerceAtLeast(1)
    val animated by animateFloatAsState(targetValue = 1f, animationSpec = tween(700), label = "donut")
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.18f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val segments = listOf(
            profitable to HeatProfit,
            losing to HeatLoss,
            breakeven to HeatBreakeven
        )
        var startAngle = -90f
        segments.forEach { (count, color) ->
            if (count > 0) {
                val sweep = (count.toFloat() / total) * 360f * animated
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                startAngle += sweep
            }
        }
    }
}

// ==========================================================================
// MONTH GRID HELPER — returns 42 cells (6 weeks), Monday-first.
// null = padding cell, otherwise day-start millis.
// ==========================================================================
private fun buildMonthGrid(year: Int, monthZero: Int): List<Long?> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZero)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Calendar.SUNDAY=1..SATURDAY=7 → Monday-first offset
    val firstDow = cal.get(Calendar.DAY_OF_WEEK)
    val leading = ((firstDow + 5) % 7) // Mon=0 ... Sun=6
    val cells = ArrayList<Long?>(42)
    repeat(leading) { cells.add(null) }
    for (d in 1..daysInMonth) {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, d)
        cells.add(c.timeInMillis)
    }
    while (cells.size < 42) cells.add(null)
    return cells
}
