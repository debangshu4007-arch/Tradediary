package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val capital: Double,
    val duration: Int,
    val strategyType: String = "Any",
    val instrument: String = "Any",
    val targetProfit: Double = 0.0,
    val createdDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
