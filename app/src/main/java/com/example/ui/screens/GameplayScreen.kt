package com.example.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.GameRepository
import com.example.ui.components.CircleActionButton
import com.example.ui.components.CoinBadge
import com.example.ui.components.GlossyButton
import com.example.ui.components.InteractivePuzzleBoard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState
import com.example.viewmodel.GameplayUiState

@Composable
fun GameplayScreen(
    appState: AppUiState,
    gameState: GameplayUiState,
    onArrowTap: (Int) -> Unit,
    onHintClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRestartClick: () -> Unit,
    onTogglePathGuide: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit,
    onWatchHintOffer: () -> Unit,
    onWatchReviveOffer: () -> Unit,
    onDismissLowCoins: () -> Unit,
    onRefillHearts: () -> Unit,
    onGoHome: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val freeHints = appState.preferences.freeHints

    val titleText = if (gameState.level.isDaily) {
        strings.dailyChallenge
    } else {
        "${strings.levelLabel} ${gameState.level.levelNumber}"
    }

    val totalArrows = gameState.level.arrows.size.coerceAtLeast(1)
    val clearedProgress = ((totalArrows - gameState.remainingCount).toFloat() / totalArrows).coerceIn(0f, 1f)

    // Clean full-bleed canvas matching Reference Images 3, 4, 5, 6, 8
    val screenBg = if (theme.isDark) theme.bgBottom else Color(0xFFFDFEFF)
    val pillBg = if (theme.isDark) theme.cardSurface else Color(0xFFF1F5F9)
    val labelInk = theme.onCardText

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar (Back chevron, centered Level X, Coins + Pause/Settings gear)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onGoHome,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gameplay_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = labelInk,
                        modifier = Modifier.testTag("gameplay_level_title")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CoinBadge(
                            coins = appState.preferences.coins,
                            testTag = "gameplay_coin_badge"
                        )
                        IconButton(
                            onClick = { onPauseToggle(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gameplay_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_pause),
                                tint = theme.primaryAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sub-HUD Row matching Reference Image 4: Remaining Arrows Pill, 3 Hearts, Difficulty Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pillBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("gameplay_remaining_pill")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NearMe,
                                contentDescription = null,
                                tint = labelInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${gameState.remainingCount}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = labelInk
                            )
                        }
                    }

                    // 3 Hearts Indicator matching Reference Images 4, 5, 6, 8
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("gameplay_star_hearts")
                    ) {
                        for (i in 1..3) {
                            val alive = i <= gameState.heartsRemaining
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = if (alive) Color(0xFFFF3B5C) else Color(0xFFC5CBE3),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = gameState.level.difficulty.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = theme.mutedCardText,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("gameplay_moves_pill")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtle thin progress bar (matches Reference Image 6)
                LinearProgressIndicator(
                    progress = { clearedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = theme.primaryAccent,
                    trackColor = theme.cardBorder.copy(alpha = 0.5f)
                )

                // Dynamic Blocked-Move Explanation Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = gameState.blockedFeedback != null,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        val msg = gameState.blockedFeedback?.message.orEmpty()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                            modifier = Modifier.testTag("blocked_feedback_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFB71C1C),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Center Interactive Snaking-Arrow Puzzle Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                InteractivePuzzleBoard(
                    level = gameState.level,
                    arrows = gameState.arrows,
                    exitingArrows = gameState.exitingArrows,
                    blockedArrowIds = gameState.blockedArrowIds,
                    hintedArrowId = gameState.hintedArrowId,
                    blockedFeedback = gameState.blockedFeedback,
                    showPathGuide = gameState.showPathGuide,
                    showTutorialHint = gameState.level.levelNumber == 1 && !gameState.level.isDaily && gameState.movesCount == 0,
                    onArrowTap = onArrowTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .testTag("puzzle_board")
                )
            }

            // Bottom Elevated Circular Action Dock (Lightbulb Hint + Ruler Guide + Undo + Restart)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameplayDockButton(
                    icon = Icons.Rounded.Lightbulb,
                    contentDescription = stringResource(R.string.cd_hint),
                    label = strings.hint,
                    badgeText = if (freeHints > 0) "$freeHints" else "${GameRepository.HINT_COST_COINS}",
                    badgeColor = if (freeHints > 0) Color(0xFF2196F3) else Color(0xFFF59E0B),
                    iconTint = Color(0xFFF59E0B),
                    onClick = onHintClick,
                    testTag = "gameplay_hint_button"
                )

                GameplayDockButton(
                    icon = Icons.Rounded.Straighten,
                    contentDescription = stringResource(R.string.cd_guide),
                    label = strings.guide,
                    iconTint = Color(0xFF5C6BC0),
                    activeHighlight = gameState.showPathGuide,
                    onClick = onTogglePathGuide,
                    testTag = "gameplay_guide_button"
                )

                GameplayDockButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.cd_undo),
                    label = strings.undo,
                    badgeText = if (gameState.undoStack.isNotEmpty()) "${gameState.undoStack.size}" else null,
                    iconTint = Color(0xFF1E88E5),
                    enabled = gameState.canUndo,
                    onClick = onUndoClick,
                    testTag = "gameplay_undo_button"
                )

                GameplayDockButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.cd_restart),
                    label = strings.restart,
                    iconTint = Color(0xFFEF4444),
                    onClick = onRestartClick,
                    testTag = "gameplay_restart_button"
                )
            }
        }
    }

    // Screen 6: Pause Menu Overlay
    if (gameState.isPaused) {
        PauseMenuOverlay(
            soundEnabled = appState.preferences.soundEnabled,
            musicEnabled = appState.preferences.musicEnabled,
            vibrationEnabled = appState.preferences.vibrationEnabled,
            onResume = { onPauseToggle(false) },
            onRestart = onRestartClick,
            onHome = onGoHome,
            onToggleSound = onToggleSound,
            onToggleMusic = onToggleMusic,
            onToggleVibration = onToggleVibration
        )
    }

    // Out of Hearts Recovery Dialog (Watch Ad, Refill with 50 Coins, or Restart)
    if (gameState.isOutOfHearts) {
        AlertDialog(
            onDismissRequest = onRestartClick,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF3B5C),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Out of Hearts!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You tapped 3 blocked arrows. Watch a short ad for +3 Hearts, refill for 50 coins, or restart the level."
                    )
                    GlossyButton(
                        text = "Watch Ad (+3 Hearts)",
                        onClick = onWatchReviveOffer,
                        topColor = Color(0xFF38BDF8),
                        bottomColor = Color(0xFF0284C7),
                        bevelColor = Color(0xFF0369A1),
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "out_of_hearts_watch_ad_button"
                    )
                }
            },
            confirmButton = {
                GlossyButton(
                    text = "Refill (50 Coins)",
                    onClick = onRefillHearts,
                    enabled = appState.preferences.coins >= 50,
                    height = 46.dp,
                    testTag = "refill_hearts_button"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = onRestartClick,
                    modifier = Modifier.testTag("out_of_hearts_restart_button")
                ) {
                    Text("Restart Level")
                }
            }
        )
    }

    // Insufficient Coins for Hint Offer Dialog
    if (gameState.showInsufficientCoinsDialog) {
        AlertDialog(
            onDismissRequest = onDismissLowCoins,
            title = { Text("Need More Coins for a Hint") },
            text = {
                Text(
                    "A hint costs ${GameRepository.HINT_COST_COINS} coins. You currently have ${appState.preferences.coins} coins. Watch an optional reward offer to get +1 Free Hint right now!"
                )
            },
            confirmButton = {
                GlossyButton(
                    text = "Get +1 Free Hint",
                    onClick = onWatchHintOffer,
                    height = 48.dp,
                    testTag = "low_coins_watch_offer_button"
                )
            },
            dismissButton = {
                TextButton(onClick = onDismissLowCoins) {
                    Text("Not Now")
                }
            }
        )
    }
}

