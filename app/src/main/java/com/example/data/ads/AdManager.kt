package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Complete Google AdMob Monetization Config:
 * 1. Home Screen Anchored Adaptive Banner Ad
 * 2. Lose / Optional Rewarded Video Ad (Revive +3 Hearts, +1 Hint, +100 Coins, 2x Level Coins)
 * 3. Win Interstitial Ad (paced every 2 wins + 75s cooldown + 60s post-rewarded protection)
 *
 * Replace the LIVE_* constants below with your own AdMob Ad Unit IDs before Play Store release.
 * Debug builds automatically route to Google's official Test Ad Unit IDs.
 */
data class AdMobConfig(
    val bannerHomeAdUnitId: String = LIVE_BANNER_AD_UNIT_ID,
    val rewardedAdUnitId: String = LIVE_REWARDED_AD_UNIT_ID,
    val interstitialWinAdUnitId: String = LIVE_INTERSTITIAL_AD_UNIT_ID,
    val interstitialEveryNLevels: Int = 2,
    val minSecondsBetweenInterstitials: Long = 75L,
    val minSecondsAfterRewardedAd: Long = 60L,
    val allowAdsDuringActiveGameplay: Boolean = false
) {
    val isLiveAdMobConfigured: Boolean
        get() = bannerHomeAdUnitId.isNotBlank() ||
            rewardedAdUnitId.isNotBlank() ||
            interstitialWinAdUnitId.isNotBlank()

    companion object {
        // Google's official AdMob Test Ad Unit IDs (safe for testing & emulators)
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        // Live AdMob Ad Unit IDs for Google Play production:
        const val LIVE_BANNER_AD_UNIT_ID = "ca-app-pub-7700773097020018/1409412750"
        const val LIVE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7700773097020018/4546838294"
        const val LIVE_REWARDED_AD_UNIT_ID = "ca-app-pub-7700773097020018/8262722027"

        // Previously shared Rewarded Interstitial unit for reference:
        // ca-app-pub-7700773097020018/6921047749
    }
}

enum class RewardedOfferType(
    val title: String,
    val rewardSummary: String
) {
    EXTRA_HINT("Free Hint Offer", "+1 Free Hint"),
    BONUS_COINS("Bonus Coins Offer", "+100 Coins"),
    DOUBLE_LEVEL_COINS("Double Reward Offer", "2x Level Coins"),
    REVIVE_HEARTS("Continue Level", "+3 Hearts")
}

/**
 * Balances strong AdMob revenue with player-friendly ad pacing:
 * - Zero ads during active puzzle solving
 * - Home screen anchored adaptive banner only
 * - Opt-in Rewarded Video on Lose (+3 Hearts) & bonus offers
 * - Smart-paced Win Interstitial (skips onboarding levels 1-2, caps frequency & cooldown,
 *   and suppresses interstitials right after a Rewarded Ad so players never see back-to-back ads)
 */
