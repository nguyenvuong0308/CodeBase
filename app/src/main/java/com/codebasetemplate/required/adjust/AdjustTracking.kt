package com.codebasetemplate.required.adjust

import android.util.Log
//import com.adjust.sdk.Adjust
//import com.adjust.sdk.AdjustAdRevenue
//import com.adjust.sdk.AdjustEvent
import com.core.analytics.AdjustAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdjustTracking @Inject constructor() : AdjustAnalytics {
    override fun trackRevenueNetwork(
        adUnitId: String,
        adSourceName: String?,
        adFormat: String,
        adValueMicros: Long,
        adValueCurrencyCode: String,
    ) {
//        if (adValueMicros <= 0L || adValueCurrencyCode.isBlank()) {
//            Log.e(TAG, "trackRevenueNetwork: invalid ad revenue for $adUnitId adValueMicros $adValueMicros adValueCurrencyCode $adValueCurrencyCode")
//            return
//        }
//        val adRevenue = AdjustAdRevenue(AD_REVENUE_SOURCE_ADMOB)
//        adRevenue.setRevenue(adValueMicros.toRevenueAmount(), adValueCurrencyCode)
//        adSourceName?.takeIf { it.isNotBlank() }?.let(adRevenue::setAdRevenueNetwork)
//        adRevenue.setAdRevenuePlacement(adFormat)
//        adRevenue.setAdRevenueUnit(adUnitId)
//        adRevenue.addPartnerParameter("ad_format", adFormat)
//        adRevenue.addPartnerParameter("ad_unit_id", adUnitId)
//        Adjust.trackAdRevenue(adRevenue)
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
    ) {
//        if (productType == PRODUCT_TYPE_SUBS) {
//            trackSubscription(
//                productId = productId,
//                purchaseToken = purchaseToken,
//                orderId = orderId,
//                priceAmountMicros = priceAmountMicros,
//                priceCurrencyCode = priceCurrencyCode
//            )
//            return
//        }
//
//        if (PURCHASE_EVENT_TOKEN.isBlank()) {
//            Log.e(TAG, "trackPurchase: missing Adjust purchase event token")
//            return
//        }
//        if (priceAmountMicros <= 0L || priceCurrencyCode.isBlank()) {
//            Log.e(TAG, "trackPurchase: invalid price for $productId $priceAmountMicros $priceCurrencyCode")
//            return
//        }
//
//        val event = AdjustEvent(PURCHASE_EVENT_TOKEN)
//        event.setRevenue(priceAmountMicros.toRevenueAmount(), priceCurrencyCode)
//        event.setProductId(productId)
//        event.setPurchaseToken(purchaseToken)
//        event.setDeduplicationId(orderId?.takeIf { it.isNotBlank() } ?: purchaseToken)
//
//        Adjust.trackEvent(event)

    }

    private fun trackSubscription(
        productId: String,
        purchaseToken: String,
        orderId: String?,
        priceAmountMicros: Long,
        priceCurrencyCode: String
    ) {
//        if (SUBS_EVENT_TOKEN.isBlank()) {
//            Log.e(TAG, "trackSubscription: missing Adjust subscription event token")
//            return
//        }
//        if (priceAmountMicros <= 0L || priceCurrencyCode.isBlank()) {
//            Log.e(TAG, "trackSubscription: invalid price for $productId $priceAmountMicros $priceCurrencyCode")
//            return
//        }
//
//        // Khởi tạo Event thay vì AdjustPlayStoreSubscription
//        val event = AdjustEvent(SUBS_EVENT_TOKEN)
//        event.setRevenue(priceAmountMicros.toRevenueAmount(), priceCurrencyCode)
//        event.setProductId(productId)
//        event.setPurchaseToken(purchaseToken)
//
//        // Bắt buộc set DeduplicationId để Google Ads và Adjust không tính trùng doanh thu
//        event.setDeduplicationId(orderId?.takeIf { it.isNotBlank() } ?: purchaseToken)
//
//        Adjust.trackEvent(event)
//        Log.d(TAG, "trackSubscription: Tracked Subscription event directly for productId=$productId")
    }

    private fun Long.toRevenueAmount(): Double = this / MICROS_PER_UNIT

    private companion object {
        const val TAG = "AdjustTracking"
        const val SUBS_EVENT_TOKEN = ""
        const val PURCHASE_EVENT_TOKEN = ""
        const val PRODUCT_TYPE_SUBS = "subs"
        const val AD_REVENUE_SOURCE_ADMOB = "admob_sdk"
        const val MICROS_PER_UNIT = 1_000_000.0
    }
}
