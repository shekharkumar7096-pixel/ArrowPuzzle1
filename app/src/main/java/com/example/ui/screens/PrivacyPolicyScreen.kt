package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalGameTheme

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val theme = LocalGameTheme.current
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    SunburstBackground(animateRays = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("privacy_policy_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                            .testTag("privacy_back_button")
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
                        text = strings.privacyPolicy,
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.onBackgroundText
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = theme.cardSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, theme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Policy,
                                contentDescription = null,
                                tint = theme.primaryAccent,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Arrow Puzzle Privacy Policy",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.onCardText
                                )
                                Text(
                                    text = "Effective Date: November 25, 2026 • Package: com.aistudio.arrowpuzzle.krmqvx",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.mutedCardText
                                )
                            }
                        }

                        HorizontalDivider(color = theme.cardBorder.copy(alpha = 0.7f))

                        PolicySection(
                            title = "1. Overview & Local Game Data",
                            body = "Arrow Puzzle is an offline-friendly puzzle game. We do not require account registration and we do not operate developer-owned servers to collect your name, email address, contacts, photos, or precise location.\n\n" +
                                "Your game progress (completed levels, stars earned, daily challenges, virtual coins, free hints, unlocked themes, and sound/language settings) is stored locally on your device using Android Room and Jetpack DataStore."
                        )

                        PolicySection(
                            title = "2. Advertising (Google AdMob)",
                            body = "Arrow Puzzle uses Google AdMob (Google Mobile Ads SDK) to display Banner Ads on the Home Screen, optional Rewarded Ads (for extra hearts, hints, or bonus coins), and paced Interstitial Ads after completing levels.\n\n" +
                                "To deliver and measure ads, Google AdMob may automatically collect device identifiers (such as the Android Advertising ID / AD_ID), IP address (for coarse region/fraud prevention), app interactions, and crash/diagnostic metrics in accordance with Google's Privacy Policy."
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { openUrl("https://policies.google.com/privacy") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("privacy_google_policy_button")
                            ) {
                                Text("Google Privacy", maxLines = 1)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { openUrl("https://myadcenter.google.com/") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("privacy_ad_settings_button")
                            ) {
                                Text("Ad Controls", maxLines = 1)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        PolicySection(
                            title = "3. Device Permissions",
                            body = "• INTERNET & ACCESS_NETWORK_STATE: Used solely to load and display Google AdMob ads when online.\n" +
                                "• VIBRATE: Used for optional tactile haptic feedback during puzzle play (can be turned off in Settings).\n" +
                                "• AD_ID: Used by Google Mobile Ads SDK in compliance with Google Play policies; you can reset or delete your Advertising ID anytime in Android Settings > Privacy > Ads."
                        )

                        PolicySection(
                            title = "4. Data Retention & Deletion",
                            body = "Because your game save data lives exclusively on your device, you can erase all stored progress at any time using 'Reset Progress' in Settings or by uninstalling Arrow Puzzle. Any diagnostic or ad-measurement data processed by Google AdMob is governed by Google's retention policies."
                        )

                        PolicySection(
                            title = "5. Children's Privacy",
                            body = "Arrow Puzzle does not knowingly collect personal information from children under 13 (or the applicable age of consent in your region). If you believe personal data has been inadvertently shared, please contact us so we can assist immediately."
                        )

                        PolicySection(
                            title = "6. Contact Us",
                            body = "For any privacy questions, Google Play Data Safety inquiries, or support requests regarding Arrow Puzzle, please contact:\nshekharkumar7096@gmail.com"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    val theme = LocalGameTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = theme.primaryAccent
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.onCardText
        )
    }
}
