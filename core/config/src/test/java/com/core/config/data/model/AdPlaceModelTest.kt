package com.core.config.data.model

import com.core.config.domain.data.NativeExpandTemplate
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPlaceModelTest {

    @Test
    fun `native collapsible flag uses its dedicated remote key`() {
        val model = Moshi.Builder()
            .build()
            .adapter(AdPlaceModel::class.java)
            .fromJson("""{"is_native_collapsible":true}""")

        assertTrue(model?.isNativeCollapsible == true)
        assertNull(model?.isCollapsible)
    }

    @Test
    fun `native expand template supports v2 remote value`() {
        val model = Moshi.Builder()
            .build()
            .adapter(AdPlaceModel::class.java)
            .fromJson("""{"native_expand_template":"native_expand_v2"}""")

        assertEquals(NativeExpandTemplate.V2, NativeExpandTemplate.getBy(model?.nativeExpandTemplate))
    }

    @Test
    fun `native expand template defaults to v1`() {
        assertEquals(NativeExpandTemplate.V1, NativeExpandTemplate.getBy("native_expand_v1"))
        assertEquals(NativeExpandTemplate.V1, NativeExpandTemplate.getBy(null))
        assertEquals(NativeExpandTemplate.V1, NativeExpandTemplate.getBy("unsupported"))
    }

    @Test
    fun `pip layout format uses its remote config key`() {
        val model = Moshi.Builder()
            .build()
            .adapter(AdPlaceModel::class.java)
            .fromJson("""{"pip_layout_format":"media_card"}""")

        assertEquals("media_card", model?.pipLayoutFormat)
    }

    @Test
    fun `pip remote defaults select media card for every flavor`() {
        listOf("dev", "prod").forEach { flavor ->
            val pipPlace = readBannerNativePlaces(flavor)
                .single { it.adPlace == "anchored_native_pip_home" }

            assertEquals("media_card", pipPlace.pipLayoutFormat)
        }
    }

    private fun readBannerNativePlaces(flavor: String): List<AdPlaceModel> {
        val relativePath = "src/$flavor/res/xml/remote_config_defaults.xml"
        val configFile = sequenceOf(File(relativePath), File("core/config/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")
        val entryRegex = Regex(
            """<entry>\s*<key>banner_native_ad_places(?:_2)?</key>\s*<value>(.*?)</value>\s*</entry>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val jsonEntries = entryRegex.findAll(configFile.readText()).map { it.groupValues[1] }.toList()
        require(jsonEntries.isNotEmpty()) {
            "Cannot find banner_native_ad_places in ${configFile.path}"
        }
        val listType = Types.newParameterizedType(List::class.java, AdPlaceModel::class.java)
        val adapter = Moshi.Builder().build().adapter<List<AdPlaceModel>>(listType)

        return jsonEntries.flatMap { json -> adapter.fromJson(json).orEmpty() }
    }
}
