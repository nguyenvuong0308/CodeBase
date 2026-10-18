package com.core.ads.customviews.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.toColorInt
import com.core.config.domain.data.NativeExpandTemplate

/** A class containing the optional styling options for the Native Template.  */
class NativeTemplateStyle : Cloneable {
    // Call to action typeface.
    var callToActionTextTypeface: Typeface? = null
        private set

    // Responsive @dimen/_Ndp token used for the call to action text.
    var callToActionTextSize = 0f
        private set

    // Call to action typeface color in the form 0xAARRGGBB.
    var callToActionTypefaceColor: Int? = null
        private set

    // Call to action background color.
    var callToActionBackgroundColor: String? = null
        private set

    var callToActionRadius: Int? = null
        private set

    var callToActionBorderColor: String? = null
        private set

    var borderColor: String? = null
        private set

    var countDownSecond: Int? = null
        private set

    var closeStepCount: Int? = null
        private set

    var step1CountDownSecond: Int? = null
        private set

    var step2CountDownSecond: Int? = null
        private set

    var mediaBackgroundColor: String? = null
        private set

    var backgroundColor: String? = null
        private set

    var backgroundFullColor: String? = null
        private set

    var backgroundResource: Int? = null
        private set

    var backgroundRadius: Int? = null
        private set

    var backgroundAdsNotifyView: Int? = null
        private set

    var textColorAdsNotifyView: String? = null
        private set

    var backgroundColorAdsNotifyView: String? = null
        private set


    // All templates have a primary text area which is populated by the native ad's headline.
    // Primary text typeface.
    var primaryTextTypeface: Typeface? = null
        private set

    // Responsive @dimen/_Ndp token used for primary text.
    var primaryTextSize = 0f
        private set

    // Primary text typeface color in the form 0xAARRGGBB.
    var primaryTextTypefaceColor: String? = null
        private set

    // Primary text background color.
    var primaryTextBackgroundColor: ColorDrawable? = null
        private set

    // The typeface, typeface color, and background color for the second row of text in the template.
    // All templates have a secondary text area which is populated either by the body of the ad or
    // by the rating of the app.
    // Secondary text typeface.
    var secondaryTextTypeface: Typeface? = null
        private set

    // Responsive @dimen/_Ndp token used for secondary text.
    var secondaryTextSize = 0f
        private set

    // Secondary text typeface color in the form 0xAARRGGBB.
    var secondaryTextTypefaceColor: Int? = null
        private set

    // Secondary text background color.
    var secondaryTextBackgroundColor: ColorDrawable? = null
        private set

    // The typeface, typeface color, and background color for the third row of text in the template.
    // The third row is used to display store name or the default tertiary text.
    // Tertiary text typeface.
    var tertiaryTextTypeface: Typeface? = null
        private set

    // Responsive @dimen/_Ndp token used for tertiary text.
    var tertiaryTextSize = 0f
        private set

    // Tertiary text typeface color in the form 0xAARRGGBB.
    var tertiaryTextTypefaceColor: String? = null
        private set

    // Tertiary text background color.
    var tertiaryTextBackgroundColor: ColorDrawable? = null
        private set

    // The background color for the bulk of the ad.
    var mainBackgroundColor: ColorDrawable? = null
        private set

    var isEnableImmersive: Boolean? = null
        private set

    var hideTextCountDown: Boolean? = null
        private set

    var hideTextSkipCountDown: Boolean? = null
        private set

    var hideProgressCountDown: Boolean? = null
        private set

    var progressBarTint: String? = null
        private set

    var controlClosePosition: String? = null
        private set

    // V4–V6: missing or unsupported values preserve the numeric countdown.
    var controlCloseStyle: String = "countdown"
        private set

    // V4–V6: opt in to the smaller, undecorated Close after the countdown.
    var controlCloseReadyStyle: String = "default"
        private set

    var collapsibleExpandCooldownSecond: Int? = null
        private set

    var alwaysHideInlineNative: Boolean = false
        private set

    var nativeExpandMarginLeftDp: Float = 0f
        private set

    var nativeExpandMarginRightDp: Float = 0f
        private set

    var nativeExpandTemplate: NativeExpandTemplate = NativeExpandTemplate.V1
        private set

    var adPlaceName: String? = null
        private set


    var nativeTheme: String? = null
        private set
    var nativeThemeColors: Map<String, Map<String, String?>> = emptyMap()
        private set

