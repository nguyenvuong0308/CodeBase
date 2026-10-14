package com.core.ads.admob

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdmobNextGenMigrationTest {

    @Test
    fun `version catalog uses next gen SDK and not the legacy ads artifact`() {
        val catalog = repositoryFile("gradle/libs.versions.toml").readText()

        assertTrue(
            catalog.contains(
                "com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk"
            )
        )
        assertFalse(catalog.contains("com.google.android.gms:play-services-ads\""))
    }

    @Test
    fun `ad managers use next gen initialization requests and callbacks`() {
        val admobManager = moduleFile(
            "src/main/java/com/core/ads/admob/AdmobManager.kt"
        ).readText()
        val appOpenAdManager = moduleFile(
            "src/main/java/com/core/ads/admob/AppOpenAdManager.kt"
        ).readText()

        assertTrue(admobManager.contains("InitializationConfig.Builder(adsSdkInitializer.admobAppId())"))
        assertTrue(admobManager.contains("AdRequest.Builder(adUnitId)"))
        assertTrue(admobManager.contains("InterstitialAdEventCallback"))
        assertTrue(appOpenAdManager.contains("adManager.getAdRequest(adUnitId)"))
        assertTrue(appOpenAdManager.contains("AppOpenAdEventCallback"))
        assertFalse(admobManager.contains("com.google.android.gms.ads"))
        assertFalse(appOpenAdManager.contains("com.google.android.gms.ads"))
    }

    @Test
    fun `native templates register ads with next gen views`() {
        val templateDirectory = moduleFile(
            "src/main/java/com/core/ads/customviews/ads"
        )
        val nativeTemplates = templateDirectory.walkTopDown()
            .filter(File::isFile)
            .filter { it.extension == "kt" }
            .map { it to it.readText() }
            .filter { (_, source) -> source.contains("override fun setNativeAd(nativeAd: NativeAd)") }
            .toList()

        assertTrue(nativeTemplates.isNotEmpty())
        nativeTemplates.forEach { (file, source) ->
            assertTrue(
                "${file.name} must register its native ad with the Next-Gen view",
                source.contains("registerNativeAd(nativeAd")
            )
            assertFalse(source.contains("com.google.android.gms.ads"))
        }
    }

    @Test
    fun `native layouts use next gen view classes`() {
        val layouts = moduleFile("src/main/res/layout")
            .listFiles { file -> file.extension == "xml" }
            .orEmpty()
            .map(File::readText)
        val nativeLayouts = layouts.filter { it.contains("NativeAdView") }

        assertTrue(nativeLayouts.isNotEmpty())
        assertTrue(
            nativeLayouts.all {
                it.contains("com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView")
            }
        )
        assertTrue(
            nativeLayouts.all { !it.contains("com.google.android.gms.ads.nativead") }
        )
    }

    private fun moduleFile(relativePath: String): File =
        sequenceOf(
            File(relativePath),
            File("core/ads/$relativePath")
        ).firstOrNull(File::exists)
            ?: error("Cannot find core/ads/$relativePath")

    private fun repositoryFile(relativePath: String): File =
        sequenceOf(
            File(relativePath),
            File("../$relativePath"),
            File("../../$relativePath")
        ).firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")
}
