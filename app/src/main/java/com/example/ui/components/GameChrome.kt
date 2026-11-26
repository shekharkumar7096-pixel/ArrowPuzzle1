package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ads.RewardedOfferType
import com.example.ui.theme.GameThemePalette
import com.example.ui.theme.LocalGameTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Radiant casual-game sunburst background matching the reference Level Completed aesthetic,
 * dynamically styled to the active unlockable theme.
 */
@Composable
fun SunburstBackground(
    modifier: Modifier = Modifier,
    theme: GameThemePalette = LocalGameTheme.current,
    animateRays: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunburst")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 52000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburst_angle"
    )
    val effectiveAngle = if (animateRays) angle else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.bgTop, theme.bgBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.42f)
            val radius = maxOf(size.width, size.height) * 1.4f
            val rayCount = 16
            val raySweep = 360f / rayCount

            rotate(degrees = effectiveAngle, pivot = center) {
                for (i in 0 until rayCount) {
                    if (i % 2 == 0) {
                        val startRad = Math.toRadians((i * raySweep).toDouble())
                        val endRad = Math.toRadians(((i + 0.52) * raySweep).toDouble())
                        val path = Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(
                                center.x + (radius * cos(startRad)).toFloat(),
                                center.y + (radius * sin(startRad)).toFloat()
                            )
                            lineTo(
                                center.x + (radius * cos(endRad)).toFloat(),
                                center.y + (radius * sin(endRad)).toFloat()
                            )
                            close()
                        }
                        drawPath(path = path, color = theme.sunburstRayColor)
                    }
                }
            }

            // Soft cartoon floating decorative orbs for playful depth
            drawCircle(
                color = Color.White.copy(alpha = if (theme.isDark) 0.05f else 0.12f),
                radius = size.width * 0.24f,
                center = Offset(size.width * 0.12f, size.height * 0.14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = if (theme.isDark) 0.04f else 0.10f),
                radius = size.width * 0.18f,
                center = Offset(size.width * 0.88f, size.height * 0.82f)
            )
        }

        content()
    }
}

/**
 * Tactile 3D glossy casual game button with specular highlight, bottom bevel lip, and spring press scale.
 */
@Composable
fun GlossyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    topColor: Color = Color(0xFF38D989),
    bottomColor: Color = Color(0xFF16A34A),
    bevelColor: Color = Color(0xFF0E7434),
    textColor: Color = Color.White,
    height: Dp = 60.dp,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "glossy_btn_scale"
    )
    val shape = RoundedCornerShape(24.dp)

    val effTop = if (enabled) topColor else Color(0xFF94A3B8)
    val effBottom = if (enabled) bottomColor else Color(0xFF64748B)
    val effBevel = if (enabled) bevelColor else Color(0xFF475569)

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .scale(scale)
            .height(height)
            .minimumInteractiveComponentSize()
            .shadow(elevation = if (pressed) 2.dp else 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(effBevel)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(bottom = if (pressed) 2.dp else 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(effTop, effBottom)))
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center
        ) {
            // Top glossy shine bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.86f)
                    .padding(top = 5.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (subtitle != null) 18.sp else 20.sp
                        ),
                        color = textColor,
                        maxLines = 1
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.88f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoinBadge(
    coins: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String = "coin_badge"
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFF8E1),
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC107))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MonetizationOn,
                contentDescription = stringResource(R.string.cd_coins),
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = coins.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF78350F)
            )
        }
    }
}

/**
 * Elevated circular action button with optional notification badge (matches Reference Image 4 bottom dock).
 */
@Composable
fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF1E88E5),
    iconTint: Color = Color(0xFF1E88E5),
    activeHighlight: Boolean = false,
    enabled: Boolean = true,
    testTag: String
) {
    val theme = LocalGameTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                color = if (activeHighlight) theme.primaryAccent else theme.cardSurface,
                shadowElevation = if (enabled) 6.dp else 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = if (activeHighlight) Color.White else theme.cardBorder
                ),
                modifier = Modifier
                    .testTag(testTag)
                    .size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = when {
                            !enabled -> theme.mutedCardText.copy(alpha = 0.45f)
                            activeHighlight -> Color.White
                            else -> iconTint
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-3).dp)
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor)
                        .border(1.5.dp, Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = theme.onBackgroundText.copy(alpha = if (enabled) 0.95f else 0.55f),
            maxLines = 1
        )
    }
}

@Composable
fun StarRatingRow(
    earnedStars: Int,
    starSize: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..3) {
            val earned = i <= earnedStars
            val scale by animateFloatAsState(
                targetValue = if (earned) 1f else 0.85f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 320f),
                label = "star_$i"
            )
            Icon(
                imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(
                    if (earned) R.string.cd_star_earned else R.string.cd_star_empty
                ),
                tint = if (earned) Color(0xFFFFC107) else Color(0xFFCBD5E1),
                modifier = Modifier
                    .size(starSize)
                    .scale(scale)
            )
        }
    }
}

private data class ConfettiPiece(
    val xFraction: Float,
    val startYOffset: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val spinSpeed: Float,
    val swayAmplitude: Float,
    val color: Color
)

/**
 * 60 FPS celebratory multicolored confetti ribbons (matches Reference Image 3 Level Complete!).
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val pieces = remember {
        val rng = Random(42)
        val colors = listOf(
            Color(0xFFFFEA00),
            Color(0xFFFF80AB),
            Color(0xFF40E0D0),
            Color(0xFF69F0AE),
            Color(0xFFFFFFFF),
            Color(0xFFFF6D00),
            Color(0xFFE040FB)
        )
        List(70) {
            ConfettiPiece(
                xFraction = rng.nextFloat(),
                startYOffset = -rng.nextFloat(),
                speed = 0.25f + rng.nextFloat() * 0.45f,
                width = 10f + rng.nextFloat() * 14f,
                height = 6f + rng.nextFloat() * 8f,
                spinSpeed = (rng.nextFloat() - 0.5f) * 360f,
                swayAmplitude = 18f + rng.nextFloat() * 30f,
                color = colors[rng.nextInt(colors.size)]
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "confetti")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (piece in pieces) {
            val cycle = (progress * (1f + piece.speed) + piece.startYOffset + 1f) % 1.25f
            val y = (cycle - 0.15f) * size.height
            val x = piece.xFraction * size.width +
                sin((progress * 2 * PI + piece.xFraction * 10).toDouble()).toFloat() * piece.swayAmplitude
            val rot = progress * piece.spinSpeed
            rotate(degrees = rot, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color,
                    topLeft = Offset(x - piece.width / 2f, y - piece.height / 2f),
                    size = Size(piece.width, piece.height)
                )
            }
        }
    }
}

/**
 * Opt-in Rewarded Ad confirmation & loading/retry dialog wired to Google Mobile Ads SDK.
 */
@Composable
fun RewardedOfferDialog(
    offer: RewardedOfferType,
    isLoading: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onWatchAd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onCancel() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = offer.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reward: ${offer.rewardSummary}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF16A34A)
                )
                Text(
                    text = when {
                        isLoading -> "Loading Rewarded Ad..."
                        errorMessage != null -> "Ad failed to load: $errorMessage"
                        else -> "Watch a short rewarded ad to unlock ${offer.rewardSummary}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMessage != null) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        },
        confirmButton = {
            GlossyButton(
                text = when {
                    isLoading -> "Loading..."
                    errorMessage != null -> "Retry Ad"
                    else -> "Watch Ad"
                },
                onClick = onWatchAd,
                enabled = !isLoading,
                height = 48.dp,
                testTag = "claim_rewarded_offer_button"
            )
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