    // Copy before overriding so one ad never changes another ad's base style.
    internal fun forTheme(theme: String?): NativeTemplateStyle {
        val colors = nativeThemeColors[theme] ?: return this
        val result = super.clone() as NativeTemplateStyle
        fun color(key: String): String? = colors[key]?.trim()?.takeIf {
            runCatching { Color.parseColor(it) }.isSuccess
        }
        color("background_color")?.let { result.backgroundColor = it }
        color("background_full_color")?.let { result.backgroundFullColor = it }
        color("media_background_color")?.let { result.mediaBackgroundColor = it }
        color("primary_text_color")?.let { result.primaryTextTypefaceColor = it }
        color("body_text_color")?.let { result.tertiaryTextTypefaceColor = it }
        color("cta_text_color")?.let { result.callToActionTypefaceColor = Color.parseColor(it) }
        color("background_cta")?.let { result.callToActionBackgroundColor = it }
        color("cta_border_color")?.let { result.callToActionBorderColor = it }
        color("border_color")?.let { result.borderColor = it }
        color("background_color_ads_notify_view")?.let { result.backgroundColorAdsNotifyView = it }
        color("text_color_ads_notify_view")?.let { result.textColorAdsNotifyView = it }
        // Follow the selected title color when a palette omits its progress tint.
        result.progressBarTint = color("progress_bar_tint") ?: color("primary_text_color") ?: progressBarTint
        return result
    }

    /** A class that provides helper methods to build a style object.  */
    class Builder {
        private val styles = NativeTemplateStyle()

        fun withCallToActionTextTypeface(callToActionTextTypeface: Typeface?): Builder {
            styles.callToActionTextTypeface = callToActionTextTypeface
            return this
        }

        fun withCallToActionTextSize(callToActionTextSize: Float): Builder {
            styles.callToActionTextSize = callToActionTextSize
            return this
        }

        fun withCallToActionTypefaceColor(callToActionTypefaceColor: String?): Builder {
            styles.callToActionTypefaceColor = runCatching { callToActionTypefaceColor?.toColorInt() }.getOrNull() ?: Color.WHITE
            return this
        }

        fun withHideTextCountDown(hideTextCountDown: Boolean?): Builder {
            styles.hideTextCountDown = hideTextCountDown ?: false
            return this
        }

        fun withHideTextSkipCountDown(hideTextSkipCountDown: Boolean?): Builder {
            styles.hideTextSkipCountDown = hideTextSkipCountDown ?: false
            return this
        }

        fun withHideProgressCountDown(hideProgressCountDown: Boolean?): Builder {
            styles.hideProgressCountDown = hideProgressCountDown ?: false
            return this
        }

        fun withProgressBarTint(progressBarTint: String?): Builder {
            styles.progressBarTint = progressBarTint
            return this
        }

        fun withControlClosePosition(controlClosePosition: String?): Builder {
            styles.controlClosePosition = controlClosePosition
            return this
        }

        fun withNativeTheme(mode: String?, colors: Map<String, Map<String, String?>?>?): Builder {
            styles.nativeTheme = mode
            styles.nativeThemeColors = colors?.mapNotNull { (name, palette) ->
                palette?.let { name to it.toMap() }
            }?.toMap() ?: emptyMap()
            return this
        }

        fun withControlCloseReadyStyle(value: String?): Builder {
            styles.controlCloseReadyStyle = if (value?.trim()?.equals("compact", ignoreCase = true) == true) {
                "compact"
            } else {
                "default"
            }
            return this
        }

        fun withControlCloseStyle(controlCloseStyle: String?): Builder {
            styles.controlCloseStyle = if (controlCloseStyle?.trim()?.equals("close", ignoreCase = true) == true) {
                "close"
            } else {
                "countdown"
            }
            return this
        }

        fun withCollapsibleExpandCooldownSecond(collapsibleExpandCooldownSecond: Int?): Builder {
            styles.collapsibleExpandCooldownSecond = collapsibleExpandCooldownSecond
            return this
        }

        fun withNativeExpandTemplate(nativeExpandTemplate: NativeExpandTemplate): Builder {
            styles.nativeExpandTemplate = nativeExpandTemplate
            return this
        }

        fun withAlwaysHideInlineNative(alwaysHideInlineNative: Boolean): Builder {
            styles.alwaysHideInlineNative = alwaysHideInlineNative
            return this
        }

        fun withNativeExpandMargins(leftDp: Float, rightDp: Float): Builder {
            styles.nativeExpandMarginLeftDp = leftDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
            styles.nativeExpandMarginRightDp = rightDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
            return this
        }

        fun withAdPlaceName(adPlaceName: String?): Builder {
            styles.adPlaceName = adPlaceName
            return this
        }

        fun withCallToActionBackgroundColor(callToActionBackgroundColor: String?): Builder {
            styles.callToActionBackgroundColor = callToActionBackgroundColor
            return this
        }

        fun withCallToActionRadius(callToActionRadius: Int?): Builder {
            styles.callToActionRadius = callToActionRadius
            return this
        }

