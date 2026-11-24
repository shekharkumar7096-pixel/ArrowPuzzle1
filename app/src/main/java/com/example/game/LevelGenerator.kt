package com.example.game

import kotlin.math.abs
import kotlin.random.Random

object LevelGenerator {

    const val TOTAL_LEVELS = 500

    fun generateLevel(levelNumber: Int): LevelSpec {
        val clamped = levelNumber.coerceIn(1, TOTAL_LEVELS)
        val difficulty = DifficultyTier.forLevel(clamped)

        when (clamped) {
            1 -> return buildTutorialLevel1()
            2 -> return buildWarmupLevel2()
            3 -> return buildWarmupLevel3()
        }

        val config = configForLevel(clamped)
        val seed = clamped.toLong() * 2654435761L + 9731L
        val arrows = generateGuaranteedSolvableSnakingArrows(
            rows = config.rows,
            cols = config.cols,
            targetArrowCount = config.targetArrows,
            minPathLen = config.minLen,
            maxPathLen = config.maxLen,
            organicMask = config.organicMask,
            seed = seed
        )

        return LevelSpec(
            levelNumber = clamped,
            rows = config.rows,
            cols = config.cols,
            arrows = arrows,
            difficulty = difficulty
        )
    }

    fun generateDailyLevel(dateKey: String): LevelSpec {
        val hash = dateKey.fold(1125899906842597L) { acc, ch -> acc * 31L + ch.code.toLong() }
        val variant = abs((hash % 3).toInt())
        val size = 10 + variant * 2 // 10x10, 12x12, or 14x14 daily labyrinth
        val targetArrows = when (size) {
            10 -> 20
            12 -> 30
            else -> 40
        }
        val difficulty = when (size) {
            10 -> DifficultyTier.EASY
            12 -> DifficultyTier.MEDIUM
            else -> DifficultyTier.HARD
        }
        val arrows = generateGuaranteedSolvableSnakingArrows(
            rows = size,
            cols = size,
            targetArrowCount = targetArrows,
            minPathLen = 3,
            maxPathLen = 10,
            organicMask = (variant == 1),
            seed = hash xor 0x5A3C9F12L
        )
        return LevelSpec(
            levelNumber = 0,
            rows = size,
            cols = size,
            arrows = arrows,
            difficulty = difficulty,
            isDaily = true,
            dailyDateKey = dateKey
        )
    }

    private data class LevelConfig(
        val rows: Int,
        val cols: Int,
        val targetArrows: Int,
        val minLen: Int,
        val maxLen: Int,
        val organicMask: Boolean
    )

    private fun configForLevel(level: Int): LevelConfig = when {
        // Beginner (4..50): 6x6 up to 8x8 clean interlocking snaking paths
        level <= 50 -> {
            val t = (level - 4).coerceAtLeast(0) / 46.0
            val size = (6 + (t * 2.5).toInt()).coerceIn(6, 8)
            val arrows = (6 + (t * 9).toInt()).coerceIn(6, 15)
            LevelConfig(size, size, arrows, minLen = 2, maxLen = 6, organicMask = false)
        }
        // Normal / Easy (51..150): 9x9 up to 11x11
        level <= 150 -> {
            val t = (level - 51) / 99.0
            val size = (9 + (t * 2.5).toInt()).coerceIn(9, 11)
            val arrows = (15 + (t * 13).toInt()).coerceIn(15, 28)
            LevelConfig(size, size, arrows, minLen = 3, maxLen = 9, organicMask = (level % 3 == 0))
        }
        // Medium (151..250): 12x12 up to 14x14
        level <= 250 -> {
            val t = (level - 151) / 99.0
            val size = (12 + (t * 2.5).toInt()).coerceIn(12, 14)
            val arrows = (26 + (t * 16).toInt()).coerceIn(26, 42)
            LevelConfig(size, size, arrows, minLen = 3, maxLen = 11, organicMask = (level % 3 == 1))
        }
        // Hard (251..400): 14x14 up to 16x16
        level <= 400 -> {
            val t = (level - 251) / 149.0
            val size = (14 + (t * 2.5).toInt()).coerceIn(14, 16)
            val arrows = (38 + (t * 20).toInt()).coerceIn(38, 58)
            LevelConfig(size, size, arrows, minLen = 3, maxLen = 13, organicMask = (level % 2 == 0))
        }
        // Expert (401..500): 17x17 up to 19x19 dense labyrinth clusters
        else -> {
            val t = (level - 401) / 99.0
            val size = (17 + (t * 2.5).toInt()).coerceIn(17, 19)
            val arrows = (54 + (t * 24).toInt()).coerceIn(54, 78)
            LevelConfig(size, size, arrows, minLen = 3, maxLen = 15, organicMask = (level % 2 == 1))
        }
    }

