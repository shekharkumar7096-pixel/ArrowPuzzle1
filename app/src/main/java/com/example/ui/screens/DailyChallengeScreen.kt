package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameRepository
import com.example.ui.components.GlossyButton
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DailyChallengeScreen(
    appState: AppUiState,
    onBack: () -> Unit,
    onPlayDaily: (String) -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val todayKey = remember { GameRepository.todayDateKey() }

    val nowCal = remember { Calendar.getInstance() }
    var displayYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    var selectedDateKey by remember { mutableStateOf(todayKey) }

    val monthCalendar = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthTitle = remember(displayYear, displayMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCalendar.time)
    }
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0

    val selectedCompletion = appState.dailyCompletions[selectedDateKey]
    val completedThisMonth = remember(displayYear, displayMonth, appState.dailyCompletions) {
        (1..daysInMonth).count { day ->
            appState.dailyCompletions.containsKey(GameRepository.formatDateKey(displayYear, displayMonth, day))
        }
    }

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("daily_challenge_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            .testTag("daily_back_button")
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
                        text = strings.dailyChallenge,
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.onBackgroundText
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Streak + Reward Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        color = theme.cardSurface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(2.dp, theme.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${appState.dailyStreak} Days",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.onCardText
                                )
                                Text(
                                    text = "Current ${strings.streak}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        color = theme.cardSurface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(2.dp, theme.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "+100 Coins",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.onCardText
                                )
                                Text(
                                    text = "Daily Reward",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Calendar View Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = theme.cardSurface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(2.dp, theme.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (displayMonth == 0) {
                                        displayMonth = 11
                                        displayYear--
                                    } else {
                                        displayMonth--
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Previous month",
                                    tint = theme.primaryAccent
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = monthTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.onCardText
                                )
                                Text(
                                    text = "$completedThisMonth / $daysInMonth Cleared",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText
                                )
                            }

                            val isCurrentMonthOrFuture =
                                displayYear > nowCal.get(Calendar.YEAR) ||
                                    (displayYear == nowCal.get(Calendar.YEAR) && displayMonth >= nowCal.get(Calendar.MONTH))

                            IconButton(
                                enabled = !isCurrentMonthOrFuture,
                                onClick = {
                                    if (displayMonth == 11) {
                                        displayMonth = 0
                                        displayYear++
                                    } else {
                                        displayMonth++
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Next month",
                                    tint = if (isCurrentMonthOrFuture) theme.mutedCardText.copy(alpha = 0.3f) else theme.primaryAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekday headers
                        val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekDays.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Rows
                        val totalCells = firstDayOfWeekOffset + daysInMonth
                        val rowCount = (totalCells + 6) / 7
                        for (r in 0 until rowCount) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val cellIdx = r * 7 + c
                                    val dayNum = cellIdx - firstDayOfWeekOffset + 1
                                    if (dayNum !in 1..daysInMonth) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val key = GameRepository.formatDateKey(displayYear, displayMonth, dayNum)
                                        val isFuture = key > todayKey
                                        val isToday = key == todayKey
                                        val isSelected = key == selectedDateKey
                                        val isDone = appState.dailyCompletions.containsKey(key)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> theme.primaryAccent
                                                        isDone -> Color(0xFFDCFCE7)
                                                        isToday -> Color(0xFFE0F2FE)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 2.dp else 0.dp,
                                                    color = if (isToday && !isSelected) theme.primaryAccent else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable(enabled = !isFuture) {
                                                    selectedDateKey = key
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isFuture -> theme.mutedCardText.copy(alpha = 0.35f)
                                                        isDone -> Color(0xFF15803D)
                                                        else -> theme.onCardText
                                                    }
                                                )
                                                if (isDone) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Star,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFFFFEA00) else Color(0xFFF59E0B),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selected Day Status + Play Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = theme.cardSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, theme.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (selectedDateKey == todayKey) "Today's Puzzle ($selectedDateKey)" else "Archive Puzzle ($selectedDateKey)",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.onCardText
                            )
                            Text(
                                text = if (selectedCompletion != null) {
                                    "Completed • ${selectedCompletion.stars}★ • Best ${selectedCompletion.moves} moves"
                                } else {
                                    "Clear all arrows to earn +100 bonus coins!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.mutedCardText
                            )
                        }
                        if (selectedCompletion != null) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlossyButton(
                    text = if (selectedCompletion != null) "Replay Daily Puzzle" else "Play Daily Challenge",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = { onPlayDaily(selectedDateKey) },
                    topColor = Color(0xFFFFB74D),
                    bottomColor = Color(0xFFF57C00),
                    bevelColor = Color(0xFFC25E00),
                    height = 64.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "daily_play_button"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
