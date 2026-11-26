package com.example.game

import kotlin.math.abs
import kotlin.random.Random

object LevelGenerator {

    const val INITIAL_VISIBLE_LEVELS = 500

    fun generateLevel(levelNumber: Int): LevelSpec {
        val clamped = levelNumber.coerceAtLeast(1)
        val difficulty = DifficultyTier.forLevel(clamped)
        val config = configForLevel(clamped)
        val baseSeed = splitMix64(clamped.toLong() * 0x9E3779B97F4A7C15uL.toLong() + 0xBF58476D1CE4E5B9uL.toLong())

        val arrows = generateVerifiedProceduralArrows(
            rows = config.rows,
            cols = config.cols,
            targetArrowCount = config.targetArrows,
            minRequiredArrows = config.minRequiredArrows,
            minPathLen = config.minLen,
            maxPathLen = config.maxLen,
            organicMask = config.organicMask,
            baseSeed = baseSeed
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
        val arrows = generateVerifiedProceduralArrows(
            rows = size,
            cols = size,
            targetArrowCount = targetArrows,
            minRequiredArrows = (targetArrows * 2) / 3,
            minPathLen = 3,
            maxPathLen = 10,
            organicMask = (variant == 1),
            baseSeed = splitMix64(hash xor 0x5A3C9F12L)
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

    private fun splitMix64(input: Long): Long {
        var z = input + 0x9E3779B97F4A7C15uL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }

    private data class LevelConfig(
        val rows: Int,
        val cols: Int,
        val targetArrows: Int,
        val minRequiredArrows: Int,
        val minLen: Int,
        val maxLen: Int,
        val organicMask: Boolean
    )

    private fun configForLevel(level: Int): LevelConfig = when {
        level == 1 -> LevelConfig(rows = 5, cols = 5, targetArrows = 4, minRequiredArrows = 3, minLen = 2, maxLen = 4, organicMask = false)
        level == 2 -> LevelConfig(rows = 6, cols = 6, targetArrows = 6, minRequiredArrows = 4, minLen = 2, maxLen = 5, organicMask = false)
        level == 3 -> LevelConfig(rows = 6, cols = 6, targetArrows = 8, minRequiredArrows = 5, minLen = 2, maxLen = 6, organicMask = false)
        // Beginner (4..50): 6x6 up to 8x8 clean interlocking snaking paths
        level <= 50 -> {
            val t = (level - 4).coerceAtLeast(0) / 46.0
            val size = (6 + (t * 2.5).toInt()).coerceIn(6, 8)
            val arrows = (7 + (t * 8).toInt()).coerceIn(7, 15)
            LevelConfig(size, size, arrows, minRequiredArrows = (arrows * 2) / 3, minLen = 2, maxLen = 6, organicMask = false)
        }
        // Normal / Easy (51..150): 9x9 up to 11x11
        level <= 150 -> {
            val t = (level - 51) / 99.0
            val size = (9 + (t * 2.5).toInt()).coerceIn(9, 11)
            val arrows = (15 + (t * 13).toInt()).coerceIn(15, 28)
            LevelConfig(size, size, arrows, minRequiredArrows = (arrows * 2) / 3, minLen = 3, maxLen = 9, organicMask = (level % 3 == 0))
        }
        // Medium (151..250): 12x12 up to 14x14
        level <= 250 -> {
            val t = (level - 151) / 99.0
            val size = (12 + (t * 2.5).toInt()).coerceIn(12, 14)
            val arrows = (26 + (t * 16).toInt()).coerceIn(26, 42)
            LevelConfig(size, size, arrows, minRequiredArrows = (arrows * 2) / 3, minLen = 3, maxLen = 11, organicMask = (level % 3 == 1))
        }
        // Hard (251..400): 14x14 up to 16x16
        level <= 400 -> {
            val t = (level - 251) / 149.0
            val size = (14 + (t * 2.5).toInt()).coerceIn(14, 16)
            val arrows = (38 + (t * 20).toInt()).coerceIn(38, 58)
            LevelConfig(size, size, arrows, minRequiredArrows = (arrows * 2) / 3, minLen = 3, maxLen = 13, organicMask = (level % 2 == 0))
        }
        // Expert / Unlimited (401+): cycles rich 16x16 to 19x19 dense labyrinth clusters endlessly
        else -> {
            val endlessOffset = level - 401
            val wave = endlessOffset % 100
            val t = wave / 99.0
            val size = (16 + (t * 3.2).toInt()).coerceIn(16, 19)
            val arrows = (50 + (t * 26).toInt() + ((endlessOffset / 100) % 4) * 2).coerceIn(50, 82)
            LevelConfig(size, size, arrows, minRequiredArrows = (arrows * 2) / 3, minLen = 3, maxLen = 15, organicMask = (level % 2 == 1))
        }
    }

    private fun generateVerifiedProceduralArrows(
        rows: Int,
        cols: Int,
        targetArrowCount: Int,
        minRequiredArrows: Int,
        minPathLen: Int,
        maxPathLen: Int,
        organicMask: Boolean,
        baseSeed: Long
    ): List<ArrowTile> {
        var bestCandidate: List<ArrowTile> = emptyList()
        for (attempt in 0 until 8) {
            val attemptSeed = splitMix64(baseSeed + attempt.toLong() * 0x517CC1B727220A95L)
            val candidate = generateGuaranteedSolvableSnakingArrows(
                rows = rows,
                cols = cols,
                targetArrowCount = targetArrowCount,
                minPathLen = minPathLen,
                maxPathLen = maxPathLen,
                organicMask = organicMask,
                seed = attemptSeed
            )
            if (candidate.isNotEmpty() && PuzzleEngine.isSolvable(candidate, rows, cols)) {
                if (candidate.size >= minRequiredArrows) {
                    return candidate
                }
                if (candidate.size > bestCandidate.size) {
                    bestCandidate = candidate
                }
            }
        }
        if (bestCandidate.isNotEmpty()) return bestCandidate

        // Ultra-safe deterministic fallback on unmasked grid
        return generateGuaranteedSolvableSnakingArrows(
            rows = rows,
            cols = cols,
            targetArrowCount = targetArrowCount,
            minPathLen = 2,
            maxPathLen = maxPathLen,
            organicMask = false,
            seed = baseSeed
        )
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
}
