package com.core.rate

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.core.rate.feedback.FeedbackActivity
import com.core.utilities.util.NetworkUtils

class RateInApp {

    companion object {
        val instance = RateInApp()
        const val TAG = "RateInApp"
    }

    private var startActivityIntent: ActivityResultLauncher<Intent>? = null
    private var onResult: ((ActivityResult) -> Unit)? = null
    private var isCanShowAppOpen = true
    private var isShowThanks = false

    val rateConfig = RateConfig()

    /**
     * Đổi màu theo theme
     */
    var primaryColorInt: Int? = null

    private var intentActivity = HashMap<Int, ActivityResultLauncher<Intent>>()

    // Call this method in onCreate() of Application
    fun registerActivityLifecycle(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                (activity as? ComponentActivity)?.let {
                    registerForFeedback(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity !is FeedbackActivity) {
                    isCanShowAppOpen = true
                    if (isShowThanks && activity is AppCompatActivity) {
                        if (rateConfig.isThankForFeedbackGravityBottom) {
                            ThankForFeedbackBottomDialog().show(activity.supportFragmentManager, "thanks")
                        } else {
                            ThankForFeedbackCenterDialog(activity).show()
                        }
                        isShowThanks = false
                    }
                }
                (activity as? ComponentActivity)?.let {
                    startActivityIntent = intentActivity[activity.hashCode()]!!
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                (activity as? ComponentActivity)?.let {
                    intentActivity[activity.hashCode()]?.unregister()
                }
            }
        })
    }

    fun showDialogRateAndFeedback(
        context: FragmentActivity,
        onShowDialogRate: () -> Unit = {}, // Show dialog rate
        onRated: (star: Int) -> Unit = {}, // User click Rate
        onIgnoreRate: () -> Unit = {}, // Khi user không click vào rate mà click back
        alwaysIgnore: () -> Unit = {}, // Mỗi lần show dialog rate đều gọi hàm này
        inAppReview: Boolean = false, // Bật in-app review
        onShowThanks: () -> Boolean = { false }, // return true nếu muốn tự xử lý rate( show in-app review hoặc nhảy sang play store), mặc định sẽ nhảy sang play store
        forceShow: Boolean = false
    ) {
        fun rateStar(star: Int) {
            onRated(star)
            if (star >= rateConfig.minStarRateOK) {
                context.rateApp(inAppReview = inAppReview)
            } else {
                showFeedback(
                    context = context,
                    onShowThanks = onShowThanks
                )
            }
        }

        if (!NetworkUtils.isInternetAvailable(context) && !forceShow) return
        if (rateConfig.isRateGravityBottom) {
            RateBottomDialog().also {
                onShowDialogRate()
                it.onRate = { star ->
                    rateStar(star = star)
                }

                it.onIgnore = {
                    onIgnoreRate()
                }
            }.show(context.supportFragmentManager, "rate_bottom")
        } else {
            RateCenterDialog(context).also {
                onShowDialogRate()
                it.onRate = { star ->
                    rateStar(star = star)
                }

                it.onIgnore = {
                    onIgnoreRate()
                }
            }.show()
        }
        alwaysIgnore()
    }

    fun showFeedback(
        context: Context,
        onShowThanks: () -> Boolean = { false }
    ) {
        isCanShowAppOpen = false
        showActivityFeedback(context = context) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (!onShowThanks()) {
                    isShowThanks = true
                }
            }
        }
    }

    // Call this method in onCreate() of Activity
    private fun registerForFeedback(activity: ComponentActivity) {
        intentActivity[activity.hashCode()] = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onResult?.invoke(it)
        }
    }

    private fun showActivityFeedback(context: Context, onResult: (ActivityResult) -> Unit) {
        this.onResult = onResult
        startActivityIntent?.launch(Intent(context, FeedbackActivity::class.java))
    }

}