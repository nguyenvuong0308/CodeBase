package com.core.ads.domain

import android.app.Activity
import androidx.fragment.app.FragmentManager
import com.core.ads.model.AppOpenAdHolder
import com.core.ads.model.NativeAdHolder
import com.core.config.domain.data.AdPlace
import com.core.config.domain.data.IAdPlaceName
import com.google.android.gms.ads.AdRequest
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AdsManager {

    val isDisableAdDueManyClickFlow: StateFlow<Boolean>

    val adLoadBannerNativeFlow: SharedFlow<AdLoadBannerNativeUiResource>

    val adFullScreenFlow: SharedFlow<AdFullScreenUiResource>

    val requestConsentFlow: SharedFlow<ConsentFormUiResource>

    fun isHasFullscreenAdShowing(): Boolean

    fun isHasAppOpenAdShowing(): Boolean

    fun isNotAbleToVisibleAdsToUser(adPlaceName: IAdPlaceName): Boolean

    fun increaseAdClickedCount()

    fun isRequestLocationInEeaOrUnknown(): Boolean

    fun requestConsentInfoUpdate(activity: Activity, isForceShow: Boolean)

    fun displayConsentForm(activity: Activity, isForceShow: Boolean)

    fun isCanNotShowInterAd(adPlace: AdPlace): Boolean

    fun getAdRequest(isCollapsible: Boolean = false): AdRequest

    fun loadFullscreenAd(
        activity: Activity,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isNeedUpdateAdPlace: Boolean = false,
        isRequestFromExternal: Boolean = true
    )

    fun loadBannerNativeAd(
        activity: Activity,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isPreload: Boolean,
        isReload: Boolean
    )


    fun showAd(
        activity: Activity,
        fragmentManager: FragmentManager,
        adPlaceName: IAdPlaceName,
        identifier: String,
        isWaitLoadToShow: Boolean = false
    )

    fun releaseBannerNative(adPlaceName: IAdPlaceName)

    fun setupAppOpenAdDefaultValue()

    fun getOrCreateAppOpenAdHolderBy(adPlace: AdPlace): AppOpenAdHolder

    fun getNativeHolder(activity: Activity, adPlaceName: IAdPlaceName): NativeAdHolder?

    fun startDisableAdCountDownTimer()

    /*
        Xóa quảng cáo để ứng dụng có thể tải lại quảng cáo khi vào lại app (thường dùng cho các quảng cáo sử dụng oneTimeLoad = true
     */
    fun removeAds(adPlaceName: IAdPlaceName)

    fun isRewardReady(adPlaceName: IAdPlaceName): Boolean
}