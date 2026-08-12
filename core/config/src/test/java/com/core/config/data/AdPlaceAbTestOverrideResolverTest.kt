package com.core.config.data

import com.core.config.data.model.AdPlaceModel
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AdPlaceAbTestOverrideResolverTest {

    private val adapter = Moshi.Builder().build().adapter(AdPlaceModel::class.java)

    @Test
    fun `valid override replaces the complete base object`() {
        val base = model(
            """{
                "place_name":"home",
                "ad_id":"base-id",
                "ad_type":"native",
                "is_enable":true,
                "native_template_size":"small"
            }"""
        )
        val override = model(
            """{
                "place_name":"home",
                "ad_id":"variant-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )

        val result = AdPlaceAbTestOverrideResolver.resolve(
            basePlaces = listOf(base),
            configuredPlaceNames = listOf("home"),
            overrideProvider = { override },
        )

        assertSame(override, result.single())
        assertEquals("variant-id", result.single().adId)
        assertNull(result.single().nativeTemplateSize)
    }

    @Test
    fun `missing or malformed override keeps base object`() {
        val base = model(
            """{
                "place_name":"home",
                "ad_id":"base-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )

        val result = AdPlaceAbTestOverrideResolver.resolve(
            basePlaces = listOf(base),
            configuredPlaceNames = listOf("home"),
            overrideProvider = { null },
        )

        assertSame(base, result.single())
    }

    @Test
    fun `override with different place name keeps base object`() {
        val base = model(
            """{
                "place_name":"home",
                "ad_id":"base-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )
        val wrongOverride = model(
            """{
                "place_name":"other",
                "ad_id":"variant-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )

        val result = AdPlaceAbTestOverrideResolver.resolve(
            basePlaces = listOf(base),
            configuredPlaceNames = listOf("home"),
            overrideProvider = { wrongOverride },
        )

        assertSame(base, result.single())
    }

    @Test
    fun `incomplete override keeps base object`() {
        val base = model(
            """{
                "place_name":"home",
                "ad_id":"base-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )
        val incompleteOverride = model(
            """{
                "place_name":"home",
                "ad_type":"native",
                "is_enable":true
            }"""
        )

        val result = AdPlaceAbTestOverrideResolver.resolve(
            basePlaces = listOf(base),
            configuredPlaceNames = listOf("home"),
            overrideProvider = { incompleteOverride },
        )

        assertSame(base, result.single())
    }

    @Test
    fun `unknown configured place does not read an override`() {
        val base = model(
            """{
                "place_name":"home",
                "ad_id":"base-id",
                "ad_type":"native",
                "is_enable":true
            }"""
        )
        var requestedPlaceName: String? = null

        val result = AdPlaceAbTestOverrideResolver.resolve(
            basePlaces = listOf(base),
            configuredPlaceNames = listOf("unknown"),
            overrideProvider = {
                requestedPlaceName = it
                null
            },
        )

        assertSame(base, result.single())
        assertNull(requestedPlaceName)
    }

    private fun model(json: String): AdPlaceModel = requireNotNull(adapter.fromJson(json))
}
