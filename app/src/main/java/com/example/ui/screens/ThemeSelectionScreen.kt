package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.ArrowColor
import com.example.ui.components.CoinBadge
import com.example.ui.components.GlossyButton
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.GameThemePalette
import com.example.ui.theme.GameThemes
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState

@Composable
fun ThemeSelectionScreen(
    appState: AppUiState,
    onBack: () -> Unit,
    onThemeClick: (GameThemePalette) -> Unit
) {
    val activeTheme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val coins = appState.preferences.coins
    val unlocked = appState.preferences.unlockedThemeIds

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("theme_selection_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
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
                        color = activeTheme.cardSurface,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("themes_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = activeTheme.primaryAccent
                            )
                        }
                    }

                    Text(
                        text = strings.themes,
                        style = MaterialTheme.typography.headlineMedium,
                        color = activeTheme.onBackgroundText
                    )

                    CoinBadge(coins = coins, testTag = "themes_coin_badge")
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(GameThemes.all, key = { it.id }) { palette ->
                        val isUnlocked = palette.id in unlocked
                        val isEquipped = palette.id == activeTheme.id
                        val canAfford = coins >= palette.priceCoins

                        ThemePreviewCard(
                            palette = palette,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            canAfford = canAfford,
                            onAction = { onThemeClick(palette) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    palette: GameThemePalette,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val activeTheme = LocalGameTheme.current
    val strings = LocalAppStrings.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_card_${palette.id}"),
        shape = RoundedCornerShape(24.dp),
        color = activeTheme.cardSurface,
        shadowElevation = if (isEquipped) 10.dp else 5.dp,
        border = BorderStroke(
            width = if (isEquipped) 3.dp else 1.5.dp,
            color = if (isEquipped) Color(0xFFFFD600) else activeTheme.cardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Live miniature theme gradient + 5 arrow block color swatches
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(palette.bgTop, palette.bgBottom)))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val previewColors = listOf(
                            ArrowColor.BLUE,
                            ArrowColor.GREEN,
                            ArrowColor.YELLOW,
                            ArrowColor.RED,
                            ArrowColor.PURPLE
                        )
                        previewColors.forEach { arrowColor ->
                            val set = palette.tileColorsFor(arrowColor)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(set.faceTop, set.faceBottom)))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = set.arrowInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isEquipped) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = strings.equipped,
                            tint = Color(0xFFFFEA00),
                            modifier = Modifier.size(30.dp)
                        )
                    } else if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = activeTheme.onCardText
                    )
                    Text(
                        text = palette.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = activeTheme.mutedCardText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                when {
                    isEquipped -> {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.5.dp, Color(0xFF22C55E))
                        ) {
                            Text(
                                text = strings.equipped,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    isUnlocked -> {
                        GlossyButton(
                            text = strings.select,
                            onClick = onAction,
                            topColor = Color(0xFF38BDF8),
                            bottomColor = Color(0xFF0284C7),
                            bevelColor = Color(0xFF0369A1),
                            height = 48.dp,
                            testTag = "select_theme_${palette.id}"
                        )
                    }

                    else -> {
                        GlossyButton(
                            text = "${palette.priceCoins}",
                            icon = Icons.Rounded.MonetizationOn,
                            onClick = onAction,
                            topColor = if (canAfford) Color(0xFFFFD54F) else Color(0xFF94A3B8),
                            bottomColor = if (canAfford) Color(0xFFF59E0B) else Color(0xFF64748B),
                            bevelColor = if (canAfford) Color(0xFFB45309) else Color(0xFF475569),
                            textColor = if (canAfford) Color(0xFF451A03) else Color.White,
                            height = 48.dp,
                            testTag = "unlock_theme_${palette.id}"
                        )
                    }
                }
            }
        }
    }
}
