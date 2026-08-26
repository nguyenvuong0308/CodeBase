package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.core.ads.databinding.GntMediumMediaLeftCtaRightBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.dimens.R
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Medium native template with a large media asset on the left and ad copy plus CTA on the right.
 */
class NativeMediumMediaLeftCtaRightTemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntMediumMediaLeftCtaRightBinding by lazy {
        GntMediumMediaLeftCtaRightBinding.inflate(LayoutInflater.from(context), this)
    }

    init {
        binding.mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.bodyView = binding.body
        binding.nativeAdView.mediaView = binding.mediaView

        binding.primary.text = nativeAd.headline
        binding.body.text = nativeAd.body.orEmpty()
        binding.cta.text = nativeAd.callToAction

        binding.nativeAdView.setNativeAd(nativeAd)
    }

    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.primary.background = it
            binding.body.background = it
        }

        styles.primaryTextTypeface?.let(binding.primary::setTypeface)
        styles.tertiaryTextTypeface?.let(binding.body::setTypeface)
        styles.callToActionTextTypeface?.let(binding.cta::setTypeface)

        styles.primaryTextTypefaceColor?.let {
            binding.primary.setTextColor(it.toColorInt())
        }
        styles.tertiaryTextTypefaceColor?.let {
            binding.body.setTextColor(it.toColorInt())
        }
        styles.callToActionTypefaceColor?.let(binding.cta::setTextColor)

        if (styles.callToActionTextSize > 0) {
            binding.cta.applyTextSizeFromDpDimen(styles.callToActionTextSize)
        }
        if (styles.primaryTextSize > 0) {
            binding.primary.applyTextSizeFromDpDimen(styles.primaryTextSize)
        }
        if (styles.tertiaryTextSize > 0) {
            binding.body.applyTextSizeFromDpDimen(styles.tertiaryTextSize)
        }

        styles.callToActionBackgroundColor?.let(binding.layoutCta::updateBackgroundColor)
        styles.callToActionRadius?.let { binding.layoutCta.updateRadius(it.toFloat()) }
        styles.callToActionBorderColor?.let {
            (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt(),
            )
        }

        styles.borderColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt(),
            )
        }
        styles.backgroundColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setColor(it.toColorInt())
        }
        styles.backgroundResource?.let(binding.background::setBackgroundResource)
        styles.backgroundRadius?.let { radius ->
            (binding.nativeAdView.background as? GradientDrawable)?.cornerRadius =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radius.toFloat(),
                    resources.displayMetrics,
                )
        }

        styles.backgroundAdsNotifyView?.let(binding.adNotificationView::setBackgroundResource)
        applyAdsNotifyViewStyles(styles, binding.adNotificationView)
        styles.primaryTextBackgroundColor?.let { binding.primary.background = it }
        styles.tertiaryTextBackgroundColor?.let { binding.body.background = it }

        invalidate()
        requestLayout()
    }
}
