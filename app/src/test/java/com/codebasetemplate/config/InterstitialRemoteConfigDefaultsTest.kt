package com.codebasetemplate.config

import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class InterstitialRemoteConfigDefaultsTest {

    @Test
    fun `app remote config defaults declare expected meaningful actions for every flavor`() {
        mapOf("dev" to 3, "prod" to 0).forEach { (flavor, expectedValue) ->
            val config = JsonParser.parseString(readInterstitialConfig(flavor)).asJsonObject

            assertEquals(
                "$flavor should use its configured meaningful action default",
                expectedValue,
                config["meaningful_actions_between_interstitial"].asInt
            )
        }
    }

    private fun readInterstitialConfig(flavor: String): String {
        val relativePath = "src/$flavor/res/xml/remote_config_defaults.xml"
        val configFile = sequenceOf(File(relativePath), File("app/$relativePath"))
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
