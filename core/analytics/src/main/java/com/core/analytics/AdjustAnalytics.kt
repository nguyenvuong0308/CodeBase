package com.core.analytics

interface AdjustAnalytics {
    fun trackRevenueNetwork(
        adUnitId: String,
        adSourceName: String?,
        adFormat: String,
        adValueMicros: Long,
        adValueCurrencyCode: String
    )

    fun trackPurchase(
        productId: String,
        purchaseToken: String,
        orderId: String?,
        signature: String,
        purchaseTime: Long,
        productType: String,
        priceAmountMicros: Long,
        priceCurrencyCode: String
    )
}