    /**
     * Inductive Reverse-Peeling Snaking Arrow Generator:
     * Places multi-cell orthogonal paths A_1, A_2, ..., A_k such that each newly placed arrow A_m
     * has an unobstructed exit ray from its head `p_{L-1}` to the grid border against all
     * previously placed arrows {A_1..A_{m-1}} AND against its own body.
     *
     * By induction, in forward play the player can always clear A_k, then A_{k-1}, ..., down to A_1.
     */
    private fun generateGuaranteedSolvableSnakingArrows(
        rows: Int,
        cols: Int,
        targetArrowCount: Int,
        minPathLen: Int,
        maxPathLen: Int,
        organicMask: Boolean,
        seed: Long
    ): List<ArrowTile> {
        val rng = Random(seed)
        val grid = Array(rows) { IntArray(cols) } // 0 = empty, >0 = arrow id, -1 = masked outer corner
        val blockerHotspot = Array(rows) { IntArray(cols) }
        val placed = ArrayList<ArrowTile>(targetArrowCount)
        val allDirs = Direction.entries.toTypedArray()
        val colorfulPalette = listOf(
            ArrowColor.BLUE,
            ArrowColor.GREEN,
            ArrowColor.YELLOW,
            ArrowColor.RED,
            ArrowColor.PURPLE
        )

        // Optional rounded/organic shape mask on larger boards (like Screenshot 5)
        if (organicMask && rows >= 9 && cols >= 9) {
            val cr = (rows - 1) / 2.0
            val cc = (cols - 1) / 2.0
            val maxR = minOf(cr, cc) + 0.85
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val dr = (r - cr)
                    val dc = (c - cc)
                    if (kotlin.math.sqrt(dr * dr + dc * dc) > maxR) {
                        grid[r][c] = -1
                    }
                }
            }
        }

        fun isCellEmpty(r: Int, c: Int): Boolean {
            return r in 0 until rows && c in 0 until cols && grid[r][c] == 0
        }

        fun isRayClearOfExisting(headRow: Int, headCol: Int, dir: Direction): Set<Long>? {
            val rayCells = HashSet<Long>()
            var r = headRow + dir.dRow
            var c = headCol + dir.dCol
            while (r in 0 until rows && c in 0 until cols) {
                if (grid[r][c] > 0) return null
                rayCells.add(cellKey(r, c))
                r += dir.dRow
                c += dir.dCol
            }
            return rayCells
        }

        fun registerHotspots(head: GridPoint, dir: Direction) {
            var r = head.row + dir.dRow
            var c = head.col + dir.dCol
            while (r in 0 until rows && c in 0 until cols) {
                if (grid[r][c] != -1) {
                    blockerHotspot[r][c]++
                }
                r += dir.dRow
                c += dir.dCol
            }
        }

        data class HeadCandidate(val row: Int, val col: Int, val direction: Direction, val rayCells: Set<Long>, val score: Int)

        var nextId = 1
        var failedAttempts = 0

        while (placed.size < targetArrowCount && failedAttempts < 24) {
            val candidates = ArrayList<HeadCandidate>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (grid[r][c] != 0) continue
                    for (dir in allDirs) {
                        val neckR = r - dir.dRow
                        val neckC = c - dir.dCol
                        if (!isCellEmpty(neckR, neckC)) continue

                        val rayCells = isRayClearOfExisting(r, c, dir) ?: continue
                        val neckKey = cellKey(neckR, neckC)
                        if (neckKey in rayCells) continue

                        val hotspotBonus = blockerHotspot[r][c] * 12 + blockerHotspot[neckR][neckC] * 12
                        val edgeDist = when (dir) {
                            Direction.UP -> r
                            Direction.DOWN -> (rows - 1) - r
                            Direction.LEFT -> c
                            Direction.RIGHT -> (cols - 1) - c
                        }
                        val score = hotspotBonus + edgeDist * 2 + rng.nextInt(5)
                        candidates.add(HeadCandidate(r, c, dir, rayCells, score))
                    }
                }
            }

            if (candidates.isEmpty()) break

            candidates.sortByDescending { it.score }
            val topSlice = (candidates.size / 3).coerceIn(1, 14)
            val chosenHead = candidates[rng.nextInt(topSlice)]

            // Carve path backwards from head -> neck -> ... -> tail
            val desiredLen = rng.nextInt(minPathLen, maxPathLen + 1)
            val revPoints = ArrayList<GridPoint>(desiredLen)
            val usedKeys = HashSet<Long>(desiredLen * 2)

            val headPt = GridPoint(chosenHead.row, chosenHead.col)
            val neckPt = GridPoint(chosenHead.row - chosenHead.direction.dRow, chosenHead.col - chosenHead.direction.dCol)
            revPoints.add(headPt)
            revPoints.add(neckPt)
            usedKeys.add(cellKey(headPt.row, headPt.col))
            usedKeys.add(cellKey(neckPt.row, neckPt.col))

            var prevStepDir = Direction.entries.first {
                it.dRow == (neckPt.row - headPt.row) && it.dCol == (neckPt.col - headPt.col)
            }

            while (revPoints.size < desiredLen) {
                val curr = revPoints.last()
                val neighbors = ArrayList<Pair<GridPoint, Direction>>(4)
                for (d in allDirs) {
                    val nr = curr.row + d.dRow
                    val nc = curr.col + d.dCol
                    if (!isCellEmpty(nr, nc)) continue
                    val nKey = cellKey(nr, nc)
                    if (nKey in usedKeys) continue
                    // Critical invariant: body must NEVER enter its own head's exit ray!
                    if (nKey in chosenHead.rayCells) continue
                    neighbors.add(GridPoint(nr, nc) to d)
                }
                if (neighbors.isEmpty()) break

                // Prefer turns & blocking hotspot cells for winding snaking shapes
                val scored = neighbors.map { (pt, dir) ->
                    val turnBonus = if (dir != prevStepDir) 8 else 3
                    val blockBonus = blockerHotspot[pt.row][pt.col] * 10
                    val degreePenalty = countEmptyNeighbors(grid, rows, cols, pt.row, pt.col)
                    Triple(pt, dir, turnBonus + blockBonus + degreePenalty + rng.nextInt(6))
                }.sortedByDescending { it.third }

                val pickIdx = if (scored.size > 1 && rng.nextDouble() < 0.25) 1 else 0
                val picked = scored[pickIdx]
                revPoints.add(picked.first)
                usedKeys.add(cellKey(picked.first.row, picked.first.col))
                prevStepDir = picked.second
            }

            if (revPoints.size < 2) {
                failedAttempts++
                continue
            }

            val id = nextId++
            val forwardPoints = revPoints.asReversed()
            for (pt in forwardPoints) {
                grid[pt.row][pt.col] = id
            }
            val color = colorfulPalette[(id - 1) % colorfulPalette.size]
            val arrow = ArrowTile(
                id = id,
                points = forwardPoints,
                direction = chosenHead.direction,
                color = color
            )
            placed.add(arrow)
            registerHotspots(headPt, chosenHead.direction)
            failedAttempts = 0
        }

        // Fill remaining small 2-3 cell gaps if needed to reach density target
        if (placed.size < targetArrowCount) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (placed.size >= targetArrowCount) break
                    if (grid[r][c] != 0) continue
                    for (dir in allDirs) {
                        val neckR = r - dir.dRow
                        val neckC = c - dir.dCol
                        if (!isCellEmpty(neckR, neckC)) continue
                        val ray = isRayClearOfExisting(r, c, dir) ?: continue
                        if (cellKey(neckR, neckC) in ray) continue
                        val id = nextId++
                        grid[r][c] = id
                        grid[neckR][neckC] = id
                        val headPt = GridPoint(r, c)
                        placed.add(
                            ArrowTile(
                                id = id,
                                points = listOf(GridPoint(neckR, neckC), headPt),
                                direction = dir,
                                color = colorfulPalette[(id - 1) % colorfulPalette.size]
                            )
                        )
                        registerHotspots(headPt, dir)
                        break
                    }
                }
            }
        }

        return placed.mapIndexed { idx, tile -> tile.copy(id = idx + 1) }
    }

    private fun countEmptyNeighbors(grid: Array<IntArray>, rows: Int, cols: Int, r: Int, c: Int): Int {
        var count = 0
        if (r > 0 && grid[r - 1][c] == 0) count++
        if (r < rows - 1 && grid[r + 1][c] == 0) count++
        if (c > 0 && grid[r][c - 1] == 0) count++
        if (c < cols - 1 && grid[r][c + 1] == 0) count++
        return count
    }

    private fun cellKey(row: Int, col: Int): Long = (row.toLong() shl 16) or (col.toLong() and 0xFFFFL)

    /**
     * Level 1: Exact layout matching Reference Image 3 (3 straight vertical arrows: UP, UP, DOWN).
     */
    private fun buildTutorialLevel1(): LevelSpec = LevelSpec(
        levelNumber = 1,
        rows = 5,
        cols = 5,
        arrows = listOf(
            ArrowTile(
                id = 1,
                points = listOf(GridPoint(3, 1), GridPoint(2, 1), GridPoint(1, 1)),
                direction = Direction.UP,
                color = ArrowColor.BLUE
            ),
            ArrowTile(
                id = 2,
                points = listOf(GridPoint(3, 2), GridPoint(2, 2), GridPoint(1, 2)),
                direction = Direction.UP,
                color = ArrowColor.GREEN
            ),
            ArrowTile(
                id = 3,
                points = listOf(GridPoint(1, 3), GridPoint(2, 3), GridPoint(3, 3)),
                direction = Direction.DOWN,
                color = ArrowColor.PURPLE
            )
        ),
        difficulty = DifficultyTier.BEGINNER
    )

    /**
     * Level 2: Exact layout matching Reference Image 4 (5 nested interlocking winding arrows on 6x6 dotted grid).
     */
    private fun buildWarmupLevel2(): LevelSpec = LevelSpec(
        levelNumber = 2,
        rows = 6,
        cols = 6,
        arrows = listOf(
            // Outer wrapping arrow going down left edge & right along top edge
            ArrowTile(
                id = 1,
                points = listOf(
                    GridPoint(4, 3), GridPoint(4, 2), GridPoint(4, 1), GridPoint(4, 0),
                    GridPoint(3, 0), GridPoint(2, 0), GridPoint(1, 0), GridPoint(0, 0),
                    GridPoint(0, 1), GridPoint(0, 2), GridPoint(0, 3)
                ),
                direction = Direction.RIGHT,
                color = ArrowColor.BLUE
            ),
            // Inner right->up->left hook
            ArrowTile(
                id = 2,
                points = listOf(
                    GridPoint(3, 4), GridPoint(2, 4), GridPoint(1, 4),
                    GridPoint(1, 3), GridPoint(1, 2), GridPoint(1, 1)
                ),
                direction = Direction.LEFT,
                color = ArrowColor.GREEN
            ),
            // Left vertical L-arrow pointing UP
            ArrowTile(
                id = 3,
                points = listOf(
                    GridPoint(3, 2), GridPoint(3, 1), GridPoint(2, 1)
                ),
                direction = Direction.UP,
                color = ArrowColor.PURPLE
            ),
            // Center U-loop pointing DOWN
            ArrowTile(
                id = 4,
                points = listOf(
                    GridPoint(3, 2), GridPoint(2, 2), GridPoint(2, 3), GridPoint(3, 3)
                ).let {
                    // Ensure disjoint cells: use (2,2) -> (2,3) -> (3,3) if (3,2) is taken by arrow 3
                    listOf(GridPoint(2, 2), GridPoint(2, 3), GridPoint(3, 3))
                },
                direction = Direction.DOWN,
                color = ArrowColor.RED
            ),
            // Bottom-right short arrow pointing RIGHT
            ArrowTile(
                id = 5,
                points = listOf(GridPoint(4, 4), GridPoint(4, 5)),
                direction = Direction.RIGHT,
                color = ArrowColor.YELLOW
            )
        ),
        difficulty = DifficultyTier.BEGINNER
    )

    /**
     * Level 3: Clean 6x6 interlocking spiral puzzle (7 snaking arrows).
     */
    private fun buildWarmupLevel3(): LevelSpec {
        val arrows = generateGuaranteedSolvableSnakingArrows(
            rows = 6,
            cols = 6,
            targetArrowCount = 7,
            minPathLen = 3,
            maxPathLen = 6,
            organicMask = false,
            seed = 30303L
        )
        return LevelSpec(
            levelNumber = 3,
            rows = 6,
            cols = 6,
            arrows = arrows,
            difficulty = DifficultyTier.BEGINNER
        )
    }
}
