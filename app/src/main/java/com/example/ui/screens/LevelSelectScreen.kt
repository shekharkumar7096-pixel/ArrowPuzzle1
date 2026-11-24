package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LevelProgressEntity
import com.example.game.DifficultyTier
import com.example.game.LevelGenerator
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState

@Composable
fun LevelSelectScreen(
    appState: AppUiState,
    onBack: () -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val highestUnlocked = appState.preferences.highestUnlockedLevel

    var selectedTier by remember {
        mutableStateOf<DifficultyTier?>(DifficultyTier.forLevel(highestUnlocked))
    }

    val levelsToDisplay = remember(selectedTier) {
        val range = selectedTier?.levelRange ?: (1..LevelGenerator.TOTAL_LEVELS)
        range.toList()
    }

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = theme.cardSurface,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("levels_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = theme.primaryAccent
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.levels,
                            style = MaterialTheme.typography.headlineMedium,
                            color = theme.onBackgroundText
                        )
                        Text(
                            text = "${appState.completedLevelsCount} / ${LevelGenerator.TOTAL_LEVELS} Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.onBackgroundText.copy(alpha = 0.88f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = theme.cardSurface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = stringResource(R.string.cd_star_earned),
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${appState.totalStars}",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.onCardText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Difficulty tier chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTier == null,
                            onClick = { selectedTier = null },
                            label = { Text("All (1–500)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color(0xFF1F2937),
                                containerColor = theme.cardSurface.copy(alpha = 0.9f),
                                labelColor = theme.onCardText
                            ),
                            modifier = Modifier.testTag("tier_chip_all")
                        )
                    }
                    items(DifficultyTier.entries) { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            label = {
                                Text("${tier.displayName} (${tier.levelRange.first}–${tier.levelRange.last})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color(0xFF1F2937),
                                containerColor = theme.cardSurface.copy(alpha = 0.9f),
                                labelColor = theme.onCardText
                            ),
                            modifier = Modifier.testTag("tier_chip_${tier.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Virtualized Level Grid
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = theme.cardSurface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(2.dp, theme.cardBorder)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(levelsToDisplay, key = { it }) { levelNumber ->
                            val progress = appState.levelProgress[levelNumber]
                            val isUnlocked = levelNumber <= highestUnlocked
                            val isCurrent = levelNumber == highestUnlocked
                            LevelCellTile(
                                levelNumber = levelNumber,
                                progress = progress,
                                isUnlocked = isUnlocked,
                                isCurrent = isCurrent,
                                onClick = {
                                    if (isUnlocked) onSelectLevel(levelNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCellTile(
    levelNumber: Int,
    progress: LevelProgressEntity?,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(18.dp)

    val bgBrush = when {
        isCurrent -> Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A)))
        progress?.isCompleted == true -> Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
        isUnlocked -> Brush.verticalGradient(listOf(theme.primaryAccent, theme.primaryAccentDark))
        else -> Brush.verticalGradient(
            listOf(
                theme.boardSurface,
                theme.cardBorder.copy(alpha = 0.65f)
            )
        )
    }

    val borderColor = when {
        isCurrent -> Color(0xFFFFEA00)
        isUnlocked -> Color.White.copy(alpha = 0.45f)
        else -> theme.cardBorder
    }

    Surface(
        shape = shape,
        shadowElevation = if (isUnlocked) 5.dp else 0.dp,
        border = BorderStroke(if (isCurrent) 2.5.dp else 1.5.dp, borderColor),
        modifier = Modifier
            .aspectRatio(0.95f)
            .testTag("level_tile_$levelNumber")
            .clickable(enabled = isUnlocked, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.cd_level_locked),
                        tint = theme.mutedCardText.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$levelNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.mutedCardText.copy(alpha = 0.75f)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$levelNumber",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (levelNumber >= 100) 17.sp else 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        val earned = progress?.stars ?: 0
                        for (s in 1..3) {
                            Icon(
                                imageVector = if (s <= earned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = if (s <= earned) Color(0xFFFFEA00) else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
