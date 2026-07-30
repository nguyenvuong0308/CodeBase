package com.core.ads.customviews.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.core.ads.databinding.GntMediumCollapsibleCtaBottomTemplateViewV2Binding
import com.core.ads.extensions.updateBackgroundColor
import com.core.ads.extensions.updateRadius
import com.core.ads.glidetransformation.RoundedCornersTransformation
import com.core.dimens.R
import com.google.android.gms.ads.nativead.NativeAd

class NativeCollapsibleMediumCtaBottomTemplateViewV2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseNativeTemplateView(context, attrs, defStyleAttr) {

    private enum class ControlClosePosition {
        LEFT,
        RIGHT
    }

    private val binding: GntMediumCollapsibleCtaBottomTemplateViewV2Binding by lazy {
        GntMediumCollapsibleCtaBottomTemplateViewV2Binding.inflate(LayoutInflater.from(context), this, true)
    }

    private var controlClosePosition: ControlClosePosition = ControlClosePosition.RIGHT

    init {
        initView()
    }

    private fun initView() {
        binding.icCloseCollapse.setOnClickListener {
            collapseToMini()
        }
    }

    override fun setNativeAd(nativeAd: NativeAd) {
        showFullLayout()

        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.mediaView = binding.mediaView

        binding.primary.text = nativeAd.headline
        binding.primaryMini.text = nativeAd.headline
        binding.cta.text = nativeAd.callToAction
        binding.ctaMini.text = nativeAd.callToAction

        nativeAd.body?.let {
            binding.body.text = it
            binding.bodyMini.text = it
            binding.nativeAdView.bodyView = binding.body
        }

        binding.nativeAdView.setNativeAd(nativeAd)
    }

    private fun loadIcon(imageView: ImageView, drawable: Any?) {
        Glide.with(this)
            .load(drawable)
            .override(resources.getDimensionPixelSize(R.dimen._44dp))
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
            .into(imageView)
    }

    private fun showFullLayout() {
        binding.background.visibility = VISIBLE
        binding.backgroundMini.visibility = GONE
        binding.mediaView.visibility = VISIBLE
        binding.icCloseCollapse.visibility = VISIBLE
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.headlineView = binding.primary
        binding.nativeAdView.bodyView = binding.body
    }

    private fun collapseToMini() {
        binding.mediaView.visibility = GONE
        binding.background.visibility = GONE
        binding.backgroundMini.visibility = VISIBLE
        binding.icCloseCollapse.visibility = GONE
        binding.nativeAdView.callToActionView = binding.ctaMini
        binding.nativeAdView.headlineView = binding.primaryMini
        binding.nativeAdView.bodyView = binding.bodyMini
    }

    override fun destroyNativeAd() {
        binding.nativeAdView.destroy()
    }

    override fun applyStyles(styles: NativeTemplateStyle) {
        styles.mainBackgroundColor?.let {
            binding.background.background = it
            binding.backgroundMini.background = it
            binding.primary.background = it
            binding.primaryMini.background = it
            binding.body.background = it
            binding.bodyMini.background = it
        }

        styles.primaryTextTypeface?.let {
            binding.primary.typeface = it
            binding.primaryMini.typeface = it
        }

        styles.tertiaryTextTypeface?.let {
            binding.body.typeface = it
            binding.bodyMini.typeface = it
        }

        styles.callToActionTextTypeface?.let {
            binding.cta.typeface = it
            binding.ctaMini.typeface = it
        }

        styles.primaryTextTypefaceColor?.let {
            binding.primary.setTextColor(it.toColorInt())
            binding.primaryMini.setTextColor(it.toColorInt())
        }

        styles.tertiaryTextTypefaceColor?.let {
            binding.body.setTextColor(it.toColorInt())
            binding.bodyMini.setTextColor(it.toColorInt())
        }

        styles.callToActionTypefaceColor?.let {
            binding.cta.setTextColor(it)
            binding.ctaMini.setTextColor(it)
        }

        val ctaTextSize = styles.callToActionTextSize
        if (ctaTextSize > 0) {
            binding.cta.textSize = ctaTextSize
            binding.ctaMini.textSize = ctaTextSize
        }

        val primaryTextSize = styles.primaryTextSize
        if (primaryTextSize > 0) {
            binding.primary.textSize = primaryTextSize
            binding.primaryMini.textSize = primaryTextSize
        }

        val tertiaryTextSize = styles.tertiaryTextSize
        if (tertiaryTextSize > 0) {
            binding.body.textSize = tertiaryTextSize
            binding.bodyMini.textSize = tertiaryTextSize
        }

        styles.callToActionBackgroundColor?.let {
            binding.layoutCta.updateBackgroundColor(it)
            binding.layoutCtaMini.updateBackgroundColor(it)
        }

        styles.callToActionRadius?.let {
            binding.layoutCta.updateRadius(it.toFloat())
            binding.layoutCtaMini.updateRadius(it.toFloat())
        }

        styles.borderColor?.let {
            (binding.nativeAdView.background as GradientDrawable).setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
        }

        styles.backgroundColor?.let {
            (binding.nativeAdView.background as GradientDrawable).setColor(it.toColorInt())
        }

        styles.backgroundResource?.let {
            binding.background.setBackgroundResource(it)
            binding.backgroundMini.setBackgroundResource(it)
        }

        styles.backgroundAdsNotifyView?.let {
            binding.adNotificationView.setBackgroundResource(it)
            binding.adNotificationViewMini.setBackgroundResource(it)
        }
        applyAdsNotifyViewStyles(styles, binding.adNotificationView, binding.adNotificationViewMini)

        styles.primaryTextBackgroundColor?.let {
            binding.primary.background = it
            binding.primaryMini.background = it
        }

        styles.tertiaryTextBackgroundColor?.let {
            binding.body.background = it
            binding.bodyMini.background = it
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
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
            (binding.layoutCtaMini.background as? GradientDrawable)?.setStroke(
                resources.getDimensionPixelSize(R.dimen._1dp),
                it.toColorInt()
            )
        }
        controlClosePosition = resolveControlClosePosition(styles.controlClosePosition)
        applyControlClosePosition()
        invalidate()
        requestLayout()
    }

    private fun resolveControlClosePosition(position: String?): ControlClosePosition {
        return when (position?.trim()?.lowercase()) {
            "left", "start" -> ControlClosePosition.LEFT
            "right", "end" -> ControlClosePosition.RIGHT
            else -> ControlClosePosition.RIGHT
        }
    }

    private fun applyControlClosePosition() {
        val params = binding.icCloseCollapse.layoutParams as? FrameLayout.LayoutParams ?: return
        params.gravity = when (controlClosePosition) {
            ControlClosePosition.LEFT -> Gravity.START
            ControlClosePosition.RIGHT -> Gravity.END
        }
        binding.icCloseCollapse.layoutParams = params
    }
}
