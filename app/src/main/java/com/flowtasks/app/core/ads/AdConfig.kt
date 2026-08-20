package com.flowtasks.app.core.ads

/**
 * AdMob configuration parameters and Ad Unit definitions.
 * Maintains strict separation between official Google test IDs and production IDs.
 */
object AdConfig {
    /**
     * Official AdMob Application ID.
     */
    const val ADMOB_APP_ID = "ca-app-pub-6638063032452766~5332707942"

    /**
     * Official Google Test Interstitial Ad Unit ID for Android.
     * MUST be used during all development and testing builds.
     */
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Production Interstitial Ad Unit ID.
     * Configured and safely stored for future production release, but NEVER requested in testing mode.
     */
    const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6638063032452766/5555390605"

    /**
     * Test mode flag. Kept true for development/testing build.
     */
    const val IS_TEST_MODE = true

    /**
     * Active interstitial ad unit ID based on current test mode.
     */
    val ACTIVE_INTERSTITIAL_AD_UNIT_ID: String
        get() = if (IS_TEST_MODE) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

    /**
     * Minimum cooldown time between interstitial ad displays: 5 minutes (300,000 ms).
     */
    const val MIN_INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L
}
