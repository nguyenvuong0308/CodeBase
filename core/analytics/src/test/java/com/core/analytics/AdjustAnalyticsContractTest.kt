package com.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AdjustAnalyticsContractTest {
    @Test
    fun `trackRevenueNetwork includes ad format`() {
        var trackedAdFormat: String? = null
        val analytics = object : AdjustAnalytics {
            override fun trackRevenueNetwork(
                adUnitId: String,
                adSourceName: String?,
                adFormat: String,
                adValueMicros: Long,
                adValueCurrencyCode: String
            ) {
                trackedAdFormat = adFormat
            }

            override fun trackPurchase(
                productId: String,
                purchaseToken: String,
                orderId: String?,
                signature: String,
                purchaseTime: Long,
                productType: String,
                priceAmountMicros: Long,
                priceCurrencyCode: String
            ) = Unit
        }

        analytics.trackRevenueNetwork(
            adUnitId = "test-ad-unit",
            adSourceName = "test-network",
            adFormat = "NATIVE",
            adValueMicros = 1_000L,
            adValueCurrencyCode = "USD"
        )

        assertEquals("NATIVE", trackedAdFormat)
    }
}
