package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.arrowPuzzleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "arrow_puzzle_prefs"
)

data class PlayerPreferences(
    val coins: Int = DEFAULT_STARTING_COINS,
    val freeHints: Int = DEFAULT_FREE_HINTS,
    val highestUnlockedLevel: Int = 1,
    val selectedThemeId: String = DEFAULT_THEME_ID,
    val unlockedThemeIds: Set<String> = setOf(DEFAULT_THEME_ID),
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val languageCode: String = "en",
    val lastDailyRewardDate: String = "",
    val dailyRewardStreak: Int = 0,
    val claimedMilestoneIds: Set<String> = emptySet()
) {
    companion object {
        const val DEFAULT_STARTING_COINS = 150
        const val DEFAULT_FREE_HINTS = 2
        const val DEFAULT_THEME_ID = "classic_blue"
    }
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val FREE_HINTS = intPreferencesKey("free_hints")
        val HIGHEST_UNLOCKED_LEVEL = intPreferencesKey("highest_unlocked_level")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val LAST_DAILY_REWARD_DATE = stringPreferencesKey("last_daily_reward_date")
        val DAILY_REWARD_STREAK = intPreferencesKey("daily_reward_streak")
        val CLAIMED_MILESTONES = stringSetPreferencesKey("claimed_milestones")
    }

    val preferencesFlow: Flow<PlayerPreferences> = context.arrowPuzzleDataStore.data.map { prefs ->
        PlayerPreferences(
            coins = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0),
            freeHints = (prefs[Keys.FREE_HINTS] ?: PlayerPreferences.DEFAULT_FREE_HINTS).coerceAtLeast(0),
            highestUnlockedLevel = (prefs[Keys.HIGHEST_UNLOCKED_LEVEL] ?: 1).coerceAtLeast(1),
            selectedThemeId = prefs[Keys.SELECTED_THEME] ?: PlayerPreferences.DEFAULT_THEME_ID,
            unlockedThemeIds = prefs[Keys.UNLOCKED_THEMES] ?: setOf(PlayerPreferences.DEFAULT_THEME_ID),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            lastDailyRewardDate = prefs[Keys.LAST_DAILY_REWARD_DATE] ?: "",
            dailyRewardStreak = (prefs[Keys.DAILY_REWARD_STREAK] ?: 0).coerceAtLeast(0),
            claimedMilestoneIds = prefs[Keys.CLAIMED_MILESTONES] ?: emptySet()
        )
    }

    suspend fun getSnapshot(): PlayerPreferences = preferencesFlow.first()

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.arrowPuzzleDataStore.edit { prefs ->
            val current = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0)
            prefs[Keys.COINS] = (current + amount).coerceAtLeast(0)
        }
    }

    /**
     * Atomically spends [amount] coins if balance is sufficient. Never permits a negative balance.
     */
    suspend fun spendCoins(amount: Int): Boolean {
        if (amount < 0) return false
        var succeeded = false
        context.arrowPuzzleDataStore.edit { prefs ->
            val current = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0)
            if (current >= amount) {
                prefs[Keys.COINS] = current - amount
                succeeded = true
            }
        }
        return succeeded
    }

    suspend fun consumeFreeHint(): Boolean {
        var consumed = false
        context.arrowPuzzleDataStore.edit { prefs ->
            val current = (prefs[Keys.FREE_HINTS] ?: PlayerPreferences.DEFAULT_FREE_HINTS).coerceAtLeast(0)
            if (current > 0) {
                prefs[Keys.FREE_HINTS] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    suspend fun addFreeHints(count: Int) {
        if (count <= 0) return
        context.arrowPuzzleDataStore.edit { prefs ->
            val current = (prefs[Keys.FREE_HINTS] ?: PlayerPreferences.DEFAULT_FREE_HINTS).coerceAtLeast(0)
            prefs[Keys.FREE_HINTS] = current + count
        }
    }

    suspend fun unlockUpToLevel(levelNumber: Int) {
        val target = levelNumber.coerceAtLeast(1)
        context.arrowPuzzleDataStore.edit { prefs ->
            val current = prefs[Keys.HIGHEST_UNLOCKED_LEVEL] ?: 1
            if (target > current) {
                prefs[Keys.HIGHEST_UNLOCKED_LEVEL] = target
            }
        }
    }

    suspend fun selectTheme(themeId: String) {
        context.arrowPuzzleDataStore.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_THEMES] ?: setOf(PlayerPreferences.DEFAULT_THEME_ID)
            if (themeId in unlocked) {
                prefs[Keys.SELECTED_THEME] = themeId
            }
        }
    }

    suspend fun unlockAndSelectTheme(themeId: String, cost: Int): Boolean {
        var unlockedNow = false
        context.arrowPuzzleDataStore.edit { prefs ->
            val unlocked = (prefs[Keys.UNLOCKED_THEMES] ?: setOf(PlayerPreferences.DEFAULT_THEME_ID)).toMutableSet()
            if (themeId in unlocked) {
                prefs[Keys.SELECTED_THEME] = themeId
                unlockedNow = true
                return@edit
            }
            val currentCoins = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0)
            if (currentCoins >= cost) {
                prefs[Keys.COINS] = currentCoins - cost
                unlocked.add(themeId)
                prefs[Keys.UNLOCKED_THEMES] = unlocked
                prefs[Keys.SELECTED_THEME] = themeId
                unlockedNow = true
            }
        }
        return unlockedNow
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.arrowPuzzleDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.arrowPuzzleDataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.arrowPuzzleDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setLanguageCode(code: String) {
        context.arrowPuzzleDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun claimDailyLoginReward(todayKey: String, yesterdayKey: String, coinsForStreak: (Int) -> Int): Int? {
        var awarded: Int? = null
        context.arrowPuzzleDataStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_DAILY_REWARD_DATE] ?: ""
            if (lastDate == todayKey) {
                return@edit
            }
            val prevStreak = prefs[Keys.DAILY_REWARD_STREAK] ?: 0
            val newStreak = if (lastDate == yesterdayKey) prevStreak + 1 else 1
            val rewardCoins = coinsForStreak(newStreak)
            val currentCoins = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0)
            prefs[Keys.COINS] = currentCoins + rewardCoins
            prefs[Keys.LAST_DAILY_REWARD_DATE] = todayKey
            prefs[Keys.DAILY_REWARD_STREAK] = newStreak
            awarded = rewardCoins
        }
        return awarded
    }

    suspend fun claimMilestone(milestoneId: String, rewardCoins: Int): Boolean {
        var claimed = false
        context.arrowPuzzleDataStore.edit { prefs ->
            val currentSet = (prefs[Keys.CLAIMED_MILESTONES] ?: emptySet()).toMutableSet()
            if (milestoneId !in currentSet) {
                currentSet.add(milestoneId)
                val currentCoins = (prefs[Keys.COINS] ?: PlayerPreferences.DEFAULT_STARTING_COINS).coerceAtLeast(0)
                prefs[Keys.COINS] = currentCoins + rewardCoins
                prefs[Keys.CLAIMED_MILESTONES] = currentSet
                claimed = true
            }
        }
        return claimed
    }

    suspend fun resetAllProgress() {
        context.arrowPuzzleDataStore.edit { prefs ->
            val sound = prefs[Keys.SOUND_ENABLED] ?: true
            val music = prefs[Keys.MUSIC_ENABLED] ?: false
            val vibration = prefs[Keys.VIBRATION_ENABLED] ?: true
            val language = prefs[Keys.LANGUAGE_CODE] ?: "en"
            prefs.clear()
            prefs[Keys.COINS] = PlayerPreferences.DEFAULT_STARTING_COINS
            prefs[Keys.FREE_HINTS] = PlayerPreferences.DEFAULT_FREE_HINTS
            prefs[Keys.HIGHEST_UNLOCKED_LEVEL] = 1
            prefs[Keys.SELECTED_THEME] = PlayerPreferences.DEFAULT_THEME_ID
            prefs[Keys.UNLOCKED_THEMES] = setOf(PlayerPreferences.DEFAULT_THEME_ID)
            prefs[Keys.SOUND_ENABLED] = sound
            prefs[Keys.MUSIC_ENABLED] = music
            prefs[Keys.VIBRATION_ENABLED] = vibration
            prefs[Keys.LANGUAGE_CODE] = language
        }
    }
}
