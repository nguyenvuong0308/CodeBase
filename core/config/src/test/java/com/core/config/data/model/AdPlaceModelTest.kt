package com.core.config.data.model

import com.core.config.domain.data.NativeExpandTemplate
import com.squareup.moshi.Moshi
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
}
