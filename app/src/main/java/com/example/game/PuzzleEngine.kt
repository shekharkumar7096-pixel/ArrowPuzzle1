package com.example.game

object PuzzleEngine {

    /**
     * Traces a straight ray from [target.head] in [target.direction] toward the board boundary.
     * Returns the first obstructing [ArrowTile] whose body or head lies on that exit ray, or null if clear.
     */
    fun findBlocker(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int,
        target: ArrowTile
    ): ArrowTile? {
        val occupied = HashMap<Long, ArrowTile>(arrows.size * 6)
        for (tile in arrows) {
            for (pt in tile.points) {
                if (tile.id == target.id && pt == target.head) continue
                occupied[cellKey(pt.row, pt.col)] = tile
            }
        }
        var r = target.head.row + target.direction.dRow
        var c = target.head.col + target.direction.dCol
        while (r in 0 until rows && c in 0 until cols) {
            val hit = occupied[cellKey(r, c)]
            if (hit != null) {
                return hit
            }
            r += target.direction.dRow
            c += target.direction.dCol
        }
        return null
    }

    fun canClear(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int,
        target: ArrowTile
    ): Boolean = findBlocker(arrows, rows, cols, target) == null

    fun evaluateTap(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int,
        targetId: Int
    ): MoveResult? {
        val target = arrows.firstOrNull { it.id == targetId } ?: return null
        val blocker = findBlocker(arrows, rows, cols, target)
        return if (blocker == null) {
            val remaining = arrows.size - 1
            MoveResult.Cleared(
                arrow = target,
                remainingCount = remaining,
                isBoardComplete = remaining == 0
            )
        } else {
            val reason = if (blocker.id == target.id) {
                "Blocked by its own path! Check the ray ${target.direction.directionNoun()}."
            } else {
                "Blocked! Clear the obstructing arrow ${target.direction.directionNoun()} first."
            }
            MoveResult.Blocked(
                arrow = target,
                blocker = blocker,
                reason = reason
            )
        }
    }

    fun getValidMoves(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int
    ): List<ArrowTile> {
        if (arrows.isEmpty()) return emptyList()
        val occupied = HashMap<Long, Int>(arrows.size * 6)
        for (tile in arrows) {
            for (pt in tile.points) {
                occupied[cellKey(pt.row, pt.col)] = tile.id
            }
        }
        val valid = ArrayList<ArrowTile>()
        for (tile in arrows) {
            var r = tile.head.row + tile.direction.dRow
            var c = tile.head.col + tile.direction.dCol
            var blocked = false
            while (r in 0 until rows && c in 0 until cols) {
                if (occupied.containsKey(cellKey(r, c))) {
                    blocked = true
                    break
                }
                r += tile.direction.dRow
                c += tile.direction.dCol
            }
            if (!blocked) {
                valid.add(tile)
            }
        }
        return valid
    }

    /**
     * Picks the best valid arrow to highlight for a Hint (one that frees up the most other arrows).
     */
    fun findHintArrow(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int
    ): ArrowTile? {
        val valid = getValidMoves(arrows, rows, cols)
        if (valid.isEmpty()) return null
        if (valid.size == 1) return valid.first()

        var bestArrow = valid.first()
        var bestScore = -1
        for (candidate in valid) {
            val afterRemoval = arrows.filter { it.id != candidate.id }
            val nextValidCount = getValidMoves(afterRemoval, rows, cols).size
            if (nextValidCount > bestScore) {
                bestScore = nextValidCount
                bestArrow = candidate
            }
        }
        return bestArrow
    }

    /**
     * Exact solvability check via monotone topological peeling.
     */
    fun isSolvable(
        arrows: List<ArrowTile>,
        rows: Int,
        cols: Int
    ): Boolean {
        val remaining = arrows.toMutableList()
        while (remaining.isNotEmpty()) {
            val valid = getValidMoves(remaining, rows, cols)
            if (valid.isEmpty()) {
                return false
            }
            val validIds = valid.mapTo(HashSet(valid.size)) { it.id }
            remaining.removeAll { it.id in validIds }
        }
        return true
    }

    fun calculateStars(mistakes: Int): Int = when {
        mistakes == 0 -> 3
        mistakes <= 2 -> 2
        else -> 1
    }

    fun calculateScore(
        difficulty: DifficultyTier,
        arrowCount: Int,
        mistakes: Int,
        stars: Int
    ): Int {
        val efficiencyBonus = (arrowCount * 15) - (mistakes * 25)
        val starBonus = stars * 50
        return (difficulty.baseScore + efficiencyBonus + starBonus).coerceAtLeast(50)
    }

    fun calculateCoinsEarned(
        difficulty: DifficultyTier,
        stars: Int,
        isDaily: Boolean,
        isReplay: Boolean
    ): Int {
        if (isDaily) return 100 + (stars * 10)
        val raw = difficulty.baseCoinReward + (stars * 5)
        return if (isReplay) (raw / 3).coerceAtLeast(10) else raw
    }

    private fun cellKey(row: Int, col: Int): Long = (row.toLong() shl 16) or (col.toLong() and 0xFFFFL)
}
