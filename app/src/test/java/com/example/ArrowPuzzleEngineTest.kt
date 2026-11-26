package com.example

import com.example.game.ArrowColor
import com.example.game.ArrowTile
import com.example.game.DifficultyTier
import com.example.game.Direction
import com.example.game.GridPoint
import com.example.game.LevelGenerator
import com.example.game.MoveResult
import com.example.game.PuzzleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowPuzzleEngineTest {

    @Test
    fun all500SnakingLevelsAreDeterministicDisjointAndSolvable() {
        for (levelNum in 1..LevelGenerator.INITIAL_VISIBLE_LEVELS) {
            val spec = LevelGenerator.generateLevel(levelNum)
            assertEquals("Level number mismatch", levelNum, spec.levelNumber)
            assertTrue("Level $levelNum must have arrows", spec.arrows.isNotEmpty())
            assertTrue("Level $levelNum grid must be 5..19", spec.rows in 5..19 && spec.cols in 5..19)

            // Every arrow must have >= 2 cells and all occupied cells must be strictly disjoint
            val occupied = HashSet<GridPoint>()
            for (arrow in spec.arrows) {
                assertTrue("Arrow ${arrow.id} in level $levelNum must have >= 2 points", arrow.points.size >= 2)
                for (pt in arrow.points) {
                    assertTrue(
                        "Cell $pt out of bounds in level $levelNum",
                        pt.row in 0 until spec.rows && pt.col in 0 until spec.cols
                    )
                    assertTrue(
                        "Overlapping cell $pt in level $levelNum",
                        occupied.add(pt)
                    )
                }
            }

            // Mathematical solvability guarantee across all 500 levels
            assertTrue(
                "Level $levelNum (${spec.rows}x${spec.cols}, ${spec.arrows.size} arrows) MUST be solvable!",
                PuzzleEngine.isSolvable(spec.arrows, spec.rows, spec.cols)
            )

            // Determinism check on sampled levels
            if (levelNum % 25 == 0 || levelNum <= 5) {
                val repeat = LevelGenerator.generateLevel(levelNum)
                assertEquals("Level $levelNum should be deterministic", spec.arrows, repeat.arrows)
            }
        }
    }

    @Test
    fun difficultyTiersScaleFromSimpleToDenseLabyrinths() {
        val lvl1 = LevelGenerator.generateLevel(1)
        assertEquals(DifficultyTier.BEGINNER, lvl1.difficulty)
        assertTrue(lvl1.arrows.size >= 3)
        assertTrue(PuzzleEngine.isSolvable(lvl1.arrows, lvl1.rows, lvl1.cols))

        val lvl2 = LevelGenerator.generateLevel(2)
        assertEquals(DifficultyTier.BEGINNER, lvl2.difficulty)
        assertTrue(lvl2.arrows.size >= 4)
        assertTrue(PuzzleEngine.isSolvable(lvl2.arrows, lvl2.rows, lvl2.cols))

        val lvl100 = LevelGenerator.generateLevel(100)
        assertEquals(DifficultyTier.EASY, lvl100.difficulty)
        assertTrue(lvl100.rows >= 9)

        val lvl200 = LevelGenerator.generateLevel(200)
        assertEquals(DifficultyTier.MEDIUM, lvl200.difficulty)
        assertTrue(lvl200.rows >= 12)

        val lvl350 = LevelGenerator.generateLevel(350)
        assertEquals(DifficultyTier.HARD, lvl350.difficulty)
        assertTrue(lvl350.rows >= 14)

        val lvl500 = LevelGenerator.generateLevel(500)
        assertEquals(DifficultyTier.EXPERT, lvl500.difficulty)
        assertTrue(lvl500.rows >= 16)

        // Verify unlimited procedural levels well beyond 500 are distinct, deterministic, and solvable
        val endlessSamples = listOf(501, 750, 1000, 2500, 10000, 1000000)
        val generatedSignatures = HashSet<List<com.example.game.ArrowTile>>()
        for (lvlNum in endlessSamples) {
            val spec = LevelGenerator.generateLevel(lvlNum)
            assertEquals(lvlNum, spec.levelNumber)
            assertEquals(DifficultyTier.EXPERT, spec.difficulty)
            assertTrue("Unlimited level $lvlNum must be solvable", PuzzleEngine.isSolvable(spec.arrows, spec.rows, spec.cols))
            assertEquals(spec.arrows, LevelGenerator.generateLevel(lvlNum).arrows)
            assertTrue("Unlimited level $lvlNum should generate unique content", generatedSignatures.add(spec.arrows))
        }
    }

    @Test
    fun dailyChallengesAreSolvableAndDeterministic() {
        val sampleDates = listOf("2026-11-22", "2026-11-23", "2026-12-01", "2027-01-15", "2027-06-30")
        for (date in sampleDates) {
            val daily = LevelGenerator.generateDailyLevel(date)
            assertTrue(daily.isDaily)
            assertEquals(date, daily.dailyDateKey)
            assertTrue("Daily $date must be solvable", PuzzleEngine.isSolvable(daily.arrows, daily.rows, daily.cols))
            assertEquals(daily.arrows, LevelGenerator.generateDailyLevel(date).arrows)
        }
    }

    @Test
    fun snakingBlockedAndValidMoveEvaluationWorksAccurately() {
        // Horizontal top arrow blocking a vertical L-shaped arrow below it
        val blocker = ArrowTile(
            id = 1,
            points = listOf(GridPoint(0, 0), GridPoint(0, 1), GridPoint(0, 2)),
            direction = Direction.RIGHT,
            color = ArrowColor.BLUE
        )
        val blockedArrow = ArrowTile(
            id = 2,
            points = listOf(GridPoint(3, 2), GridPoint(3, 1), GridPoint(2, 1)),
            direction = Direction.UP,
            color = ArrowColor.GREEN
        )
        val arrows = listOf(blocker, blockedArrow)

        // Tapping blocked arrow fails because (0,1) lies on its upward exit ray
        val blockedResult = PuzzleEngine.evaluateTap(arrows, rows = 5, cols = 5, targetId = 2)
        assertTrue(blockedResult is MoveResult.Blocked)
        val b = blockedResult as MoveResult.Blocked
        assertEquals(1, b.blocker.id)

        // Hint must recommend the unobstructed blocker (id = 1)
        val hint = PuzzleEngine.findHintArrow(arrows, rows = 5, cols = 5)
        assertNotNull(hint)
        assertEquals(1, hint!!.id)

        // Clearing blocker unblocks arrow 2
        val clearFirst = PuzzleEngine.evaluateTap(arrows, rows = 5, cols = 5, targetId = 1)
        assertTrue(clearFirst is MoveResult.Cleared)
        assertFalse((clearFirst as MoveResult.Cleared).isBoardComplete)

        val remaining = listOf(blockedArrow)
        val clearSecond = PuzzleEngine.evaluateTap(remaining, rows = 5, cols = 5, targetId = 2)
        assertTrue(clearSecond is MoveResult.Cleared)
        assertTrue((clearSecond as MoveResult.Cleared).isBoardComplete)
    }

    @Test
    fun starRatingCalculatesThreeTwoOneCorrectly() {
        assertEquals(3, PuzzleEngine.calculateStars(0))
        assertEquals(2, PuzzleEngine.calculateStars(1))
        assertEquals(2, PuzzleEngine.calculateStars(2))
        assertEquals(1, PuzzleEngine.calculateStars(3))
        assertEquals(1, PuzzleEngine.calculateStars(12))
    }

    @Test
    fun admobMonetizationFormatsAndAntiIrritationPacingWorkCorrectly() {
        val manager = com.example.data.ads.AdManager()
        assertEquals("ca-app-pub-7700773097020018/1409412750", manager.bannerUnitId())
        assertEquals("ca-app-pub-7700773097020018/4546838294", manager.interstitialUnitId())
        val expectedRewarded = "ca-app-pub-7700773097020018/8262722027"
        assertEquals(expectedRewarded, manager.rewardedUnitIdFor(com.example.data.ads.RewardedOfferType.REVIVE_HEARTS))
        assertEquals(expectedRewarded, manager.rewardedUnitIdFor(com.example.data.ads.RewardedOfferType.EXTRA_HINT))
        assertEquals(expectedRewarded, manager.rewardedUnitIdFor(com.example.data.ads.RewardedOfferType.BONUS_COINS))
        assertEquals(expectedRewarded, manager.rewardedUnitIdFor(com.example.data.ads.RewardedOfferType.DOUBLE_LEVEL_COINS))
        assertFalse(manager.config.allowAdsDuringActiveGameplay)

        // Onboarding protection: Levels 1 & 2 never trigger win interstitials
        assertFalse(manager.shouldShowPostLevelInterstitial(1, nowMillis = 100_000L))
        assertFalse(manager.shouldShowPostLevelInterstitial(2, nowMillis = 200_000L))

        // Frequency cap: every 2 wins after Level 2 with 75s cooldown
        assertFalse(manager.shouldShowPostLevelInterstitial(3, nowMillis = 300_000L))
        assertTrue(manager.shouldShowPostLevelInterstitial(4, nowMillis = 310_000L))

        // Cooldown protection: even after 2 wins, <75s since last interstitial stays suppressed
        assertFalse(manager.shouldShowPostLevelInterstitial(5, nowMillis = 330_000L))
        assertFalse(manager.shouldShowPostLevelInterstitial(6, nowMillis = 350_000L))
        assertTrue(manager.shouldShowPostLevelInterstitial(7, nowMillis = 390_000L))

        // Anti-double-ad protection: watching a Rewarded Ad suppresses Win Interstitial for 60s
        manager.recordRewardedAdWatched(nowMillis = 500_000L)
        assertFalse(manager.shouldShowPostLevelInterstitial(8, nowMillis = 520_000L))
        assertFalse(manager.shouldShowPostLevelInterstitial(9, nowMillis = 540_000L))
        assertFalse(manager.shouldShowPostLevelInterstitial(10, nowMillis = 580_000L))
        assertTrue(manager.shouldShowPostLevelInterstitial(11, nowMillis = 600_000L))
    }
}
