package com.example.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles JSON export/import of the trade ledger and cache clearing.
 * Backups are written to the app-private external files dir (no runtime permission needed).
 */
class BackupManager(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, TradeEntity::class.java)
    private val adapter = moshi.adapter<List<TradeEntity>>(listType).indent("  ")

    private fun backupDir(): File =
        File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    fun toJson(trades: List<TradeEntity>): String = adapter.toJson(trades)

    /** Writes a timestamped JSON file and returns it. [prefix] distinguishes export vs backup. */
    fun writeBackup(trades: List<TradeEntity>, prefix: String = "trade_export"): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(System.currentTimeMillis()))
        val file = File(backupDir(), "${prefix}_$stamp.json")
        file.writeText(toJson(trades))
        return file
    }

    /** Parses a JSON document at [uri] into trades. Throws on malformed input. */
    fun readFromUri(uri: Uri): List<TradeEntity> {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Could not open the selected file.")
        return adapter.fromJson(text) ?: throw IllegalStateException("File did not contain valid trade data.")
    }

    fun latestBackup(): File? =
        backupDir().listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.maxByOrNull { it.lastModified() }

    /** Clears the app cache directory. Returns bytes freed. */
    fun clearCache(): Long {
        var freed = 0L
        fun deleteRecursively(f: File) {
            if (f.isDirectory) f.listFiles()?.forEach { deleteRecursively(it) }
            else { freed += f.length(); f.delete() }
        }
        context.cacheDir?.listFiles()?.forEach { deleteRecursively(it) }
        context.externalCacheDir?.listFiles()?.forEach { deleteRecursively(it) }
        return freed
    }
}
