package com.codebasetemplate.config

import com.google.gson.Gson
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LowDeviceRemoteConfigDefaultsTest {
    @Test
    fun `both flavors declare the flag on placements and never on type configs`() {
        for (flavor in listOf("dev", "prod")) {
            val path = "src/$flavor/res/xml/remote_config_defaults.xml"
            val file = sequenceOf(File(path), File("app/$path")).first(File::isFile)
            val entries = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(file).getElementsByTagName("entry")
            var placementCount = 0
            for (index in 0 until entries.length) {
                val entry = entries.item(index) as Element
                val key = entry.getElementsByTagName("key").item(0).textContent
                val json = entry.getElementsByTagName("value").item(0).textContent.trim()
                if (!json.startsWith("{") && !json.startsWith("[")) continue
                val value = Gson().fromJson(json, Any::class.java)
                if (key.endsWith("_ad_config")) {
                    assertFalse("$flavor/$key", (value as Map<*, *>).containsKey("is_disable_for_low_device"))
                }
                placementCount += checkPlacements(value)
            }
            assertTrue("No placements checked in $flavor", placementCount > 0)
        }
    }

    private fun checkPlacements(value: Any?): Int = when (value) {
        is Map<*, *> -> {
            val isPlacement = value.containsKey("place_name")
            if (isPlacement) {
                assertEquals("Placement ${value["place_name"]}", false, value["is_disable_for_low_device"])
            }
            (if (isPlacement) 1 else 0) + value.values.sumOf(::checkPlacements)
        }
        is List<*> -> value.sumOf(::checkPlacements)
        else -> 0
    }
}
