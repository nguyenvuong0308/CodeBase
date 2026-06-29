package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.core.ads.R
import com.core.ads.databinding.GntFullCtaBottomTemplateViewOnboardingBinding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateBackgroundColorBottomToTop
import com.core.ads.extensions.updateRadius
import com.core.utilities.margin
import com.core.utilities.padding
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd

class NativeFullCtaBottomOnboardingTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private val binding: GntFullCtaBottomTemplateViewOnboardingBinding by lazy {
        GntFullCtaBottomTemplateViewOnboardingBinding.inflate(LayoutInflater.from(context), this)
    }

    private var isEnableImmersive: Boolean = false

    init {
        initView(context)
    }

    private fun initView(context: Context) {}

    override fun setNativeAd(nativeAd: NativeAd) {

        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.mediaView = binding.mediaView
        binding.mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)

        binding.primary.text = nativeAd.headline
        binding.cta.text = nativeAd.callToAction

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

        // Get the video controller for the ad. One will always be provided,
        // even if the ad doesn't have a video asset.
        val videoController = nativeAd.mediaContent?.videoController ?: return

        // Updates the UI to say whether or not this ad has a video asset.
        if (videoController.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController.
            // The VideoController will call methods on this object when events occur in the
            // video lifecycle.
            videoController.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    super.onVideoEnd()
                }
            }
        }

        if (isEnableImmersive && (nativeAd.mediaContent?.aspectRatio ?: 1f) < 1f) { // Nếu bật chế độ trong suốt và mediaview dạng dọc thì hiển thị native dạng trong suốt
            binding.background.margin(left = 0, right = 0)
            binding.background.background = null
            binding.adNotificationView.setBackgroundResource(R.drawable.gnt_rounded_bottom_corner_shape)
            binding.primary.setTextColor(ContextCompat.getColor(context, R.color.neutral_dark_primary))
            binding.primary.setShadowLayer(10f, 2f, 2f, Color.BLACK)
            binding.body.setTextColor(ContextCompat.getColor(context, R.color.neutral_dark_primary))
            binding.body.setShadowLayer(10f, 2f, 2f, Color.BLACK)
            return
        }

        binding.background.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val height = binding.background.height
                if (height <= 0) return
                //native đè lên media view, khi lấy được chiều cao native, padding dưới cho mediaview (trong trường hợp isEnableImmersive false hoặc mediaview dạng ngang/ vuông)
                binding.background.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.mediaView.padding(bottom = height + resources.getDimensionPixelSize(com.core.dimens.R.dimen._20dp))
            }
        })
    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * https://developers.google.com/admob/adroid/native-unified#destroy_ad
     */
    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.primary.background = it
            binding.body.background = it
        }

        styles.primaryTextTypeface?.let {
            binding.primary.typeface = it
        }

        styles.tertiaryTextTypeface?.let {
            binding.body.typeface = it
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


        val tertiaryTextSize = styles.tertiaryTextSize
        if (tertiaryTextSize > 0) {
            binding.body.textSize = tertiaryTextSize
        }

        styles.callToActionBackgroundColor?.let {
            binding.layoutCta.updateBackgroundColor(it)
        }

        styles.backgroundResource?.let {
            binding.background.setBackgroundResource(it)
        }


        styles.callToActionRadius?.let {
            binding.layoutCta.updateRadius(it.toFloat())
        }

        runCatching {
            styles.borderColor?.let {
                (binding.background.background as GradientDrawable).setStroke(resources.getDimensionPixelSize(com.core.dimens.R.dimen._1dp), it.toColorInt())
            }
        }


        styles.backgroundColor?.let {
            binding.background.updateBackgroundColorBottomToTop(it)
        }

        styles.backgroundFullColor?.let {
            binding.nativeAdView.setBackgroundColor(runCatching { it.toColorInt() }.getOrNull() ?: Color.WHITE)
        }

        styles.backgroundAdsNotifyView?.let {
            binding.adNotificationView.setBackgroundResource(it)
        }

        styles.primaryTextBackgroundColor?.let {
            binding.primary.background = it
        }

        styles.tertiaryTextBackgroundColor?.let {
            binding.body.background = it
        }

        styles.isEnableImmersive?.let {
            isEnableImmersive = it
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
                    com.core.dimens.R.dimen._1dp
                ), it.toColorInt()
            )
        }
        invalidate()
        requestLayout()
    }
}