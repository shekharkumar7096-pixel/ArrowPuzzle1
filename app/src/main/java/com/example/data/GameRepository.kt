package com.example.data

import com.example.data.ads.AdManager
import com.example.data.local.DailyCompletionEntity
import com.example.data.local.GameDao
import com.example.data.local.LevelProgressEntity
import com.example.data.local.PlayerPreferences
import com.example.data.local.PreferencesManager
import com.example.game.LevelGenerator
import com.example.game.LevelSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MilestoneDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val rewardCoins: Int,
    val targetValue: Int,
    val currentValue: Int,
    val isClaimed: Boolean
) {
    val isReadyToClaim: Boolean get() = !isClaimed && currentValue >= targetValue
}

class GameRepository(
    private val gameDao: GameDao,
    private val preferencesManager: PreferencesManager,
    val adManager: AdManager = AdManager()
) {
    val preferences: Flow<PlayerPreferences> = preferencesManager.preferencesFlow

    val levelProgressMap: Flow<Map<Int, LevelProgressEntity>> =
        gameDao.observeAllLevelProgress().map { list -> list.associateBy { it.levelNumber } }

    val dailyCompletionsMap: Flow<Map<String, DailyCompletionEntity>> =
        gameDao.observeAllDailyCompletions().map { list -> list.associateBy { it.dateKey } }

    fun getLevel(levelNumber: Int): LevelSpec = LevelGenerator.generateLevel(levelNumber)

    fun getDailyLevel(dateKey: String): LevelSpec = LevelGenerator.generateDailyLevel(dateKey)

    suspend fun recordLevelCompletion(
        levelNumber: Int,
        stars: Int,
        moves: Int,
        score: Int,
        coinsEarned: Int
    ) {
        val existing = gameDao.getLevelProgress(levelNumber)
        val merged = LevelProgressEntity(
            levelNumber = levelNumber,
            isCompleted = true,
            stars = maxOf(existing?.stars ?: 0, stars),
            bestMoves = if (existing == null || existing.bestMoves == 0) moves else minOf(existing.bestMoves, moves),
            bestScore = maxOf(existing?.bestScore ?: 0, score),
            completedAtMillis = System.currentTimeMillis()
        )
        gameDao.upsertLevelProgress(merged)
        preferencesManager.addCoins(coinsEarned)
        if (levelNumber < LevelGenerator.TOTAL_LEVELS) {
            preferencesManager.unlockUpToLevel(levelNumber + 1)
        }
    }

    suspend fun recordDailyCompletion(
        dateKey: String,
        stars: Int,
        moves: Int,
        score: Int,
        coinsEarned: Int
    ): Boolean {
        val existing = gameDao.getDailyCompletion(dateKey)
        val firstTime = existing == null
        val merged = DailyCompletionEntity(
            dateKey = dateKey,
            stars = maxOf(existing?.stars ?: 0, stars),
            moves = if (existing == null || existing.moves == 0) moves else minOf(existing.moves, moves),
            score = maxOf(existing?.score ?: 0, score),
            completedAtMillis = System.currentTimeMillis()
        )
        gameDao.upsertDailyCompletion(merged)
        preferencesManager.addCoins(if (firstTime) coinsEarned else 20)
        return firstTime
    }

    suspend fun isLevelAlreadyCompleted(levelNumber: Int): Boolean {
        return gameDao.getLevelProgress(levelNumber)?.isCompleted == true
    }

    suspend fun consumeHint(hintCoinCost: Int = HINT_COST_COINS): HintSpendOutcome {
        if (preferencesManager.consumeFreeHint()) {
            return HintSpendOutcome.UsedFreeHint
        }
        return if (preferencesManager.spendCoins(hintCoinCost)) {
            HintSpendOutcome.PaidWithCoins(hintCoinCost)
        } else {
            HintSpendOutcome.InsufficientCoins
        }
    }

    suspend fun addBonusCoins(amount: Int) = preferencesManager.addCoins(amount)

    suspend fun spendCoinsForRefill(cost: Int): Boolean = preferencesManager.spendCoins(cost)

    suspend fun addFreeHint(count: Int = 1) = preferencesManager.addFreeHints(count)

    suspend fun unlockOrSelectTheme(themeId: String, cost: Int): Boolean =
        preferencesManager.unlockAndSelectTheme(themeId, cost)

    suspend fun selectTheme(themeId: String) = preferencesManager.selectTheme(themeId)

    suspend fun setSoundEnabled(enabled: Boolean) = preferencesManager.setSoundEnabled(enabled)
    suspend fun setMusicEnabled(enabled: Boolean) = preferencesManager.setMusicEnabled(enabled)
    suspend fun setVibrationEnabled(enabled: Boolean) = preferencesManager.setVibrationEnabled(enabled)
    suspend fun setLanguageCode(code: String) = preferencesManager.setLanguageCode(code)

    suspend fun claimDailyLoginReward(): Int? {
        val today = todayDateKey()
        val yesterday = offsetDateKey(-1)
        return preferencesManager.claimDailyLoginReward(today, yesterday) { streak ->
            dailyRewardCoinsForDay(streak)
        }
    }

    suspend fun claimMilestone(milestoneId: String, rewardCoins: Int): Boolean {
        return preferencesManager.claimMilestone(milestoneId, rewardCoins)
    }

    suspend fun resetAllProgress() {
        gameDao.clearLevelProgress()
        gameDao.clearDailyCompletions()
        preferencesManager.resetAllProgress()
    }

    companion object {
        const val HINT_COST_COINS = 50
        val DAILY_REWARD_LADDER = listOf(50, 75, 100, 125, 150, 200, 300)

        fun dailyRewardCoinsForDay(streakDay: Int): Int {
            val idx = ((streakDay - 1).coerceAtLeast(0)) % DAILY_REWARD_LADDER.size
            return DAILY_REWARD_LADDER[idx]
        }

        fun todayDateKey(): String = formatCalendar(Calendar.getInstance())

        fun offsetDateKey(daysOffset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            return formatCalendar(cal)
        }

        fun formatDateKey(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
            val cal = Calendar.getInstance()
            cal.set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            return formatCalendar(cal)
        }

        private fun formatCalendar(cal: Calendar): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(cal.time)
        }

        fun calculateDailyStreak(completions: Map<String, DailyCompletionEntity>): Int {
            if (completions.isEmpty()) return 0
            val today = todayDateKey()
            val yesterday = offsetDateKey(-1)
            val startOffset = when {
                completions.containsKey(today) -> 0
                completions.containsKey(yesterday) -> -1
                else -> return 0
            }
            var streak = 0
            var offset = startOffset
            while (completions.containsKey(offsetDateKey(offset))) {
                streak++
                offset--
            }
            return streak
        }

        fun buildMilestones(
            completedLevelsCount: Int,
            totalStars: Int,
            dailyCompletedCount: Int,
            claimedIds: Set<String>
        ): List<MilestoneDefinition> = listOf(
            MilestoneDefinition(
                id = "lvl_5",
                title = "Warm-Up Champion",
                subtitle = "Complete 5 levels",
                rewardCoins = 75,
                targetValue = 5,
                currentValue = completedLevelsCount,
                isClaimed = "lvl_5" in claimedIds
            ),
            MilestoneDefinition(
                id = "stars_30",
                title = "Star Collector",
                subtitle = "Earn 30 total stars",
                rewardCoins = 120,
                targetValue = 30,
                currentValue = totalStars,
                isClaimed = "stars_30" in claimedIds
            ),
            MilestoneDefinition(
                id = "lvl_25",
                title = "Arrow Tactician",
                subtitle = "Complete 25 levels",
                rewardCoins = 200,
                targetValue = 25,
                currentValue = completedLevelsCount,
                isClaimed = "lvl_25" in claimedIds
            ),
            MilestoneDefinition(
                id = "daily_3",
                title = "Daily Regular",
                subtitle = "Finish 3 Daily Challenges",
                rewardCoins = 150,
                targetValue = 3,
                currentValue = dailyCompletedCount,
                isClaimed = "daily_3" in claimedIds
            ),
            MilestoneDefinition(
                id = "lvl_100",
                title = "Century Solver",
                subtitle = "Complete 100 levels",
                rewardCoins = 500,
                targetValue = 100,
                currentValue = completedLevelsCount,
                isClaimed = "lvl_100" in claimedIds
            )
        )
    }
}

sealed interface HintSpendOutcome {
    data object UsedFreeHint : HintSpendOutcome
    data class PaidWithCoins(val cost: Int) : HintSpendOutcome
    data object InsufficientCoins : HintSpendOutcome
}
