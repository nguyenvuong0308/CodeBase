package com.core.ads.extensions

import android.app.Activity
import com.core.utilities.getBannerAdWidth
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize

fun Activity.getBannerAdaptiveSize(): AdSize {
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
        this,
        this.getBannerAdWidth()
    )
}