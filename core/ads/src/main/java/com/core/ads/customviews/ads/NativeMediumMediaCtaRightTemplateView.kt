package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntMediumMediaCtaRightBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.utilities.isValidGlideContext
import com.google.android.gms.ads.nativead.NativeAd
import com.core.dimens.R as DimenR

/**
 * Medium native template with app identity in the header, a 124dp-high media row, and a
 * description plus CTA in the footer.
 */
class NativeMediumMediaCtaRightTemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntMediumMediaCtaRightBinding by lazy {
        GntMediumMediaCtaRightBinding.inflate(LayoutInflater.from(context), this)
    }

    init {
        binding.mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.bodyView = binding.body
        binding.nativeAdView.iconView = binding.icon
        binding.nativeAdView.mediaView = binding.mediaView

        binding.primary.text = nativeAd.headline.orEmpty()
        binding.body.text = nativeAd.body.orEmpty()
        binding.cta.text = nativeAd.callToAction.orEmpty()

        binding.icon.visibility = GONE
        nativeAd.icon?.drawable?.let { iconDrawable ->
            binding.icon.visibility = VISIBLE
            if (context.isValidGlideContext()) {
                Glide.with(this)
                    .load(iconDrawable)
                    .override(resources.getDimensionPixelSize(DimenR.dimen._44dp))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .apply(
                        RequestOptions.bitmapTransform(
                            RoundedCornersTransformation(
                                resources.getDimensionPixelSize(DimenR.dimen._8dp),
                                0,
                                RoundedCornersTransformation.CornerType.ALL,
                            )
                        )
                    )
                    .into(binding.icon)
            }
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
            binding.sponsored.background = it
            binding.body.background = it
        }

        styles.primaryTextTypeface?.let(binding.primary::setTypeface)
        styles.tertiaryTextTypeface?.let {
            binding.sponsored.typeface = it
            binding.body.typeface = it
        }
        styles.callToActionTextTypeface?.let(binding.cta::setTypeface)

        styles.primaryTextTypefaceColor?.let {
            binding.primary.setTextColor(it.toColorInt())
        }
        styles.tertiaryTextTypefaceColor?.let {
            val color = it.toColorInt()
            binding.sponsored.setTextColor(color)
            binding.body.setTextColor(color)
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
                resources.getDimensionPixelSize(DimenR.dimen._1dp),
                it.toColorInt(),
            )
        }

        styles.borderColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(DimenR.dimen._1dp),
                it.toColorInt(),
            )
        }
        styles.backgroundColor?.let {
            (binding.nativeAdView.background as? GradientDrawable)?.setColor(it.toColorInt())
        }
        styles.mediaBackgroundColor?.let {
            runCatching { binding.mediaView.setBackgroundColor(it.toColorInt()) }
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
        styles.tertiaryTextBackgroundColor?.let {
            binding.sponsored.background = it
            binding.body.background = it
        }

        invalidate()
        requestLayout()
    }
}
