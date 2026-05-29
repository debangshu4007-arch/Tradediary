package com.example.data

/** All non-sensitive user preferences, surfaced as a single immutable snapshot. */
data class AppSettings(
    val defaultTradeType: String = "INTRADAY",
    val defaultRiskPct: Float = 1.0f,
    val language: String = "English",
    val themeMode: String = "Dark",
    val autoSave: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
    val showTooltips: Boolean = true,
    val enableAnalytics: Boolean = true,
    val enableAiCoach: Boolean = true,
    val autoBackup: Boolean = false,
    val lastBackupMillis: Long = 0L
) {
    companion object {
        val TRADE_TYPES = listOf("INTRADAY", "SWING", "POSITIONAL", "SCALP")
        val LANGUAGES = listOf("English", "Hindi", "Hinglish")
        val THEME_MODES = listOf("Dark", "System")
    }
}
