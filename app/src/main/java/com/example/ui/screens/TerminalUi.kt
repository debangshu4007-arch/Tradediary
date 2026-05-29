package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ChallengeEntity
import com.example.data.DailyAnalysisEntity
import com.example.data.FeedbackEntity
import com.example.data.TradeAnalytics
import com.example.data.Medal
import com.example.data.HourBucket
import com.example.data.AnalyticsHelper
import com.example.data.FeedbackSubmitState
import com.example.data.TradeEntity
import com.example.ui.TradeViewModel
import androidx.compose.animation.core.tween
import com.example.ui.theme.*
import java.io.File
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
    val feedbackItems by viewModel.allFeedback.collectAsStateWithLifecycle()
    val aiAnalysis by viewModel.aiAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(0) }
    var editingTrade by remember { mutableStateOf<TradeEntity?>(null) }
    var pendingChallengeId by remember { mutableStateOf<Long?>(null) }
    var selectedDayMillis by remember { mutableStateOf<Long?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Live Terminal State Clock simulation
    var timeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            timeString = "${sdf.format(Date())} UTC"
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(NeonBluePrimary, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = "Trading terminal",
                                    tint = TerminalBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TRADING TERMINAL",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TerminalTextPrimary,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Journal, analyze, improve",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TerminalTextSecondary
                                )
                            }
                        }

                        // Live Clock & Stats Panel
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(TerminalCardBackground, RoundedCornerShape(18.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreenProfit,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = timeString.removeSuffix(" UTC"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TerminalTextPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground,
                    titleContentColor = TerminalTextPrimary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        },
        bottomBar = {
            CustomBottomBar(
                currentTab = currentTab,
                onTabSelected = { index ->
                    currentTab = index
                    selectedDayMillis = null
                    showFeedback = false
                    if (index == 3) editingTrade = null
                    AnalyticsHelper.logTabOpen(
                        when (index) {
                            0 -> "dashboard"
                            1 -> "analytics"
                            2 -> "diary"
                            3 -> "entry"
                            4 -> "medals"
                            5 -> "ai_coach"
                            6 -> "settings"
                            else -> "unknown"
                        }
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TerminalCardBackground,
                    contentColor = TerminalTextPrimary
                )
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
            val dayOverlay = selectedDayMillis
            if (dayOverlay != null) {
                DayDetailScreen(
                    trades = trades,
                    dayStartMillis = dayOverlay,
                    onBack = { selectedDayMillis = null }
                )
            } else when (currentTab) {
                0 -> DashboardScreen(
                    trades = trades,
                    viewModel = viewModel,
                    onNavigateToEntry = {
                        editingTrade = null
                        pendingChallengeId = null
                        currentTab = 3
                    },
                    onEditTrade = { trade ->
                        editingTrade = trade
                        currentTab = 3
                    }
                )
                1 -> AnalyticsScreen(
                    trades = trades,
                    onOpenDay = { day -> selectedDayMillis = day }
                )
                2 -> DailyDiaryScreen(
                    dailyAnalyses = dailyAnalyses,
                    onSaveDiary = { date, right, wrong, condition, followed ->
                        viewModel.addDailyAnalysis(date, right, wrong, condition, followed)
                    }
                )
                3 -> TradeEntryScreen(
                    editTrade = editingTrade,
                    challenges = viewModel.allChallenges.collectAsStateWithLifecycle().value,
                    preselectedChallengeId = pendingChallengeId,
                    onSaveTrade = { entry, exit, sl, qty, setup, thesis, mistake, eBefore, eAfter, lessons, chartB, chartA, optionType, tradeAction, strike, tradeDate, instrument, segment, partialExits, behaviorTags, challengeId, plannedTarget, entryTimeMillis, confirmations, mfePrice ->
                        if (editingTrade != null) {
                            viewModel.updateTrade(editingTrade!!, entry, exit, sl, qty, setup, thesis, mistake, eBefore, eAfter, lessons, chartB, chartA, optionType, tradeAction, strike, tradeDate, instrument, segment, partialExits, behaviorTags, plannedTarget, entryTimeMillis, confirmations, mfePrice)
                        } else {
                            viewModel.addTrade(entry, exit, sl, qty, setup, thesis, mistake, eBefore, eAfter, lessons, chartB, chartA, optionType, tradeAction, strike, tradeDate, instrument, segment, partialExits, behaviorTags, challengeId = challengeId, plannedTarget = plannedTarget, entryTimeMillis = entryTimeMillis, confirmations = confirmations, mfePrice = mfePrice)
                        }
                        editingTrade = null
                        pendingChallengeId = null
                        currentTab = 0
                    },
                    onDismiss = {
                        editingTrade = null
                        pendingChallengeId = null
                        currentTab = 0
                    }
                )
                4 -> MedalsScreen(trades = trades)
                5 -> AiCoachScreen(
                    aiAnalysis = aiAnalysis,
                    isAnalyzing = isAnalyzing,
                    onTriggerAnalysis = { viewModel.triggerAIAnalysis() },
                    tradeCount = trades.size,
                    savedApiKey = viewModel.groqApiKey.collectAsStateWithLifecycle().value,
                    onSaveApiKey = { key -> viewModel.saveGroqApiKey(key) }
                )
                6 -> if (showFeedback) {
                    FeedbackScreen(
                        feedbackItems = feedbackItems,
                        feedbackSubmitState = viewModel.feedbackSubmitState.collectAsStateWithLifecycle().value,
                        onSaveFeedback = { rating, message -> viewModel.submitFeedback(rating, message) },
                        onBack = { showFeedback = false }
                    )
                } else {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenFeedback = { showFeedback = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("DASHBOARD", Icons.Filled.GridView, Icons.Outlined.GridView),
        NavigationItem("ANALYTICS", Icons.Filled.Insights, Icons.Outlined.Insights),
        NavigationItem("DIARY", Icons.Filled.Book, Icons.Outlined.Book),
        NavigationItem("ENTRY", Icons.Default.Add, Icons.Default.Add),
        NavigationItem("MEDALS", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
        NavigationItem("AI COACH", Icons.Filled.Psychology, Icons.Outlined.Psychology),
        NavigationItem("SETTINGS", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = TerminalCardBackground,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
                    .height(72.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = currentTab == index
                    val isCenter = index == 3

                    val shape = if (isCenter) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)
                    val boxModifier = if (isCenter) {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(2.dp, NeonBluePrimary, shape)
                            .clip(shape)
                            .background(
                                color = if (selected) NeonBluePrimary else Color.Transparent,
                                shape = shape
                            )
                    } else {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(shape)
                            .background(
                                color = if (selected) NeonBluePrimary else Color.Transparent,
                                shape = shape
                            )
                    }

                    Box(
                        modifier = boxModifier
                            .clickable { onTabSelected(index) }
                            .testTag("nav_tab_${item.label.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = when {
                                selected -> TerminalBackground
                                isCenter -> NeonBluePrimary
                                else -> TerminalTextSecondary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

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
    onNavigateToEntry: () -> Unit,
    onEditTrade: (TradeEntity) -> Unit = {}
) {
    var showSeedPrompt by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Financial ledger overview row
        item {
            LedgerStatsOverview(trades = trades)
        }

        // Real-time Equity Curve Canvas Plot
        item {
            EquityCurveCard(trades = trades)
        }

        item {
            WeekdayEdgeCard(trades = trades)
        }

        item {
            BehavioralAnalyticsCard(trades = trades)
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
                            text = "Start indexing your breakout, scalp, or trend trades to unlock active terminal feedback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToEntry,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                            shape = RoundedCornerShape(14.dp),
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
                TradeLogCard(trade = trade, onDelete = { viewModel.deleteTrade(trade) }, onEdit = { onEditTrade(trade) })
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
                    "This will populate your trading ledger with 5 high-fidelity demo trades (including win ratios, breakout logic, specific mistakes, and chart screenshots) so you can immediately inspect the equity curve, mistakes matrices, and AI dashboard analytics.",
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
        val reward = abs(it.effectiveExitPrice() - it.entryPrice)
        if (risk > 0) reward / risk else 1.0
    }
    val avgRR = if (sumRatios.isNotEmpty()) sumRatios.average() else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NeonBluePrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Text(
                    text = "Total ledger P&L",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TerminalBackground.copy(alpha = 0.72f),
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net gain / loss",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${if (netPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", netPL)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TerminalBackground,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                color = TerminalBackground,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (netPL >= 0) "PROFITABLE" else "UNDERWATER",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPill(Icons.Default.Add, "Record", Modifier.weight(1f))
                    ActionPill(Icons.Default.Analytics, "Analyze", Modifier.weight(1f))
                    ActionPill(Icons.Default.AutoGraph, "Review", Modifier.weight(1f))
                }
            }
        }

        // Metric Grid (4 elements)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
fun ActionPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(TerminalBackground, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TerminalTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TerminalBackground.copy(alpha = 0.74f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun WeekdayEdgeCard(trades: List<TradeEntity>) {
    val weekdayStats = TradeAnalytics.weekdayPerformance(trades)
    val bestDay = TradeAnalytics.bestWeekday(weekdayStats)
    val worstDay = TradeAnalytics.worstWeekday(weekdayStats)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Weekday Edge",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailGroup(
                    label = "BEST DAY",
                    content = bestDay?.let { "${it.dayName.uppercase(Locale.US)} • ${it.winRate}%" } ?: "NO DATA",
                    modifier = Modifier.weight(1f)
                )
                DetailGroup(
                    label = "WEAKEST DAY",
                    content = worstDay?.let { "${it.dayName.uppercase(Locale.US)} • ${it.winRate}%" } ?: "NO DATA",
                    modifier = Modifier.weight(1f)
                )
            }

            weekdayStats.forEach { day ->
                val accent = when {
                    !day.hasTrades -> TerminalTextSecondary
                    day.winRate >= 70 -> NeonGreenProfit
                    day.winRate >= 40 -> AccentColor
                    else -> NeonRedLoss
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${day.dayName.uppercase(Locale.US)} (WIN RATIO ${if (day.hasTrades) "${day.winRate}%" else "0%"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (day.hasTrades) "${day.wins}/${day.total} wins • Avg P&L ₹${String.format(Locale.US, "%,.1f", day.averagePL)}" else "No trades logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = if (day.hasTrades) "${day.winRate}%" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun BehavioralAnalyticsCard(trades: List<TradeEntity>) {
    val behaviorPairs = trades.flatMap { trade -> trade.parsedBehaviorTags().map { it to trade.getPL() } }
    val mostFrequentBehavior = behaviorPairs.groupBy { it.first }.maxByOrNull { it.value.size }
    val mostProfitableBehavior = behaviorPairs.groupBy { it.first }.maxByOrNull { it.value.sumOf { item -> item.second } }
    val emotionalTrades = behaviorPairs.size
    val avgRR = if (trades.isNotEmpty()) {
        trades.map {
            val risk = abs(it.entryPrice - it.stopLoss)
            val reward = abs(it.effectiveExitPrice() - it.entryPrice)
            if (risk > 0) reward / risk else 0.0
        }.average()
    } else 0.0
    val earlyCuts = trades.count { it.mistakeTag == "Early exit" || it.parsedBehaviorTags().contains("Fear Exit") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Behavior Insights", style = MaterialTheme.typography.titleMedium, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricMiniCard("AVG RR", "1:${String.format(Locale.US, "%.1f", avgRR)}", NeonBluePrimary, Modifier.weight(1f))
                MetricMiniCard("EMOTIONAL TRADES", "$emotionalTrades", if (emotionalTrades > 0) AccentColor else NeonGreenProfit, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailGroup("TOP BEHAVIOR", mostProfitableBehavior?.key ?: "NO DATA", Modifier.weight(1f))
                DetailGroup("CUT WINNERS EARLY", "$earlyCuts trades", Modifier.weight(1f))
            }
            if (mostFrequentBehavior != null) {
                Text(
                    text = "Most repeated behavior: ${mostFrequentBehavior.key} (${mostFrequentBehavior.value.size}x)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary
                )
            }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = TerminalTextSecondary.copy(alpha = 0.88f),
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
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Curve",
                    style = MaterialTheme.typography.titleMedium,
                    color = TerminalTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cumulative P&L",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary
                )
            }

            if (trades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(TerminalBackground.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
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
                        .background(TerminalBackground, RoundedCornerShape(18.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
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
fun TradeLogCard(trade: TradeEntity, onDelete: () -> Unit, onEdit: () -> Unit = {}) {
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
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trade.contractLabel(),
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

                    val autoTagList = trade.parsedAutoTags()
                    if (autoTagList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "AUTO-DETECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                autoTagList.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(NeonRedLoss.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .border(1.dp, NeonRedLoss.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = tag.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonRedLoss,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
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

                    if (trade.partialExits.isNotBlank()) {
                        DetailGroup(label = "PARTIAL EXIT PLAN", content = trade.partialExits)
                    }

                    if (trade.behaviorTags.isNotBlank()) {
                        DetailGroup(label = "BEHAVIOR TAGS", content = trade.behaviorTags)
                    }

                    // Display Screenshots (Before & After)
                    if (!trade.beforeChartUri.isNullOrEmpty() || !trade.afterChartUri.isNullOrEmpty()) {
                        Text(
                            text = "CHART RECORDS",
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
                                    Text("BEFORE-ENTRY", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CardChartImage(uriStr = trade.beforeChartUri)
                                }
                            }
                            if (!trade.afterChartUri.isNullOrEmpty()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AFTER-EXIT", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 8.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CardChartImage(uriStr = trade.afterChartUri)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit Button
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, NeonBluePrimary),
                            modifier = Modifier.testTag("edit_trade_${trade.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit trade", tint = NeonBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EDIT", color = NeonBluePrimary, style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Delete Button
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRedLoss.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, NeonRedLoss),
                            modifier = Modifier.testTag("delete_trade_${trade.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete trade", tint = NeonRedLoss, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELETE", color = NeonRedLoss, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardChartImage(uriStr: String) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val imageSource = remember(uriStr) {
        if (!uriStr.startsWith("simulated_") && !uriStr.contains("/") && !uriStr.startsWith("content://") && !uriStr.startsWith("file://")) {
            File(File(context.filesDir, "trade_chart_screenshots"), uriStr)
        } else if (uriStr.startsWith("file://")) {
            val fileName = uriStr.substringAfterLast("/")
            val resolvedFile = File(File(context.filesDir, "trade_chart_screenshots"), fileName)
            if (resolvedFile.exists()) resolvedFile else Uri.parse(uriStr)
        } else {
            Uri.parse(uriStr)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .clickable { expanded = true },
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
            AsyncImage(
                model = imageSource,
                contentDescription = "Chart Screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Icon(
            imageVector = Icons.Default.ZoomOutMap,
            contentDescription = "Expand screenshot",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(bottomStart = 6.dp))
                .padding(5.dp)
                .size(16.dp)
        )
    }

    if (expanded) {
        AnalyticsHelper.logScreenshotExpand()
        Dialog(onDismissRequest = { expanded = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 620.dp)
                    .background(TerminalBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, NeonBluePrimary, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (uriStr.startsWith("simulated_")) {
                    CardChartImage(uriStr = uriStr)
                } else {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = "Expanded chart screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = { expanded = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close screenshot", tint = Color.White)
                }
            }
        }
    }
}

private fun Context.copyChartImageToLocalUri(sourceUri: Uri): String? {
    return try {
        val screenshotsDir = File(filesDir, "trade_chart_screenshots").apply { mkdirs() }
        val fileName = "chart_${System.currentTimeMillis()}.jpg"
        val targetFile = File(screenshotsDir, fileName)
        contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        fileName
    } catch (t: Throwable) {
        android.util.Log.e("TradeEntryScreen", "Failed to persist selected chart screenshot", t)
        null
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
    editTrade: TradeEntity? = null,
    challenges: List<ChallengeEntity> = emptyList(),
    preselectedChallengeId: Long? = null,
    onSaveTrade: (Double, Double, Double, Int, String, String, String, String, String, String, String?, String?, String, String, Double, Long, String, String, String, String, Long?, Double, Long, String, Double) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val isEditing = editTrade != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        TradeEntryForm(
            initialTrade = editTrade,
            isEditing = isEditing,
            challenges = challenges,
            preselectedChallengeId = preselectedChallengeId,
            onSaveTrade = onSaveTrade,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun TradeEntryForm(
    initialTrade: TradeEntity? = null,
    isEditing: Boolean = false,
    challenges: List<ChallengeEntity> = emptyList(),
    preselectedChallengeId: Long? = null,
    onSaveTrade: (Double, Double, Double, Int, String, String, String, String, String, String, String?, String?, String, String, Double, Long, String, String, String, String, Long?, Double, Long, String, Double) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current

    // Form Inputs
    var entryStr by remember { mutableStateOf(if (initialTrade != null) initialTrade.entryPrice.toString() else "") }
    var exitStr by remember { mutableStateOf(if (initialTrade != null) initialTrade.exitPrice.toString() else "") }
    var slStr by remember { mutableStateOf(if (initialTrade != null) initialTrade.stopLoss.toString() else "") }
    var qtyStr by remember { mutableStateOf(if (initialTrade != null) initialTrade.quantity.toString() else "") }
    var strikeStr by remember { mutableStateOf(if (initialTrade != null && initialTrade.strikePrice > 0.0) String.format(java.util.Locale.US, "%.0f", initialTrade.strikePrice) else "") }
    var tradeDateStr by remember { mutableStateOf(if (initialTrade != null) TradeAnalytics.formatTradeDate(initialTrade.tradeDateMillis) else TradeAnalytics.formatTradeDate(System.currentTimeMillis())) }
    var optionType by remember { mutableStateOf(initialTrade?.optionType ?: "CE") }
    var tradeAction by remember { mutableStateOf(initialTrade?.tradeAction ?: "BUY") }
    var instrument by remember { mutableStateOf(initialTrade?.instrument ?: "Nifty") }
    var marketSegment by remember { mutableStateOf(initialTrade?.marketSegment ?: "F&O") }
    var partialExits by remember { mutableStateOf(initialTrade?.partialExits ?: "") }
    var behaviorTags by remember { mutableStateOf(if (initialTrade != null) initialTrade.parsedBehaviorTags().toSet() else setOf()) }
    var plannedTargetStr by remember { mutableStateOf(if (initialTrade != null && initialTrade.plannedTarget > 0.0) initialTrade.plannedTarget.toString() else "") }
    var mfePriceStr by remember { mutableStateOf(if (initialTrade != null && initialTrade.mfePrice > 0.0) initialTrade.mfePrice.toString() else "") }
    var entryTimeStr by remember {
        mutableStateOf(
            if (initialTrade != null && initialTrade.entryTimeMillis > 0L) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(initialTrade.entryTimeMillis))
            } else ""
        )
    }
    var confirmationsSet by remember {
        mutableStateOf(
            if (initialTrade != null) initialTrade.parsedConfirmations().toSet() else emptySet()
        )
    }
    val confirmationOptions = listOf("Volume", "Breakout Close", "Liquidity Sweep", "Trend Alignment")
    
    var setupLogic by remember { mutableStateOf(initialTrade?.setupLogic ?: "") }
    var tradeThesis by remember { mutableStateOf(initialTrade?.tradeThesis ?: "") }
    var mistakeTag by remember { mutableStateOf(initialTrade?.mistakeTag ?: "None") }
    var emotionBefore by remember { mutableStateOf(initialTrade?.emotionBefore ?: "Calm") }
    var emotionAfter by remember { mutableStateOf(initialTrade?.emotionAfter ?: "Calm") }
    var lessonsLearned by remember { mutableStateOf(initialTrade?.lessonsLearned ?: "") }

    var beforeChartUri by remember { mutableStateOf(initialTrade?.beforeChartUri) }
    var afterChartUri by remember { mutableStateOf(initialTrade?.afterChartUri) }
    var selectedChallengeId by remember { mutableStateOf(preselectedChallengeId ?: initialTrade?.challengeId) }

    // System Image selection launchers
    val beforeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { beforeChartUri = context.copyChartImageToLocalUri(it) ?: it.toString() }
    }

    val afterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { afterChartUri = context.copyChartImageToLocalUri(it) ?: it.toString() }
    }

    // Parsing Live Calculations
    val entry = entryStr.toDoubleOrNull() ?: 0.0
    val exit = exitStr.toDoubleOrNull() ?: 0.0
    val sl = slStr.toDoubleOrNull() ?: 0.0
    val qty = qtyStr.toIntOrNull() ?: 0
    val strike = strikeStr.toDoubleOrNull() ?: 0.0
    val tradeDateMillis = TradeAnalytics.parseTradeDate(tradeDateStr)

    // Auto calculate:
    val riskVal = abs(entry - sl)
    val rewardVal = abs(exit - entry)
    val riskAmount = riskVal * qty
    val rewardAmount = rewardVal * qty
    val netResultPL = (exit - entry) * qty * if (tradeAction == "SELL") -1 else 1
    
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
                text = "DATA RECORD",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose F&O or Stocks, then record entries, exits, behavior, and screenshots.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TRADING CATEGORY", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptionChip("F&O", marketSegment == "F&O", Modifier.weight(1f).testTag("segment_fno")) { marketSegment = "F&O" }
                    OptionChip("STOCKS", marketSegment == "Stocks", Modifier.weight(1f).testTag("segment_stocks")) {
                        marketSegment = "Stocks"
                        optionType = "EQ"
                    }
                }
            }
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
            OutlinedTextField(
                value = tradeDateStr,
                onValueChange = { tradeDateStr = it },
                label = { Text("Trade Date (YYYY-MM-DD)") },
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
                    .testTag("trade_date_input")
            )
        }

        if (marketSegment == "F&O") {
        item {
            OutlinedTextField(
                value = instrument,
                onValueChange = { instrument = it },
                label = { Text("Instrument (e.g. NIFTY, SENSEX, HDFC)") },
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
                    .testTag("instrument_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OPTION CONTRACT", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptionChip("CE", optionType == "CE", Modifier.weight(1f).testTag("option_type_ce")) { optionType = "CE" }
                    OptionChip("PE", optionType == "PE", Modifier.weight(1f).testTag("option_type_pe")) { optionType = "PE" }
                    OptionChip("BUY", tradeAction == "BUY", Modifier.weight(1f).testTag("trade_action_buy")) { tradeAction = "BUY" }
                    OptionChip("SELL", tradeAction == "SELL", Modifier.weight(1f).testTag("trade_action_sell")) { tradeAction = "SELL" }
                }
            }
        }

        item {
            OutlinedTextField(
                value = strikeStr,
                onValueChange = { strikeStr = it },
                label = { Text("Strike Price (e.g. 23400)") },
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
                    .fillMaxWidth()
                    .testTag("strike_price_input")
            )
        }
        } else {
        item {
            OutlinedTextField(
                value = instrument,
                onValueChange = { instrument = it },
                label = { Text("Stock Symbol / Name (e.g. HDFC, RELIANCE)") },
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
                    .testTag("stock_symbol_input")
            )
        }
        }

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

        // STRATEGY SELECTION CHIPS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TRADING STRATEGY", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                val strategies = listOf("Breakout", "Pivot Swipe", "Support & Resistance", "Liquidity Sweep", "Indicator Based", "Custom")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(strategies) { strategy ->
                        val isSelected = setupLogic == strategy
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(20.dp))
                                .border(1.dp, if (isSelected) NeonBluePrimary else BorderColor, RoundedCornerShape(20.dp))
                                .clickable { setupLogic = strategy }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("strategy_chip_$strategy")
                        ) {
                            Text(
                                text = strategy.uppercase(),
                                color = if (isSelected) NeonBluePrimary else TerminalTextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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

        item {
            OutlinedTextField(
                value = partialExits,
                onValueChange = { partialExits = it },
                label = { Text("Partial Exits (e.g. 150@245, 150@260)") },
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
                    .testTag("partial_exits_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("EMOTIONAL / RE-ENTRY BEHAVIOR", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                val behaviors = listOf("Fear Exit", "FOMO Re-entry", "Revenge Trade", "Impulsive Entry", "Planned Re-entry")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(behaviors) { behavior ->
                        val selected = behaviorTags.contains(behavior)
                        Box(
                            modifier = Modifier
                                .background(if (selected) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(20.dp))
                                .border(1.dp, if (selected) NeonBluePrimary else BorderColor, RoundedCornerShape(20.dp))
                                .clickable { behaviorTags = if (selected) behaviorTags - behavior else behaviorTags + behavior }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("behavior_chip_$behavior")
                        ) {
                            Text(behavior, color = if (selected) NeonBluePrimary else TerminalTextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
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

        // STRATEGIC INPUTS (Auto-mistake detector signals)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("STRATEGIC INPUTS (FOR AUTO-MISTAKE DETECTION)", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = plannedTargetStr,
                        onValueChange = { plannedTargetStr = it },
                        label = { Text("Planned Target") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBluePrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = NeonBluePrimary,
                            unfocusedLabelColor = TerminalTextSecondary,
                            focusedTextColor = TerminalTextPrimary,
                            unfocusedTextColor = TerminalTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("planned_target_input")
                    )
                    OutlinedTextField(
                        value = mfePriceStr,
                        onValueChange = { mfePriceStr = it },
                        label = { Text("Max Favorable Price") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBluePrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = NeonBluePrimary,
                            unfocusedLabelColor = TerminalTextSecondary,
                            focusedTextColor = TerminalTextPrimary,
                            unfocusedTextColor = TerminalTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("mfe_price_input")
                    )
                }
                OutlinedTextField(
                    value = entryTimeStr,
                    onValueChange = { entryTimeStr = it },
                    label = { Text("Entry Time (HH:MM, 24h)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBluePrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = NeonBluePrimary,
                        unfocusedLabelColor = TerminalTextSecondary,
                        focusedTextColor = TerminalTextPrimary,
                        unfocusedTextColor = TerminalTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("entry_time_input")
                )
                Text("CONFIRMATIONS USED", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    confirmationOptions.forEach { opt ->
                        val selected = opt in confirmationsSet
                        OptionChip(opt.uppercase(), selected, Modifier.weight(1f)) {
                            confirmationsSet = if (selected) confirmationsSet - opt else confirmationsSet + opt
                        }
                    }
                }
            }
        }

        // CHALLENGE SELECTOR
        if (challenges.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LINK TO CHALLENGE", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(challenges.filter { it.isActive }) { challenge ->
                            val isSelected = selectedChallengeId == challenge.id
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) NeonGreenProfit.copy(alpha = 0.15f) else TerminalCardBackground, RoundedCornerShape(20.dp))
                                    .border(1.dp, if (isSelected) NeonGreenProfit else BorderColor, RoundedCornerShape(20.dp))
                                    .clickable { selectedChallengeId = if (isSelected) null else challenge.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = challenge.name.uppercase(),
                                    color = if (isSelected) NeonGreenProfit else TerminalTextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // SAVE BUTTON
        item {
            val isFno = marketSegment == "F&O"
            val isFormValid = entry > 0.0 && sl > 0.0 && exit > 0.0 && qty > 0 && (!isFno || strike > 0.0) && tradeDateMillis != null && instrument.isNotEmpty()
            val plannedTarget = plannedTargetStr.toDoubleOrNull() ?: 0.0
            val mfePrice = mfePriceStr.toDoubleOrNull() ?: 0.0
            val entryTimeMillis = run {
                val baseDate = tradeDateMillis ?: System.currentTimeMillis()
                val parts = entryTimeStr.trim().split(":")
                if (parts.size != 2) return@run 0L
                val hour = parts[0].toIntOrNull() ?: return@run 0L
                val minute = parts[1].toIntOrNull() ?: return@run 0L
                if (hour !in 0..23 || minute !in 0..59) return@run 0L
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = baseDate
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            Button(
                onClick = {
                    onSaveTrade(
                        entry,
                        exit,
                        sl,
                        qty,
                        if (setupLogic.isEmpty()) "Breakout" else setupLogic,
                        tradeThesis,
                        mistakeTag,
                        emotionBefore,
                        emotionAfter,
                        lessonsLearned,
                        beforeChartUri,
                        afterChartUri,
                        if (isFno) optionType else "EQ",
                        tradeAction,
                        if (isFno) strike else 0.0,
                        tradeDateMillis ?: System.currentTimeMillis(),
                        instrument,
                        marketSegment,
                        partialExits,
                        behaviorTags.joinToString(", "),
                        selectedChallengeId,
                        plannedTarget,
                        entryTimeMillis,
                        confirmationsSet.joinToString(","),
                        mfePrice
                    )
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
                    text = if (isEditing) "UPDATE JOURNAL ENTRY" else "COMMIT JOURNAL TO LEDGER",
                    color = if (isFormValid) TerminalBackground else TerminalTextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Cancel / Close button
        item {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CANCEL", color = TerminalTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(if (selected) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(6.dp))
            .border(1.dp, if (selected) NeonBluePrimary else BorderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) NeonBluePrimary else TerminalTextSecondary,
            fontWeight = FontWeight.Bold
        )
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

    val summary = remember(trades) { TradeAnalytics.dashboardSummary(trades) }
    val autoMistakes = remember(trades) { TradeAnalytics.mistakeDistribution(trades) }
    val autoMistakeTotal = autoMistakes.values.sum()
    val winningSetups = remember(trades) { TradeAnalytics.winningSetupDistribution(trades) }
    val emotionStats = remember(trades) { TradeAnalytics.emotionPerformance(trades) }
    val hourBuckets = remember(trades) { TradeAnalytics.hourPerformance(trades) }
    val scores = remember(trades) { TradeAnalytics.traderScores(trades) }
    val weeklyReport = remember(trades) { TradeAnalytics.weeklyReport(trades) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

        item { TraderScoreCard(scores) }

        item { WeeklyReportCard(weeklyReport) }

        item { PerformanceSummaryCard(summary) }

        item { HourHeatmapCard(hourBuckets) }

        item {
            MistakeHeatmapCard(autoMistakes, autoMistakeTotal)
        }

        item {
            PieChartCard(
                title = "AUTO-MISTAKE DISTRIBUTION",
                slices = autoMistakes.entries.map { it.key to it.value }
            )
        }

        item {
            PieChartCard(
                title = "WINNING SETUP DISTRIBUTION",
                slices = winningSetups.entries.map { it.key to it.value }
            )
        }

        item {
            PieChartCard(
                title = "EMOTION DISTRIBUTION",
                slices = emotionStats.map { it.emotion to it.total },
                valueLabel = { "$it trades" }
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EMOTIONAL PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                    if (emotionStats.isEmpty()) {
                        Text("No emotion data yet.", style = MaterialTheme.typography.bodySmall, color = TerminalTextSecondary)
                    } else {
                        emotionStats.forEach { e ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(e.emotion.uppercase(), style = MaterialTheme.typography.labelSmall, color = TerminalTextPrimary, fontSize = 11.sp)
                                Text(
                                    "${e.total} · ${e.winRate}% win · ${if (e.netPL >= 0) "+" else ""}₹${String.format(Locale.US, "%,.0f", e.netPL)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (e.netPL >= 0) NeonGreenProfit else NeonRedLoss,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

@Composable
fun FeedbackScreen(
    feedbackItems: List<FeedbackEntity>,
    feedbackSubmitState: FeedbackSubmitState,
    onSaveFeedback: (Int, String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var rating by remember { mutableStateOf(5) }
    var message by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackSubmitState) {
        if (feedbackSubmitState is FeedbackSubmitState.Success) {
            message = ""
            rating = 5
            hasSubmitted = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TerminalCardBackground)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = TerminalTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "CUSTOMER FEEDBACK",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Collect trader feedback for improving the next build.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalTextSecondary
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = TerminalCardBackground), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("RATING", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { value ->
                            IconButton(onClick = { rating = value }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating $value",
                                    tint = if (value <= rating) AccentColor else TerminalTextSecondary
                                )
                            }
                        }
                    }
                    Text(
                        text = "Selected: $rating/5",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Feedback / feature request") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBluePrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = NeonBluePrimary,
                            unfocusedLabelColor = TerminalTextSecondary,
                            focusedTextColor = TerminalTextPrimary,
                            unfocusedTextColor = TerminalTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("feedback_input")
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val isLoading = feedbackSubmitState is FeedbackSubmitState.Loading
                        Button(
                            onClick = {
                                if (!hasSubmitted) {
                                    hasSubmitted = true
                                    onSaveFeedback(rating, message)
                                }
                            },
                            enabled = message.isNotBlank() && !isLoading && !hasSubmitted,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_feedback_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = TerminalBackground,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SUBMITTING...", color = TerminalBackground, fontWeight = FontWeight.Bold)
                            } else if (feedbackSubmitState is FeedbackSubmitState.Success) {
                                Text("THANK YOU", color = TerminalBackground, fontWeight = FontWeight.Bold)
                            } else {
                                Text("SUBMIT FEEDBACK", color = TerminalBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        items(feedbackItems) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = TerminalCardBackground), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.rating}/5", color = AccentColor, fontWeight = FontWeight.Bold)
                    Text(item.message, color = TerminalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


// ==========================================
// MEDALS MODE — PERSONAL RECORDS
// ==========================================
@Composable
fun MedalsScreen(trades: List<TradeEntity>) {
    val medals = remember(trades) { TradeAnalytics.medals(trades) }
    val earned = medals.count { it.achieved }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "MEDALS & RECORDS",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonBluePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Beat your personal best to earn a medal. $earned of ${medals.size} unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TerminalTextSecondary
                )
            }
        }

        items(medals) { medal ->
            MedalCard(medal)
        }
    }
}

@Composable
private fun MedalCard(medal: Medal) {
    val accent = if (medal.achieved) NeonGreenProfit else TerminalTextSecondary
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, if (medal.achieved) NeonGreenProfit.copy(alpha = 0.5f) else BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (medal.achieved) NeonGreenProfit.copy(alpha = 0.15f) else TerminalBackground,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (medal.achieved) NeonGreenProfit.copy(alpha = 0.6f) else BorderColor,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (medal.achieved) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = if (medal.achieved) NeonGreenProfit else TerminalTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medal.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TerminalTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = medal.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = medal.recordValue,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// 5. AI COGNITIVE TRADING COACH SYSTEM (GROQ)
// ==========================================
@Composable
fun AiCoachScreen(
    aiAnalysis: String,
    isAnalyzing: Boolean,
    onTriggerAnalysis: () -> Unit,
    tradeCount: Int,
    savedApiKey: String = "",
    onSaveApiKey: (String) -> Unit = {}
) {
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var keyExpanded by remember { mutableStateOf(savedApiKey.isBlank()) }
    var keyVisible by remember { mutableStateOf(false) }
    val hasKey = savedApiKey.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                text = "Powered by Groq (llama-3.1-8b-instant). Pulls your trade ledger, behavior tags, and compliance history.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        // ---- Groq API key configuration ----
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { keyExpanded = !keyExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasKey) Icons.Default.Key else Icons.Default.KeyOff,
                            contentDescription = null,
                            tint = if (hasKey) NeonGreenProfit else TerminalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasKey) "GROQ API KEY • CONNECTED" else "GROQ API KEY • NOT SET",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasKey) NeonGreenProfit else TerminalTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (keyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TerminalTextSecondary
                    )
                }

                if (keyExpanded) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("gsk_...", color = TerminalTextSecondary) },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle key visibility",
                                    tint = TerminalTextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TerminalTextPrimary,
                            unfocusedTextColor = TerminalTextPrimary,
                            focusedBorderColor = NeonBluePrimary,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = NeonBluePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Stored only on this device. Get a free key at console.groq.com/keys.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary
                    )
                    Button(
                        onClick = {
                            onSaveApiKey(apiKeyInput.trim())
                            keyExpanded = false
                        },
                        enabled = apiKeyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE KEY", color = TerminalBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            enabled = hasKey && !isAnalyzing,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonBluePrimary,
                disabledContainerColor = TerminalCardBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("initiate_ai_analysis_button")
        ) {
            Text(
                if (hasKey) "SYNTHESIZE terminal COMPLIANCE DIAGNOSTICS" else "SET GROQ API KEY TO BEGIN",
                color = if (hasKey) TerminalBackground else TerminalTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

// ==========================================
// 6. STRATEGY ANALYTICS SCREEN
// ==========================================
@Composable
fun StrategyScreen(
    trades: List<TradeEntity>,
    onNavigateToEntry: () -> Unit = {}
) {
    val strategies = remember(trades) {
        trades.groupBy { it.setupLogic.ifEmpty { "Unspecified" } }
            .map { (strategy, items) ->
                val wins = items.count { it.isWin() }
                val total = items.size
                val totalPL = items.sumOf { it.getPL() }
                val avgRR = items.mapNotNull { t ->
                    val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
                    val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
                    if (risk > 0) reward / risk else null
                }.average().let { if (it.isNaN()) 0.0 else it }
                StrategyCardData(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "STRATEGY ANALYTICS",
                style = MaterialTheme.typography.titleMedium,
                color = NeonBluePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Win rate & profitability breakdown by strategy.",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalTextSecondary
            )
        }

        if (strategies.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = NeonBluePrimary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("NO STRATEGY DATA YET", style = MaterialTheme.typography.titleMedium, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Tag trades with a strategy to see win rate analysis here.", style = MaterialTheme.typography.bodyMedium, color = TerminalTextSecondary, textAlign = TextAlign.Center)
                        Button(onClick = onNavigateToEntry, colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary)) {
                            Text("RECORD FIRST TRADE", color = TerminalBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(strategies) { data ->
            val winRateColor = when {
                data.winRate >= 70 -> NeonGreenProfit
                data.winRate >= 50 -> AccentColor
                else -> NeonRedLoss
            }
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.strategy.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TerminalTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", data.winRate)}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = winRateColor,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MiniStat("TRADES", "${data.totalTrades}", TerminalTextSecondary, Modifier.weight(1f))
                        MiniStat("WINS", "${data.wins}", NeonGreenProfit, Modifier.weight(1f))
                        MiniStat("LOSSES", "${data.totalTrades - data.wins}", NeonRedLoss, Modifier.weight(1f))
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NET P&L", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                            Text(
                                text = "${if (data.totalPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.1f", data.totalPL)}",
                                color = if (data.totalPL >= 0) NeonGreenProfit else NeonRedLoss,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("AVG R:R", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                            Text(
                                text = "1:${String.format(java.util.Locale.US, "%.1f", data.avgRR)}",
                                color = NeonBluePrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

data class StrategyCardData(
    val strategy: String,
    val totalTrades: Int,
    val wins: Int,
    val winRate: Double,
    val totalPL: Double,
    val avgRR: Double
)

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 7. CHALLENGE SECTION
// ==========================================
@Composable
fun ChallengeScreen(
    challenges: List<ChallengeEntity>,
    trades: List<TradeEntity>,
    onCreateChallenge: (String, Double, Int, String, String, Double) -> Unit,
    onDeleteChallenge: (ChallengeEntity) -> Unit,
    onToggleActive: (ChallengeEntity) -> Unit,
    onNavigateToEntry: (Long?) -> Unit = {}
) {
    var showCreateForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        text = "CHALLENGES",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track performance goals with separate challenge accounts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalTextSecondary
                    )
                }
                Button(
                    onClick = { showCreateForm = !showCreateForm },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showCreateForm) NeonRedLoss else NeonGreenProfit),
                    modifier = Modifier.testTag("toggle_challenge_form")
                ) {
                    Text(if (showCreateForm) "CANCEL" else "NEW CHALLENGE", color = TerminalBackground, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        if (showCreateForm) {
            item {
                ChallengeCreateForm(onCreate = { name, capital, duration, strategy, inst, target ->
                    onCreateChallenge(name, capital, duration, strategy, inst, target)
                    showCreateForm = false
                })
            }
        }

        if (challenges.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentColor.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("NO CHALLENGES YET", style = MaterialTheme.typography.titleMedium, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Create a trading challenge to track performance against specific goals.", style = MaterialTheme.typography.bodyMedium, color = TerminalTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        items(challenges) { challenge ->
            ChallengeDashboardCard(
                challenge = challenge,
                trades = trades.filter { it.challengeId == challenge.id },
                onDelete = { onDeleteChallenge(challenge) },
                onToggleActive = { onToggleActive(challenge) },
                onNavigateToEntry = onNavigateToEntry
            )
        }
    }
}

@Composable
private fun ChallengeCreateForm(
    onCreate: (String, Double, Int, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var capital by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var strategyType by remember { mutableStateOf("Any") }
    var instrument by remember { mutableStateOf("Any") }
    var targetProfit by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(2.dp, NeonBluePrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CREATE NEW CHALLENGE", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Challenge Name") }, colors = fieldColors(), modifier = Modifier.fillMaxWidth().testTag("challenge_name_input"))
            OutlinedTextField(value = capital, onValueChange = { capital = it }, label = { Text("Starting Capital (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth().testTag("challenge_capital_input"))
            OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth().testTag("challenge_duration_input"))
            OutlinedTextField(value = targetProfit, onValueChange = { targetProfit = it }, label = { Text("Target Profit (₹, optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth().testTag("challenge_target_input"))

            Column {
                Text("STRATEGY FOCUS", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Any", "Breakout", "Pivot Swipe", "S&R", "Liquidity Sweep", "Indicator")) { s ->
                        val sel = strategyType == s
                        Box(Modifier.background(if (sel) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(4.dp)).border(1.dp, if (sel) NeonBluePrimary else BorderColor, RoundedCornerShape(4.dp)).clickable { strategyType = s }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(s, color = if (sel) NeonBluePrimary else TerminalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column {
                Text("INSTRUMENT", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Any", "NIFTY", "BANKNIFTY", "SENSEX", "Stocks")) { inst ->
                        val sel = instrument == inst
                        Box(Modifier.background(if (sel) NeonBluePrimary.copy(alpha = 0.2f) else TerminalCardBackground, RoundedCornerShape(4.dp)).border(1.dp, if (sel) NeonBluePrimary else BorderColor, RoundedCornerShape(4.dp)).clickable { instrument = inst }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(inst, color = if (sel) NeonBluePrimary else TerminalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val cap = capital.toDoubleOrNull() ?: 0.0
                    val dur = duration.toIntOrNull() ?: 30
                    val target = targetProfit.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && cap > 0) {
                        onCreate(name, cap, dur, strategyType, instrument, target)
                    }
                },
                enabled = name.isNotBlank() && capital.toDoubleOrNull() ?: 0.0 > 0,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreenProfit),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_challenge_button")
            ) {
                Text("LAUNCH CHALLENGE", color = TerminalBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChallengeDashboardCard(
    challenge: ChallengeEntity,
    trades: List<TradeEntity>,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onNavigateToEntry: (Long?) -> Unit
) {
    val wins = trades.count { it.isWin() }
    val total = trades.size
    val winRate = if (total > 0) wins.toDouble() / total * 100.0 else 0.0
    val totalPL = trades.sumOf { it.getPL() }
    val bestPL = trades.maxByOrNull { it.getPL() }?.getPL() ?: 0.0
    val worstPL = trades.minByOrNull { it.getPL() }?.getPL() ?: 0.0
    val avgRR = trades.mapNotNull { t ->
        val risk = kotlin.math.abs(t.entryPrice - t.stopLoss)
        val reward = kotlin.math.abs(t.effectiveExitPrice() - t.entryPrice)
        if (risk > 0) reward / risk else null
    }.average().let { if (it.isNaN()) 0.0 else it }

    var expanded by remember { mutableStateOf(false) }
    val daysElapsed = ((System.currentTimeMillis() - challenge.createdDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    val progress = (daysElapsed.toDouble() / challenge.duration.coerceAtLeast(1) * 100).coerceIn(0.0, 100.0)
    val winRateColor = when {
        winRate >= 70 -> NeonGreenProfit
        winRate >= 50 -> AccentColor
        else -> NeonRedLoss
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(300)),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, if (challenge.isActive) NeonGreenProfit.copy(alpha = 0.5f) else BorderColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = challenge.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%,.0f", challenge.capital)} capital • ${challenge.duration}d • ${challenge.strategyType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary,
                        fontSize = 9.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(if (challenge.isActive) NeonGreenProfit.copy(alpha = 0.15f) else TerminalTextSecondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, if (challenge.isActive) NeonGreenProfit else TerminalTextSecondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onToggleActive() }
                ) {
                    Text(
                        if (challenge.isActive) "ACTIVE" else "PAUSED",
                        fontSize = 8.sp,
                        color = if (challenge.isActive) NeonGreenProfit else TerminalTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress bar
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DAY $daysElapsed / ${challenge.duration}", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                    Text("${String.format(java.util.Locale.US, "%.0f", progress)}%", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 9.sp)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.toFloat() / 100f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (totalPL >= 0) NeonGreenProfit else NeonRedLoss)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStat("TRADES", "$total", TerminalTextSecondary, Modifier.weight(1f))
                MiniStat("WIN RATE", "${String.format(java.util.Locale.US, "%.0f", winRate)}%", winRateColor, Modifier.weight(1f))
                MiniStat("P&L", "${if (totalPL >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.0f", totalPL)}", if (totalPL >= 0) NeonGreenProfit else NeonRedLoss, Modifier.weight(1f))
                MiniStat("AVG RR", "1:${String.format(java.util.Locale.US, "%.1f", avgRR)}", NeonBluePrimary, Modifier.weight(1f))
            }

            if (expanded) {
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MiniStat("BEST TRADE", "₹${String.format(java.util.Locale.US, "%,.0f", bestPL)}", NeonGreenProfit, Modifier.weight(1f))
                    MiniStat("WORST TRADE", "₹${String.format(java.util.Locale.US, "%,.0f", worstPL)}", NeonRedLoss, Modifier.weight(1f))
                }

                Button(
                    onClick = { onNavigateToEntry(challenge.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADD TRADE TO CHALLENGE", color = TerminalBackground, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRedLoss.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonRedLoss),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("DELETE CHALLENGE", color = NeonRedLoss, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "SHOW LESS" else "SHOW DETAILS", color = NeonBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonBluePrimary,
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = NeonBluePrimary,
    unfocusedLabelColor = TerminalTextSecondary,
    focusedTextColor = TerminalTextPrimary,
    unfocusedTextColor = TerminalTextPrimary
)

// Seeder logic to simulate fully features app instantly!
private fun seedDemoData(viewModel: TradeViewModel) {
    fun dateMillis(date: String): Long = TradeAnalytics.parseTradeDate(date) ?: System.currentTimeMillis()

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
        afterChartUri = "simulated_breakout_win",
        optionType = "CE",
        tradeAction = "BUY",
        strikePrice = 23300.0,
        tradeDateMillis = dateMillis("2026-05-18")
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
        afterChartUri = "simulated_trend_loss",
        optionType = "PE",
        tradeAction = "BUY",
        strikePrice = 23400.0,
        tradeDateMillis = dateMillis("2026-05-19")
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
        afterChartUri = "simulated_reversal_loss",
        optionType = "CE",
        tradeAction = "BUY",
        strikePrice = 23500.0,
        tradeDateMillis = dateMillis("2026-05-20")
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
        afterChartUri = "simulated_chop_win",
        optionType = "PE",
        tradeAction = "BUY",
        strikePrice = 23200.0,
        tradeDateMillis = dateMillis("2026-05-21")
    )

    viewModel.addTrade(
        entryPrice = 220.0,
        exitPrice = 190.0,
        stopLoss = 200.0,
        quantity = 150,
        setupLogic = "Gap Fill",
        tradeThesis = "Perfect short premium fade after gap fill rejection at index morning high with heavy options build up.",
        mistakeTag = "None",
        emotionBefore = "Calm",
        emotionAfter = "Calm",
        lessonsLearned = "Morning trade entry gives premium speed. Setup holds beautifully.",
        beforeChartUri = "simulated_gap_win",
        afterChartUri = "simulated_gap_win",
        optionType = "CE",
        tradeAction = "SELL",
        strikePrice = 23600.0,
        tradeDateMillis = dateMillis("2026-05-22")
    )

    // Demo Diaries
    viewModel.addDailyAnalysis("2026-05-20", "Protected capitals on high news volatility and let winning trade run.", "FOMO chased the morning spike briefly.", "High VIX", true)
    viewModel.addDailyAnalysis("2026-05-19", "Followed precise breakout rules strictly and shut down active trading station after 2 trades.", "None", "Trending", true)
}

// ==========================================
// PHASE 2: ANALYTICS HUB BUILDING BLOCKS
// ==========================================

private val pieColorPalette = listOf(
    NeonBluePrimary,
    NeonGreenProfit,
    NeonRedLoss,
    Color(0xFFFFB020),
    Color(0xFFB066FF),
    Color(0xFF00C2C7),
    Color(0xFFFF6FB5),
    Color(0xFFA0E060)
)

@Composable
fun PieChartCard(
    title: String,
    slices: List<Pair<String, Int>>,
    valueLabel: (Int) -> String = { "$it" }
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
            val total = slices.sumOf { it.second }
            if (total == 0) {
                Text(
                    "No data yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalTextSecondary
                )
                return@Column
            }
            val sorted = slices.sortedByDescending { it.second }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(120.dp)
                ) {
                    var startAngle = -90f
                    sorted.forEachIndexed { index, (_, value) ->
                        val sweep = (value.toFloat() / total.toFloat()) * 360f
                        val color = pieColorPalette[index % pieColorPalette.size]
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                        startAngle += sweep
                    }
                    drawArc(
                        color = TerminalCardBackground,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
                        size = Size(size.width * 0.56f, size.height * 0.56f)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sorted.forEachIndexed { index, (label, value) ->
                        val pct = (value.toDouble() / total * 100).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(pieColorPalette[index % pieColorPalette.size], RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextPrimary,
                                modifier = Modifier.weight(1f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${pct}%  ${valueLabel(value)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourHeatmapCard(buckets: List<HourBucket>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("INTRADAY P&L HEATMAP", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
            val maxAbs = buckets.maxOfOrNull { kotlin.math.abs(it.netPL) } ?: 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                buckets.forEach { b ->
                    val intensity = if (maxAbs > 0) (kotlin.math.abs(b.netPL) / maxAbs).toFloat() else 0f
                    val base = if (b.netPL >= 0) NeonGreenProfit else NeonRedLoss
                    val bg = if (b.hasTrades) base.copy(alpha = 0.15f + intensity * 0.6f) else TerminalBackground
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg, RoundedCornerShape(6.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${b.label}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalTextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (b.hasTrades) "${b.winRate}%" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (b.hasTrades) base else TerminalTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Text(
                "Win-rate by entry hour. Color = net P&L direction & intensity.",
                style = MaterialTheme.typography.labelSmall,
                color = TerminalTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun MistakeHeatmapCard(distribution: Map<String, Int>, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MISTAKE FREQUENCY HEATMAP", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
            if (distribution.isEmpty() || total == 0) {
                Text("No auto-detected mistakes yet.", style = MaterialTheme.typography.bodySmall, color = TerminalTextSecondary)
                return@Column
            }
            val max = distribution.values.max()
            val sorted = distribution.entries.sortedByDescending { it.value }
            sorted.forEach { (tag, count) ->
                val pct = (count.toDouble() / total * 100).toInt()
                val intensity = (count.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tag.uppercase(), style = MaterialTheme.typography.labelSmall, color = TerminalTextPrimary, fontSize = 10.sp)
                        Text("$count · $pct%", style = MaterialTheme.typography.labelSmall, color = TerminalTextSecondary, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(TerminalBackground, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(intensity)
                                .background(NeonRedLoss.copy(alpha = 0.35f + intensity * 0.55f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

private fun scoreColor(value: Int): Color = when {
    value >= 80 -> NeonGreenProfit
    value >= 50 -> Color(0xFFFFB020)
    else -> NeonRedLoss
}

private fun scoreLabel(value: Int): String = when {
    value >= 90 -> "ELITE"
    value >= 80 -> "STRONG"
    value >= 65 -> "STEADY"
    value >= 50 -> "WORK ON IT"
    else -> "RISK ZONE"
}

@Composable
fun TraderScoreCard(scores: com.example.data.TraderScores) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TRADER DISCIPLINE INDEX", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                    Text(
                        if (scores.sampleSize > 0) "Across ${scores.sampleSize} trades" else "No data yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${scores.overall}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = scoreColor(scores.overall),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        scoreLabel(scores.overall),
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor(scores.overall),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            ScoreBar("DISCIPLINE", scores.discipline)
            ScoreBar("EMOTIONAL CONTROL", scores.emotionalControl)
            ScoreBar("PATIENCE", scores.patience)
            ScoreBar("CONSISTENCY", scores.consistency)
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Int) {
    val color = scoreColor(value)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TerminalTextPrimary, fontSize = 10.sp)
            Text("$value%", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(TerminalBackground, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun WeeklyReportCard(report: com.example.data.WeeklyReport) {
    val df = java.text.SimpleDateFormat("MMM d", Locale.US)
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, NeonBluePrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WEEKLY REPORT CARD", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
                    Text(
                        "${df.format(java.util.Date(report.rangeStart))} – ${df.format(java.util.Date(report.rangeEnd))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Text(
                    "${report.tradesInRange} TRADES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (report.tradesInRange == 0) {
                Text("No trades logged in the last 7 days.", style = MaterialTheme.typography.bodySmall, color = TerminalTextSecondary)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("WIN RATE", "${report.winRate}%", scoreColor(report.winRate), Modifier.weight(1f))
                    MiniStat(
                        "NET P&L",
                        "${if (report.netPL >= 0) "+" else ""}₹${String.format(Locale.US, "%,.0f", report.netPL)}",
                        if (report.netPL >= 0) NeonGreenProfit else NeonRedLoss,
                        Modifier.weight(1f)
                    )
                    MiniStat("OVERALL", "${report.scores.overall}", scoreColor(report.scores.overall), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("DISCIPLINE", "${report.scores.discipline}%", scoreColor(report.scores.discipline), Modifier.weight(1f))
                    MiniStat("EMO. CTRL", "${report.scores.emotionalControl}%", scoreColor(report.scores.emotionalControl), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("PATIENCE", "${report.scores.patience}%", scoreColor(report.scores.patience), Modifier.weight(1f))
                    MiniStat("CONSISTENCY", "${report.scores.consistency}%", scoreColor(report.scores.consistency), Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonRedLoss.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "TOP LEAK: ${report.mostCommonMistake.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonRedLoss,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceSummaryCard(summary: com.example.data.DashboardSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PERFORMANCE SNAPSHOT", style = MaterialTheme.typography.labelSmall, color = NeonBluePrimary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("WIN RATE", "${summary.winRate}%", NeonGreenProfit, Modifier.weight(1f))
                MiniStat("AVG RR", String.format(Locale.US, "1:%.2f", summary.avgRR), NeonBluePrimary, Modifier.weight(1f))
                MiniStat("NET P&L", "₹${String.format(Locale.US, "%,.0f", summary.totalPL)}", if (summary.totalPL >= 0) NeonGreenProfit else NeonRedLoss, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("BEST SETUP", summary.bestSetup.uppercase(), NeonGreenProfit, Modifier.weight(1f))
                MiniStat("WORST SETUP", summary.worstSetup.uppercase(), NeonRedLoss, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("BEST HOUR", summary.bestTradingTime, NeonGreenProfit, Modifier.weight(1f))
                MiniStat("WORST HOUR", summary.worstTradingTime, NeonRedLoss, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("TOP MISTAKE", summary.mostCommonMistake.uppercase(), NeonRedLoss, Modifier.weight(1f))
                MiniStat("CALM-WIN %", "${summary.emotionalAccuracy}%", NeonBluePrimary, Modifier.weight(1f))
            }
        }
    }
}
