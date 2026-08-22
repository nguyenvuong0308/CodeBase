package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntSmallBannerCtaRightBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R
import com.google.android.gms.ads.nativead.NativeAd

class NativeSmallBannerCtaRightTemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntSmallBannerCtaRightBinding by lazy {
        GntSmallBannerCtaRightBinding.inflate(LayoutInflater.from(context), this)
    }

    init {
        binding.root
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary

        binding.primary.text = nativeAd.headline
        binding.cta.text = nativeAd.callToAction

        binding.icon.visibility = GONE
        nativeAd.icon?.let { icon ->
            binding.icon.visibility = VISIBLE
            Glide.with(this)
                .load(icon.drawable)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .apply(
                    RequestOptions.bitmapTransform(
                        RoundedCornersTransformation(
                            context.resources.getDimensionPixelSize(R.dimen._8dp),
                            0,
                            RoundedCornersTransformation.CornerType.ALL,
                        )
                    )
                )
                .into(binding.icon)
        }

        nativeAd.body?.let { body ->
            binding.body.text = body
            binding.nativeAdView.bodyView = binding.body
        }

        binding.nativeAdView.setNativeAd(nativeAd)
    }

    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.primary.background = it
        }
        styles.primaryTextTypeface?.let { binding.primary.typeface = it }
        styles.callToActionTextTypeface?.let { binding.cta.typeface = it }
        styles.primaryTextTypefaceColor?.let { binding.primary.setTextColor(it.toColorInt()) }
        styles.tertiaryTextTypefaceColor?.let { binding.body.setTextColor(it.toColorInt()) }
        styles.callToActionTypefaceColor?.let { binding.cta.setTextColor(it) }

        if (styles.callToActionTextSize > 0) {
            binding.cta.applyTextSizeFromDpDimen(styles.callToActionTextSize)
        }
        if (styles.primaryTextSize > 0) {
            binding.primary.applyTextSizeFromDpDimen(styles.primaryTextSize)
        }
        if (styles.tertiaryTextSize > 0) {
            binding.body.applyTextSizeFromDpDimen(styles.tertiaryTextSize)
        }

        styles.callToActionBackgroundColor?.let { binding.layoutCta.updateBackgroundColor(it) }
        styles.callToActionRadius?.let { binding.layoutCta.updateRadius(it.toFloat()) }
        styles.borderColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt(),
            )
        }
        styles.backgroundColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setColor(it.toColorInt())
        }
        styles.backgroundResource?.let { binding.background.setBackgroundResource(it) }
        styles.backgroundAdsNotifyView?.let {
            binding.adNotificationView.setBackgroundResource(it)
        }
        applyAdsNotifyViewStyles(styles, binding.adNotificationView)
        styles.primaryTextBackgroundColor?.let { binding.primary.background = it }
        styles.backgroundRadius?.let { radius ->
            (binding.nativeAdView.background as? GradientDrawable)?.cornerRadius =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radius.toFloat(),
                    resources.displayMetrics,
                )
        }
        styles.callToActionBorderColor?.let {
            (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt(),
            )
        }

        invalidate()
        requestLayout()
    }
}
