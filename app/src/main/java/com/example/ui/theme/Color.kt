package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================================================
// SPEC PALETTE — premium dark trading terminal, neon-yellow accent.
//   Primary Yellow   #FFD600
//   Success Green    #22C55E
//   Danger Red       #FF4D4F
//   Background       #0A0A0A
//   Surface          #121212
//   Surface Variant  #1E1E1E
//   Border           #2A2A2A
// The legacy variable names are kept so existing screens recolor centrally.
// ==========================================================================
val TerminalBackground = Color(0xFF0A0A0A)
val TerminalCardBackground = Color(0xFF121212)
val TerminalSurfaceVariant = Color(0xFF1E1E1E)
val NeonBluePrimary = Color(0xFFFFD600)
val NeonBlueSecondary = Color(0xFFE6C200)
val NeonGreenProfit = Color(0xFF22C55E)
val NeonRedLoss = Color(0xFFFF4D4F)
val TerminalTextPrimary = Color(0xFFF6F6F1)
val TerminalTextSecondary = Color(0xFF8A8F8C)
val AccentColor = Color(0xFFFFD600)
val TerminalAccentGlow = Color(0xFF2A2710)
val BorderColor = Color(0xFF2A2A2A)

// Heatmap intensity ramp (loss → breakeven → profit).
val HeatLossStrong = Color(0xFF7F1D1D)   // dark red — large loss
val HeatLoss = Color(0xFFEF4444)         // red — loss
val HeatBreakeven = Color(0xFFFACC15)    // yellow — breakeven
val HeatProfit = Color(0xFF22C55E)       // green — profit
val HeatProfitStrong = Color(0xFF15803D) // bright/deep green — high profit
val HeatEmpty = Color(0xFF1A1A1A)        // no trades
