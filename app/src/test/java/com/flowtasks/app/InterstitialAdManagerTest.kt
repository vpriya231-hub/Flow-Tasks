package com.flowtasks.app

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flowtasks.app.core.ads.AdConfig
import com.flowtasks.app.core.ads.InterstitialAdManager
import com.flowtasks.app.core.ads.InterstitialAdManagerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterstitialAdManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAdConfigConstants() {
        // Verify AdMob App ID
        assertEquals("ca-app-pub-6638063032452766~5332707942", AdConfig.ADMOB_APP_ID)

        // Verify Test Ad Unit ID
        assertEquals("ca-app-pub-3940256099942544/1033173712", AdConfig.TEST_INTERSTITIAL_AD_UNIT_ID)

        // Verify Production Ad Unit ID
        assertEquals("ca-app-pub-6638063032452766/5555390605", AdConfig.PRODUCTION_INTERSTITIAL_AD_UNIT_ID)

        // Verify test mode is active and uses test unit ID
        assertTrue(AdConfig.IS_TEST_MODE)
        assertEquals(AdConfig.TEST_INTERSTITIAL_AD_UNIT_ID, AdConfig.ACTIVE_INTERSTITIAL_AD_UNIT_ID)
        assertNotEquals(AdConfig.PRODUCTION_INTERSTITIAL_AD_UNIT_ID, AdConfig.ACTIVE_INTERSTITIAL_AD_UNIT_ID)

        // Verify frequency cap is 5 minutes
        assertEquals(5 * 60 * 1000L, AdConfig.MIN_INTERSTITIAL_INTERVAL_MS)
    }

    @Test
    fun testInitializationAndPreloadWithoutCrashing() {
        val adManager: InterstitialAdManager = InterstitialAdManagerImpl()
        // Should initialize safely and idempotently
        adManager.initialize(context)
        adManager.initialize(context)
        adManager.preloadAd(context)
    }

    @Test
    fun testShowAdFallbackWhenNoAdReady() {
        val adManager: InterstitialAdManager = InterstitialAdManagerImpl()
        var callbackInvoked = false

        // With no ad cached, it must execute onAdDismissed callback immediately without delay
        val fakeActivity = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).get()
        adManager.showAdIfAvailable(fakeActivity) {
            callbackInvoked = true
        }

        assertTrue(callbackInvoked)
    }
}
