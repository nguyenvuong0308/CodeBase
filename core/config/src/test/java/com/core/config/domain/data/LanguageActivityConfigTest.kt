package com.core.config.domain.data

import com.core.config.data.model.LanguageActivityConfigModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageActivityConfigTest {

    @Test
    fun `click guide defaults to enabled when config is missing`() {
        assertTrue(LanguageActivityConfig.from(model = null).isShowClickGuide)
        assertTrue(LanguageActivityConfig.from(model = model(isShowClickGuide = null)).isShowClickGuide)
    }

    @Test
    fun `click guide follows remote config value`() {
        assertTrue(LanguageActivityConfig.from(model(isShowClickGuide = true)).isShowClickGuide)
        assertFalse(LanguageActivityConfig.from(model(isShowClickGuide = false)).isShowClickGuide)
    }

    private fun model(isShowClickGuide: Boolean?): LanguageActivityConfigModel {
        return LanguageActivityConfigModel(
            version = 2,
            timeShowLoadingLfo = 3,
            isShowClickGuide = isShowClickGuide
        )
    }
}
