package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DailyAnalysisEntity
import com.example.data.TradeEntity
import com.example.ui.TradeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTerminalContainer(
    viewModel: TradeViewModel,
    modifier: Modifier = Modifier
) {
    val trades by viewModel.allTrades.collectAsStateWithLifecycle()
    val dailyAnalyses by viewModel.allDailyAnalyses.collectAsStateWithLifecycle()
    val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(0) }
    
    // Live Terminal State Clock simulation
    var timeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss UTC", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            timeString = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.tbCircleSize())
                                        .background(Color.Green, shape = RoundedCornerShape(50))
                                        .testTag("live_connection_indicator")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NIFTY FEED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "TRADING TERMINAL",
                                style = MaterialTheme.typography.titleMedium,
                                color = TerminalTextPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        // Live Clock & Stats Panel
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "VIX: 13.84 (-1.2%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreenProfit,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground,
                    titleContentColor = TerminalTextPrimary
                ),
                modifier = Modifier.border(0.dp, Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = TerminalCardBackground,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    NavigationItem("LEDGER", Icons.Filled.GridView, Icons.Outlined.GridView),
                    NavigationItem("ENTRY", Icons.Filled.AddBox, Icons.Outlined.AddBox),
                    NavigationItem("MISTAKES", Icons.Filled.Warning, Icons.Outlined.Warning),
                    NavigationItem("DIARY", Icons.Filled.Book, Icons.Outlined.Book),
                    NavigationItem("AI COACH", Icons.Filled.Psychology, Icons.Outlined.Psychology)
                )

                items.forEachIndexed { index, item ->
                    val selected = currentTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selected) NeonBluePrimary else TerminalTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) NeonBluePrimary else TerminalTextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonBlueSecondary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${item.label.lowercase()}")
                    )
                }
            }
        },
        containerColor = TerminalBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .background(TerminalBackground)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    trades = trades,
                    viewModel = viewModel,
                    onNavigateToEntry = { currentTab = 1 }
                )
                1 -> TradeEntryScreen(
                    onSaveTrade = { entry, exit, sl, qty, setup, thesis, mistake, eBefore, eAfter, lessons, chartB, chartA ->
                        viewModel.addTrade(entry, exit, sl, qty, setup, thesis, mistake, eBefore, eAfter, lessons, chartB, chartA)
                        currentTab = 0 // Go back to dashboard to view the trade
                    }
                )
                2 -> MistakesScreen(trades = trades)
                3 -> DailyDiaryScreen(
                    dailyAnalyses = dailyAnalyses,
                    onSaveDiary = { date, right, wrong, condition, followed ->
                        viewModel.addDailyAnalysis(date, right, wrong, condition, followed)
                    }
                )
                4 -> AiCoachScreen(
                    aiAnalysis = aiAnalysis,
                    isAnalyzing = isAnalyzing,
                    onTriggerAnalysis = { viewModel.triggerAIAnalysis() },
                    tradeCount = trades.size
                )
            }
        }
    }
}

// Custom Ext helper for visual heights
private fun Int.tbCircleSize(): androidx.compose.ui.unit.Dp = this.dp

data class NavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ==========================================
// 1. DASHBOARD SCREEN & STATS
// ==========================================
@Composable
fun DashboardScreen(
    trades: List<TradeEntity>,
    viewModel: TradeViewModel,
    onNavigateToEntry: () -> Unit
) {
    var showSeedPrompt by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Financial ledger overview row
        item {
            LedgerStatsOverview(trades = trades)
        }

        // Real-time Equity Curve Canvas Plot
        item {
            EquityCurveCard(trades = trades)
        }

        // Title and controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE TRADE LOGS (${trades.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonBluePrimary,
                    fontWeight = FontWeight.Bold
                )
                
                if (trades.isEmpty()) {
                    Button(
                        onClick = { showSeedPrompt = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                        modifier = Modifier.testTag("seed_demo_data_button")
                    ) {
                        Text("LOAD DEMO LEDGER", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (trades.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Empty trades icon",
                            tint = NeonBluePrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "NO ACTIVE TRADES SAVED",
                            style = MaterialTheme.typography.titleMedium,
                            color = TerminalTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start indexing your Nifty breakout, scalp, or trend trades to unlock active terminal feedback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToEntry,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                            modifier = Modifier.testTag("empty_state_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add trade")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RECORD FIRST TRADE", color = TerminalBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(trades) { trade ->
                TradeLogCard(trade = trade, onDelete = { viewModel.deleteTrade(trade) })
            }
        }
    }

    if (showSeedPrompt) {
        AlertDialog(
            onDismissRequest = { showSeedPrompt = false },
            containerColor = TerminalCardBackground,
            title = { Text("Load Simulated Trading logs?", color = NeonBluePrimary) },
            text = {
                Text(
                    "This will populate your Nifty ledger with 5 high-fidelity demo trades (including win ratios, breakout logic, specific mistakes, and chart screenshots) so you can immediately inspect the equity curve, mistakes matrices, and AI dashboard analytics.",
                    color = TerminalTextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        seedDemoData(viewModel)
                        showSeedPrompt = false
                    },
                    modifier = Modifier.testTag("confirm_seed_button")
                ) {
                    Text("LOAD DATA", color = NeonGreenProfit, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeedPrompt = false }) {
                    Text("CANCEL", color = TerminalTextSecondary)
                }
            }
        )
    }
}