@Composable
private fun GameplayDockButton(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF2196F3),
    iconTint: Color = Color(0xFF1E88E5),
    activeHighlight: Boolean = false,
    enabled: Boolean = true,
    testTag: String
) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleActionButton(
            icon = icon,
            contentDescription = contentDescription,
            label = "",
            onClick = onClick,
            badgeText = badgeText,
            badgeColor = badgeColor,
            iconTint = iconTint,
            activeHighlight = activeHighlight,
            enabled = enabled,
            testTag = testTag
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = theme.mutedCardText.copy(alpha = if (enabled) 1f else 0.45f),
            maxLines = 1
        )
    }
}

/**
 * Screen 6: Stylized Pause Menu Overlay with Resume, Restart, Home, and Sound/Music/Vibration settings.
 */
@Composable
fun PauseMenuOverlay(
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    vibrationEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current

    Dialog(
        onDismissRequest = onResume,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .testTag("pause_menu_dialog"),
                shape = RoundedCornerShape(30.dp),
                color = theme.cardSurface,
                shadowElevation = 18.dp,
                border = BorderStroke(3.dp, theme.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.paused.uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = theme.onCardText,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = theme.boardSurface,
                        border = BorderStroke(1.5.dp, theme.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            PauseToggleRow(
                                icon = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                label = strings.soundEffects,
                                checked = soundEnabled,
                                onToggle = onToggleSound
                            )
                            PauseToggleRow(
                                icon = if (musicEnabled) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                                label = strings.backgroundMusic,
                                checked = musicEnabled,
                                onToggle = onToggleMusic
                            )
                            PauseToggleRow(
                                icon = Icons.Rounded.Vibration,
                                label = strings.vibration,
                                checked = vibrationEnabled,
                                onToggle = onToggleVibration
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    GlossyButton(
                        text = strings.resume,
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onResume,
                        topColor = Color(0xFF4ADE80),
                        bottomColor = Color(0xFF16A34A),
                        bevelColor = Color(0xFF15803D),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "pause_resume_button"
                    )

                    GlossyButton(
                        text = strings.restart,
                        icon = Icons.Rounded.Refresh,
                        onClick = onRestart,
                        topColor = Color(0xFFFFB74D),
                        bottomColor = Color(0xFFF57C00),
                        bevelColor = Color(0xFFC25E00),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "pause_restart_button"
                    )

                    GlossyButton(
                        text = strings.home,
                        icon = Icons.Rounded.Home,
                        onClick = onHome,
                        topColor = Color(0xFF60A5FA),
                        bottomColor = Color(0xFF2563EB),
                        bevelColor = Color(0xFF1D4ED8),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "pause_home_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun PauseToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.primaryAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = theme.onCardText
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
