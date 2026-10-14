package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class CollapsibleInlineConfigTest {
    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)
    private val mapper = AdPlaceModelMapper(
        mock(IAppProviderAdPlaceName::class.java), mock(Context::class.java),
    )

    @Test
    fun `remote flag is enabled only for explicit true`() {
        for ((value, expected) in listOf("true" to true, "false" to false, "null" to false)) {
            val model = adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native",
                "is_native_collapsible":true, "always_hide_inline_native":$value
            }""")!!

            assertEquals(expected, (mapper.toData(model) as NativeAdPlace).alwaysHideInlineNative)
        }
    }

    @Test
    fun `missing remote flag preserves existing inline behavior`() {
        val model = adapter.fromJson("""{
            "place_name":"action_app_open", "ad_type":"native", "is_native_collapsible":true
        }""")!!

        assertEquals(false, (mapper.toData(model) as NativeAdPlace).alwaysHideInlineNative)
    }
}
