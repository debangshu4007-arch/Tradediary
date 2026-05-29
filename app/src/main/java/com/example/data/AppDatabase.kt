package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TradeEntity::class, DailyAnalysisEntity::class, FeedbackEntity::class, ChallengeEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        var initError: String? = null
            private set

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN optionType TEXT NOT NULL DEFAULT 'CE'")
                db.execSQL("ALTER TABLE trades ADD COLUMN tradeAction TEXT NOT NULL DEFAULT 'BUY'")
                db.execSQL("ALTER TABLE trades ADD COLUMN strikePrice REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE trades ADD COLUMN tradeDateMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE trades SET tradeDateMillis = timestamp WHERE tradeDateMillis = 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN marketSegment TEXT NOT NULL DEFAULT 'F&O'")
                db.execSQL("ALTER TABLE trades ADD COLUMN partialExits TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN behaviorTags TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS feedback (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, rating INTEGER NOT NULL, message TEXT NOT NULL, timestamp INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN challengeId INTEGER DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS challenges (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, capital REAL NOT NULL DEFAULT 0.0, duration INTEGER NOT NULL DEFAULT 30, strategyType TEXT NOT NULL DEFAULT 'Any', instrument TEXT NOT NULL DEFAULT 'Any', targetProfit REAL NOT NULL DEFAULT 0.0, createdDate INTEGER NOT NULL, isActive INTEGER NOT NULL DEFAULT 1)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN plannedTarget REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE trades ADD COLUMN entryTimeMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trades ADD COLUMN confirmations TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN mfePrice REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE trades ADD COLUMN autoTags TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "trade_diary_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
