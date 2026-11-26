package com.example.game

enum class Direction(val dRow: Int, val dCol: Int, val label: String) {
    UP(-1, 0, "upward"),
    DOWN(1, 0, "downward"),
    LEFT(0, -1, "to the left"),
    RIGHT(0, 1, "to the right");

    fun directionNoun(): String = when (this) {
        UP -> "above"
        DOWN -> "below"
        LEFT -> "to the left"
        RIGHT -> "to the right"
    }
}

enum class ArrowColor(val label: String) {
    NAVY("Navy"),
    BLUE("Blue"),
    GREEN("Green"),
    YELLOW("Yellow"),
    RED("Red"),
    PURPLE("Purple")
}

enum class DifficultyTier(
    val displayName: String,
    val levelRange: IntRange,
    val rangeLabel: String,
    val baseCoinReward: Int,
    val baseScore: Int
) {
    BEGINNER("Beginner", 1..50, "1–50", 20, 100),
    EASY("Normal", 51..150, "51–150", 30, 180),
    MEDIUM("Medium", 151..250, "151–250", 45, 280),
    HARD("Hard", 251..400, "251–400", 60, 420),
    EXPERT("Expert", 401..Int.MAX_VALUE, "401+", 80, 600);

    companion object {
        fun forLevel(levelNumber: Int): DifficultyTier = when {
            levelNumber <= 50 -> BEGINNER
            levelNumber <= 150 -> EASY
            levelNumber <= 250 -> MEDIUM
            levelNumber <= 400 -> HARD
            else -> EXPERT
        }
    }
}

data class GridPoint(
    val row: Int,
    val col: Int
)

/**
 * A multi-cell snaking arrow path on the grid.
 * [points] is ordered from tail -> ... -> head (at least 2 points).
 * [direction] is the exit direction at the head (points.last()).
 */
data class ArrowTile(
    val id: Int,
    val points: List<GridPoint>,
    val direction: Direction,
    val color: ArrowColor = ArrowColor.NAVY
) {
    val head: GridPoint get() = points.last()
    val tail: GridPoint get() = points.first()
}

data class LevelSpec(
    val levelNumber: Int,
    val rows: Int,
    val cols: Int,
    val arrows: List<ArrowTile>,
    val difficulty: DifficultyTier,
    val isDaily: Boolean = false,
    val dailyDateKey: String? = null
)

data class GameMoveSnapshot(
    val arrows: List<ArrowTile>,
    val movesCount: Int,
    val mistakesCount: Int,
    val heartsRemaining: Int,
    val blockedArrowIds: Set<Int>
)

sealed interface MoveResult {
    data class Cleared(
        val arrow: ArrowTile,
        val remainingCount: Int,
        val isBoardComplete: Boolean
    ) : MoveResult

    data class Blocked(
        val arrow: ArrowTile,
        val blocker: ArrowTile,
        val reason: String
    ) : MoveResult
}

data class LevelCompletionSummary(
    val levelNumber: Int,
    val isDaily: Boolean,
    val dailyDateKey: String?,
    val stars: Int,
    val coinsEarned: Int,
    val score: Int,
    val moves: Int,
    val mistakes: Int,
    val initialLevel: LevelSpec,
    val bonusClaimed: Boolean = false
)
