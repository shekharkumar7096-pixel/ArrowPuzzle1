package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.ads.RewardedOfferType
import com.example.game.LevelGenerator
import com.example.ui.components.RewardedOfferDialog
import com.example.ui.screens.DailyChallengeScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelCompleteScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.RewardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ThemeSelectionScreen
import com.example.ui.theme.ArrowPuzzleTheme
import com.example.viewmodel.ArrowPuzzleViewModel
import com.example.viewmodel.GameNavEvent
import com.example.viewmodel.RewardedAdUiState

object Routes {
    const val ROUTE_SPLASH = "splash"
    const val ROUTE_HOME = "home"
    const val ROUTE_LEVEL_SELECT = "level_select"
    const val ROUTE_GAMEPLAY = "gameplay"
    const val ROUTE_LEVEL_COMPLETE = "level_complete"
    const val ROUTE_DAILY_CHALLENGE = "daily_challenge"
    const val ROUTE_THEMES = "themes"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_PRIVACY_POLICY = "privacy_policy"
    const val ROUTE_REWARDS = "rewards"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ArrowPuzzleApp(
    viewModel: ArrowPuzzleViewModel,
    startDestination: String = Routes.ROUTE_SPLASH
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val gameplayState by viewModel.gameplayState.collectAsStateWithLifecycle()
    val lastCompletion by viewModel.lastCompletion.collectAsStateWithLifecycle()
    val activeOffer by viewModel.activeRewardOffer.collectAsStateWithLifecycle()
    val rewardedAdState by viewModel.rewardedAdState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is GameNavEvent.OpenLevelComplete -> {
                    val navigateToComplete = {
                        navController.navigate(Routes.ROUTE_LEVEL_COMPLETE) {
                            popUpTo(Routes.ROUTE_GAMEPLAY) { inclusive = true }
                        }
                    }
                    if (event.showInterstitial && hostActivity != null) {
                        viewModel.adController.showWinInterstitialIfReady(hostActivity) {
                            navigateToComplete()
                        }
                    } else {
                        navigateToComplete()
                    }
                }

                is GameNavEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    ArrowPuzzleTheme(
        gameTheme = appState.activeTheme,
        languageCode = appState.preferences.languageCode
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Routes.ROUTE_SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(Routes.ROUTE_HOME) {
                                popUpTo(Routes.ROUTE_SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.ROUTE_HOME) {
                    HomeScreen(
                        appState = appState,
                        bannerAdUnitId = viewModel.repository.adManager.effectiveBannerUnitId(),
                        onPlayNextLevel = {
                            viewModel.soundManager.playButtonClick()
                            viewModel.startLevel(appState.preferences.highestUnlockedLevel)
                            navController.navigate(Routes.ROUTE_GAMEPLAY)
                        },
                        onOpenLevels = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_LEVEL_SELECT)
                        },
                        onOpenDailyChallenge = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_DAILY_CHALLENGE)
                        },
                        onOpenThemes = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_THEMES)
                        },
                        onOpenSettings = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_SETTINGS)
                        },
                        onOpenRewards = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_REWARDS)
                        },
                        onToggleSound = viewModel::toggleSound
                    )
                }

                composable(Routes.ROUTE_LEVEL_SELECT) {
                    LevelSelectScreen(
                        appState = appState,
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        },
                        onSelectLevel = { levelNumber ->
                            viewModel.soundManager.playButtonClick()
                            viewModel.startLevel(levelNumber)
                            navController.navigate(Routes.ROUTE_GAMEPLAY)
                        }
                    )
                }

                composable(Routes.ROUTE_GAMEPLAY) {
                    GameplayScreen(
                        appState = appState,
                        gameState = gameplayState,
                        onArrowTap = viewModel::onArrowTapped,
                        onHintClick = viewModel::onHintClicked,
                        onUndoClick = viewModel::onUndoClicked,
                        onRestartClick = viewModel::onRestartClicked,
                        onTogglePathGuide = viewModel::togglePathGuide,
                        onPauseToggle = viewModel::setPaused,
                        onToggleSound = viewModel::toggleSound,
                        onToggleMusic = viewModel::toggleMusic,
                        onToggleVibration = viewModel::toggleVibration,
                        onWatchHintOffer = {
                            viewModel.startRewardedOffer(RewardedOfferType.EXTRA_HINT)
                        },
                        onWatchReviveOffer = {
                            viewModel.startRewardedOffer(RewardedOfferType.REVIVE_HEARTS)
                        },
                        onDismissLowCoins = viewModel::dismissInsufficientCoinsDialog,
                        onRefillHearts = viewModel::refillHeartsWithCoins,
                        onGoHome = {
                            viewModel.soundManager.playButtonClick()
                            viewModel.setPaused(false)
                            navController.navigate(Routes.ROUTE_HOME) {
                                popUpTo(Routes.ROUTE_HOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.ROUTE_LEVEL_COMPLETE) {
                    val summary = lastCompletion
                    if (summary != null) {
                        LevelCompleteScreen(
                            summary = summary,
                            onNextLevel = {
                                viewModel.soundManager.playButtonClick()
                                if (summary.isDaily) {
                                    viewModel.startLevel(appState.preferences.highestUnlockedLevel)
                                } else {
                                    val next = summary.levelNumber + 1
                                    viewModel.startLevel(next)
                                }
                                navController.navigate(Routes.ROUTE_GAMEPLAY) {
                                    popUpTo(Routes.ROUTE_LEVEL_COMPLETE) { inclusive = true }
                                }
                            },
                            onReplay = {
                                viewModel.soundManager.playButtonClick()
                                if (summary.isDaily && summary.dailyDateKey != null) {
                                    viewModel.startDailyChallenge(summary.dailyDateKey)
                                } else {
                                    viewModel.startLevel(summary.levelNumber)
                                }
                                navController.navigate(Routes.ROUTE_GAMEPLAY) {
                                    popUpTo(Routes.ROUTE_LEVEL_COMPLETE) { inclusive = true }
                                }
                            },
                            onHome = {
                                viewModel.soundManager.playButtonClick()
                                navController.navigate(Routes.ROUTE_HOME) {
                                    popUpTo(Routes.ROUTE_HOME) { inclusive = true }
                                }
                            },
                            onDoubleCoinsOffer = {
                                viewModel.startRewardedOffer(RewardedOfferType.DOUBLE_LEVEL_COINS)
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.ROUTE_HOME) {
                                popUpTo(Routes.ROUTE_HOME) { inclusive = true }
                            }
                        }
                    }
                }

                composable(Routes.ROUTE_DAILY_CHALLENGE) {
                    DailyChallengeScreen(
                        appState = appState,
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        },
                        onPlayDaily = { dateKey ->
                            viewModel.soundManager.playButtonClick()
                            viewModel.startDailyChallenge(dateKey)
                            navController.navigate(Routes.ROUTE_GAMEPLAY)
                        }
                    )
                }

                composable(Routes.ROUTE_THEMES) {
                    ThemeSelectionScreen(
                        appState = appState,
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        },
                        onThemeClick = viewModel::onThemeClicked
                    )
                }

                composable(Routes.ROUTE_SETTINGS) {
                    SettingsScreen(
                        appState = appState,
                        adStatusDescription = viewModel.repository.adManager.getStatusDescription(),
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        },
                        onToggleSound = viewModel::toggleSound,
                        onToggleMusic = viewModel::toggleMusic,
                        onToggleVibration = viewModel::toggleVibration,
                        onSelectLanguage = viewModel::setLanguage,
                        onOpenPrivacyPolicy = {
                            viewModel.soundManager.playButtonClick()
                            navController.navigate(Routes.ROUTE_PRIVACY_POLICY)
                        },
                        onResetProgress = viewModel::resetProgress
                    )
                }

                composable(Routes.ROUTE_PRIVACY_POLICY) {
                    PrivacyPolicyScreen(
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.ROUTE_REWARDS) {
                    RewardScreen(
                        appState = appState,
                        onBack = {
                            viewModel.soundManager.playButtonClick()
                            navController.popBackStack()
                        },
                        onClaimDailyReward = viewModel::claimDailyLoginReward,
                        onStartRewardedOffer = viewModel::startRewardedOffer,
                        onClaimMilestone = viewModel::claimMilestone
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )

            activeOffer?.let { offer ->
                val isLoading = (rewardedAdState as? RewardedAdUiState.Loading)?.offer == offer
                val errorMessage = (rewardedAdState as? RewardedAdUiState.Error)
                    ?.takeIf { it.offer == offer }
                    ?.message
                RewardedOfferDialog(
                    offer = offer,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onCancel = viewModel::cancelRewardedOffer,
                    onWatchAd = { viewModel.launchRewardedAd(hostActivity, offer) }
                )
            }
        }
    }
}
