package com.core.config.data.mapper

import android.content.Context
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class CollapsiblePopupMarginConfigTest {
    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)
    private val mapper = AdPlaceModelMapper(
        mock(IAppProviderAdPlaceName::class.java), mock(Context::class.java),
    )

    @Test
    fun `firebase supports independent fractional margins`() {
        val place = parse(""", "native_expand_margin_left_dp":12.5, "native_expand_margin_right_dp":24""")
        assertEquals(12.5f, place.nativeExpandMarginLeftDp, 0f)
        assertEquals(24f, place.nativeExpandMarginRightDp, 0f)
    }

    @Test
    fun `missing null and negative margins fall back to zero`() {
        listOf(
            "",
            """, "native_expand_margin_left_dp":null, "native_expand_margin_right_dp":null""",
            """, "native_expand_margin_left_dp":-16, "native_expand_margin_right_dp":-8""",
        ).forEach { fields ->
            val place = parse(fields)
            assertEquals(0f, place.nativeExpandMarginLeftDp, 0f)
            assertEquals(0f, place.nativeExpandMarginRightDp, 0f)
        }
    }

    @Test
    fun `one configured margin leaves the other side unchanged`() {
        val place = parse(""", "native_expand_margin_right_dp":20""")
        assertEquals(0f, place.nativeExpandMarginLeftDp, 0f)
        assertEquals(20f, place.nativeExpandMarginRightDp, 0f)
    }

    private fun parse(fields: String): NativeAdPlace = mapper.toData(adapter.fromJson("""{
        "place_name":"action_app_open", "ad_type":"native", "is_native_collapsible":true
        $fields
    }""")!!) as NativeAdPlace
}
