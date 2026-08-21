package com.codebasetemplate.features.feature_language.ui.v2

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageV2ApplyModeTest {

    @Test
    fun `uses icon mode when text is disabled`() {
        assertEquals(LanguageV2ApplyMode.ICON, LanguageV2ApplyMode.from(useText = false))
    }

    @Test
    fun `uses text mode when text is enabled`() {
        assertEquals(LanguageV2ApplyMode.TEXT, LanguageV2ApplyMode.from(useText = true))
    }
}
