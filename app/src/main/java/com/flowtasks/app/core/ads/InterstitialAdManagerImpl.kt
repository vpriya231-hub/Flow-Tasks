package com.flowtasks.app.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.flowtasks.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust implementation of [InterstitialAdManager] supporting background preloading,
 * full-screen lifecycle handling, 5-minute frequency capping, and leak-free callbacks.
 */
class InterstitialAdManagerImpl(
    private val adUnitId: String = AdConfig.ACTIVE_INTERSTITIAL_AD_UNIT_ID,
    private val minIntervalMs: Long = AdConfig.MIN_INTERSTITIAL_INTERVAL_MS
) : InterstitialAdManager {

    private val isInitialized = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)

    @Volatile
    private var cachedInterstitialAd: InterstitialAd? = null

    @Volatile
    private var lastAdShownTimestamp: Long = 0L

    override fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val appContext = context.applicationContext
            try {
                MobileAds.initialize(appContext) { initializationStatus ->
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "AdMob MobileAds initialized: $initializationStatus")
                    }
                    // Immediately preload the first test interstitial ad
                    preloadAd(appContext)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to initialize MobileAds SDK", e)
                }
            }
        }
    }

    override fun preloadAd(context: Context) {
        val appContext = context.applicationContext

        // If an ad is already cached or currently loading, skip redundant request
        if (cachedInterstitialAd != null || isLoading.get()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Ad preload skipped: already loaded or loading in progress")
            }
            return
        }

        isLoading.set(true)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting interstitial ad request with unitId: $adUnitId")
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading.set(false)
                    cachedInterstitialAd = ad
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Interstitial ad successfully loaded and cached")
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading.set(false)
                    cachedInterstitialAd = null
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Interstitial ad failed to load: code=${loadAdError.code}, msg=${loadAdError.message}")
                    }
                }
            }
        )
    }

    override fun isAdLoaded(): Boolean {
        return cachedInterstitialAd != null
    }

    override fun isCooldownElapsed(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastAdShownTimestamp) >= minIntervalMs
    }

    override fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit) {
        val currentAd = cachedInterstitialAd
        val cooldownPassed = isCooldownElapsed()

        if (currentAd != null && cooldownPassed && !activity.isFinishing && !activity.isDestroyed) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Displaying interstitial ad")
            }

            // Consume cached ad reference to avoid re-showing same ad object
            cachedInterstitialAd = null
            lastAdShownTimestamp = System.currentTimeMillis()

            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Interstitial ad displayed full screen")
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Interstitial ad dismissed by user")
                    }
                    // Automatically preload next ad in advance
                    preloadAd(activity.applicationContext)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Interstitial ad failed to show: code=${adError.code}, msg=${adError.message}")
                    }
                    // Automatically reload fresh ad
                    preloadAd(activity.applicationContext)
                    onAdDismissed()
                }

                override fun onAdClicked() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Interstitial ad clicked")
                    }
                }

                override fun onAdImpression() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Interstitial ad recorded impression")
                    }
                }
            }

            try {
                currentAd.show(activity)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Exception during ad.show(activity)", e)
                }
                preloadAd(activity.applicationContext)
                onAdDismissed()
            }
        } else {
            if (BuildConfig.DEBUG) {
                val reason = when {
                    currentAd == null -> "No ad cached"
                    !cooldownPassed -> "Cooldown active (${(minIntervalMs - (System.currentTimeMillis() - lastAdShownTimestamp)) / 1000}s remaining)"
                    else -> "Activity is finishing or destroyed"
                }
                Log.d(TAG, "Skipping interstitial ad: $reason")
            }

            // If no ad is cached and not loading, trigger a background preload
            if (currentAd == null && !isLoading.get()) {
                preloadAd(activity.applicationContext)
            }

            // Continue user flow immediately without any blocking
            onAdDismissed()
        }
    }

    companion object {
        private const val TAG = "InterstitialAdManager"
    }
}