class AdManager(
    val config: AdMobConfig = AdMobConfig()
) {
    private var completedLevelsSinceInterstitial = 0
    private var lastInterstitialShownMillis = 0L
    private var lastRewardedWatchedMillis = 0L

    fun bannerUnitId(): String = config.bannerHomeAdUnitId

    fun rewardedUnitIdFor(offer: RewardedOfferType): String = config.rewardedAdUnitId

    fun interstitialUnitId(): String = config.interstitialWinAdUnitId

    fun effectiveBannerUnitId(): String =
        if (BuildConfig.DEBUG) AdMobConfig.TEST_BANNER_AD_UNIT_ID else bannerUnitId()

    fun effectiveRewardedUnitId(offer: RewardedOfferType): String =
        if (BuildConfig.DEBUG) AdMobConfig.TEST_REWARDED_AD_UNIT_ID else rewardedUnitIdFor(offer)

    fun effectiveInterstitialUnitId(): String =
        if (BuildConfig.DEBUG) AdMobConfig.TEST_INTERSTITIAL_AD_UNIT_ID else interstitialUnitId()

    fun recordRewardedAdWatched(nowMillis: Long = System.currentTimeMillis()) {
        lastRewardedWatchedMillis = nowMillis
        // Reset level counter so player isn't hit with an interstitial right after watching a rewarded ad
        completedLevelsSinceInterstitial = 0
    }

    /**
     * Evaluates whether a win interstitial should show after completing a level.
     * Respects:
     * 1) Onboarding protection (skip levels 1 & 2)
     * 2) Every N levels frequency cap (every 2 wins)
     * 3) Minimum time cooldown between interstitials (75s)
     * 4) Anti-double-ad cooldown after watching a Rewarded Ad (60s)
     */
    fun shouldShowPostLevelInterstitial(
        completedLevelNumber: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (completedLevelNumber <= 2 || config.interstitialWinAdUnitId.isBlank()) return false

        val secSinceRewarded = (nowMillis - lastRewardedWatchedMillis) / 1000L
        if (lastRewardedWatchedMillis > 0L && secSinceRewarded < config.minSecondsAfterRewardedAd) {
            return false
        }

        completedLevelsSinceInterstitial++
        val elapsedSec = (nowMillis - lastInterstitialShownMillis) / 1000L
        val meetsLevelCap = completedLevelsSinceInterstitial >= config.interstitialEveryNLevels
        val meetsTimeCap = lastInterstitialShownMillis == 0L || elapsedSec >= config.minSecondsBetweenInterstitials

        return if (meetsLevelCap && meetsTimeCap) {
            completedLevelsSinceInterstitial = 0
            lastInterstitialShownMillis = nowMillis
            true
        } else {
            false
        }
    }

    fun getStatusDescription(): String {
        return "AdMob Active: Home Banner • Lose Rewarded • Paced Win Interstitial (Zero Gameplay Ads)"
    }
}

/**
 * Unified Google Mobile Ads SDK controller managing initialization, preloading, and display
 * for both Rewarded Ads (on Lose / Bonuses) and Interstitial Ads (on Win).
 */
