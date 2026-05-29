package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChevronRight
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.AppSettings
import com.example.ui.ConnectionTestState
import com.example.ui.DataOpState
import com.example.ui.TradeViewModel
import com.example.ui.theme.AccentColor
import com.example.ui.theme.BorderColor
import com.example.ui.theme.NeonBluePrimary
import com.example.ui.theme.NeonGreenProfit
import com.example.ui.theme.NeonRedLoss
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBackground
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsSection { HOME, API, PREFERENCES, DATA, VERSION, ABOUT }

@Composable
fun SettingsScreen(
    viewModel: TradeViewModel,
    onOpenFeedback: () -> Unit
) {
    var section by remember { mutableStateOf(SettingsSection.HOME) }
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()

    when (section) {
        SettingsSection.HOME -> SettingsHome(onSelect = { section = it })
        SettingsSection.API -> ApiIntegrationsSection(viewModel) { section = SettingsSection.HOME }
        SettingsSection.PREFERENCES -> PreferencesSection(settings, viewModel) { section = SettingsSection.HOME }
        SettingsSection.DATA -> DataBackupSection(settings, viewModel) { section = SettingsSection.HOME }
        SettingsSection.VERSION -> VersionHistorySection { section = SettingsSection.HOME }
        SettingsSection.ABOUT -> AboutSection(onOpenFeedback) { section = SettingsSection.HOME }
    }
}

// --------------------------------------------------------------------------
// HOME MENU
// --------------------------------------------------------------------------
@Composable
private fun SettingsHome(onSelect: (SettingsSection) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("SETTINGS", color = NeonBluePrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("Manage preferences, API keys, data, and more.", color = TerminalTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        item { MenuRow(Icons.Filled.Api, "API & Integrations", "Manage API keys and services") { onSelect(SettingsSection.API) } }
        item { MenuRow(Icons.Filled.Tune, "Preferences", "App behavior and defaults") { onSelect(SettingsSection.PREFERENCES) } }
        item { MenuRow(Icons.Filled.Backup, "Data & Backup", "Export, import and sync data") { onSelect(SettingsSection.DATA) } }
        item { MenuRow(Icons.Filled.History, "Version History", "What's new and changelog") { onSelect(SettingsSection.VERSION) } }
        item { MenuRow(Icons.Filled.Info, "About", "App info and support") { onSelect(SettingsSection.ABOUT) } }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(TerminalSurfaceVariant),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = NeonBluePrimary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TerminalTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = TerminalTextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ChevronRight, contentDescription = null, tint = TerminalTextSecondary)
        }
    }
}

