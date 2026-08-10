package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntNativeExpandViewV2Binding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R
import com.core.utilities.isValidGlideContext
import com.google.android.gms.ads.nativead.NativeAd

/** Media-first expanded native renderer used only by [CollapsibleNativeHostView]. */
internal class NativeExpandViewV2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntNativeExpandViewV2Binding by lazy {
        GntNativeExpandViewV2Binding.inflate(LayoutInflater.from(context), this)
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.bodyView = binding.body
        binding.nativeAdView.iconView = binding.icon
        binding.nativeAdView.mediaView = binding.mediaView

        binding.primary.text = nativeAd.headline.orEmpty()
        binding.cta.text = nativeAd.callToAction.orEmpty()
        binding.body.text = nativeAd.body.orEmpty()
        binding.body.isVisible = !nativeAd.body.isNullOrBlank()

        binding.icon.isVisible = nativeAd.icon != null
        nativeAd.icon?.let { icon ->
            if (context.isValidGlideContext()) {
                loadIcon(binding.icon, icon.drawable)
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
            binding.body.background = it
        }

        styles.primaryTextTypeface?.let { binding.primary.typeface = it }
        styles.tertiaryTextTypeface?.let { binding.body.typeface = it }
        styles.callToActionTextTypeface?.let { binding.cta.typeface = it }
        styles.primaryTextTypefaceColor?.let { binding.primary.setTextColor(it.toColorInt()) }
        styles.tertiaryTextTypefaceColor?.let { binding.body.setTextColor(it.toColorInt()) }
        styles.callToActionTypefaceColor?.let(binding.cta::setTextColor)

        styles.callToActionTextSize.takeIf { it > 0 }?.let { binding.cta.textSize = it }
        styles.primaryTextSize.takeIf { it > 0 }?.let { binding.primary.textSize = it }
        styles.tertiaryTextSize.takeIf { it > 0 }?.let { binding.body.textSize = it }

        styles.callToActionBackgroundColor?.let(binding.layoutCta::updateBackgroundColor)
        styles.callToActionRadius?.let { binding.layoutCta.updateRadius(it.toFloat()) }

        styles.borderColor?.let { color ->
            (binding.nativeAdView.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                color.toColorInt(),
            )
        }
        styles.backgroundColor?.let { color ->
            (binding.nativeAdView.background as? GradientDrawable)?.setColor(color.toColorInt())
        }
        styles.backgroundResource?.let(binding.background::setBackgroundResource)
        styles.backgroundAdsNotifyView?.let(binding.adNotificationView::setBackgroundResource)
        applyAdsNotifyViewStyles(styles, binding.adNotificationView)

        styles.primaryTextBackgroundColor?.let { binding.primary.background = it }
        styles.tertiaryTextBackgroundColor?.let { binding.body.background = it }

        styles.backgroundRadius?.let { radius ->
            (binding.nativeAdView.background as? GradientDrawable)?.cornerRadius =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radius.toFloat(),
                    resources.displayMetrics,
                )
        }
        styles.callToActionBorderColor?.let { color ->
            (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                color.toColorInt(),
            )
        }

        invalidate()
        requestLayout()
    }

    private fun loadIcon(imageView: ImageView, drawable: Any?) {
        Glide.with(this)
            .load(drawable)
            .override(resources.getDimensionPixelSize(R.dimen._46dp))
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        resources.getDimensionPixelSize(R.dimen._8dp),
                        0,
                        RoundedCornersTransformation.CornerType.ALL,
                    ),
                ),
            )
            .into(imageView)
    }
}
