package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import com.example.data.HintSpendOutcome
import com.example.data.local.ArrowPuzzleDatabase
import com.example.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArrowPuzzleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: ArrowPuzzleDatabase
    private lateinit var prefs: PreferencesManager
    private lateinit var repo: GameRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ArrowPuzzleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = PreferencesManager(context)
        repo = GameRepository(db.gameDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appNameIsArrowPuzzle() {
        assertEquals("Arrow Puzzle", context.getString(R.string.app_name))
    }

    @Test
    fun levelCompletionCoinsHintsAndNonNegativeGuardWorkEndToEnd() = runTest {
        repo.resetAllProgress()
        val initial = prefs.getSnapshot()
        assertEquals(150, initial.coins)
        assertEquals(2, initial.freeHints)
        assertEquals(1, initial.highestUnlockedLevel)

        // Complete Level 1 -> unlocks Level 2 and adds coins
        repo.recordLevelCompletion(levelNumber = 1, stars = 3, moves = 4, score = 310, coinsEarned = 35)
        val afterWin = prefs.getSnapshot()
        assertEquals(185, afterWin.coins)
        assertEquals(2, afterWin.highestUnlockedLevel)
        assertTrue(repo.isLevelAlreadyCompleted(1))

        // First two hints consume free hints without charging coins
        assertEquals(HintSpendOutcome.UsedFreeHint, repo.consumeHint())
        assertEquals(HintSpendOutcome.UsedFreeHint, repo.consumeHint())
        assertEquals(185, prefs.getSnapshot().coins)

        // Next hint costs 50 coins
        assertEquals(HintSpendOutcome.PaidWithCoins(50), repo.consumeHint())
        assertEquals(135, prefs.getSnapshot().coins)

        // Cannot unlock 400-coin Sunset theme with 135 coins -> balance stays 135 (never negative!)
        val unlockedExpensive = repo.unlockOrSelectTheme("sunset", 400)
        assertFalse(unlockedExpensive)
        assertEquals(135, prefs.getSnapshot().coins)

        // Daily login reward grants coins once per day
        val firstClaim = repo.claimDailyLoginReward()
        assertNotNull(firstClaim)
        assertEquals(135 + firstClaim!!, prefs.getSnapshot().coins)
        val secondClaimSameDay = repo.claimDailyLoginReward()
        assertNull(secondClaimSameDay)

        // Daily challenge completion persists locally
        val today = GameRepository.todayDateKey()
        val firstDaily = repo.recordDailyCompletion(today, stars = 3, moves = 19, score = 450, coinsEarned = 130)
        assertTrue(firstDaily)
        val dailyMap = repo.dailyCompletionsMap.first()
        assertTrue(dailyMap.containsKey(today))
        assertEquals(1, GameRepository.calculateDailyStreak(dailyMap))
    }
}
