package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameRepository
import com.example.data.MilestoneDefinition
import com.example.data.ads.RewardedOfferType
import com.example.ui.components.CoinBadge
import com.example.ui.components.GlossyButton
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState

@Composable
fun RewardScreen(
    appState: AppUiState,
    onBack: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onStartRewardedOffer: (RewardedOfferType) -> Unit,
    onClaimMilestone: (MilestoneDefinition) -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val prefs = appState.preferences
    val todayAvailable = appState.isDailyRewardAvailableToday
    val activeDayIdx = if (todayAvailable) {
        prefs.dailyRewardStreak % GameRepository.DAILY_REWARD_LADDER.size
    } else {
        ((prefs.dailyRewardStreak - 1).coerceAtLeast(0)) % GameRepository.DAILY_REWARD_LADDER.size
    }

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("reward_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .testTag("rewards_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = theme.primaryAccent
                            )
                        }
                    }

                    Text(
                        text = strings.rewards,
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.onBackgroundText
                    )

                    CoinBadge(coins = prefs.coins, testTag = "rewards_coin_badge")
                }

                // 1. Daily Login Coin Reward Ladder
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = theme.cardSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Daily Coin Gift",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = theme.onCardText
                                    )
                                    Text(
                                        text = "Come back daily to grow your bonus!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.mutedCardText
                                    )
                                }
                            }
                            Text(
                                text = "Day ${prefs.dailyRewardStreak.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryAccent
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GameRepository.DAILY_REWARD_LADDER.forEachIndexed { idx, amount ->
                                val isCurrent = idx == activeDayIdx
                                val isPast = idx < activeDayIdx || (idx == activeDayIdx && !todayAvailable)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isCurrent && todayAvailable -> Color(0xFFFFF8E1)
                                        isPast -> Color(0xFFDCFCE7)
                                        else -> theme.boardSurface
                                    },
                                    border = BorderStroke(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = when {
                                            isCurrent -> Color(0xFFF59E0B)
                                            isPast -> Color(0xFF22C55E)
                                            else -> theme.cardBorder
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "D${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                            color = theme.mutedCardText
                                        )
                                        Text(
                                            text = "$amount",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isPast) Color(0xFF15803D) else theme.onCardText
                                        )
                                    }
                                }
                            }
                        }

                        val nextAmount = GameRepository.DAILY_REWARD_LADDER[activeDayIdx]
                        GlossyButton(
                            text = if (todayAvailable) "${strings.claimReward} (+$nextAmount Coins)" else "Come Back Tomorrow ✓",
                            icon = if (todayAvailable) Icons.Rounded.MonetizationOn else Icons.Rounded.CheckCircle,
                            enabled = todayAvailable,
                            onClick = onClaimDailyReward,
                            topColor = Color(0xFFFFD54F),
                            bottomColor = Color(0xFFF59E0B),
                            bevelColor = Color(0xFFB45309),
                            textColor = if (todayAvailable) Color(0xFF451A03) else Color.White,
                            height = 56.dp,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "claim_daily_reward_button"
                        )
                    }
                }

                // 2. Optional Rewarded Ad Offers (AdMob-Ready, strictly opt-in)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = theme.cardSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = theme.primaryAccent,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Optional Bonus Offers",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.onCardText
                                )
                                Text(
                                    text = "Free Hints Held: ${prefs.freeHints} • Always optional",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlossyButton(
                                text = "+1 Hint",
                                subtitle = "Free Offer",
                                icon = Icons.Rounded.Lightbulb,
                                onClick = { onStartRewardedOffer(RewardedOfferType.EXTRA_HINT) },
                                topColor = Color(0xFF38BDF8),
                                bottomColor = Color(0xFF0284C7),
                                bevelColor = Color(0xFF0369A1),
                                height = 62.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "reward_extra_hint_button"
                            )
                            GlossyButton(
                                text = "+100 Coins",
                                subtitle = "Bonus Offer",
                                icon = Icons.Rounded.MonetizationOn,
                                onClick = { onStartRewardedOffer(RewardedOfferType.BONUS_COINS) },
                                topColor = Color(0xFF4ADE80),
                                bottomColor = Color(0xFF16A34A),
                                bevelColor = Color(0xFF15803D),
                                height = 62.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "reward_bonus_coins_button"
                            )
                        }
                    }
                }

                // 3. Achievement Milestones
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = theme.cardSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Puzzle Milestones",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.onCardText
                        )

                        appState.milestones.forEach { milestone ->
                            MilestoneRow(
                                milestone = milestone,
                                onClaim = { onClaimMilestone(milestone) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MilestoneRow(
    milestone: MilestoneDefinition,
    onClaim: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val ratio = (milestone.currentValue.toFloat() / milestone.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.boardSurface,
        border = BorderStroke(1.5.dp, theme.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.onCardText
                )
                Text(
                    text = "${milestone.subtitle} (${milestone.currentValue.coerceAtMost(milestone.targetValue)}/${milestone.targetValue})",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.mutedCardText
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.cardBorder),
                    color = if (milestone.isClaimed) Color(0xFF16A34A) else theme.primaryAccent,
                    trackColor = theme.cardBorder
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            when {
                milestone.isClaimed -> {
                    Text(
                        text = "${strings.claimed} ✓",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF16A34A)
                    )
                }

                milestone.isReadyToClaim -> {
                    GlossyButton(
                        text = "+${milestone.rewardCoins}",
                        icon = Icons.Rounded.MonetizationOn,
                        onClick = onClaim,
                        topColor = Color(0xFFFFD54F),
                        bottomColor = Color(0xFFF59E0B),
                        bevelColor = Color(0xFFB45309),
                        textColor = Color(0xFF451A03),
                        height = 46.dp,
                        testTag = "claim_milestone_${milestone.id}"
                    )
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = theme.cardSurface,
                        border = BorderStroke(1.dp, theme.cardBorder)
                    ) {
                        Text(
                            text = "+${milestone.rewardCoins} 🪙",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.mutedCardText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