// --------------------------------------------------------------------------
// API & INTEGRATIONS
// --------------------------------------------------------------------------
@Composable
private fun ApiIntegrationsSection(viewModel: TradeViewModel, onBack: () -> Unit) {
    val savedKey by viewModel.groqApiKey.collectAsStateWithLifecycle()
    val testState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    var input by remember(savedKey) { mutableStateOf(savedKey) }
    var visible by remember { mutableStateOf(false) }
    val hasKey = savedKey.isNotBlank()

    SectionScaffold("API & Integrations", onBack) {
        item {
            SettingsCard {
                Text("GROQ API", color = NeonBluePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("Groq API Key", color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                Text("Your key is stored securely, encrypted on this device.", color = TerminalTextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("gsk_...", color = TerminalTextSecondary) },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle visibility",
                            tint = TerminalTextSecondary,
                            modifier = Modifier.clickable { visible = !visible }
                        )
                    },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.testGroqConnection() },
                        enabled = hasKey && testState !is ConnectionTestState.Testing,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (testState is ConnectionTestState.Testing) {
                            CircularProgressIndicator(color = NeonBluePrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("TEST CONNECTION", color = TerminalTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.saveGroqApiKey(input) },
                        enabled = input.isNotBlank() && input != savedKey,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBluePrimary),
                        modifier = Modifier.weight(1f)
                    ) { Text("SAVE KEY", color = TerminalBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                if (hasKey) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.deleteGroqApiKey(); input = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("DELETE KEY", color = NeonRedLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                when (val s = testState) {
                    is ConnectionTestState.Success -> StatusBanner(s.message, NeonGreenProfit)
                    is ConnectionTestState.Failure -> StatusBanner(s.message, NeonRedLoss)
                    else -> {}
                }
            }
        }
        item {
            SettingsCard {
                Text("OTHER INTEGRATIONS", color = TerminalTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                IntegrationStatus("Firebase", "Connected", NeonGreenProfit)
                Spacer(Modifier.height(8.dp))
                IntegrationStatus("Analytics", "Enabled", NeonGreenProfit)
                Spacer(Modifier.height(8.dp))
                IntegrationStatus("Crashlytics", "Enabled", NeonGreenProfit)
            }
        }
    }
}

@Composable
private fun IntegrationStatus(name: String, status: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = TerminalTextPrimary, fontSize = 13.sp)
        Text(status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// --------------------------------------------------------------------------
// PREFERENCES
// --------------------------------------------------------------------------
@Composable
private fun PreferencesSection(settings: AppSettings, viewModel: TradeViewModel, onBack: () -> Unit) {
    SectionScaffold("Preferences", onBack) {
        item {
            SettingsCard {
                DropdownPref("Default Trade Type", settings.defaultTradeType, AppSettings.TRADE_TYPES) { v ->
                    viewModel.updateSettings { it.copy(defaultTradeType = v) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Default Risk Per Trade", color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                Text("${String.format(Locale.US, "%.2f", settings.defaultRiskPct)}%", color = AccentColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Slider(
                    value = settings.defaultRiskPct,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(defaultRiskPct = (Math.round(v * 4) / 4f)) } },
                    valueRange = 0.25f..5f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonBluePrimary,
                        activeTrackColor = NeonBluePrimary,
                        inactiveTrackColor = TerminalSurfaceVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                DropdownPref("Language", settings.language, AppSettings.LANGUAGES) { v ->
                    viewModel.updateSettings { it.copy(language = v) }
                }
                Spacer(Modifier.height(16.dp))
                DropdownPref("Theme Mode", settings.themeMode, AppSettings.THEME_MODES) { v ->
                    viewModel.updateSettings { it.copy(themeMode = v) }
                }
            }
        }
        item {
            SettingsCard {
                TogglePref("Auto Save Entries", settings.autoSave) { v -> viewModel.updateSettings { it.copy(autoSave = v) } }
                TogglePref("Confirm Before Deleting", settings.confirmBeforeDelete) { v -> viewModel.updateSettings { it.copy(confirmBeforeDelete = v) } }
                TogglePref("Show Tips & Insights", settings.showTooltips) { v -> viewModel.updateSettings { it.copy(showTooltips = v) } }
                TogglePref("Enable Analytics", settings.enableAnalytics) { v -> viewModel.updateSettings { it.copy(enableAnalytics = v) } }
                TogglePref("Enable AI Coach", settings.enableAiCoach) { v -> viewModel.updateSettings { it.copy(enableAiCoach = v) } }
            }
        }
    }
}

// --------------------------------------------------------------------------
// DATA & BACKUP
// --------------------------------------------------------------------------
@Composable
private fun DataBackupSection(settings: AppSettings, viewModel: TradeViewModel, onBack: () -> Unit) {
    val dataOp by viewModel.dataOpState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importTrades(it) } }

    SectionScaffold("Data & Backup", onBack) {
        item {
            SettingsCard {
                ActionRow(Icons.Filled.Download, "Export Data", "Download your data as JSON", NeonBluePrimary) { viewModel.exportTrades() }
                ActionRow(Icons.Filled.Upload, "Import Data", "Restore from a backup file", NeonBluePrimary) { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                ActionRow(Icons.Filled.CloudUpload, "Manual Backup", "Save a snapshot now", NeonBluePrimary) { viewModel.backupNow() }
            }
        }
        item {
            SettingsCard {
                TogglePref("Automatic Backup", settings.autoBackup) { v -> viewModel.updateSettings { it.copy(autoBackup = v) } }
                if (settings.lastBackupMillis > 0L) {
                    Text(
                        "Last backup: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(settings.lastBackupMillis))}",
                        color = NeonGreenProfit, fontSize = 11.sp
                    )
                } else {
                    Text("No backups yet.", color = TerminalTextSecondary, fontSize = 11.sp)
                }
            }
        }
        item {
            SettingsCard {
                ActionRow(Icons.Filled.DeleteSweep, "Clear Cache", "Free temporary storage", TerminalTextSecondary) { viewModel.clearCache() }
                ActionRow(Icons.Filled.Delete, "Clear All Data", "This action cannot be undone", NeonRedLoss) { confirmDelete = true }
            }
        }
        item {
            when (val s = dataOp) {
                is DataOpState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NeonBluePrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp)); Text("Working...", color = TerminalTextSecondary, fontSize = 12.sp)
                }
                is DataOpState.Success -> StatusBanner(s.message, NeonGreenProfit)
                is DataOpState.Failure -> StatusBanner(s.message, NeonRedLoss)
                else -> {}
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = TerminalCardBackground,
            title = { Text("Delete all data?", color = TerminalTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This permanently removes every trade from this device. Consider exporting a backup first.", color = TerminalTextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteAllTrades() }) {
                    Text("DELETE", color = NeonRedLoss, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL", color = TerminalTextSecondary) } }
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TerminalTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = TerminalTextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ChevronRight, contentDescription = null, tint = TerminalTextSecondary)
    }
}

// --------------------------------------------------------------------------
// VERSION HISTORY
// --------------------------------------------------------------------------
private data class ChangelogEntry(val version: String, val date: String, val notes: List<String>)

@Composable
private fun VersionHistorySection(onBack: () -> Unit) {
    val changelog = listOf(
        ChangelogEntry("2.1.0", "May 2026", listOf(
            "Heatmap & calendar analytics with equity curve",
            "Settings hub: secure API keys, preferences, backups",
            "Challenge Mode with XP & medals",
            "Refreshed neon-yellow terminal theme"
        )),
        ChangelogEntry("2.0.0", "2025", listOf(
            "AI Cognitive Coach powered by Groq",
            "Automatic mistake detection & behavior tags",
            "Personal-best medals"
        )),
        ChangelogEntry("1.0.0", "2024", listOf("Initial trading journal release"))
    )
    SectionScaffold("Version History", onBack) {
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Current Version", color = TerminalTextSecondary, fontSize = 11.sp)
                        Text(BuildConfig.VERSION_NAME, color = TerminalTextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Build", color = TerminalTextSecondary, fontSize = 11.sp)
                        Text(BuildConfig.VERSION_CODE.toString(), color = NeonBluePrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }
        items(changelog.size) { i ->
            val e = changelog[i]
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(e.version, color = NeonBluePrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(e.date, color = TerminalTextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                e.notes.forEach { note ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•  ", color = AccentColor)
                        Text(note, color = TerminalTextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// ABOUT
// --------------------------------------------------------------------------
@Composable
private fun AboutSection(onOpenFeedback: () -> Unit, onBack: () -> Unit) {
    SectionScaffold("About", onBack) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(NeonBluePrimary),
                    contentAlignment = Alignment.Center
                ) { Text("TT", color = TerminalBackground, fontWeight = FontWeight.Black, fontSize = 22.sp) }
                Spacer(Modifier.height(10.dp))
                Text("TRADING TERMINAL", color = TerminalTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Version ${BuildConfig.VERSION_NAME}", color = TerminalTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
            }
        }
        item { LinkRow("Privacy Policy") {} }
        item { LinkRow("Terms of Service") {} }
        item { LinkRow("Support / Feedback", onClick = onOpenFeedback) }
        item {
            SettingsCard {
                Text("Developer", color = TerminalTextSecondary, fontSize = 11.sp)
                Text("Upsurge Labs", color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Journal, analyze, improve — a premium trading diary for disciplined traders.", color = TerminalTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TerminalTextPrimary, fontSize = 14.sp)
            Icon(Icons.AutoMirrored.Filled.ChevronRight, contentDescription = null, tint = TerminalTextSecondary)
        }
    }
}

// --------------------------------------------------------------------------
// SHARED
// --------------------------------------------------------------------------
@Composable
private fun SectionScaffold(
    title: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(TerminalSurfaceVariant).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = TerminalTextPrimary) }
            Spacer(Modifier.width(12.dp))
            Text(title, color = NeonBluePrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@Composable
private fun TogglePref(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TerminalTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TerminalBackground,
                checkedTrackColor = NeonBluePrimary,
                uncheckedThumbColor = TerminalTextSecondary,
                uncheckedTrackColor = TerminalSurfaceVariant,
                uncheckedBorderColor = BorderColor
            )
        )
    }
}

@Composable
private fun DropdownPref(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TerminalTextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val selected = opt == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) NeonBluePrimary else TerminalSurfaceVariant)
                        .clickable { onSelect(opt) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt,
                        color = if (selected) TerminalBackground else TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(message, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TerminalTextPrimary,
    unfocusedTextColor = TerminalTextPrimary,
    focusedBorderColor = NeonBluePrimary,
    unfocusedBorderColor = BorderColor,
    cursorColor = NeonBluePrimary
)