@Composable
fun LedgerStatsOverview(trades: List<TradeEntity>) {
    val totalTrades = trades.size
    val wins = trades.count { it.isWin() }
    val winRate = if (totalTrades > 0) (wins.toFloat() / totalTrades * 100).toInt() else 0
    val netPL = trades.sumOf { it.getPL() }
    
    // Psychology score calculator logic
    // Begins at 100, drops by 10 points for each tagged mistake, and gains 5 points for setups followed
    val psyScore = if (totalTrades > 0) {
        val mistakesPenalty = trades.count { it.mistakeTag.isNotEmpty() && it.mistakeTag != "None" } * 12
        val baseScore = 100 - mistakesPenalty
        baseScore.coerceIn(20, 100)
    } else 100

    // Average Risk Reward Ratio
    val sumRatios = trades.map {
        val risk = abs(it.entryPrice - it.stopLoss)
        val reward = abs(it.exitPrice - it.entryPrice)
        if (risk > 0) reward / risk else 1.0
    }
    val avgRR = if (sumRatios.isNotEmpty()) sumRatios.average() else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
            border = BorderStroke(1.dp, NeonBluePrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "NIFTY LEDGER COCKPIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonBluePrimary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NET GAIN / LOSS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalTextSecondary
                        )
                        Text(
                            text = "${if (netPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", netPL)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (netPL >= 0) NeonGreenProfit else NeonRedLoss,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (netPL >= 0) NeonGreenProfit.copy(alpha = 0.1f) else NeonRedLoss.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (netPL >= 0) NeonGreenProfit else NeonRedLoss,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (netPL >= 0) "PROFITABLE" else "UNDERWATER",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (netPL >= 0) NeonGreenProfit else NeonRedLoss,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Metric Grid (4 elements)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricMiniCard(
                title = "WIN RATE",
                value = "$winRate%",
                accentColor = NeonBluePrimary,
                modifier = Modifier.weight(1f)
            )
            MetricMiniCard(
                title = "AVG RISK REWARD",
                value = "1:${String.format(java.util.Locale.US, "%.1f", avgRR)}",
                accentColor = NeonBluePrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricMiniCard(
                title = "PSYCH SCORE",
                value = "$psyScore / 100",
                accentColor = when {
                    psyScore >= 80 -> NeonGreenProfit
                    psyScore >= 50 -> AccentColor
                    else -> NeonRedLoss
                },
                modifier = Modifier.weight(1f)
            )
            MetricMiniCard(
                title = "TOTAL TRADES",
                value = "$totalTrades",
                accentColor = NeonBluePrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = TerminalTextSecondary,
                fontSize = 10.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EquityCurveCard(trades: List<TradeEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EQUITY CURVE FEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonBluePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CUMULATIVE P&L TREND",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary
                )
            }

            if (trades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(TerminalBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Curve displays automatically as trades stack.",
                        color = TerminalTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Calculate accumulated points
                val netList = trades.reversed().map { it.getPL() }
                val cumulativeList = mutableListOf<Float>()
                var sum = 0f
                cumulativeList.add(0f) // Start baseline at 0
                for (pl in netList) {
                    sum += pl.toFloat()
                    cumulativeList.add(sum)
                }

                val minPl = cumulativeList.minOrNull() ?: 0f
                val maxPl = cumulativeList.maxOrNull() ?: 0f
                val plRange = maxPl - minPl
                val rangeSafe = if (plRange == 0f) 100f else plRange

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(TerminalBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val stepX = canvasWidth / (cumulativeList.size - 1).coerceAtLeast(1)

                    // Draw baseline (Zero profit axis)
                    val zeroY = canvasHeight - ((0f - minPl) / rangeSafe) * canvasHeight
                    drawLine(
                        color = BorderColor.copy(alpha = 0.6f),
                        start = Offset(0f, zeroY),
                        end = Offset(canvasWidth, zeroY),
                        strokeWidth = 2f,
                        pathEffect = null
                    )

                    val path = Path()
                    val filledPath = Path()
                    
                    cumulativeList.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        val normalizedY = (value - minPl) / rangeSafe
                        // Invert Y axis for canvas grid coordinates
                        val y = canvasHeight - (normalizedY * canvasHeight)

                        if (idx == 0) {
                            path.moveTo(x, y)
                            filledPath.moveTo(x, canvasHeight)
                            filledPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            filledPath.lineTo(x, y)
                        }
                    }

                    filledPath.lineTo(canvasWidth, canvasHeight)
                    filledPath.close()

                    // Draw filled gradient area
                    drawPath(
                        path = filledPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NeonBluePrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw trace line
                    drawPath(
                        path = path,
                        color = NeonBluePrimary,
                        style = Stroke(width = 4f)
                    )

                    // Draw individual data dots
                    cumulativeList.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        val normalizedY = (value - minPl) / rangeSafe
                        val y = canvasHeight - (normalizedY * canvasHeight)
                        
                        drawCircle(
                            color = if (value >= 0) NeonGreenProfit else NeonRedLoss,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TradeLogCard(trade: TradeEntity, onDelete: () -> Unit) {
    val pl = trade.getPL()
    var expanded by remember { mutableStateOf(false) }
    
    val sdf = SimpleDateFormat("dd MMM, yy • HH:mm", Locale.US)
    val formattedDate = sdf.format(Date(trade.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("trade_card_${trade.id}"),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, if (pl >= 0) NeonGreenProfit.copy(alpha = 0.4f) else NeonRedLoss.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NIFTY OPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(BorderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trade.setupLogic.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = TerminalTextPrimary
                            )
                        }
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary
                    )
                }

                // Profit or Loss readout
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (pl >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", pl)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pl >= 0) NeonGreenProfit else NeonRedLoss,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Qty: ${trade.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Sub-grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                    Text("₹${trade.entryPrice}", style = MaterialTheme.typography.labelLarge, color = TerminalTextPrimary)
                }
                Column {
                    Text("EXIT", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                    Text("₹${trade.exitPrice}", style = MaterialTheme.typography.labelLarge, color = TerminalTextPrimary)
                }
                Column {
                    Text("STOP LOSS", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                    Text("₹${trade.stopLoss}", style = MaterialTheme.typography.labelLarge, color = TerminalTextPrimary)
                }
                Column {
                    Text("RR RATIO", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                    Text(trade.getRiskRewardRatioString(), style = MaterialTheme.typography.labelLarge, color = NeonBluePrimary)
                }
            }

            // Expanded detail panel
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = BorderColor)

                    if (trade.tradeThesis.isNotEmpty()) {
                        DetailGroup(label = "TRADE THESIS / SETUP REASON", content = trade.tradeThesis)
                    }

                    if (trade.mistakeTag.isNotEmpty() && trade.mistakeTag != "None") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Mistake warning", tint = NeonRedLoss, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IDENTIFIED MISTAKE: ${trade.mistakeTag.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonRedLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailGroup(label = "EMOTION BEFORE", content = trade.emotionBefore, modifier = Modifier.weight(1f))
                        DetailGroup(label = "EMOTION AFTER", content = trade.emotionAfter, modifier = Modifier.weight(1f))
                    }

                    if (trade.lessonsLearned.isNotEmpty()) {
                        DetailGroup(label = "LESSONS LEARNED", content = trade.lessonsLearned)
                    }

                    // Display Screenshots (Before & After)
                    if (!trade.beforeChartUri.isNullOrEmpty() || !trade.afterChartUri.isNullOrEmpty()) {
                        Text(
                            text = "TRADINGVIEW SCREENSHOT RECORDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!trade.beforeChartUri.isNullOrEmpty()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("BEFORE-ENTRY CHART", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CardChartImage(uriStr = trade.beforeChartUri)
                                }
                            }
                            if (!trade.afterChartUri.isNullOrEmpty()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AFTER-EXIT CHART", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CardChartImage(uriStr = trade.afterChartUri)
                                }
                            }
                        }
                    }

                    // Delete Button
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRedLoss.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonRedLoss),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("delete_trade_${trade.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete trade", tint = NeonRedLoss, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETE JOURNAL ENTRY", color = NeonRedLoss, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun CardChartImage(uriStr: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (uriStr.startsWith("simulated_")) {
            // Draw stunning simulated chart visuals using custom Canvas rather than loading web links!
            val type = uriStr.substringAfter("simulated_")
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Draw grid lines
                for (i in 1..4) {
                    val y = h * (i / 5f)
                    drawLine(color = BorderColor.copy(alpha = 0.3f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                }

                // Draw candlesticks mock path
                val path = Path()
                val isProfit = type.contains("win") || type.contains("breakout")
                path.moveTo(0f, if (isProfit) h * 0.8f else h * 0.2f)
                path.lineTo(w * 0.3f, if (isProfit) h * 0.6f else h * 0.4f)
                path.lineTo(w * 0.6f, if (isProfit) h * 0.5f else h * 0.5f)
                path.lineTo(w, if (isProfit) h * 0.15f else h * 0.85f)
                
                drawPath(path = path, color = if (isProfit) NeonGreenProfit else NeonRedLoss, style = Stroke(width = 3f))
                
                // Draw pulsing mock candles
                drawRect(
                    color = if (isProfit) NeonGreenProfit else NeonRedLoss,
                    topLeft = Offset(w * 0.4f, h * 0.4f),
                    size = Size(15f, h * 0.3f)
                )
            }
            Text(
                text = "TV MOCKUP: ${uriStr.substringAfter("simulated_").uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.BottomCenter),
                fontSize = 8.sp
            )
        } else {
            // Real selected photo uri
            AsyncImage(
                model = Uri.parse(uriStr),
                contentDescription = "Chart Screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DetailGroup(label: String, content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TerminalTextSecondary,
            fontSize = 9.sp
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = TerminalTextPrimary
        )
    }
}


// ==========================================
// 2. TRADE ENTRY SCREEN (WITH RR CALCULATOR)
// ==========================================
@Composable
fun TradeEntryScreen(
    onSaveTrade: (Double, Double, Double, Int, String, String, String, String, String, String, String?, String?) -> Unit
) {
    // Form Inputs
    var entryStr by remember { mutableStateOf("") }
    var exitStr by remember { mutableStateOf("") }
    var slStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    
    var setupLogic by remember { mutableStateOf("") }
    var tradeThesis by remember { mutableStateOf("") }
    var mistakeTag by remember { mutableStateOf("None") }
    var emotionBefore by remember { mutableStateOf("Calm") }
    var emotionAfter by remember { mutableStateOf("Calm") }
    var lessonsLearned by remember { mutableStateOf("") }

    var beforeChartUri by remember { mutableStateOf<String?>(null) }
    var afterChartUri by remember { mutableStateOf<String?>(null) }

    // System Image selection launchers
    val beforeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { beforeChartUri = it.toString() } }

    val afterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { afterChartUri = it.toString() } }

    // Parsing Live Calculations
    val entry = entryStr.toDoubleOrNull() ?: 0.0
    val exit = exitStr.toDoubleOrNull() ?: 0.0
    val sl = slStr.toDoubleOrNull() ?: 0.0
    val qty = qtyStr.toIntOrNull() ?: 0

    // Auto calculate:
    val riskVal = abs(entry - sl)
    val rewardVal = abs(exit - entry)
    val riskAmount = riskVal * qty
    val rewardAmount = rewardVal * qty
    val netResultPL = (exit - entry) * qty
    
    val calculatedRRString = if (riskVal > 0) {
        val ratio = rewardVal / riskVal
        "1 : ${String.format(java.util.Locale.US, "%.1f", ratio)}"
    } else "1 : 1.0"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "INDEX JOURNAL: NIFTY ONLY",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fill in trade metrics. Output values auto-resolve instantly in terminal view below.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        // AUTO RR CALCULATOR VISUAL TERMINAL OUTPUT
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                border = BorderStroke(2.dp, NeonBluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "REAL-TIME RISK ANALYZER FEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PROFIT / LOSS FEED", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary)
                            Text(
                                text = if (qty > 0) "${if (netResultPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", netResultPL)}" else "₹0.00",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (netResultPL >= 0) NeonGreenProfit else NeonRedLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RISK REWARD (RR) INDEX", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary)
                            Text(
                                text = calculatedRRString,
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("RISK AMOUNT", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary)
                            Text("₹${String.format(java.util.Locale.US, "%,.1f", riskAmount)}", style = MaterialTheme.typography.bodyMedium, color = NeonRedLoss)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("REWARD AMOUNT", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary)
                            Text("₹${String.format(java.util.Locale.US, "%,.1f", rewardAmount)}", style = MaterialTheme.typography.bodyMedium, color = NeonGreenProfit)
                        }
                    }
                }
            }
        }

        // ENTRY METRICS FORM ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = entryStr,
                    onValueChange = { entryStr = it },
                    label = { Text("Entry Price") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBluePrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonBluePrimary,
                        unfocusedLabelColor = TerminalTextSecondary,
                        focusedTextColor = TerminalTextPrimary,
                        unfocusedTextColor = TerminalTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("entry_price_input")
                )

                OutlinedTextField(
                    value = exitStr,
                    onValueChange = { exitStr = it },
                    label = { Text("Exit Price") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBluePrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonBluePrimary,
                        unfocusedLabelColor = TerminalTextSecondary,
                        focusedTextColor = TerminalTextPrimary,
                        unfocusedTextColor = TerminalTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exit_price_input")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = slStr,
                    onValueChange = { slStr = it },
                    label = { Text("Stop Loss") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBluePrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonBluePrimary,
                        unfocusedLabelColor = TerminalTextSecondary,
                        focusedTextColor = TerminalTextPrimary,
                        unfocusedTextColor = TerminalTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stop_loss_input")
                )

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Quantity") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBluePrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonBluePrimary,
                        unfocusedLabelColor = TerminalTextSecondary,
                        focusedTextColor = TerminalTextPrimary,
                        unfocusedTextColor = TerminalTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quantity_input")
                )
            }
        }

        // TEXT FIELDS FOR THESIS & LOGIC
        item {
            OutlinedTextField(
                value = setupLogic,
                onValueChange = { setupLogic = it },
                label = { Text("Setup Logic (e.g. Breakout, Support Bounce, EMA Cross)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonBluePrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = NeonBluePrimary,
                    unfocusedLabelColor = TerminalTextSecondary,
                    focusedTextColor = TerminalTextPrimary,
                    unfocusedTextColor = TerminalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_logic_input")
            )
        }

        item {
            OutlinedTextField(
                value = tradeThesis,
                onValueChange = { tradeThesis = it },
                label = { Text("Trade Thesis / Strategy Notes") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonBluePrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = NeonBluePrimary,
                    unfocusedLabelColor = TerminalTextSecondary,
                    focusedTextColor = TerminalTextPrimary,
                    unfocusedTextColor = TerminalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade_thesis_input")
            )
        }

        // MISTAKE CHIPS SELECTION PANEL
        item {
            Text("IDENTIFY STRATEGIC MISTAKE (IF ANY)", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val mistakesList = listOf("None", "Overtrading", "Early exit", "No SL", "Revenge trade", "FOMO entry", "Wrong RR", "Against trend")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mistakesList) { item ->
                    val isSelected = mistakeTag == item
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) NeonRedLoss.copy(alpha = 0.2f) else TerminalCardBackground,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonRedLoss else BorderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { mistakeTag = item }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("mistake_chip_$item")
                    ) {
                        Text(
                            text = item.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonRedLoss else TerminalTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // EMOTION CONTROLS (BEFORE & AFTER)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PSYCHOLOGICAL EMOTIONAL STATE PROFILE", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val emotions = listOf("Calm", "Excited", "Anxious", "Fearful", "Greedy")
                    
                    // Before Dropdown simulator
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EMOTION BEFORE", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(emotions) { emo ->
                                val selected = emotionBefore == emo
                                Box(
                                    modifier = Modifier
                                        .background(if (selected) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(4.dp))
                                        .border(1.dp, if (selected) NeonBluePrimary else BorderColor, RoundedCornerShape(4.dp))
                                        .clickable { emotionBefore = emo }
                                        .padding(6.dp)
                                ) {
                                    Text(emo, fontSize = 9.sp, color = if (selected) NeonBluePrimary else TerminalTextSecondary)
                                }
                            }
                        }
                    }

                    // After Dropdown simulator
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EMOTION AFTER", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(emotions) { emo ->
                                val selected = emotionAfter == emo
                                Box(
                                    modifier = Modifier
                                        .background(if (selected) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(4.dp))
                                        .border(1.dp, if (selected) NeonBluePrimary else BorderColor, RoundedCornerShape(4.dp))
                                        .clickable { emotionAfter = emo }
                                        .padding(6.dp)
                                ) {
                                    Text(emo, fontSize = 9.sp, color = if (selected) NeonBluePrimary else TerminalTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = lessonsLearned,
                onValueChange = { lessonsLearned = it },
                label = { Text("Lessons Learned / Review Guidelines") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonBluePrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = NeonBluePrimary,
                    unfocusedLabelColor = TerminalTextSecondary,
                    focusedTextColor = TerminalTextPrimary,
                    unfocusedTextColor = TerminalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lessons_input")
            )
        }

        // SCREENSHOTS UPLOAD SELECTOR PANEL
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TRADINGVIEW SCREENSHOT ARCHIVER", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Before Chart Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BEFORE ENTRY CHART", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentBeforeChartUri = beforeChartUri
                        if (currentBeforeChartUri != null) {
                            CardChartImage(uriStr = currentBeforeChartUri)
                            TextButton(onClick = { beforeChartUri = null }) {
                                Text("CLEAR IMAGE", color = NeonRedLoss, fontSize = 10.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { beforeLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCardBackground),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f).testTag("pick_before_image_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Pick image", tint = NeonBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { beforeChartUri = "simulated_breakout_win" },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCardBackground),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1.2f).testTag("sim_before_chart_button")
                                ) {
                                    Text("TV MOCK", color = NeonBluePrimary, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // After Chart Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AFTER EXIT CHART", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentAfterChartUri = afterChartUri
                        if (currentAfterChartUri != null) {
                            CardChartImage(uriStr = currentAfterChartUri)
                            TextButton(onClick = { afterChartUri = null }) {
                                Text("CLEAR IMAGE", color = NeonRedLoss, fontSize = 10.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { afterLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCardBackground),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f).testTag("pick_after_image_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Pick image", tint = NeonBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { afterChartUri = "simulated_reversal_loss" },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCardBackground),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1.2f).testTag("sim_after_chart_button")
                                ) {
                                    Text("TV MOCK", color = NeonBluePrimary, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // SAVE BUTTON
        item {
            val isFormValid = entryStr.isNotEmpty() && slStr.isNotEmpty() && exitStr.isNotEmpty() && qtyStr.isNotEmpty()
            Button(
                onClick = {
                    onSaveTrade(entry, exit, sl, qty, if (setupLogic.isEmpty()) "Breakout" else setupLogic, tradeThesis, mistakeTag, emotionBefore, emotionAfter, lessonsLearned, beforeChartUri, afterChartUri)
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBluePrimary,
                    disabledContainerColor = BorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_trade_button")
            ) {
                Text(
                    text = "COMMIT JOURNAL TO LEDGER",
                    color = if (isFormValid) TerminalBackground else TerminalTextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


// ==========================================
// 3. MISTAKES ANALYTICS SYSTEM
// ==========================================
@Composable
fun MistakesScreen(trades: List<TradeEntity>) {
    val invalidTrades = trades.filter { it.mistakeTag.isNotEmpty() && it.mistakeTag != "None" }

    // Aggregate statistics
    val mistakeCounts = invalidTrades.groupBy { it.mistakeTag }
        .mapValues { it.value.size }

    val mistakeLosses = invalidTrades.groupBy { it.mistakeTag }
        .mapValues { e -> e.value.sumOf { it.getPL() } }

    val mostRepeatedMistake = mistakeCounts.maxByOrNull { it.value }?.key ?: "None"
    val mostExpensiveMistake = mistakeLosses.minByOrNull { it.value }?.key ?: "None"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "MISTAKE MATRIX SYSTEMS",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Deep dive reviews tracking psychological pitfalls.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        // Summary Analytics Matrix
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "PITFALL SYNOPSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MOST REPEATED", fontSize = 9.sp, color = TerminalTextSecondary)
                                Text(
                                    text = mostRepeatedMistake.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mostRepeatedMistake != "None") NeonRedLoss else NeonBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${mistakeCounts[mostRepeatedMistake] ?: 0} occurrences",
                                    fontSize = 11.sp,
                                    color = TerminalTextSecondary
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MAX LOSS SOURCE", fontSize = 9.sp, color = TerminalTextSecondary)
                                Text(
                                    text = mostExpensiveMistake.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mostExpensiveMistake != "None") NeonRedLoss else NeonBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${String.format(java.util.Locale.US, "%,.1f", mistakeLosses[mostExpensiveMistake] ?: 0.0)} net",
                                    fontSize = 11.sp,
                                    color = TerminalTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detail list
        item {
            Text(
                text = "MISTAKE FREQUENCY & DAMAGE RATIO",
                style = MaterialTheme.typography.labelSmall,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (invalidTrades.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Zero mistakes", tint = NeonGreenProfit, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ZERO RECENT MISTAKES RECORDED", style = MaterialTheme.typography.titleMedium, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Spectacular setup discipline! Record a mistake tag on Trade entries to track pitfalls here.", style = MaterialTheme.typography.bodyMedium, color = TerminalTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            val sortedMistakes = mistakeCounts.entries.sortedByDescending { it.value }
            items(sortedMistakes) { entry ->
                val mTag = entry.key
                val count = entry.value
                val totalPL = mistakeLosses[mTag] ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = mTag.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonRedLoss,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Occurred $count times",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("CUMULATIVE PL DAMAGE", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                            Text(
                                text = "${if (totalPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", totalPL)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (totalPL >= 0) NeonGreenProfit else NeonRedLoss,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. DAILY REVIEW DIARY SYSTEM
// ==========================================
@Composable
fun DailyDiaryScreen(
    dailyAnalyses: List<DailyAnalysisEntity>,
    onSaveDiary: (String, String, String, String, Boolean) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }

    // Diary Form variables
    var dateString by remember { mutableStateOf("") }
    var whatWentRight by remember { mutableStateOf("") }
    var whatWentWrong by remember { mutableStateOf("") }
    var marketCondition by remember { mutableStateOf("Trending") }
    var followedSetup by remember { mutableStateOf(true) }

    LaunchedEffect(showForm) {
        if (showForm && dateString.isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateString = sdf.format(Date())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY REVIEW JOURNAL",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "End-of-day compliance check-ins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalTextSecondary
                    )
                }

                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showForm) NeonRedLoss else NeonBluePrimary),
                    modifier = Modifier.testTag("toggle_diary_form_button")
                ) {
                    Text(if (showForm) "CLOSE" else "REVIEW EOD", color = TerminalBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Expandable input form card
        if (showForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(2.dp, NeonBluePrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "RECORD TODAY'S EOD COMPLIANCE LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBluePrimary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = dateString,
                            onValueChange = { dateString = it },
                            label = { Text("Log Date (YYYY-MM-DD)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBluePrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = NeonBluePrimary,
                                unfocusedLabelColor = TerminalTextSecondary,
                                focusedTextColor = TerminalTextPrimary,
                                unfocusedTextColor = TerminalTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diary_date_input")
                        )

                        OutlinedTextField(
                            value = whatWentRight,
                            onValueChange = { whatWentRight = it },
                            label = { Text("What went right today?") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBluePrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = NeonBluePrimary,
                                unfocusedLabelColor = TerminalTextSecondary,
                                focusedTextColor = TerminalTextPrimary,
                                unfocusedTextColor = TerminalTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diary_right_input")
                        )

                        OutlinedTextField(
                            value = whatWentWrong,
                            onValueChange = { whatWentWrong = it },
                            label = { Text("What went wrong / mistakes?") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBluePrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = NeonBluePrimary,
                                unfocusedLabelColor = TerminalTextSecondary,
                                focusedTextColor = TerminalTextPrimary,
                                unfocusedTextColor = TerminalTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diary_wrong_input")
                        )

                        // Market condition selector
                        Column {
                            Text("MARKET STRUCTURAL REGIME", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val regimes = listOf("Trending", "Range bound", "High VIX", "News driven")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(regimes) { regime ->
                                    val isSel = marketCondition == regime
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSel) NeonBluePrimary.copy(alpha = 0.2f) else TerminalBackground, RoundedCornerShape(4.dp))
                                            .border(1.dp, if (isSel) NeonBluePrimary else BorderColor, RoundedCornerShape(4.dp))
                                            .clickable { marketCondition = regime }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(regime, color = if (isSel) NeonBluePrimary else TerminalTextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Followed Setup adherence switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DID YOU STRICTLY ADHERE TO SETUPS?",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = followedSetup,
                                onCheckedChange = { followedSetup = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGreenProfit,
                                    checkedTrackColor = NeonGreenProfit.copy(alpha = 0.3f),
                                    uncheckedThumbColor = NeonRedLoss,
                                    uncheckedTrackColor = NeonRedLoss.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("adherence_switch")
                            )
                        }

                        Button(
                            onClick = {
                                check(dateString.isNotEmpty()) { "Date is required" }
                                onSaveDiary(dateString, whatWentRight, whatWentWrong, marketCondition, followedSetup)
                                // Reset form
                                whatWentRight = ""
                                whatWentWrong = ""
                                showForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreenProfit),
                            enabled = dateString.isNotEmpty() && whatWentRight.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("submit_diary_button")
                        ) {
                            Text("SAVE COMPLIANCE RECORD", color = TerminalBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // past logs title
        item {
            Text(
                text = "LOG DATE REGISTRY (${dailyAnalyses.size})",
                style = MaterialTheme.typography.labelSmall,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (dailyAnalyses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Book, contentDescription = "Empty diary", tint = NeonBluePrimary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DIARY REGISTRY IS EMPTY", style = MaterialTheme.typography.titleMedium, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Record a compliance review at end of the trading day to secure discipline telemetry.", style = MaterialTheme.typography.bodyMedium, color = TerminalTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(dailyAnalyses) { analysis ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOG DATE: ${analysis.dateStr}",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (analysis.followedSetup) NeonGreenProfit.copy(alpha = 0.1f) else NeonRedLoss.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (analysis.followedSetup) NeonGreenProfit else NeonRedLoss,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (analysis.followedSetup) "DISCIPLINE SECURED" else "RULE BROKEN",
                                    color = if (analysis.followedSetup) NeonGreenProfit else NeonRedLoss,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DetailGroup(label = "MARKET REGIME TYPE", content = analysis.marketCondition)
                        
                        if (analysis.whatWentRight.isNotEmpty()) {
                            DetailGroup(label = "WHAT WENT WELL Today", content = analysis.whatWentRight)
                        }

                        if (analysis.whatWentWrong.isNotEmpty()) {
                            DetailGroup(label = "WHAT WENT WRONG / SLIPPAGE", content = analysis.whatWentWrong)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. AI COGNITIVE TRADING COACH SYSTEM (GEMINI)
// ==========================================
@Composable
fun AiCoachScreen(
    aiAnalysis: String,
    isAnalyzing: Boolean,
    onTriggerAnalysis: () -> Unit,
    tradeCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AI COGNITIVE COACH SCANNER",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Predictive analysis feeding from Nifty ledger arrays & compliance history.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        // Live terminal scanner layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TerminalBackground, RoundedCornerShape(8.dp))
                .border(2.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (aiAnalysis.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Coach brain icon",
                        tint = TerminalTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "COGNITIVE MODULE OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Press initiate to synthesize trading ledger compliance logic.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "TERMINAL SCAN RESULTS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(
                            text = aiAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TerminalBackground.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = NeonBluePrimary)
                        Text(
                            text = "AI ANALYZER ONLINE...",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Button(
            onClick = onTriggerAnalysis,
            colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("initiate_ai_analysis_button")
        ) {
            Text(
                "SYNTHESIZE terminal COMPLIANCE DIAGNOSTICS",
                color = TerminalBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

// Seeder logic to simulate fully features app instantly!
private fun seedDemoData(viewModel: TradeViewModel) {
    // Demo trades
    viewModel.addTrade(
        entryPrice = 150.0,
        exitPrice = 180.0,
        stopLoss = 140.0,
        quantity = 250,
        setupLogic = "Breakout",
        tradeThesis = "Nifty took breakout support on 15M candle. Volume was spiked above 20MA.",
        mistakeTag = "None",
        emotionBefore = "Calm",
        emotionAfter = "Excited",
        lessonsLearned = "Sticking to the plan got a healthy 1:3 reward. Let the winners run.",
        beforeChartUri = "simulated_breakout_win",
        afterChartUri = "simulated_breakout_win"
    )

    viewModel.addTrade(
        entryPrice = 160.0,
        exitPrice = 140.0,
        stopLoss = 150.0,
        quantity = 300,
        setupLogic = "EMA Cross",
        tradeThesis = "Took trade on 5M EMA cross but against the main hourly trend.",
        mistakeTag = "Against trend",
        emotionBefore = "Greedy",
        emotionAfter = "Fearful",
        lessonsLearned = "Never buy long EMA crossover when Nifty is under a powerful daily downtrend.",
        beforeChartUri = "simulated_trend_loss",
        afterChartUri = "simulated_trend_loss"
    )

    viewModel.addTrade(
        entryPrice = 200.0,
        exitPrice = 170.0,
        stopLoss = 195.0,
        quantity = 200,
        setupLogic = "Support Bounce",
        tradeThesis = "Panicked and modified Stop Loss lower when Nifty started breaking it.",
        mistakeTag = "No SL",
        emotionBefore = "Fearful",
        emotionAfter = "Anxious",
        lessonsLearned = "Accepting stop loss is the single shield from huge account drawdowns.",
        beforeChartUri = "simulated_reversal_loss",
        afterChartUri = "simulated_reversal_loss"
    )

    viewModel.addTrade(
        entryPrice = 120.0,
        exitPrice = 122.0,
        stopLoss = 110.0,
        quantity = 500,
        setupLogic = "Scalp",
        tradeThesis = "Greed kicked in. Got out too early on small noise before main targets.",
        mistakeTag = "Early exit",
        emotionBefore = "Excited",
        emotionAfter = "Anxious",
        lessonsLearned = "Trust the mathematical Risk-Reward target values rather than micro candle sizes.",
        beforeChartUri = "simulated_chop_win",
        afterChartUri = "simulated_chop_win"
    )

    viewModel.addTrade(
        entryPrice = 220.0,
        exitPrice = 270.0,
        stopLoss = 200.0,
        quantity = 150,
        setupLogic = "Gap Fill",
        tradeThesis = "Perfect gap fill support test at index morning low with heavy options build up.",
        mistakeTag = "None",
        emotionBefore = "Calm",
        emotionAfter = "Calm",
        lessonsLearned = "Morning trade entry gives premium speed. Setup holds beautifully.",
        beforeChartUri = "simulated_gap_win",
        afterChartUri = "simulated_gap_win"
    )

    // Demo Diaries
    viewModel.addDailyAnalysis("2026-05-20", "Protected capitals on high news volatility and let winning trade run.", "FOMO chased the morning spike briefly.", "High VIX", true)
    viewModel.addDailyAnalysis("2026-05-19", "Followed precise breakout rules strictly and shut down active trading station after 2 trades.", "None", "Trending", true)
}
