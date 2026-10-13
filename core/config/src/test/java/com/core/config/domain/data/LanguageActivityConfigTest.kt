package com.core.config.domain.data

import com.core.config.data.model.LanguageActivityConfigModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import com.squareup.moshi.Moshi
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageActivityConfigTest {

    private val adapter = Moshi.Builder().build().adapter(LanguageActivityConfigModel::class.java)

    @Test
    fun `v1 action defaults to icon for missing or null config`() {
        assertFalse(LanguageActivityConfig().v1ActionUseText)
        assertFalse(LanguageActivityConfig.from(null).v1ActionUseText)
        for (json in listOf("{}", "{\"v1_action_use_text\":null}")) {
            assertFalse(LanguageActivityConfig.from(adapter.fromJson(json)).v1ActionUseText)
        }
    }

    @Test
    fun `v1 action mode follows firebase boolean without changing the delay`() {
        for (useText in listOf(false, true)) {
            val model = adapter.fromJson("{\"v1_action_use_text\":$useText,\"v1_confirm_delay_ms\":1500}")
            val config = LanguageActivityConfig.from(model)
            assertEquals(useText, config.v1ActionUseText)
            assertEquals(1_500L, config.v1ConfirmDelayMs)
        }
    }

    @Test
    fun `v1 confirm delay defaults to three seconds for absent null and negative config`() {
        assertEquals(3_000L, LanguageActivityConfig().v1ConfirmDelayMs)
        assertEquals(3_000L, LanguageActivityConfig.from(null).v1ConfirmDelayMs)
        for (json in listOf("{}", "{\"v1_confirm_delay_ms\":null}", "{\"v1_confirm_delay_ms\":-1}")) {
            assertEquals(3_000L, LanguageActivityConfig.from(adapter.fromJson(json)).v1ConfirmDelayMs)
        }
    }

    @Test
    fun `v1 confirm delay accepts zero and custom milliseconds from remote json`() {
        for (milliseconds in listOf(0L, 1_500L, 3_000L, 5_000L)) {
            val model = adapter.fromJson("{\"v1_confirm_delay_ms\":$milliseconds}")
            assertEquals(milliseconds, LanguageActivityConfig.from(model).v1ConfirmDelayMs)
        }
    }

    @Test
    fun `both flavors ship the same confirm delay and icon action defaults`() {
        for (flavor in listOf("dev", "prod")) {
            val path = "src/$flavor/res/xml/remote_config_defaults.xml"
            val file = listOf(File(path), File("core/config/$path")).first { it.isFile }
            val entries = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                .getElementsByTagName("entry")
            val languageEntries = (0 until entries.length)
                .map { entries.item(it) as org.w3c.dom.Element }
                .filter { it.getElementsByTagName("key").item(0).textContent == "language_activity_config" }
            assertEquals(1, languageEntries.size)
            val json = languageEntries.single().getElementsByTagName("value").item(0).textContent
            val config = LanguageActivityConfig.from(adapter.fromJson(json))
            assertEquals(3_000L, config.v1ConfirmDelayMs)
            assertEquals(false, adapter.fromJson(json)!!.v1ActionUseText)
            assertFalse(config.v1ActionUseText)
        }
    }

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
