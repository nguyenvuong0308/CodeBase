package com.core.baseui

import android.app.Activity
import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.core.ads.admob.AppOpenAdManager
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.ads.domain.AdsManager
import com.core.analytics.AnalyticsEvent
import com.core.analytics.AnalyticsManager
import com.core.baseui.customviews.keyboard.KeyboardHeightProviderApi30Below
import com.core.baseui.customviews.keyboard.KeyboardHeightProviderApi30Plus
import com.core.baseui.customviews.keyboard.KeyboardListener
import com.core.baseui.customviews.keyboard.KeyboardProvider
import com.core.baseui.ext.TransitionType
import com.core.baseui.ext.bindLiveData
import com.core.baseui.ext.collectFlowOn
import com.core.baseui.ext.handleAdd
import com.core.baseui.ext.handleReplace
import com.core.baseui.ext.viewBinding
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.IAdPlaceName
import com.core.preference.AppPreferences
import com.core.preference.PurchasePreferences
import com.core.utilities.checkIfActivityAlive
import com.core.utilities.getCurrentLanguageCode
import com.core.utilities.hideNavigationBar
import com.core.utilities.manager.NetworkConnectionManager
import com.core.utilities.manager.isNetworkConnected
import com.core.utilities.showNavigationBar
import com.core.utilities.util.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.collections.forEach
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

private const val PICKED_IMAGE_DIR = "picked_images"
private const val MAX_PICKED_IMAGE_AGE_MS = 24 * 60 * 60 * 1000L
private const val MAX_PICKED_IMAGE_FILES = 21
abstract class BaseActivity<T : ViewBinding> : AppCompatActivity(), CoroutineScope {
    private lateinit var askCheckPermission: ActivityResultLauncher<Array<String>>
    private lateinit var pickImageIntent: ActivityResultLauncher<PickVisualMediaRequest>
    private var onImagePicked: ((Uri?) -> Unit)? = null
    private val insetsViewModel: InsetsViewModel by viewModels()
    private val keyboardChangeViewModel: KeyboardChangeViewModel by viewModels()
    private var _onPermissionResult: ((Map<String, Boolean>) -> Unit)? = null
    private lateinit var startActivityIntent: ActivityResultLauncher<Intent>
    private var onResult: ((ActivityResult) -> Unit)? = null
    private var isVip = false
    protected open val isRegisterOnKeyboardListener = false

    open val isWaitingAds = false
    protected open val isSpaceKeyboard = false
    private var keyboardHeightProvider: KeyboardProvider? = null
    private var onChangeKeyBoardHeight = ArrayList<(Int) -> Unit>()

    fun addOnChangeKeyBoardHeightListener(listener: (Int) -> Unit) {
        onChangeKeyBoardHeight.add(listener)
    }

    fun removeOnChangeKeyBoardHeightListener(listener: (Int) -> Unit) {
        onChangeKeyBoardHeight.remove(listener)
    }

