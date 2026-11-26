package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlossyButton
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.AppStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme
import com.example.viewmodel.AppUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    appState: AppUiState,
    adStatusDescription: String,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleVibration: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onResetProgress: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val prefs = appState.preferences

    var showTermsDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("settings_screen"),
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
                            .testTag("settings_back_button")
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
                        text = strings.settings,
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.onBackgroundText
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Audio & Haptics Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audio & Feedback",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SettingsSwitchRow(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            title = strings.soundEffects,
                            subtitle = "Arrow slide, hint, and victory SFX",
                            checked = prefs.soundEnabled,
                            onCheckedChange = { onToggleSound() },
                            testTag = "settings_sound_switch"
                        )
                        HorizontalDivider(color = theme.cardBorder.copy(alpha = 0.6f))
                        SettingsSwitchRow(
                            icon = Icons.Rounded.MusicNote,
                            title = strings.backgroundMusic,
                            subtitle = "Calm offline puzzle melody",
                            checked = prefs.musicEnabled,
                            onCheckedChange = { onToggleMusic() },
                            testTag = "settings_music_switch"
                        )
                        HorizontalDivider(color = theme.cardBorder.copy(alpha = 0.6f))
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Vibration,
                            title = strings.vibration,
                            subtitle = "Tactile haptic tap feedback",
                            checked = prefs.vibrationEnabled,
                            onCheckedChange = { onToggleVibration() },
                            testTag = "settings_vibration_switch"
                        )
                    }
                }

                // Language Selection Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = theme.primaryAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.language,
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppStrings.supportedLanguages.forEach { option ->
                                FilterChip(
                                    selected = prefs.languageCode == option.code,
                                    onClick = { onSelectLanguage(option.code) },
                                    label = { Text(option.nativeName) },
                                    modifier = Modifier.testTag("lang_chip_${option.code}")
                                )
                            }
                        }
                    }
                }

                // Monetization & AdMob Configuration Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                tint = theme.primaryAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AdMob & Fair Play Policy",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.primaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = adStatusDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.onCardText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No ads ever appear during active puzzle gameplay. All rewarded offers are strictly optional.",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.mutedCardText
                        )
                    }
                }

                // Legal Documents Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Legal",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SettingsLinkRow(
                            icon = Icons.Rounded.Policy,
                            title = strings.privacyPolicy,
                            onClick = onOpenPrivacyPolicy,
                            testTag = "settings_privacy_button"
                        )
                        HorizontalDivider(color = theme.cardBorder.copy(alpha = 0.6f))
                        SettingsLinkRow(
                            icon = Icons.Rounded.Description,
                            title = strings.termsAndConditions,
                            onClick = { showTermsDialog = true },
                            testTag = "settings_terms_button"
                        )
                    }
                }

                // Reset Progress Button
                GlossyButton(
                    text = strings.resetProgress,
                    icon = Icons.Rounded.DeleteForever,
                    onClick = { showResetConfirm = true },
                    topColor = Color(0xFFFF6B6B),
                    bottomColor = Color(0xFFDC2626),
                    bevelColor = Color(0xFF991B1B),
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "settings_reset_button"
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(strings.termsAndConditions) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Welcome to Arrow Puzzle!\n\n" +
                            "1. Gameplay & Virtual Coins: Coins and free hints earned in Arrow Puzzle are virtual in-game items with no real-world monetary value and cannot be transferred or redeemed for cash.\n\n" +
                            "2. Local Storage: Progress is saved locally on your device. Resetting progress or uninstalling the application removes local save data.\n\n" +
                            "3. Fair Use: All unlimited puzzle levels and daily challenges are deterministically verified to be solvable for personal, non-commercial entertainment.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Accept & Close") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Progress?") },
            text = {
                Text("This will reset unlocked levels back to Level 1, clear stars and daily challenge history, reset coins to 150, and re-lock purchased themes. Audio and language settings will be kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetProgress()
                    },
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.primaryAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = theme.onCardText)
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = theme.mutedCardText)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.primaryAccent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = theme.onCardText
        )
    }
}
