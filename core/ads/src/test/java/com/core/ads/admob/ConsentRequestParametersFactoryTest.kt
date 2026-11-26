package com.core.ads.admob

import android.app.Application
import android.content.pm.ApplicationInfo
import com.core.ads.AdsSdkInitializer
import com.core.ads.ConsentDebugConfig
import com.google.android.ump.ConsentDebugSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ConsentRequestParametersFactoryTest {
    private lateinit var context: Application
    private var originalFlags = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags = originalFlags or ApplicationInfo.FLAG_DEBUGGABLE
    }

    @After
    fun tearDown() {
        context.applicationInfo.flags = originalFlags
    }

    @Test
    fun `default app configuration does not override geography`() {
        val params = TestInitializer().createConsentRequestParameters(context)

        assertNull(params.consentDebugSettings)
        assertFalse(params.isTagForUnderAgeOfConsent)
    }

    @Test
    fun `debug app can force EEA without physical device IDs for emulator testing`() {
        val params = initializer(ConsentDebugConfig(forceEea = true))
            .createConsentRequestParameters(context)

        assertEquals(
            ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
            params.consentDebugSettings?.debugGeography,
        )
        assertFalse(params.isTagForUnderAgeOfConsent)
    }

    @Test
    fun `disabled EEA does not attach debug settings even with test IDs configured`() {
        val params = initializer(ConsentDebugConfig(testDeviceHashedIds = listOf("test-device")))
            .createConsentRequestParameters(context)

        assertNull(params.consentDebugSettings)
    }

    @Test
    fun `non-debuggable app ignores enabled EEA and test IDs`() {
        context.applicationInfo.flags = originalFlags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val params = initializer(
            ConsentDebugConfig(forceEea = true, testDeviceHashedIds = listOf("test-device")),
        ).createConsentRequestParameters(context)

        assertNull(params.consentDebugSettings)
        assertFalse(params.isTagForUnderAgeOfConsent)
    }

    @Test
    fun `subsequent request uses the latest app debug configuration`() {
        val initializer = object : TestInitializer() {
            override var consentDebugConfig = ConsentDebugConfig(forceEea = true)
        }
        assertEquals(
            ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
            initializer.createConsentRequestParameters(context).consentDebugSettings?.debugGeography,
        )

        initializer.consentDebugConfig = ConsentDebugConfig(forceEea = false)

        assertNull(initializer.createConsentRequestParameters(context).consentDebugSettings)
    }

    private fun initializer(config: ConsentDebugConfig) = object : TestInitializer() {
        override val consentDebugConfig = config
    }

    private open class TestInitializer : AdsSdkInitializer {
        override fun admobAppId() = ""
        override fun onUpdateGdprConsent(consentGranted: Boolean?) = Unit
        override fun onAdInitCompleted() = Unit
    }
}
