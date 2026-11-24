package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val isCompleted: Boolean,
    val stars: Int,
    val bestMoves: Int,
    val bestScore: Int,
    val completedAtMillis: Long
)

@Entity(tableName = "daily_completions")
data class DailyCompletionEntity(
    @PrimaryKey val dateKey: String,
    val stars: Int,
    val moves: Int,
    val score: Int,
    val completedAtMillis: Long
)

@Dao
interface GameDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAllLevelProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgress(progress: LevelProgressEntity)

    @Query("SELECT * FROM daily_completions ORDER BY dateKey DESC")
    fun observeAllDailyCompletions(): Flow<List<DailyCompletionEntity>>

    @Query("SELECT * FROM daily_completions WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyCompletion(dateKey: String): DailyCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyCompletion(completion: DailyCompletionEntity)

    @Query("DELETE FROM level_progress")
    suspend fun clearLevelProgress()

    @Query("DELETE FROM daily_completions")
    suspend fun clearDailyCompletions()
}

@Database(
    entities = [LevelProgressEntity::class, DailyCompletionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ArrowPuzzleDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: ArrowPuzzleDatabase? = null

        fun getInstance(context: Context): ArrowPuzzleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArrowPuzzleDatabase::class.java,
                    "arrow_puzzle_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
