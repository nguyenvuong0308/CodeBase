package com.core.config.data.mapper

import com.core.config.data.helper.ConfigParam
import com.core.config.data.model.InterstitialAdConfigModel
import com.squareup.moshi.Moshi
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InterstitialAdConfigModelMapperTest {

    private val adapter = Moshi.Builder()
        .build()
        .adapter(InterstitialAdConfigModel::class.java)
    private val mapper = InterstitialAdConfigModelMapper()

    @Test
    fun `maps meaningful actions from remote config`() {
        val model = adapter.fromJson(
            """{"meaningful_actions_between_interstitial":3}"""
        )

        assertNotNull(model)
        assertEquals(3, mapper.toData(model!!).meaningfulActionsBetweenInterstitial)
    }

    @Test
    fun `defaults meaningful actions to zero when remote field is absent`() {
        val model = adapter.fromJson("{}")

        assertNotNull(model)
        assertEquals(
            ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_MEANINGFUL_ACTIONS,
            mapper.toData(model!!).meaningfulActionsBetweenInterstitial
        )
    }

    @Test
    fun `core remote config defaults declare meaningful actions for every flavor`() {
        listOf("dev", "prod").forEach { flavor ->
            val model = adapter.fromJson(readInterstitialConfig(flavor))

            assertNotNull(model)
            assertEquals(
                "$flavor should disable meaningful action capping by default",
                ConfigParam.INTERSTITIAL_AD_CONFIG_DEFAULT_MEANINGFUL_ACTIONS,
                model!!.meaningfulActionsBetweenInterstitial
            )
        }
    }

    private fun readInterstitialConfig(flavor: String): String {
        val relativePath = "src/$flavor/res/xml/remote_config_defaults.xml"
        val configFile = sequenceOf(File(relativePath), File("core/config/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")
        val entryRegex = Regex(
            """<entry>\s*<key>interstitial_ad_config</key>\s*<value>(.*?)</value>\s*</entry>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return requireNotNull(entryRegex.find(configFile.readText())) {
            "Cannot find interstitial_ad_config in ${configFile.path}"
        }.groupValues[1]
    }
}
