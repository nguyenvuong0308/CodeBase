package com.codebasetemplate.features.feature_language.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageBannerVisibilityResolverTest {

    @Test
    fun `shows step 1 before user selects a language`() {
        assertEquals(
            LanguageBannerStep.STEP_1,
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = false,
                step1AdState = LanguageBannerAdState.PENDING,
                step2AdState = LanguageBannerAdState.PENDING,
            )
        )
    }

    @Test
    fun `shows step 2 after user selects a language`() {
        assertEquals(
            LanguageBannerStep.STEP_2,
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = true,
                step1AdState = LanguageBannerAdState.AVAILABLE,
                step2AdState = LanguageBannerAdState.AVAILABLE,
            )
        )
    }

    @Test
    fun `falls back to step 2 when step 1 has no ad`() {
        assertEquals(
            LanguageBannerStep.STEP_2,
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = false,
                step1AdState = LanguageBannerAdState.UNAVAILABLE,
                step2AdState = LanguageBannerAdState.AVAILABLE,
            )
        )
    }

    @Test
    fun `falls back to step 1 when step 2 has no ad`() {
        assertEquals(
            LanguageBannerStep.STEP_1,
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = true,
                step1AdState = LanguageBannerAdState.AVAILABLE,
                step2AdState = LanguageBannerAdState.UNAVAILABLE,
            )
        )
    }

    @Test
    fun `hides banners when neither step has an ad`() {
        assertNull(
            resolveLanguageBannerStep(
                hasUserSelectedLanguage = true,
                step1AdState = LanguageBannerAdState.UNAVAILABLE,
                step2AdState = LanguageBannerAdState.UNAVAILABLE,
            )
        )
    }
}
