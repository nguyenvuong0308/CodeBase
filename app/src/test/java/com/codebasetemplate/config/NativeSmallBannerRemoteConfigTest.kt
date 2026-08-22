package com.codebasetemplate.config

import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeSmallBannerRemoteConfigTest {

    @Test
    fun `dev anchored native test uses small banner cta right`() {
        val places = JsonParser.parseString(readBannerNativeAdPlaces("dev")).asJsonArray
        val testPlace = places
            .map { it.asJsonObject }
            .single { it["place_name"].asString == "anchored_native_test" }

        assertEquals("native", testPlace["ad_type"].asString)
        assertEquals("small_banner_cta_right", testPlace["native_template_size"].asString)
    }

    private fun readBannerNativeAdPlaces(flavor: String): String {
        val relativePath = "src/$flavor/res/xml/remote_config_defaults.xml"
        val configFile = sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")
        val entryRegex = Regex(
            """<entry>\s*<key>banner_native_ad_places</key>\s*<value>(.*?)</value>\s*</entry>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        return requireNotNull(entryRegex.find(configFile.readText())) {
            "Cannot find banner_native_ad_places in ${configFile.path}"
        }.groupValues[1]
    }
}
