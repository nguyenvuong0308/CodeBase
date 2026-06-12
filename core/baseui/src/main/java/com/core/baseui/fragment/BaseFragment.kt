package com.core.baseui.fragment

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.analytics.AnalyticsEvent
import com.core.analytics.AnalyticsManager
import com.core.baseui.ContextAds
import com.core.baseui.executor.AppExecutors
import com.core.baseui.ext.bindLiveData
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAdPlaceName
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.core.utilities.checkIfFragmentAttached
import com.core.utilities.manager.NetworkConnectionManager
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.util.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BaseFragment"
private const val PICKED_IMAGE_DIR = "picked_images"
private const val MAX_PICKED_IMAGE_AGE_MS = 24 * 60 * 60 * 1000L
private const val MAX_PICKED_IMAGE_FILES = 20
abstract class BaseFragment<B : ViewBinding>(
) : Fragment(), CoroutineScope {

    private var _viewBinding: B? = null

    protected val viewBinding
        get() = _viewBinding
            ?: throw RuntimeException("Should only use binding after onCreateView and before onDestroyView")

    protected abstract fun bindingProvider(inflater: LayoutInflater, container: ViewGroup?): B

    private lateinit var pickImageIntent: ActivityResultLauncher<PickVisualMediaRequest>
    private var onImagePicked: ((Uri?) -> Unit)? = null
    private lateinit var startActivityIntent: ActivityResultLauncher<Intent>
    private var onResult: ((ActivityResult) -> Unit)? = null
    private lateinit var askCheckPermission: ActivityResultLauncher<Array<String>>

    private var _onPermissionResult: ((Map<String, Boolean>) -> Unit)? = null

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

    private val _onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            this@BaseFragment.handleOnBackPressed()
        }
    }

    open val isHandleBackPress = false

    private var previousNetworkConnection = true


    open fun handleOnBackPressed() {

    }

    fun launchForResult(intent: Intent, onResult: (ActivityResult) -> Unit) {
        this.onResult = onResult
        startActivityIntent.launch(intent)
    }

    fun launchImagePicker(onResult: (Uri?) -> Unit) {
        onImagePicked = onResult
        pickImageIntent.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }


    abstract val screenType: ScreenType


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        askCheckPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                _onPermissionResult?.invoke(permissions)
            }

        startActivityIntent = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            onResult?.invoke(result)
        }

        pickImageIntent = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            viewLifecycleOwner.lifecycleScope.launch {
                val cachedUri = withContext(Dispatchers.IO) {
                    uri?.let(::copyPickedImageToCache)
                }
                onImagePicked?.invoke(cachedUri)
            }
        }
        _viewBinding = bindingProvider(inflater, container)
        return _viewBinding?.root
    }

    private fun copyPickedImageToCache(uri: Uri): Uri? {
        val context = context ?: return null
        val imageDirs = listOfNotNull(
            context.cacheDir,
            context.externalCacheDir,
            context.filesDir
        ).map { File(it, PICKED_IMAGE_DIR) }

        cleanupPickedImageDirs(imageDirs)

        imageDirs.forEach { imageDir ->
            val copiedUri = runCatching {
                if (!imageDir.exists() && !imageDir.mkdirs()) return@runCatching null
                val imageFile = File(imageDir, "picked_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                } ?: return@runCatching null
                Uri.fromFile(imageFile)
            }.getOrNull()
            if (copiedUri != null) return copiedUri
        }

        return null
    }

    protected fun deletePickedImageCacheFile(uri: Uri) {
        if (uri.scheme != "file") return
        val context = context ?: return
        val file = File(uri.path ?: return)
        val parent = file.parentFile ?: return
        if (parent.name != PICKED_IMAGE_DIR) return

        val pickedImageDirs = listOfNotNull(
            context.cacheDir,
            context.externalCacheDir,
            context.filesDir
        ).map { File(it, PICKED_IMAGE_DIR).canonicalFile }

        val canonicalParent = runCatching { parent.canonicalFile }.getOrNull() ?: return
        if (pickedImageDirs.any { it == canonicalParent }) {
            runCatching { file.delete() }
        }
    }

    private fun cleanupPickedImageDirs(imageDirs: List<File>) {
        val cutoff = System.currentTimeMillis() - MAX_PICKED_IMAGE_AGE_MS
        imageDirs.forEach { imageDir ->
            val files = imageDir.listFiles()?.filter { it.isFile } ?: return@forEach
            files.filter { it.lastModified() < cutoff }.forEach { file ->
                runCatching { file.delete() }
            }
            imageDir.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_PICKED_IMAGE_FILES)
                ?.forEach { file ->
                    runCatching { file.delete() }
                }
        }
    }

    fun requiredOpenSettingPermission(permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permission
                )
            ) return true
        }
        return false
    }

    open fun init(savedInstanceState: Bundle?) {}

    open var isAwaitCallInitView = false
    var contextAds: ContextAds?= null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isVip = purchasePreferences.isUserVip()
        previousNetworkConnection = requireActivity().isNetworkConnected()
        if (savedInstanceState == null) {
            Log.i(TAG, "$screenType onViewCreated savedInstanceState is null")
        } else {
            Log.i(TAG, "$screenType onViewCreated savedInstanceState is non null")
        }
        analyticsManager.trackScreen(screenType.screenName)
        if (isHandleBackPress) {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                _onBackPressedCallback
            )
        }
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
                this@BaseFragment.onBannerNativeResult(adResource)
            }
        }
        init(savedInstanceState)
        if (!isAwaitCallInitView) {
            initViews(savedInstanceState)
        }
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

    fun requestPermission(
        permissions: Array<String>,
        onPermissionGrant: (Map<String, Boolean>) -> Unit
    ) {
        _onPermissionResult = onPermissionGrant
        askCheckPermission.launch(
            permissions
        )
    }

    open fun onChangeVipState(isVip: Boolean) {

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
                    if(previousNetworkConnection != isNetworkConnected) {
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

    override fun onStart() {
        Glide.with(this).onStart()
        super.onStart()
        Log.i(TAG, "$screenType onStart")
//        preloadAds()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "$screenType onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "$screenType onPause")
    }

    override fun onStop() {
        Glide.with(this).onStop()
        super.onStop()
        Log.i(TAG, "$screenType onStop")
    }

    override fun onDestroyView() {
        coroutineContext.cancelChildren()
        try {
            Glide.with(this).onDestroy()
        } catch (_: Exception) {
        }
        super.onDestroyView()
        contextAds?.onDestroy()
        contextAds = null
        Log.i(TAG, "$screenType onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "$screenType onDestroy")
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
     * @return Danh sách các [IAdPlaceName] cho quảng cáo banner/native cần tải trước.
     */
    open fun providerPreloadBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo interstitial.
     *
     * @return Danh sách các [IAdPlaceName] cho quảng cáo interstitial.
     */
    open fun providerInterAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo reward.
     *
     * @return Danh sách các [IAdPlaceName] cho quảng cáo reward.
     */
    open fun providerRewardAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Cung cấp danh sách các vị trí quảng cáo banner/native.
     *
     * @return Danh sách các [IAdPlaceName] cho quảng cáo banner/native.
     */
    open fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> = listOf()

    /**
     * Được gọi khi quảng cáo banner/native đã được tải.
     *
     * @param adResource Tài nguyên quảng cáo banner/native đã tải.
     */
    open fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
    }

    fun isRewardReady(adPlaceName: IAdPlaceName) = contextAds?.isRewardReady(adPlaceName) == true
    //endregion

    open fun displayFirstData() {}

    fun logEvent(eventName: String, params: Map<String, String> = mapOf()) {
        analyticsManager.logEvent(eventName, params)
    }
}