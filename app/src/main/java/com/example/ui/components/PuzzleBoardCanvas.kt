package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ArrowTile
import com.example.game.Direction
import com.example.game.GridPoint
import com.example.game.LevelSpec
import com.example.game.PuzzleEngine
import com.example.ui.theme.GameThemePalette
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.BlockedFeedback
import com.example.viewmodel.ExitingArrow
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Interactive 60 FPS Snaking-Path Arrow Puzzle Board matching the reference ▲rrows screenshots:
 * - Crisp dotted grid background
 * - Continuous snaking orthogonal arrow paths with sharp triangular arrowheads
 * - Tapped blocked arrows turn coral-red and shake gently
 * - Cleared arrows smoothly slide forward along their own track and off the board edge
 * - Toggleable Ruler sight-line guide (dashed rays from every arrowhead)
 * - Golden pulsing Halo on Hinted arrow
 * - Pinch-to-zoom & pan on larger labyrinth grids (>= 10x10)
 * - Level 1 blue "Tap to remove" tooltip bubble with hand pointer
 */
@Composable
fun InteractivePuzzleBoard(
    level: LevelSpec,
    arrows: List<ArrowTile>,
    exitingArrows: List<ExitingArrow>,
    blockedArrowIds: Set<Int>,
    hintedArrowId: Int?,
    blockedFeedback: BlockedFeedback?,
    showPathGuide: Boolean,
    showTutorialHint: Boolean,
    onArrowTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current

    val infiniteTransition = rememberInfiniteTransition(label = "board_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_pulse"
    )

    val tutorialBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorial_bob"
    )

    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(blockedFeedback?.nonce) {
        if (blockedFeedback != null) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = LinearEasing)
            )
        }
    }

    // Track slide-out animation progress for each exiting arrow
    val exitingProgressMap = remember { mutableStateMapOf<Long, Float>() }
    for (exiting in exitingArrows) {
        LaunchedEffect(exiting.key) {
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            ) {
                exitingProgressMap[exiting.key] = value
            }
            exitingProgressMap.remove(exiting.key)
        }
    }

    val validIds = remember(arrows, level.rows, level.cols, showPathGuide) {
        if (showPathGuide) {
            PuzzleEngine.getValidMoves(arrows, level.rows, level.cols).mapTo(HashSet()) { it.id }
        } else {
            emptySet()
        }
    }

    var zoomScale by remember(level.levelNumber, level.dailyDateKey) { mutableFloatStateOf(1f) }
    var panX by remember(level.levelNumber, level.dailyDateKey) { mutableFloatStateOf(0f) }
    var panY by remember(level.levelNumber, level.dailyDateKey) { mutableFloatStateOf(0f) }

    val boardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (theme.isDark) 10.dp else 4.dp,
                    shape = boardShape,
                    clip = false
                ),
            shape = boardShape,
            color = theme.boardSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.cardBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(boardShape)
                    .then(
                        if (level.cols >= 10) {
                            Modifier.pointerInput(level.levelNumber, level.dailyDateKey) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 2.6f)
                                    zoomScale = newScale
                                    if (newScale > 1.02f) {
                                        val maxPanX = (size.width * (newScale - 1f)) * 0.5f
                                        val maxPanY = (size.height * (newScale - 1f)) * 0.5f
                                        panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                        panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                    } else {
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .pointerInput(arrows, level.rows, level.cols, zoomScale, panX, panY) {
                        detectTapGestures { tapOffset ->
                            // Map screen tap back through zoom/pan to canvas coordinates
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val localX = (tapOffset.x - cx - panX) / zoomScale + cx
                            val localY = (tapOffset.y - cy - panY) / zoomScale + cy

                            val hitId = findTappedArrowId(
                                tapX = localX,
                                tapY = localY,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                rows = level.rows,
                                cols = level.cols,
                                arrows = arrows
                            )
                            if (hitId != null) {
                                onArrowTap(hitId)
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panX
                            translationY = panY
                        }
                ) {
                    val pad = size.minDimension * 0.06f
                    val innerW = size.width - pad * 2f
                    val innerH = size.height - pad * 2f
                    val cw = innerW / level.cols
                    val ch = innerH / level.rows
                    val cellUnit = min(cw, ch)

                    fun cellCenter(pt: GridPoint): Offset = Offset(
                        x = pad + pt.col * cw + cw * 0.5f,
                        y = pad + pt.row * ch + ch * 0.5f
                    )

                    // 1. Subtle grid dots (matching Reference Images 4 & 6)
                    val dotRadius = (cellUnit * 0.085f).coerceIn(2f, 5.5f)
                    for (r in 0 until level.rows) {
                        for (c in 0 until level.cols) {
                            drawCircle(
                                color = theme.boardDotColor,
                                radius = dotRadius,
                                center = Offset(pad + c * cw + cw * 0.5f, pad + r * ch + ch * 0.5f)
                            )
                        }
                    }

                    // 2. Optional Ruler sight-line guide (dashed rays from arrowheads)
                    if (showPathGuide) {
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        for (tile in arrows) {
                            val isClear = tile.id in validIds
                            val headPos = cellCenter(tile.head)
                            val edgePos = when (tile.direction) {
                                Direction.UP -> Offset(headPos.x, 0f)
                                Direction.DOWN -> Offset(headPos.x, size.height)
                                Direction.LEFT -> Offset(0f, headPos.y)
                                Direction.RIGHT -> Offset(size.width, headPos.y)
                            }
                            drawLine(
                                color = if (isClear) Color(0xFF22C55E).copy(alpha = 0.55f) else Color(0xFFEF4444).copy(alpha = 0.30f),
                                start = headPos,
                                end = edgePos,
                                strokeWidth = (cellUnit * 0.11f).coerceIn(2.5f, 5f),
                                cap = StrokeCap.Round,
                                pathEffect = dashEffect
                            )
                        }
                    }

                    val baseStroke = (cellUnit * 0.22f).coerceIn(4.5f, 14f)
                    val headLength = (cellUnit * 0.44f).coerceIn(10f, 28f)
                    val headHalfWidth = headLength * 0.62f

                    // 3. Draw active snaking arrow paths
                    for (tile in arrows) {
                        val isHinted = hintedArrowId == tile.id
                        val isBlockedMarked = tile.id in blockedArrowIds
                        val isShaking = blockedFeedback?.arrowId == tile.id && shakeAnim.value < 0.99f
                        val shakePx = if (isShaking) {
                            (sin(shakeAnim.value * Math.PI * 6.0) * (1f - shakeAnim.value) * 10f).toFloat()
                        } else {
                            0f
                        }
                        val shakeOffset = if (tile.direction == Direction.UP || tile.direction == Direction.DOWN) {
                            Offset(shakePx, 0f)
                        } else {
                            Offset(0f, shakePx)
                        }

                        val screenPts = tile.points.map { cellCenter(it) + shakeOffset }
                        val inkColor = theme.pathColorFor(tile.color, isBlockedMarked)

                        if (isHinted) {
                            // Golden glowing halo + clear guide ray for Hint
                            drawSnakingArrow(
                                points = screenPts,
                                direction = tile.direction,
                                color = Color(0xFFFFD600).copy(alpha = 0.42f + pulseProgress * 0.38f),
                                strokeWidth = baseStroke * 2.1f,
                                headLength = headLength * 1.25f,
                                headHalfWidth = headHalfWidth * 1.25f
                            )
                            val headPos = screenPts.last()
                            val edgePos = when (tile.direction) {
                                Direction.UP -> Offset(headPos.x, 0f)
                                Direction.DOWN -> Offset(headPos.x, size.height)
                                Direction.LEFT -> Offset(0f, headPos.y)
                                Direction.RIGHT -> Offset(size.width, headPos.y)
                            }
                            drawLine(
                                color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                                start = headPos,
                                end = edgePos,
                                strokeWidth = baseStroke * 0.55f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        }

                        drawSnakingArrow(
                            points = screenPts,
                            direction = tile.direction,
                            color = inkColor,
                            strokeWidth = baseStroke,
                            headLength = headLength,
                            headHalfWidth = headHalfWidth
                        )
                    }

                    // 4. Draw exiting arrows sliding along their own polyline track off the board edge!
                    for (exiting in exitingArrows) {
                        val t = exitingProgressMap[exiting.key] ?: 0f
                        val tile = exiting.tile
                        val screenPts = tile.points.map { cellCenter(it) }
                        val inkColor = theme.pathColorFor(tile.color, isBlockedMarked = false)
                        drawSlidingExitingArrow(
                            points = screenPts,
                            direction = tile.direction,
                            progress = t,
                            boardSpan = max(size.width, size.height),
                            color = inkColor,
                            strokeWidth = baseStroke,
                            headLength = headLength,
                            headHalfWidth = headHalfWidth
                        )
                    }
                }

                // 5. Level 1 "Tap to remove" tooltip bubble + hand icon (matches Reference Image 3!)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTutorialHint && arrows.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-tutorialBob).dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.tapToRemove,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // Zoom Reset Pill when zoomed in on large labyrinth levels
                if (zoomScale > 1.05f) {
                    Surface(
                        onClick = {
                            zoomScale = 1f
                            panX = 0f
                            panY = 0f
                        },
                        shape = RoundedCornerShape(50),
                        color = theme.primaryAccent,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .testTag("reset_zoom_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOutMap,
                                contentDescription = "Reset zoom",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miniature puzzle layout snapshot rendered inside the white Level Complete showcase card
 * (matching Reference Image 1!).
 */
@Composable
fun PuzzleMiniPreview(
    level: LevelSpec,
    theme: GameThemePalette = LocalGameTheme.current,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val pad = size.minDimension * 0.08f
        val innerW = size.width - pad * 2f
        val innerH = size.height - pad * 2f
        val cw = innerW / level.cols
        val ch = innerH / level.rows
        val cellUnit = min(cw, ch)

        val dotRadius = (cellUnit * 0.07f).coerceIn(1.2f, 3f)
        for (r in 0 until level.rows) {
            for (c in 0 until level.cols) {
                drawCircle(
                    color = theme.boardDotColor,
                    radius = dotRadius,
                    center = Offset(pad + c * cw + cw * 0.5f, pad + r * ch + ch * 0.5f)
                )
            }
        }

        val stroke = (cellUnit * 0.22f).coerceIn(2.2f, 9f)
        val headLen = (cellUnit * 0.44f).coerceIn(5f, 18f)
        val headHalfW = headLen * 0.62f

        for (tile in level.arrows) {
            val pts = tile.points.map {
                Offset(pad + it.col * cw + cw * 0.5f, pad + it.row * ch + ch * 0.5f)
            }
            val color = theme.pathColorFor(tile.color, isBlockedMarked = false)
            drawSnakingArrow(
                points = pts,
                direction = tile.direction,
                color = color,
                strokeWidth = stroke,
                headLength = headLen,
                headHalfWidth = headHalfW
            )
        }
    }
}

private fun DrawScope.drawSnakingArrow(
    points: List<Offset>,
    direction: Direction,
    color: Color,
    strokeWidth: Float,
    headLength: Float,
    headHalfWidth: Float
) {
    if (points.size < 2) return

    val headCenter = points.last()
    // Tip extends slightly past the final grid center for crisp silhouette; shaft stops cleanly at triangle base
    val tip = Offset(
        x = headCenter.x + direction.dCol * (headLength * 0.45f),
        y = headCenter.y + direction.dRow * (headLength * 0.45f)
    )
    val base = Offset(
        x = tip.x - direction.dCol * headLength,
        y = tip.y - direction.dRow * headLength
    )

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            lineTo(points[i].x, points[i].y)
        }
        lineTo(base.x, base.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawTriangleHead(
        tip = tip,
        base = base,
        direction = direction,
        halfWidth = headHalfWidth,
        color = color
    )
}

private fun DrawScope.drawSlidingExitingArrow(
    points: List<Offset>,
    direction: Direction,
    progress: Float,
    boardSpan: Float,
    color: Color,
    strokeWidth: Float,
    headLength: Float,
    headHalfWidth: Float
) {
    if (points.size < 2) return

    // Build track polyline: original points + straight exit extension past the board edge
    val head = points.last()
    val exitEnd = Offset(
        x = head.x + direction.dCol * (boardSpan * 1.25f),
        y = head.y + direction.dRow * (boardSpan * 1.25f)
    )
    val track = points + exitEnd

    // Compute cumulative arc lengths along original arrow and full track
    val cumDist = FloatArray(track.size)
    for (i in 1 until track.size) {
        cumDist[i] = cumDist[i - 1] + hypot(track[i].x - track[i - 1].x, track[i].y - track[i - 1].y)
    }
    val arrowLen = cumDist[points.size - 1]
    val totalTrackLen = cumDist.last()
    val slideDist = progress * (totalTrackLen - arrowLen * 0.35f)
    val startS = slideDist.coerceIn(0f, totalTrackLen)
    val endS = (slideDist + arrowLen).coerceIn(startS, totalTrackLen)

    val subPoints = samplePolylineWindow(track, cumDist, startS, endS)
    if (subPoints.size < 2) return

    val headPt = subPoints.last()
    val neckPt = subPoints[subPoints.size - 2]
    val dx = headPt.x - neckPt.x
    val dy = headPt.y - neckPt.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len

    val tip = Offset(headPt.x + ux * (headLength * 0.45f), headPt.y + uy * (headLength * 0.45f))
    val base = Offset(tip.x - ux * headLength, tip.y - uy * headLength)

    val path = Path().apply {
        moveTo(subPoints[0].x, subPoints[0].y)
        for (i in 1 until subPoints.size - 1) {
            lineTo(subPoints[i].x, subPoints[i].y)
        }
        lineTo(base.x, base.y)
    }

    val alpha = (1f - progress * 0.55f).coerceIn(0f, 1f)
    val fadedColor = color.copy(alpha = alpha)

    drawPath(
        path = path,
        color = fadedColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val perpX = -uy
    val perpY = ux
    val tri = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + perpX * headHalfWidth, base.y + perpY * headHalfWidth)
        lineTo(base.x - perpX * headHalfWidth, base.y - perpY * headHalfWidth)
        close()
    }
    drawPath(path = tri, color = fadedColor)
}

private fun DrawScope.drawTriangleHead(
    tip: Offset,
    base: Offset,
    direction: Direction,
    halfWidth: Float,
    color: Color
) {
    val perpX = -direction.dRow.toFloat()
    val perpY = direction.dCol.toFloat()
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + perpX * halfWidth, base.y + perpY * halfWidth)
        lineTo(base.x - perpX * halfWidth, base.y - perpY * halfWidth)
        close()
    }
    drawPath(path = path, color = color)
}

private fun samplePolylineWindow(
    track: List<Offset>,
    cumDist: FloatArray,
    startS: Float,
    endS: Float
): List<Offset> {
    if (endS <= startS) return emptyList()
    val result = ArrayList<Offset>()
    result.add(interpolateAtDist(track, cumDist, startS))
    for (i in 1 until track.size - 1) {
        if (cumDist[i] > startS && cumDist[i] < endS) {
            result.add(track[i])
        }
    }
    result.add(interpolateAtDist(track, cumDist, endS))
    return result
}

private fun interpolateAtDist(track: List<Offset>, cumDist: FloatArray, s: Float): Offset {
    if (s <= 0f) return track.first()
    if (s >= cumDist.last()) return track.last()
    for (i in 1 until track.size) {
        if (s <= cumDist[i]) {
            val segLen = (cumDist[i] - cumDist[i - 1]).coerceAtLeast(1e-4f)
            val t = (s - cumDist[i - 1]) / segLen
            return Offset(
                x = track[i - 1].x + (track[i].x - track[i - 1].x) * t,
                y = track[i - 1].y + (track[i].y - track[i - 1].y) * t
            )
        }
    }
    return track.last()
}

private fun findTappedArrowId(
    tapX: Float,
    tapY: Float,
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    arrows: List<ArrowTile>
): Int? {
    if (arrows.isEmpty() || width <= 0f || height <= 0f) return null
    val pad = min(width, height) * 0.06f
    val innerW = width - pad * 2f
    val innerH = height - pad * 2f
    val cw = innerW / cols
    val ch = innerH / rows
    val maxHitDist = max(min(cw, ch) * 0.65f, 28f)

    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE

    for (tile in arrows) {
        for (i in 0 until tile.points.size - 1) {
            val p1 = tile.points[i]
            val p2 = tile.points[i + 1]
            val ax = pad + p1.col * cw + cw * 0.5f
            val ay = pad + p1.row * ch + ch * 0.5f
            val bx = pad + p2.col * cw + cw * 0.5f
            val by = pad + p2.row * ch + ch * 0.5f
            val d = pointToSegmentDistance(tapX, tapY, ax, ay, bx, by)
            if (d < bestDist) {
                bestDist = d
                bestId = tile.id
            }
        }
    }

    return if (bestDist <= maxHitDist) bestId else null
}

private fun pointToSegmentDistance(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float
): Float {
    val dx = bx - ax
    val dy = by - ay
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1e-4f) return hypot(px - ax, py - ay)
    val t = (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0f, 1f)
    val projX = ax + t * dx
    val projY = ay + t * dy
    return hypot(px - projX, py - projY)
}
