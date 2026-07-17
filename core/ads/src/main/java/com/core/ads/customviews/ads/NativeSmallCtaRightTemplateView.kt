package com.core.ads.customviews.ads

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntSmallCtaRightBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R
import com.core.utilities.dpToPx
import com.google.android.gms.ads.nativead.NativeAd

class NativeSmallCtaRightTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntSmallCtaRightBinding by lazy {
        GntSmallCtaRightBinding.inflate(LayoutInflater.from(context), this)
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {}

    override fun setNativeAd(nativeAd: NativeAd) {

        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary

        binding.primary.text = nativeAd.headline
        binding.cta.text = nativeAd.callToAction

        binding.icon.visibility = GONE
        nativeAd.icon?.let {
            binding.icon.visibility = VISIBLE

            Glide.with(this)
                .load(it.drawable)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .apply(
                    RequestOptions.bitmapTransform(
                        RoundedCornersTransformation(
                            context.resources.getDimensionPixelSize(
                                R.dimen._8dp
                            ), 0, RoundedCornersTransformation.CornerType.ALL
                        )
                    )
                )
                .into(binding.icon)
        }

        nativeAd.body?.let {
            binding.body.text = it
            binding.nativeAdView.bodyView = binding.body
        }

//        val extras = nativeAd.extras
//        if (extras.containsKey(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)) {
//            val socialContext = extras.get(FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET)
//            if (socialContext is String) {
//                if (binding.primary.text.isBlank()) {
//                    binding.primary.text = socialContext
//                } else {
//                    if (binding.body.text.isBlank()) {
//                        binding.body.text = socialContext
//                    }
//                }
//            }
//        }
        binding.nativeAdView.setNativeAd(nativeAd)
    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * https://developers.google.com/admob/android/native-unified#destroy_ad
     */
    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.primary.background = it
        }

        styles.primaryTextTypeface?.let {
            binding.primary.typeface = it
        }

        styles.callToActionTextTypeface?.let {
            binding.cta.typeface = it
        }

        styles.primaryTextTypefaceColor?.let {
            binding.primary.setTextColor(it.toColorInt())
        }

        styles.tertiaryTextTypefaceColor?.let {
            binding.body.setTextColor(it.toColorInt())
        }

        styles.callToActionTypefaceColor?.let {
            binding.cta.setTextColor(it)
        }

        val ctaTextSize = styles.callToActionTextSize
        if (ctaTextSize > 0) {
            binding.cta.textSize = ctaTextSize
        }

        val primaryTextSize = styles.primaryTextSize
        if (primaryTextSize > 0) {
            binding.primary.textSize = primaryTextSize
        }

         styles.callToActionBackgroundColor?.let {
             binding.layoutCta.updateBackgroundColor(it)
        }

        styles.callToActionRadius?.let {
            binding.layoutCta.updateRadius(it.toFloat())
        }

        styles.borderColor?.let {
            (binding.nativeAdView.background as GradientDrawable).setStroke(resources.getDimensionPixelSize(R.dimen._1dp), it.toColorInt())
        }

        styles.backgroundColor?.let {
            (binding.nativeAdView.background as GradientDrawable).setColor(it.toColorInt())
        }

        styles.backgroundResource?.let {
            binding.background.setBackgroundResource(it)
        }

        styles.backgroundAdsNotifyView?.let {
            binding.adNotificationView.setBackgroundResource(it)
        }
        applyAdsNotifyViewStyles(styles, binding.adNotificationView)

        styles.primaryTextBackgroundColor?.let {
            binding.primary.background = it
        }

        styles.backgroundRadius?.let { radius ->
            val bg = binding.nativeAdView.background
            if (bg is GradientDrawable) {
                val radiusPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radius.toFloat(),
                    resources.displayMetrics
                )
                bg.cornerRadius = radiusPx
            }
        }

        styles.callToActionBorderColor?.let {
            (binding.layoutCta.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(
                    R.dimen._1dp
                ), it.toColorInt()
            )
        }
        invalidate()
        requestLayout()
    }
}
