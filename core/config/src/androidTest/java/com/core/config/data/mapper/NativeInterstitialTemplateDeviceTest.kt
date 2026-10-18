package com.core.config.data.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.core.config.data.model.AdPlaceModel
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.config.domain.data.NativeTemplateSize
import com.squareup.moshi.Moshi
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies JSON parsing and template mapping in the installed Android build. */
@RunWith(AndroidJUnit4::class)
class NativeInterstitialTemplateDeviceTest {
    private val mapper = AdPlaceModelMapper(
        object : IAppProviderAdPlaceName {
            override fun findAdPlaceName(key: String): IAdPlaceName? = null
        },
        InstrumentationRegistry.getInstrumentation().targetContext,
    )
    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)

    @Test
    fun missingAndInvalidTemplatesFallBackToV3() {
        listOf("", ",\"native_template_size\":null", ",\"native_template_size\":\"\"",
            ",\"native_template_size\":\"small\"", ",\"native_template_size\":\"typo\"").forEach { field ->
            val place = mapper.toData(adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native_interstitial" $field
            }""")!!) as NativeAdPlace
            assertSame("JSON field: $field", NativeTemplateSize.FullInterstitialV3, place.nativeTemplateSize)
        }
    }

    @Test
    fun supportedFullscreenTemplatesArePreserved() {
        listOf(NativeTemplateSize.FullInterstitialV1, NativeTemplateSize.FullInterstitialV2,
            NativeTemplateSize.FullInterstitialV3, NativeTemplateSize.FullInterstitialV4,
            NativeTemplateSize.FullInterstitialV5, NativeTemplateSize.FullInterstitialV6).forEach { template ->
            val place = mapper.toData(adapter.fromJson("""{
                "place_name":"action_app_open", "ad_type":"native_interstitial",
                "native_template_size":"${template.key}"
            }""")!!) as NativeAdPlace
            assertSame(template, place.nativeTemplateSize)
        }
    }
}
