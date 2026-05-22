package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_analyses")
data class DailyAnalysisEntity(
    @PrimaryKey val dateStr: String, // Format: YYYY-MM-DD
    val whatWentRight: String,
    val whatWentWrong: String,
    val marketCondition: String, // "Trending", "Range bound", "High VIX", "News driven"
    val followedSetup: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
