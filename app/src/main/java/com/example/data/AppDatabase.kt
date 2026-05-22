package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TradeEntity::class, DailyAnalysisEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        var initError: String? = null
            private set

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "trade_diary_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                } catch (t: Throwable) {
                    Log.e("AppDatabase", "Failed to build persistent trade_diary_database: ${t.localizedMessage}", t)
                    initError = "Persistent DB initialization failure: ${t.localizedMessage}. Fallback to in-memory."
                    try {
                        Room.inMemoryDatabaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                    } catch (t2: Throwable) {
                        Log.e("AppDatabase", "In-memory fallback also failed", t2)
                        throw RuntimeException("All database setup approaches failed components", t2)
                    }
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
