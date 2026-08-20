package com.flowtasks.app.core.ads

import android.app.Activity
import android.content.Context

/**
 * Interface defining interstitial ad management, preloading, and display operations.
 */
interface InterstitialAdManager {
    /**
     * Initializes the Mobile Ads SDK safely (idempotent, background initialized).
     */
    fun initialize(context: Context)

    /**
     * Preloads the next interstitial ad in advance.
     */
    fun preloadAd(context: Context)

    /**
     * Returns true if an interstitial ad is currently cached and ready to show.
     */
    fun isAdLoaded(): Boolean

    /**
     * Returns true if the cooldown period (5 minutes) has elapsed since the last ad impression.
     */
    fun isCooldownElapsed(): Boolean

    /**
     * Shows an interstitial ad if ready and cooldown has elapsed.
     * Always invokes [onAdDismissed] when the ad is closed or immediately if no ad is shown.
     *
     * @param activity The host Activity to display full-screen ad content.
     * @param onAdDismissed Callback executed when ad finishes, fails, or is skipped.
     */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit)
}
