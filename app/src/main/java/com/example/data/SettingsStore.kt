package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

/** On-device persisted app preferences (non-sensitive). API keys live in [SecureKeyStore]. */
class SettingsStore(private val context: Context) {

    companion object {
        private val DEFAULT_TRADE_TYPE = stringPreferencesKey("default_trade_type")
        private val DEFAULT_RISK_PCT = floatPreferencesKey("default_risk_pct")
        private val LANGUAGE = stringPreferencesKey("language")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val CONFIRM_DELETE = booleanPreferencesKey("confirm_before_delete")
        private val SHOW_TOOLTIPS = booleanPreferencesKey("show_tooltips")
        private val ENABLE_ANALYTICS = booleanPreferencesKey("enable_analytics")
        private val ENABLE_AI_COACH = booleanPreferencesKey("enable_ai_coach")
        private val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        private val LAST_BACKUP = longPreferencesKey("last_backup_millis")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            defaultTradeType = p[DEFAULT_TRADE_TYPE] ?: defaults.defaultTradeType,
            defaultRiskPct = p[DEFAULT_RISK_PCT] ?: defaults.defaultRiskPct,
            language = p[LANGUAGE] ?: defaults.language,
            themeMode = p[THEME_MODE] ?: defaults.themeMode,
            autoSave = p[AUTO_SAVE] ?: defaults.autoSave,
            confirmBeforeDelete = p[CONFIRM_DELETE] ?: defaults.confirmBeforeDelete,
            showTooltips = p[SHOW_TOOLTIPS] ?: defaults.showTooltips,
            enableAnalytics = p[ENABLE_ANALYTICS] ?: defaults.enableAnalytics,
            enableAiCoach = p[ENABLE_AI_COACH] ?: defaults.enableAiCoach,
            autoBackup = p[AUTO_BACKUP] ?: defaults.autoBackup,
            lastBackupMillis = p[LAST_BACKUP] ?: defaults.lastBackupMillis
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val current = AppSettings(
                defaultTradeType = p[DEFAULT_TRADE_TYPE] ?: AppSettings().defaultTradeType,
                defaultRiskPct = p[DEFAULT_RISK_PCT] ?: AppSettings().defaultRiskPct,
                language = p[LANGUAGE] ?: AppSettings().language,
                themeMode = p[THEME_MODE] ?: AppSettings().themeMode,
                autoSave = p[AUTO_SAVE] ?: AppSettings().autoSave,
                confirmBeforeDelete = p[CONFIRM_DELETE] ?: AppSettings().confirmBeforeDelete,
                showTooltips = p[SHOW_TOOLTIPS] ?: AppSettings().showTooltips,
                enableAnalytics = p[ENABLE_ANALYTICS] ?: AppSettings().enableAnalytics,
                enableAiCoach = p[ENABLE_AI_COACH] ?: AppSettings().enableAiCoach,
                autoBackup = p[AUTO_BACKUP] ?: AppSettings().autoBackup,
                lastBackupMillis = p[LAST_BACKUP] ?: AppSettings().lastBackupMillis
            )
            val next = transform(current)
            p[DEFAULT_TRADE_TYPE] = next.defaultTradeType
            p[DEFAULT_RISK_PCT] = next.defaultRiskPct
            p[LANGUAGE] = next.language
            p[THEME_MODE] = next.themeMode
            p[AUTO_SAVE] = next.autoSave
            p[CONFIRM_DELETE] = next.confirmBeforeDelete
            p[SHOW_TOOLTIPS] = next.showTooltips
            p[ENABLE_ANALYTICS] = next.enableAnalytics
            p[ENABLE_AI_COACH] = next.enableAiCoach
            p[AUTO_BACKUP] = next.autoBackup
            p[LAST_BACKUP] = next.lastBackupMillis
        }
    }
}
