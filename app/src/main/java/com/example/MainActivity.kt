package com.example

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.ui.TradeViewModel
import com.example.ui.screens.MainTerminalContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBackground
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.NeonRedLoss
import com.example.ui.theme.NeonBlueSecondary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.AccentColor
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    private var crashLogState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Step 1: Initialize global crash interceptor to completely prevent OS-level "keeps stopping" app crash dialogs
        val isUnderTest = try {
            Class.forName("org.robolectric.RobolectricTestRunner") != null
        } catch (e: Exception) {
            false
        }

        if (!isUnderTest) {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val errorMsg = sw.toString()
                Log.e("CRASH_RECOVERY", "Intercepted fatal crash in thread ${thread.name}: $errorMsg", throwable)
                
                // Start MainActivity in a brand new fresh process Task to avoid zombie states
                try {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("FATAL_ERROR_MSG", errorMsg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("CRASH_RECOVERY", "Failed to start clean recovery activity", e)
                }
                
                // Terminate the broken process immediately so the native screen channel closes cleanly
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read recovery extra if we came from a fresh process restart
        val restartError = intent?.getStringExtra("FATAL_ERROR_MSG")
        if (restartError != null) {
            crashLogState = restartError
        }

        // Step 2: Initialize ViewModel safely outside Compose tree to catch DB load issues early
        var initialViewModel: TradeViewModel? = null
        try {
            initialViewModel = ViewModelProvider(this)[TradeViewModel::class.java]
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            crashLogState = sw.toString()
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TerminalBackground
                ) {
                    val currentCrashMsg = crashLogState
                    if (currentCrashMsg != null) {
                        RecoveryTerminalScreen(
                            errorMsg = currentCrashMsg,
                            onResetDatabase = {
                                try {
                                    deleteDatabase("trade_diary_database")
                                    val intent = Intent(this, MainActivity::class.java)
                                    finish()
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("CRASH_RECOVERY", "Could not delete database", e)
                                }
                            },
                            onIgnoreAndRetry = {
                                crashLogState = null
                            }
                        )
                    } else if (initialViewModel != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Diagnostic Banner alert if the SQLite builder defaulted to an in-memory instance
                            val dbInitError = AppDatabase.initError
                            if (dbInitError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NeonRedLoss.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, NeonRedLoss),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "▲ EMERGENCY STATUS: MEMORY FALLBACK LOADED ▲",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonRedLoss,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dbInitError,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TerminalTextPrimary,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                            
                            MainTerminalContainer(
                                viewModel = initialViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Safe placeholder indicator while booting recovery variables
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TerminalBackground)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryTerminalScreen(
    errorMsg: String,
    onResetDatabase: () -> Unit,
    onIgnoreAndRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "▲ SYSTEM RECOVERY TERMINAL ▲",
            style = MaterialTheme.typography.titleLarge,
            color = NeonRedLoss,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "The Trade Terminal system encountered a fatal exception during view-hierarchy assembly or database indexing. Press below to reset variables or proceed with an in-memory cache.",
            style = MaterialTheme.typography.bodyMedium,
            color = TerminalTextPrimary
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onResetDatabase,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRedLoss),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "HARD DB RESET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Button(
                onClick = onIgnoreAndRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                modifier = Modifier.weight(1.2f)
            ) {
                Text(
                    text = "IGNORE & ATTEMPT BOOT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Text(
            text = "FATAL EXCEPTION DEPLOYMENT TRACE LOG:",
            style = MaterialTheme.typography.titleMedium,
            color = AccentColor,
            fontWeight = FontWeight.SemiBold
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalCardBackground),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            SelectionContainer {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Yellow,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 16.sp,
                    fontSize = 10.sp
                )
            }
        }
    }
}
