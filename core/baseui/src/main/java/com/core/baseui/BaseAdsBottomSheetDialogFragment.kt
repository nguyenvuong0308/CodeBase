package com.core.baseui

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.analytics.AnalyticsEvent
import com.core.analytics.AnalyticsManager
import com.core.baseui.executor.AppExecutors
import com.core.baseui.ext.bindLiveData
import com.core.baseui.fragment.collectFlowOn
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAdPlaceName
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.core.utilities.checkIfFragmentAttached
import com.core.utilities.manager.NetworkConnectionManager
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.util.Timber
import com.core.utilities.util.hideNavigationBarForBottomDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


abstract class BaseAdsBottomSheetDialogFragment<B : ViewBinding> : BottomSheetDialogFragment(), CoroutineScope {

    open val isHideNavigationBar: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isHideNavigationBar
    }

    private var _viewBinding: B? = null

    protected val viewBinding
        get() = _viewBinding
            ?: throw RuntimeException("Should only use binding after onCreateView and before onDestroyView")

    protected abstract fun bindingProvider(inflater: LayoutInflater, container: ViewGroup?): B

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var purchasePreferences: PurchasePreferences

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    @Inject
    lateinit var appExecutors: AppExecutors

    private var isVip = false

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    private fun setupDialog() {

        val isTablet = false
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val dialogWidth = if (isTablet) {
            Integer.min(screenWidth, screenHeight) * BaseDialog.TABLET_RATIO_WIDTH_DIALOG
        } else {
            Integer.min(
                screenWidth,
                screenHeight
            ) - requireContext().resources.getDimensionPixelOffset(com.core.dimens.R.dimen._48dp)
        }

        // Set transparent background and no title
        requireDialog().window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setBackgroundDrawableResource(R.drawable.bg_radius_8)
            it.setLayout(dialogWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    }

    private var previousNetworkConnection = true


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinding = bindingProvider(inflater, container)
        return _viewBinding?.root
    }

    open fun init(savedInstanceState: Bundle?) {}

    var contextAds: ContextAds? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        if (isHideNavigationBar) {
            requireDialog().hideNavigationBarForBottomDialog()
        }
        isVip = purchasePreferences.isUserVip()
        previousNetworkConnection = requireActivity().isNetworkConnected()

        contextAds = object : ContextAds(
            adsManager = adsManager,
            lifecycleOwner = this,
            lifecycleScope = lifecycleScope,
            activity = requireActivity(),
            fragmentManager = childFragmentManager,
            remoteConfigRepository = remoteConfigRepository,
            initRewardAdPlaceName = providerRewardAdPlaceName(),
            initInterstitialAdPlaceName = providerInterAdPlaceName(),
            initBannerNativeAdPlaceName = providerBannerNativeAdPlaceName(),
            initPreloadBannerNativeAdPlaceName = providerPreloadBannerNativeAdPlaceName(),
        ) {
            override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
                this@BaseAdsBottomSheetDialogFragment.onBannerNativeResult(adResource)
            }
        }
        initViews(savedInstanceState)
        handleObservable()
        displayFirstData()
        preloadData()
        handleNetworkChange()
        bindLiveData(purchasePreferences.changeVipState) { state ->
            Timber.Forest.d("Listener $state")
            if (isVip != state) {
                isVip = state
                contextAds?.preloadAds()
                Timber.Forest.d("onChangeVipState $state")
                onChangeVipState(isVip)
            }
        }
    }

    /**
     * Xử lý sự kiện thay đổi trạng thái kết nối mạng.
     *
     * Hàm này lắng nghe sự thay đổi trạng thái kết nối mạng và thực hiện các hành động tương ứng,
     * chẳng hạn như tải lại quảng cáo hoặc gửi sự kiện phân tích.
     */
    private fun handleNetworkChange() {
        collectFlowOn(networkConnectionManager.isNetworkConnectedFlow, Lifecycle.State.RESUMED) {
            CoroutineScope(coroutineContext).launch {
                delay(1000)
                checkIfFragmentAttached {
                    val isNetworkConnected = isNetworkConnected()
                    if (isNetworkConnected && !previousNetworkConnection) {
                        analyticsManager.logEvent(AnalyticsEvent.NETWORK_OFF_TO_ON)
                        contextAds?.preloadAds()
                    }
                    if (!isNetworkConnected && previousNetworkConnection) {
                        analyticsManager.logEvent(AnalyticsEvent.NETWORK_ON_TO_OFF)
                    }
                    if (previousNetworkConnection != isNetworkConnected) {
                        onNetworkChange(it)
                    }
                    previousNetworkConnection = isNetworkConnected
                }
            }
        }
    }

    open fun onNetworkChange(isNetworkConnected: Boolean) {
        Timber.d("onNetworkChange $isNetworkConnected")
    }

    open fun preloadData() {}

    open fun onChangeVipState(isVip: Boolean) {

    }

    override fun onStart() {
        Glide.with(this).onStart()
        super.onStart()
//        preloadAds()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        Glide.with(this).onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        coroutineContext.cancelChildren()
        try {
            Glide.with(this).onDestroy()
        } catch (_: Exception) {
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Glide.with(this).onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    open fun initViews(savedInstanceState: Bundle?) {}

    open fun handleObservable() {}

    //region Ads implement

    /**
     * Tải quảng cáo banner hoặc native.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param oneTimeLoad Chỉ tải quảng cáo một lần nếu true.
     */
    fun loadBannerOrNativeAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean, isReload: Boolean = false) {
        contextAds?.loadBannerOrNativeAds(adPlaceName, oneTimeLoad, isReload = isReload)
    }

    /**
     * Tải quảng cáo interstitial.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param oneTimeLoad Chỉ tải quảng cáo một lần nếu true.
     */
    fun loadInterstitialAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean) {
        contextAds?.loadInterstitialAds(adPlaceName, oneTimeLoad)
    }

    /**
     * Tải quảng cáo reward.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param oneTimeLoad Chỉ tải quảng cáo một lần nếu true.
     */
    fun loadRewardAds(adPlaceName: IAdPlaceName, oneTimeLoad: Boolean) {
        contextAds?.loadRewardAds(adPlaceName, oneTimeLoad)
    }

    /**
     * Hiển thị quảng cáo reward.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param onHandleCompleted Callback được gọi khi quảng cáo được hiển thị hoặc không và người dùng có nhận được phần thưởng hay không.
     */
    fun showRewardAd(
        adPlaceName: IAdPlaceName,
        onHandleCompleted: ((isShown: Boolean, isEarnedReward: Boolean, isNoAds: Boolean) -> Unit)
    ) {
        contextAds?.showRewardAd(adPlaceName, onHandleCompleted)
    }

    /**
     * Hiển thị quảng cáo interstitial.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param onHandleCompleted Callback được gọi khi quảng cáo được hiển thị hoặc không.
     */
    fun showInterAd(adPlaceName: IAdPlaceName, onHandleCompleted: ((isShown: Boolean) -> Unit)) {
        contextAds?.showInterAd(adPlaceName, onHandleCompleted)
    }

    /**
     * Cung cấp danh sách các vị trí quảng cáo banner/native cần tải trước.
     *
     * @return Danh sách các [AdPlaceName] cho quảng cáo banner/native cần tải trước.
     */
    open fun providerPreloadBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo interstitial.
     *
     * @return Danh sách các [AdPlaceName] cho quảng cáo interstitial.
     */
    open fun providerInterAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo reward.
     *
     * @return Danh sách các [AdPlaceName] cho quảng cáo reward.
     */
    open fun providerRewardAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo banner/native.
     *
     * @return Danh sách các [AdPlaceName] cho quảng cáo banner/native.
     */
    open fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Được gọi khi quảng cáo banner/native đã được tải.
     *
     * @param adResource Tài nguyên quảng cáo banner/native đã tải.
     */
    open fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
    }
    //endregion

    open fun displayFirstData() {}

    fun logEvent(eventName: String, params: Map<String, String> = mapOf()) {
        analyticsManager.logEvent(eventName, params)
    }
}