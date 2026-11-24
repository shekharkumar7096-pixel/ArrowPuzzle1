package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.LevelGenerator
import com.example.ui.components.CoinBadge
import com.example.ui.components.GlossyButton
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState

@Composable
fun HomeScreen(
    appState: AppUiState,
    onPlayNextLevel: () -> Unit,
    onOpenLevels: () -> Unit,
    onOpenDailyChallenge: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRewards: () -> Unit,
    onToggleSound: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val nextLevel = appState.preferences.highestUnlockedLevel
    val progressRatio = (appState.completedLevelsCount.toFloat() / LevelGenerator.TOTAL_LEVELS).coerceIn(0f, 1f)
    val hasUnclaimedReward = appState.isDailyRewardAvailableToday || appState.milestones.any { it.isReadyToClaim }

    val infiniteTransition = rememberInfiniteTransition(label = "home_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )

    val homeBg = if (theme.isDark) theme.bgBottom else Color(0xFFFDFEFF)
    val brandInk = if (theme.isDark) Color.White else Color(0xFF1F2752)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Coins, Sound Toggle, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinBadge(
                    coins = appState.preferences.coins,
                    onClick = onOpenRewards,
                    testTag = "home_coin_badge"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeIconPill(
                        icon = if (appState.preferences.soundEnabled) {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        },
                        contentDescription = stringResource(R.string.cd_sound_toggle),
                        onClick = onToggleSound,
                        testTag = "home_sound_toggle"
                    )
                    HomeIconPill(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        onClick = onOpenSettings,
                        testTag = "home_settings_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Iconic ▲rrows Wordmark Logo matching Reference Image 7
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = floatOffset.dp)
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.08f)
                        lineTo(size.width * 0.96f, size.height * 0.90f)
                        lineTo(size.width * 0.04f, size.height * 0.90f)
                        close()
                    }
                    drawPath(path = path, color = brandInk)
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "rrows",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = brandInk
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${strings.levelLabel} $nextLevel",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = theme.primaryAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Primary Royal-Blue Play Pill CTA Button (matching Reference Image 7!)
            GlossyButton(
                text = strings.play,
                icon = Icons.Rounded.PlayArrow,
                onClick = onPlayNextLevel,
                topColor = Color(0xFF667FFF),
                bottomColor = Color(0xFF4F6BF6),
                bevelColor = Color(0xFF344EDB),
                height = 66.dp,
                modifier = Modifier.fillMaxWidth(0.86f),
                testTag = "home_play_button"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Player Progress Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = theme.cardSurface,
                shadowElevation = 5.dp,
                border = BorderStroke(1.5.dp, theme.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progress: ${appState.completedLevelsCount} / ${LevelGenerator.TOTAL_LEVELS}",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.onCardText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = stringResource(R.string.cd_star_earned),
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${appState.totalStars}",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.onCardText
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio.coerceAtLeast(0.02f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = theme.primaryAccent,
                        trackColor = theme.cardBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Theme: ${theme.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.mutedCardText
                        )
                        Text(
                            text = "🔥 ${appState.dailyStreak}d ${strings.streak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFEA580C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Feature Menu Grid (Levels, Daily Challenge, Themes, Rewards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuTile(
                    title = strings.levels,
                    subtitle = "500 Puzzles",
                    icon = Icons.Rounded.GridView,
                    topColor = Color(0xFF38BDF8),
                    bottomColor = Color(0xFF0284C7),
                    onClick = onOpenLevels,
                    modifier = Modifier.weight(1f),
                    testTag = "home_levels_button"
                )
                HomeMenuTile(
                    title = strings.dailyChallenge,
                    subtitle = if (appState.isTodayDailyChallengeDone) "Completed ✓" else "Today Ready!",
                    icon = Icons.Rounded.CalendarMonth,
                    topColor = Color(0xFFFFB74D),
                    bottomColor = Color(0xFFF57C00),
                    showBadge = !appState.isTodayDailyChallengeDone,
                    onClick = onOpenDailyChallenge,
                    modifier = Modifier.weight(1f),
                    testTag = "home_daily_button"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuTile(
                    title = strings.themes,
                    subtitle = "${appState.preferences.unlockedThemeIds.size}/7 Unlocked",
                    icon = Icons.Rounded.Palette,
                    topColor = Color(0xFFC084FC),
                    bottomColor = Color(0xFF7E22CE),
                    onClick = onOpenThemes,
                    modifier = Modifier.weight(1f),
                    testTag = "home_themes_button"
                )
                HomeMenuTile(
                    title = strings.rewards,
                    subtitle = if (hasUnclaimedReward) "Claim Coins!" else "Daily & Bonus",
                    icon = Icons.Rounded.CardGiftcard,
                    topColor = Color(0xFFFF708D),
                    bottomColor = Color(0xFFE11D48),
                    showBadge = hasUnclaimedReward,
                    onClick = onOpenRewards,
                    modifier = Modifier.weight(1f),
                    testTag = "home_rewards_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeIconPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    val theme = LocalGameTheme.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = theme.cardSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.5.dp, theme.cardBorder),
        modifier = Modifier
            .testTag(testTag)
            .size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = theme.primaryAccent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    topColor: Color,
    bottomColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    testTag: String
) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = shape,
            shadowElevation = 6.dp,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .testTag(testTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                    .padding(14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEA00))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}
