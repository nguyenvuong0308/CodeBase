package com.core.rate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory

fun Activity.rateApp(inAppReview: Boolean = false) {
    val sharedPreferences = getSharedPreferences("RateInApp", Context.MODE_PRIVATE)
    if (inAppReview && sharedPreferences.getBoolean("rate_in_app", false)) {
        val mReviewManager = ReviewManagerFactory.create(this)
        val request = mReviewManager.requestReviewFlow()
        request.addOnCompleteListener { taskInfo ->
            if (taskInfo.isSuccessful) {
                val reviewInfo = taskInfo.result
                val flow = mReviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { flowTask ->
                    sharedPreferences.edit { putBoolean("rate_in_app", true) }
                }
            } else {
                openAppInStore()
            }
        }
    } else {
        openAppInStore()
    }
}


fun Context.openAppInStore() {
    val uri = ("market://details?id=" + this.packageName).toUri()
    val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
    val webUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
    try {
        startActivity(myAppLinkToMarket.apply { setPackage("com.android.vending") })
    } catch (e: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            startActivity(webIntent)
        } catch (webException: ActivityNotFoundException) {
            // Notify the user if no browser or Play Store app is available
            Toast.makeText(this, getString(R.string.fb_common_unable_find_market), Toast.LENGTH_SHORT).show()
        }
    }
}

//region set primary color Rate runtime
private fun ratePrimaryButtonTint(primaryColor: Int): ColorStateList {
    return ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled)
        ),
        intArrayOf(
            primaryColor,
            ColorUtils.setAlphaComponent(primaryColor, 0x80)
        )
    )
}

internal fun View.applyRatePrimaryBackgroundTint(primaryColor: Int) {
    backgroundTintList = ratePrimaryButtonTint(primaryColor)
}

internal fun TextView.applyRateFeedbackOptionColors(primaryColor: Int) {
    val radius = resources.getDimension(R.dimen.fb_button_feedback_matter_radius)
    val unselectedBackground = ContextCompat.getColor(context, R.color.fb_bg_feedback_matter_unselected)
    val unselectedText = ContextCompat.getColor(context, R.color.fb_text_feedback_matter_unselected)

    background = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_selected), rounded(primaryColor, radius))
        addState(intArrayOf(), rounded(unselectedBackground, radius))
    }
    setTextColor(
        ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(Color.WHITE, unselectedText)
        )
    )
}

private fun rounded(color: Int, radius: Float): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
    }
}
//endregion