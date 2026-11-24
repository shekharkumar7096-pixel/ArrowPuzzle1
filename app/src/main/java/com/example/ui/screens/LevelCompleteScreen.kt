package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.LevelCompletionSummary
import com.example.game.LevelGenerator
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.GlossyButton
import com.example.ui.components.PuzzleMiniPreview
import com.example.ui.components.StarRatingRow
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme

/**
 * Screen 5: Celebratory Level Complete screen inspired directly by Reference Image 3:
 * Vibrant rotating sunburst rays + falling multicolored confetti + crisp white puzzle showcase card
 * + 3-star reward burst + Score & Coins Earned + optional 2x Bonus + Next Level / Replay / Home!
 */
@Composable
fun LevelCompleteScreen(
    summary: LevelCompletionSummary,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onHome: () -> Unit,
    onDoubleCoinsOffer: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val cardScale = remember { Animatable(0.72f) }

    LaunchedEffect(summary) {
        cardScale.snapTo(0.72f)
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val nextLabel = when {
        summary.isDaily -> "Play Main Levels"
        summary.levelNumber >= LevelGenerator.TOTAL_LEVELS -> "Replay Expert 500"
        else -> "${strings.levelLabel} ${summary.levelNumber + 1}"
    }

    SunburstBackground {
        ConfettiOverlay()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("level_complete_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = strings.levelCompleted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Centered White Showcase Card (matches Reference Image 3)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(cardScale.value),
                    shape = RoundedCornerShape(30.dp),
                    color = theme.cardSurface,
                    shadowElevation = 18.dp,
                    border = BorderStroke(3.dp, Color.White.copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StarRatingRow(
                            earnedStars = summary.stars,
                            starSize = 46.dp,
                            modifier = Modifier.testTag("complete_stars_row")
                        )

                        // Completed puzzle layout miniature
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = theme.boardSurface,
                            border = BorderStroke(2.dp, theme.cardBorder),
                            modifier = Modifier.size(168.dp)
                        ) {
                            PuzzleMiniPreview(
                                level = summary.initialLevel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }

                        // Score & Coins Earned stat chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatRewardCard(
                                label = strings.score,
                                value = "${summary.score}",
                                icon = Icons.Rounded.EmojiEvents,
                                iconTint = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                            StatRewardCard(
                                label = strings.coinsEarned,
                                value = "+${summary.coinsEarned}",
                                icon = Icons.Rounded.MonetizationOn,
                                iconTint = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "${strings.moves}: ${summary.moves}  •  Mistakes: ${summary.mistakes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.mutedCardText
                        )

                        // Optional 2x Bonus Coins rewarded offer (never forced!)
                        if (!summary.bonusClaimed) {
                            GlossyButton(
                                text = "2x Bonus Coins (+${summary.coinsEarned})",
                                icon = Icons.Rounded.AutoAwesome,
                                onClick = onDoubleCoinsOffer,
                                topColor = Color(0xFFFFD54F),
                                bottomColor = Color(0xFFF59E0B),
                                bevelColor = Color(0xFFB45309),
                                textColor = Color(0xFF451A03),
                                height = 50.dp,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "complete_double_coins_button"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Primary Next Level CTA Button
                GlossyButton(
                    text = strings.nextLevel,
                    subtitle = nextLabel,
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onNextLevel,
                    topColor = Color(0xFF4ADE80),
                    bottomColor = Color(0xFF16A34A),
                    bevelColor = Color(0xFF15803D),
                    height = 66.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "complete_next_level_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Replay & Home secondary buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlossyButton(
                        text = strings.replay,
                        icon = Icons.Rounded.Refresh,
                        onClick = onReplay,
                        topColor = Color(0xFF60A5FA),
                        bottomColor = Color(0xFF2563EB),
                        bevelColor = Color(0xFF1D4ED8),
                        height = 54.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "complete_replay_button"
                    )
                    GlossyButton(
                        text = strings.home,
                        icon = Icons.Rounded.Home,
                        onClick = onHome,
                        topColor = Color(0xFFA78BFA),
                        bottomColor = Color(0xFF7C3AED),
                        bevelColor = Color(0xFF5B21B6),
                        height = 54.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "complete_home_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRewardCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.boardSurface,
        border = BorderStroke(1.5.dp, theme.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.onCardText
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = theme.mutedCardText
            )
        }
    }
}
