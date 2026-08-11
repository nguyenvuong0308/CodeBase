package com.core.ads.customviews.ads

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.core.ads.extensions.updateBackgroundColor
import com.core.dimens.R as DimenR
import com.google.android.gms.ads.nativead.NativeAd

abstract class BaseNativeTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onClose: (() -> Unit) ?= null
    private var adsNotifyViewStyles: NativeTemplateStyle? = null
    abstract fun destroyNativeAd()

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        try {
//            removeAllViews()
//            destroyNativeAd()
//        } catch (e: Exception) {
//            Log.e("BaseNativeTemplateView", "onDetachedFromWindow : $e")
//        }

    }

    abstract fun setNativeAd(nativeAd: NativeAd)

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * https://developers.google.com/admob/android/native-unified#destroy_ad
     */
    abstract fun applyStyles(styles: NativeTemplateStyle)

    open fun onHostPause() = Unit

    open fun onHostResume() = Unit

    protected fun applyAdsNotifyViewStyles(styles: NativeTemplateStyle, vararg views: TextView) {
        adsNotifyViewStyles = styles
        updateAdsNotifyViews(styles, *views)
    }

    protected fun reapplyAdsNotifyViewStyles(vararg views: TextView) {
        adsNotifyViewStyles?.let { styles ->
            updateAdsNotifyViews(styles, *views)
        }
    }

    private fun updateAdsNotifyViews(styles: NativeTemplateStyle, vararg views: TextView) {
        styles.backgroundColorAdsNotifyView?.let { color ->
            views.forEach { view ->
                view.updateBackgroundColor(color)
            }
        }

        styles.textColorAdsNotifyView?.let { color ->
            runCatching {
                val colorInt = color.toColorInt()
                views.forEach { view ->
                    view.setTextColor(colorInt)
                }
            }
        }
    }

    /**
     * Maps values from 1 to 35 to the matching responsive dimension token
     * (for example, 14 maps to @dimen/_14dp). Other values keep the layout default.
     */
    protected fun TextView.applyTextSizeFromDpDimen(textSizeDp: Float) {
        val textSizeDpInt = textSizeDp.toInt()
        if (textSizeDpInt <= 0 || textSizeDpInt.toFloat() != textSizeDp) return

        val dimenResourceId = when (textSizeDpInt) {
            1 -> DimenR.dimen._1dp
            2 -> DimenR.dimen._2dp
            3 -> DimenR.dimen._3dp
            4 -> DimenR.dimen._4dp
            5 -> DimenR.dimen._5dp
            6 -> DimenR.dimen._6dp
            7 -> DimenR.dimen._7dp
            8 -> DimenR.dimen._8dp
            9 -> DimenR.dimen._9dp
            10 -> DimenR.dimen._10dp
            11 -> DimenR.dimen._11dp
            12 -> DimenR.dimen._12dp
            13 -> DimenR.dimen._13dp
            14 -> DimenR.dimen._14dp
            15 -> DimenR.dimen._15dp
            16 -> DimenR.dimen._16dp
            17 -> DimenR.dimen._17dp
            18 -> DimenR.dimen._18dp
            19 -> DimenR.dimen._19dp
            20 -> DimenR.dimen._20dp
            21 -> DimenR.dimen._21dp
            22 -> DimenR.dimen._22dp
            23 -> DimenR.dimen._23dp
            24 -> DimenR.dimen._24dp
            25 -> DimenR.dimen._25dp
            26 -> DimenR.dimen._26dp
            27 -> DimenR.dimen._27dp
            28 -> DimenR.dimen._28dp
            29 -> DimenR.dimen._29dp
            30 -> DimenR.dimen._30dp
            31 -> DimenR.dimen._31dp
            32 -> DimenR.dimen._32dp
            33 -> DimenR.dimen._33dp
            34 -> DimenR.dimen._34dp
            35 -> DimenR.dimen._35dp
            else -> return
        }

        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(dimenResourceId))
    }

    fun adHasOnlyStore(nativeAd: NativeAd): Boolean {
        val store = nativeAd.store
        val advertiser = nativeAd.advertiser
        return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser)
    }
}
