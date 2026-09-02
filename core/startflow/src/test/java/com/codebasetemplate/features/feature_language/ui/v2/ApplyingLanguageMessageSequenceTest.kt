package com.codebasetemplate.features.feature_language.ui.v2

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyingLanguageMessageSequenceTest {

    @Test
    fun `cycles through one two and three dots`() {
        val sequence = ApplyingLanguageMessageSequence("Applying language")

        assertEquals("Applying language.", sequence.next())
        assertEquals("Applying language..", sequence.next())
        assertEquals("Applying language...", sequence.next())
        assertEquals("Applying language.", sequence.next())
    }

    @Test
    fun `reset starts the sequence from one dot`() {
        val sequence = ApplyingLanguageMessageSequence("Applying language")

        sequence.next()
        sequence.next()
        sequence.reset()

        assertEquals("Applying language.", sequence.next())
    }

    @Test
    fun `keeps the complete localized message while adding dots`() {
        val localizedMessage = "Đang thiết lập ngôn ngữ cho bạn;\nvui lòng đợi trong giây lát"
        val sequence = ApplyingLanguageMessageSequence(localizedMessage)

        assertEquals("$localizedMessage.", sequence.next())
        assertEquals("$localizedMessage..", sequence.next())
        assertEquals("$localizedMessage...", sequence.next())
    }
}