class GameAdController(
    private val adManager: AdManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var initialized = false

    private var rewardedAd: RewardedAd? = null
    private var isPreloadingRewarded = false

    private var interstitialAd: InterstitialAd? = null
    private var isPreloadingInterstitial = false

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun ensureSdkInitialized(context: Context, onReady: () -> Unit) {
        val appContext = context.applicationContext
        if (initialized) {
            onReady()
            return
        }
        initialized = true
        try {
            if (BuildConfig.DEBUG) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                        .build()
                )
            }
            MobileAds.initialize(appContext) {
                runOnMainThread(onReady)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "MobileAds initialization skipped: ${t.message}")
            onReady()
        }
    }

    fun initializeAndPreload(context: Context) {
        val appContext = context.applicationContext
        runOnMainThread {
            ensureSdkInitialized(appContext) {
                preloadRewardedAd(appContext)
                preloadInterstitialAd(appContext)
            }
        }
    }

    fun preloadRewardedAd(
        context: Context,
        adUnitId: String = adManager.effectiveRewardedUnitId(RewardedOfferType.REVIVE_HEARTS)
    ) {
        val appContext = context.applicationContext
        runOnMainThread {
            if (adUnitId.isBlank() || rewardedAd != null || isPreloadingRewarded) return@runOnMainThread
            isPreloadingRewarded = true
            try {
                RewardedAd.load(
                    appContext,
                    adUnitId,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.d(TAG, "Rewarded ad preloaded ($adUnitId).")
                            rewardedAd = ad
                            isPreloadingRewarded = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Rewarded preload failed (${error.code}): ${error.message}")
                            rewardedAd = null
                            isPreloadingRewarded = false
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Rewarded preload exception: ${t.message}")
                isPreloadingRewarded = false
            }
        }
    }

    fun preloadInterstitialAd(
        context: Context,
        adUnitId: String = adManager.effectiveInterstitialUnitId()
    ) {
        val appContext = context.applicationContext
        runOnMainThread {
            if (adUnitId.isBlank() || interstitialAd != null || isPreloadingInterstitial) return@runOnMainThread
            isPreloadingInterstitial = true
            try {
                InterstitialAd.load(
                    appContext,
                    adUnitId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Win interstitial ad preloaded ($adUnitId).")
                            interstitialAd = ad
                            isPreloadingInterstitial = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Interstitial preload failed (${error.code}): ${error.message}")
                            interstitialAd = null
                            isPreloadingInterstitial = false
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Interstitial preload exception: ${t.message}")
                isPreloadingInterstitial = false
            }
        }
    }

    /**
     * Shows a preloaded Win Interstitial Ad immediately if ready, otherwise continues straight
     * to [onComplete] without making the user wait for a network spinner.
     */
    fun showWinInterstitialIfReady(
        activity: Activity?,
        onComplete: () -> Unit
    ) {
        runOnMainThread {
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                onComplete()
                return@runOnMainThread
            }
            val unitId = adManager.effectiveInterstitialUnitId()
            val cached = interstitialAd
            if (cached == null) {
                preloadInterstitialAd(activity.applicationContext, unitId)
                onComplete()
                return@runOnMainThread
            }

            interstitialAd = null
            var finished = false
            fun finishOnce() {
                if (!finished) {
                    finished = true
                    onComplete()
                }
            }

            cached.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Win interstitial failed to show: ${adError.message}")
                    preloadInterstitialAd(activity.applicationContext, unitId)
                    finishOnce()
                }

                override fun onAdDismissedFullScreenContent() {
                    preloadInterstitialAd(activity.applicationContext, unitId)
                    finishOnce()
                }
            }
            try {
                cached.show(activity)
            } catch (t: Throwable) {
                Log.w(TAG, "Win interstitial show exception: ${t.message}")
                preloadInterstitialAd(activity.applicationContext, unitId)
                finishOnce()
            }
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
        runOnMainThread {
            val unitId = adManager.effectiveRewardedUnitId(offer)
            val cached = rewardedAd
            if (cached != null) {
                rewardedAd = null
                showLoadedRewardedAd(
                    activity = activity,
                    ad = cached,
                    unitId = unitId,
                    onAdOpened = onAdOpened,
                    onRewardEarned = onRewardEarned,
                    onDismissedWithoutReward = onDismissedWithoutReward,
                    onFailed = onFailed
                )
                return@runOnMainThread
            }

            onLoadingStarted()
            ensureSdkInitialized(activity.applicationContext) {
                if (activity.isFinishing || activity.isDestroyed) {
                    onFailed("Screen is no longer active.")
                    return@ensureSdkInitialized
                }
                try {
                    RewardedAd.load(
                        activity,
                        unitId,
                        AdRequest.Builder().build(),
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                showLoadedRewardedAd(
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
                                Log.w(TAG, "Rewarded ad failed to load (${error.code}): ${error.message}")
                                val detail = when (error.code) {
                                    AdRequest.ERROR_CODE_NETWORK_ERROR -> "Network error. Check internet connection."
                                    AdRequest.ERROR_CODE_NO_FILL -> "No ad available right now. Please try again shortly."
                                    AdRequest.ERROR_CODE_INVALID_REQUEST -> "Invalid AdMob App/Unit ID configuration."
                                    else -> error.message.ifBlank { "Unable to load rewarded ad (code ${error.code})." }
                                }
                                onFailed(detail)
                            }
                        }
                    )
                } catch (t: Throwable) {
                    onFailed(t.message ?: "Unable to initialize Google Mobile Ads.")
                }
            }
        }
    }

    private fun showLoadedRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        unitId: String,
        onAdOpened: () -> Unit,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFailed("Unable to show ad because screen closed.")
            return
        }
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
            adManager.recordRewardedAdWatched()
            onRewardEarned()
        }
    }

    companion object {
        private const val TAG = "GameAdController"
    }
}