    private fun getKeyboardListener() = object : KeyboardListener {
        override fun onHeightChanged(height: Int) {
            val _height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                height
            } else {
                if (isHideNavigationBar) {
                    if (height == 0) {
                        0
                    } else {
                        height + (getNavigationBarSize())
                    }
                } else {
                    height
                }
            }
            if (isHideNavigationBar && height == 0) {
                hideNavigationBar()
            }

            if (isHideStatusBar) {
                window?.let {
                    hideSystemBars(it)
                }
            }

            if (height > 0) {
                showNavigationBar()
            }

            keyboardChangeViewModel.updateKeyboardHeight(_height)

            onChangeKeyBoardHeight.forEach {
                it.invoke(_height)
            }
        }
    }

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var purchasePreferences: PurchasePreferences

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    open val isHideNavigationBar by lazy {
        remoteConfigRepository.getAppConfig().isHideNavigationBar
    }

    private var previousNetworkConnection = true

    protected abstract fun bindingProvider(inflater: LayoutInflater): T

    protected val viewBinding by viewBinding { inflater ->
        bindingProvider(inflater)
    }

    private val job = Job()

    open val isHideStatusBar = true
    open val isSpaceStatusBar = true
    open val isSpaceDisplayCutout = true


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    open fun showFirstScreen() {}

    /**
     * Kiểm tra xem người dùng có từ chối quyền vĩnh viễn không
     *
     * @param permissions Danh sách các quyền cần kiểm tra
     * @return Trả về true nếu người dùng từ chối quyền vĩnh viễn, ngược lại trả về false
     */
    fun requiredOpenSettingPermission(permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permission
                )
            ) return true
        }
        return false
    }

    fun Activity.getNavigationBarSize(): Int {
        val metrics = resources.displayMetrics
        val usableHeight = metrics.heightPixels
        val realMetrics = android.util.DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(realMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(realMetrics)
        }

        val realHeight = realMetrics.heightPixels

        return if (realHeight > usableHeight) {
            realHeight - usableHeight // Chiều cao navigation bar
        } else {
            0 // Không có navigation bar
        }
    }

    private var _innerPadding: Insets? = null

    /**Store system padding*/
    val innerPadding: Insets?
        get() {
            return _innerPadding
        }

    private var listVipListener: MutableList<() -> Unit> = mutableListOf()

    var contextAds: ContextAds? = null

    open fun init(savedInstanceState: Bundle?) {}

    open var isAwaitCallInitView = false

    open fun getSurfaceView(): View = viewBinding.root

    open var enableEdgeToEdge = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRegisterOnKeyboardListener) {
            keyboardHeightProvider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) KeyboardHeightProviderApi30Plus(this) else KeyboardHeightProviderApi30Below(this)
            keyboardHeightProvider?.addKeyboardListener(getKeyboardListener())
        }
        isVip = purchasePreferences.isUserVip()
        previousNetworkConnection = isNetworkConnected()
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
            lifecycleScope.launch {
                val cachedUri = withContext(Dispatchers.IO) {
                    uri?.let(::copyPickedImageToCache)
                }
                onImagePicked?.invoke(cachedUri)
            }
        }
        setContentView(viewBinding.root)
        if (isHideNavigationBar) {
            hideNavigationBar()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(getCurrentLanguageCode())
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    getCurrentLanguageCode()
                )
            )
        }
        if(enableEdgeToEdge) {
            enableEdgeToEdge()
        }

        if (isHideStatusBar) {
            hideSystemBars(window)
        }
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, windowInsets ->
            // Lấy thông tin về kích thước của các thanh hệ thống
            val systemBarInsets = windowInsets.resolveSystemBarInsets()
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            Timber.d("insets $systemBarInsets ")
            Timber.d("cutoutInsets $cutoutInsets ")

            // Áp dụng padding cho view để nó không bị che
            // Ở đây, ta thêm padding ở trên cùng và dưới cùng của layout
            val topPadding =
                if (isSpaceDisplayCutout && cutoutInsets.top > 0) cutoutInsets.top else if (isSpaceStatusBar) systemBarInsets.top else 0
            val bottomPadding = if (isSpaceKeyboard) {
                if(isHideNavigationBar && imeInsets.bottom == 0) 0 else max(systemBarInsets.bottom, imeInsets.bottom)
            } else {
                if (isHideNavigationBar) 0 else systemBarInsets.bottom
            }
            getSurfaceView().setPadding(systemBarInsets.left, topPadding, systemBarInsets.right, bottomPadding)

            insetsViewModel.updateInsets(
                InsetsViewModel.WrapInsets(
                    insets = Insets.of(
                        systemBarInsets.left,
                        max(cutoutInsets.top, systemBarInsets.top),
                        systemBarInsets.right,
                        bottomPadding
                    ),
                    hideStatusBar = isHideStatusBar,
                    isActivitySpaceStatusBar = isSpaceStatusBar || isSpaceDisplayCutout,
                )
            )
            // Trả về windowInsets để các view con có thể tiếp tục xử lý
            windowInsets // Hoặc windowInsets nếu muốn các view con tiếp tục nhận
        }
        ViewCompat.requestApplyInsets(viewBinding.root)
        contextAds = object : ContextAds(
            adsManager = adsManager,
            lifecycleOwner = this,
            lifecycleScope = lifecycleScope,
            activity = this,
            fragmentManager = supportFragmentManager,
            remoteConfigRepository = remoteConfigRepository,
            initRewardAdPlaceName = providerRewardAdPlaceName(),
            initInterstitialAdPlaceName = providerInterAdPlaceName(),
            initBannerNativeAdPlaceName = providerBannerNativeAdPlaceName(),
            initPreloadBannerNativeAdPlaceName = providerPreloadBannerNativeAdPlaceName(),
            isWaitingLoad = isWaitingAds
        ) {

            override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
                this@BaseActivity.onBannerNativeResult(adResource)
            }
        }
        showFirstScreen()
        init(savedInstanceState)
        if (!isAwaitCallInitView) {
            initViews(savedInstanceState)
        }
        preloadData()
        handleObservable()

        handleNetworkChange()

        bindLiveData(purchasePreferences.changeVipState) { state ->
            Timber.d("Listener $state")
            if (isVip != state) {
                isVip = state
                contextAds?.preloadAds()
                Timber.d("onChangeVipState $state")
                onChangeVipState(isVip)
                listVipListener.forEach { it.invoke() }
            }
        }
    }

    fun launchImagePicker(onResult: (Uri?) -> Unit) {
        onImagePicked = onResult
        pickImageIntent.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun copyPickedImageToCache(uri: Uri): Uri? {
        val imageDirs = listOfNotNull(
            cacheDir,
            externalCacheDir,
            filesDir
        ).map { File(it, PICKED_IMAGE_DIR) }

        cleanupPickedImageDirs(imageDirs)

        imageDirs.forEach { imageDir ->
            val copiedUri = runCatching {
                if (!imageDir.exists() && !imageDir.mkdirs()) return@runCatching null
                val imageFile = File(imageDir, "picked_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
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
        val file = File(uri.path ?: return)
        val parent = file.parentFile ?: return
        if (parent.name != PICKED_IMAGE_DIR) return

        val pickedImageDirs = listOfNotNull(
            cacheDir,
            externalCacheDir,
            filesDir
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

    fun addListenerVipChange(listener: () -> Unit) {
        listener.invoke()
    }

    fun removeListenerVipChange(listener: () -> Unit) {
        listVipListener.remove(listener)
    }

    open fun onChangeVipState(isVip: Boolean) {

    }

    private fun hideSystemBars(window: android.view.Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController ?: return
            insetsController.hide(WindowInsets.Type.statusBars())
            insetsController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            val insetsController = WindowInsetsControllerCompat(
                window,
                window.decorView
            )
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun WindowInsetsCompat.resolveSystemBarInsets(): Insets {
        val visibleInsets = getInsets(WindowInsetsCompat.Type.systemBars())
        val stableInsets = getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        return Insets.of(
            max(visibleInsets.left, stableInsets.left),
            max(visibleInsets.top, stableInsets.top),
            max(visibleInsets.right, stableInsets.right),
            max(visibleInsets.bottom, stableInsets.bottom)
        )
    }

    open fun preloadData() {}

    open fun initViews(savedInstanceState: Bundle?) {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (isHideNavigationBar) {
            hideNavigationBar()
        }

        if(isHideStatusBar) {
            window?.let {
                hideSystemBars(it)
            }
        }
    }

    /**
     * Yêu cầu quyền từ người dùng
     *
     * @param permissions Danh sách các quyền cần yêu cầu
     * @param onPermissionGrant Callback được gọi khi người dùng cấp quyền hoặc từ chối quyền
     * @see _onPermissionResult
     */
    fun requestPermission(
        permissions: Array<String>,
        onPermissionGrant: (Map<String, Boolean>) -> Unit
    ) {
        _onPermissionResult = onPermissionGrant
        askCheckPermission.launch(
            permissions
        )
    }

    /**
     * Khởi chạy một Activity để lấy kết quả trả về.
     *
     * @param intent Intent để khởi chạy Activity.
     * @param onResult Callback được gọi khi Activity trả về kết quả.
     */
    fun launchForResult(intent: Intent, onResult: (ActivityResult) -> Unit) {
        this.onResult = onResult
        startActivityIntent.launch(intent)
    }

    fun replaceFragment(
        fragment: Fragment,
        transitionType: TransitionType? = null,
        isEnableDuplicateFragment: Boolean = false,
        addToBackStack: Boolean = true,
        containerId: Int
    ) {
        supportFragmentManager.handleReplace(
            containerId = containerId,
            transitionType = transitionType,
            fragment = fragment,
            isEnableDuplicateFragment = isEnableDuplicateFragment,
            addToBackStack = addToBackStack
        )
    }

    fun addFragment(
        fragment: Fragment,
        transitionType: TransitionType? = null,
        isEnableDuplicateFragment: Boolean = false,
        addToBackStack: Boolean = true,
        containerId: Int
    ) {
        supportFragmentManager.handleAdd(
            containerId = containerId,
            transitionType = transitionType,
            fragment = fragment,
            isEnableDuplicateFragment = isEnableDuplicateFragment,
            addToBackStack = addToBackStack
        )
    }

    override fun onStart() {
        super.onStart()
        Timber.d("${this::class.java.simpleName} onStart")
//        preloadAds()
    }

    override fun onResume() {
        super.onResume()
        keyboardHeightProvider?.onResume()
        Timber.d("${this::class.java.simpleName} onResume")
    }

    override fun onPause() {
        try {
            super.onPause()
        } catch (e: Exception) {
            e.printStackTrace()
            analyticsManager.logEvent("transaction_error_${this.javaClass.simpleName}")
        }
        Timber.d("${this::class.java.simpleName} onPause")
        keyboardHeightProvider?.onPause()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("${this::class.java.simpleName} onStop")
    }


    override fun onDestroy() {
        super.onDestroy()
        Timber.d("${this::class.java.simpleName} onDestroy")
    }

    open fun handleObservable() {

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
                checkIfActivityAlive {
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

    //region Ads implement

    /**
     * Tải quảng cáo banner hoặc native.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param oneTimeLoad Chỉ tải quảng cáo một lần nếu true.
     */
    fun loadBannerOrNativeAds(
        adPlaceName: IAdPlaceName,
        oneTimeLoad: Boolean,
        isReload: Boolean = false
    ) {
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
     * Hiển thị quảng cáo reward
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param callbackSuccess Quảng cáo đã xem xong
     * @param callbackClose Cancel quảng cáo
     * @param callbackRetry Quảng cáo lỗi có thể gọi thử lại
     */
    fun unlockWithRewarded(
        adPlaceName: IAdPlaceName,
        callbackSuccess: () -> Unit,
        callbackClose: (() -> Unit)? = null,
        callbackRetry: (() -> Unit)? = null,
        callbackNoAds: (() -> Unit)? = null
    ) {
        showRewardAd(
            adPlaceName = adPlaceName,
            onHandleCompleted = { isShown, isEarnedReward, isNoAds ->
                if (isEarnedReward) {
                    callbackSuccess.invoke()
                } else if (isShown) {
                    callbackClose?.invoke()
                } else if (isNoAds) {
                    callbackNoAds?.invoke()
                } else {
                    callbackRetry?.invoke()
                }
            })
    }

    /**
     * Hiển thị quảng cáo reward.
     *
     * @param adPlaceName Tên vị trí quảng cáo.
     * @param onHandleCompleted Callback được gọi khi quảng cáo được hiển thị hoặc không và người dùng có nhận được phần thưởng hay không.
     */
    private fun showRewardAd(
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
    open fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {}

    fun reinitAdPlaceName(
        initPreloadBannerNativeAdPlaceName: List<IAdPlaceName>? = null,
        initBannerNativeAdPlaceName: List<IAdPlaceName>? = null,
        initInterstitialAdPlaceName: List<IAdPlaceName>? = null,
        initRewardAdPlaceName: List<IAdPlaceName>? = null
    ) {
        contextAds?.reinitAdPlaceName(
            initPreloadBannerNativeAdPlaceName = initPreloadBannerNativeAdPlaceName,
            initBannerNativeAdPlaceName = initBannerNativeAdPlaceName,
            initRewardAdPlaceName = initRewardAdPlaceName,
            initInterstitialAdPlaceName = initInterstitialAdPlaceName
        )
    }

    fun readyAds() {
        contextAds?.ready()
    }

    fun preloadAds() {
        contextAds?.preloadAds()
    }

    fun isRewardReady(adPlaceName: IAdPlaceName) = contextAds?.isRewardReady(adPlaceName) == true

    //endregion

    companion object {
        var globalInnerPadding: Insets? = null
        var globalDisplayCutout: Int = 0
    }
}
