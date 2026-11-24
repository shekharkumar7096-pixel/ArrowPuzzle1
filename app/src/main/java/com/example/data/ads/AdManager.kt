package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Configurable AdMob integration.
 * Configured with Google's official Rewarded Test Ad Unit ID:
 * ca-app-pub-3940256099942544/5224354917
 */
data class AdMobConfig(
    val rewardedHintAdUnitId: String = TEST_REWARDED_AD_UNIT_ID,
    val rewardedCoinsAdUnitId: String = TEST_REWARDED_AD_UNIT_ID,
    val interstitialAdUnitId: String = "",
    val interstitialEveryNLevels: Int = 3,
    val minSecondsBetweenInterstitials: Long = 120L,
    val allowAdsDuringActiveGameplay: Boolean = false
) {
    val isLiveAdMobConfigured: Boolean
        get() = rewardedHintAdUnitId.isNotBlank() ||
            rewardedCoinsAdUnitId.isNotBlank() ||
            interstitialAdUnitId.isNotBlank()

    companion object {
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}

enum class RewardedOfferType(
    val title: String,
    val rewardSummary: String
) {
    EXTRA_HINT("Free Hint Offer", "+1 Free Hint"),
    BONUS_COINS("Bonus Coins Offer", "+100 Coins"),
    DOUBLE_LEVEL_COINS("Double Reward Offer", "2x Level Coins")
}

/**
 * Manages ad frequency capping and optional rewarded ad placements.
 * Strictly enforces:
 * - No ads during active gameplay
 * - No forced interstitial on every level
 * - Rewarded ads are 100% opt-in
 */
class AdManager(
    val config: AdMobConfig = AdMobConfig()
) {
    private var completedLevelsSinceInterstitial = 0
    private var lastInterstitialShownMillis = 0L

    fun rewardedUnitIdFor(offer: RewardedOfferType): String = when (offer) {
        RewardedOfferType.EXTRA_HINT -> config.rewardedHintAdUnitId
        RewardedOfferType.BONUS_COINS,
        RewardedOfferType.DOUBLE_LEVEL_COINS -> config.rewardedCoinsAdUnitId
    }

    /**
     * Evaluates whether an interstitial placement is eligible after completing a level.
     * Respects both level-count frequency cap and minimum cooldown timer.
     */
    fun shouldShowPostLevelInterstitial(
        completedLevelNumber: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (completedLevelNumber <= 2) return false
        completedLevelsSinceInterstitial++
        val elapsedSec = (nowMillis - lastInterstitialShownMillis) / 1000L
        val meetsLevelCap = completedLevelsSinceInterstitial >= config.interstitialEveryNLevels
        val meetsTimeCap = lastInterstitialShownMillis == 0L || elapsedSec >= config.minSecondsBetweenInterstitials

        return if (meetsLevelCap && meetsTimeCap && config.interstitialAdUnitId.isNotBlank()) {
            completedLevelsSinceInterstitial = 0
            lastInterstitialShownMillis = nowMillis
            true
        } else {
            false
        }
    }

    fun getStatusDescription(): String {
        return "Google AdMob Rewarded Test Unit Active (${config.rewardedHintAdUnitId}) • Zero Gameplay Ads"
    }
}

/**
 * Handles Google Mobile Ads SDK initialization, preloading, and display of Rewarded Ads.
 */
class RewardedAdController(
    private val adManager: AdManager
) {
    private var initialized = false
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun initializeAndPreload(context: Context) {
        if (!initialized) {
            initialized = true
            try {
                MobileAds.initialize(context.applicationContext) {
                    preloadRewardedAd(context.applicationContext)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "MobileAds initialization skipped: ${t.message}")
            }
        } else {
            preloadRewardedAd(context.applicationContext)
        }
    }

    fun preloadRewardedAd(context: Context, adUnitId: String = adManager.config.rewardedCoinsAdUnitId) {
        if (adUnitId.isBlank() || rewardedAd != null || isLoading) return
        isLoading = true
        try {
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                context.applicationContext,
                adUnitId,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded test ad loaded.")
                        rewardedAd = ad
                        isLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Rewarded ad preload failed: ${error.message}")
                        rewardedAd = null
                        isLoading = false
                    }
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Rewarded ad preload exception: ${t.message}")
            isLoading = false
        }
    }

    fun loadAndShowRewardedAd(
        activity: Activity,
        offer: RewardedOfferType,
        onLoadingStarted: () -> Unit,
        onAdOpened: () -> Unit,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val unitId = adManager.rewardedUnitIdFor(offer)
        val cached = rewardedAd
        if (cached != null) {
            rewardedAd = null
            showLoadedAd(
                activity = activity,
                ad = cached,
                unitId = unitId,
                onAdOpened = onAdOpened,
                onRewardEarned = onRewardEarned,
                onDismissedWithoutReward = onDismissedWithoutReward,
                onFailed = onFailed
            )
            return
        }

        onLoadingStarted()
        try {
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                activity,
                unitId,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        showLoadedAd(
                            activity = activity,
                            ad = ad,
                            unitId = unitId,
                            onAdOpened = onAdOpened,
                            onRewardEarned = onRewardEarned,
                            onDismissedWithoutReward = onDismissedWithoutReward,
                            onFailed = onFailed
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                        onFailed(error.message.ifBlank { "Unable to load test rewarded ad. Check internet connection." })
                    }
                }
            )
        } catch (t: Throwable) {
            onFailed(t.message ?: "Unable to initialize Google Mobile Ads.")
        }
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: RewardedAd,
        unitId: String,
        onAdOpened: () -> Unit,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                onAdOpened()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                preloadRewardedAd(activity.applicationContext, unitId)
                onFailed(adError.message.ifBlank { "Unable to display rewarded ad." })
            }

            override fun onAdDismissedFullScreenContent() {
                preloadRewardedAd(activity.applicationContext, unitId)
                if (!rewardGranted) {
                    onDismissedWithoutReward()
                }
            }
        }

        onAdOpened()
        ad.show(activity) {
            rewardGranted = true
            onRewardEarned()
        }
    }

    companion object {
        private const val TAG = "RewardedAdController"
    }
}