        fun withBorderColor(borderColor: String?): Builder {
            styles.borderColor = borderColor/* ?: "#CBCBCB"*/
            return this
        }

        fun withCtaBorderColor(borderColor: String?): Builder {
            styles.callToActionBorderColor = borderColor/* ?: "#CBCBCB"*/
            return this
        }

        fun withBackgroundColor(bgColor: String?): Builder {
            styles.backgroundColor = bgColor/* ?: "#FFFFFF"*/
            return this
        }

        fun withCountDownSecond(times: Int?): Builder {
            styles.countDownSecond = times
            return this
        }

        fun withCloseStepCount(closeStepCount: Int?): Builder {
            styles.closeStepCount = closeStepCount
            return this
        }

        fun withStep1CountDownSecond(times: Int?): Builder {
            styles.step1CountDownSecond = times
            return this
        }

        fun withStep2CountDownSecond(times: Int?): Builder {
            styles.step2CountDownSecond = times
            return this
        }

        fun withBackgroundFullColor(bgColor: String?): Builder {
            styles.backgroundFullColor = bgColor/* ?: "#FFFFFF"*/
            return this
        }
        fun withMediaBackgroundColor(bgColor: String?): Builder {
            styles.mediaBackgroundColor = bgColor
            return this
        }

        fun withBackgroundResource(backgroundRes: Int?): Builder {
            styles.backgroundResource = backgroundRes
            return this
        }

        fun withBackgroundAdsNotifyView(backgroundAdsNotifyView: Int?): Builder {
            styles.backgroundAdsNotifyView = backgroundAdsNotifyView
            return this
        }

        fun withBackgroundColorAdsNotifyView(backgroundColorAdsNotifyView: String?): Builder {
            styles.backgroundColorAdsNotifyView = backgroundColorAdsNotifyView
            return this
        }

        fun withTextColorAdsNotifyView(textColorAdsNotifyView: String?): Builder {
            styles.textColorAdsNotifyView = textColorAdsNotifyView
            return this
        }

        fun withPrimaryTextTypeface(primaryTextTypeface: Typeface?): Builder {
            styles.primaryTextTypeface = primaryTextTypeface
            return this
        }

        fun withPrimaryTextSize(primaryTextSize: Float): Builder {
            styles.primaryTextSize = primaryTextSize
            return this
        }

        fun withPrimaryTextTypefaceColor(primaryTextTypefaceColor: String?): Builder {
            styles.primaryTextTypefaceColor = primaryTextTypefaceColor
            return this
        }

        fun withPrimaryTextBackgroundColor(primaryTextBackgroundColor: ColorDrawable?): Builder {
            styles.primaryTextBackgroundColor = primaryTextBackgroundColor
            return this
        }

        fun withSecondaryTextTypeface(secondaryTextTypeface: Typeface?): Builder {
            styles.secondaryTextTypeface = secondaryTextTypeface
            return this
        }

        fun withSecondaryTextSize(secondaryTextSize: Float): Builder {
            styles.secondaryTextSize = secondaryTextSize
            return this
        }

        fun withSecondaryTextTypefaceColor(secondaryTextTypefaceColor: Int): Builder {
            styles.secondaryTextTypefaceColor = secondaryTextTypefaceColor
            return this
        }

        fun withSecondaryTextBackgroundColor(secondaryTextBackgroundColor: ColorDrawable?): Builder {
            styles.secondaryTextBackgroundColor = secondaryTextBackgroundColor
            return this
        }

        fun withTertiaryTextTypeface(tertiaryTextTypeface: Typeface?): Builder {
            styles.tertiaryTextTypeface = tertiaryTextTypeface
            return this
        }

        fun withTertiaryTextSize(tertiaryTextSize: Float): Builder {
            styles.tertiaryTextSize = tertiaryTextSize
            return this
        }

        fun withTertiaryTextTypefaceColor(tertiaryTextTypefaceColor: String?): Builder {
            styles.tertiaryTextTypefaceColor = tertiaryTextTypefaceColor
            return this
        }

        fun withTertiaryTextBackgroundColor(tertiaryTextBackgroundColor: ColorDrawable?): Builder {
            styles.tertiaryTextBackgroundColor = tertiaryTextBackgroundColor
            return this
        }

        fun withMainBackgroundColor(mainBackgroundColor: ColorDrawable?): Builder {
            styles.mainBackgroundColor = mainBackgroundColor
            return this
        }

        fun withMainBackgroundRadius(radius: Int?): Builder {
            styles.backgroundRadius = radius
            return this
        }

        fun withIsEnableImmersive(isEnableImmersive: Boolean?): Builder {
            styles.isEnableImmersive = isEnableImmersive
            return this
        }

        fun build(): NativeTemplateStyle {
            return styles
        }
    }
}
