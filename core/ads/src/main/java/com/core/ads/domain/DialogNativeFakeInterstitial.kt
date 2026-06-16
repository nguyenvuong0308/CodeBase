package com.core.ads.domain

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.core.ads.R
import com.core.ads.databinding.DialogNativeFakeInterstitialBinding
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.CoreAdPlaceName
import com.core.config.domain.data.CoreAdPlaceName.NONE
import com.core.config.domain.data.IAdPlaceName
import com.core.config.domain.data.IAppProviderAdPlaceName
import com.core.config.domain.data.NativeAdPlace
import com.core.utilities.isAppDebuggable
import com.core.utilities.util.Timber
import com.core.utilities.util.toast.Toasty
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DialogNativeFakeInterstitial : DialogFragment() {

    companion object {
        private const val ARG_AD_PLACE_NAME = "adPlaceName"
        fun newInstance(adPlaceName: IAdPlaceName): DialogNativeFakeInterstitial {
            return DialogNativeFakeInterstitial().apply {
                arguments = Bundle().apply {
                    putString(ARG_AD_PLACE_NAME, adPlaceName.name)
                }
            }
        }
    }

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    open val isHideNavigationBar: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isHideNavigationBar
    }
    var onClose: (() -> Unit)? = null

    private val adPlaceNameString by lazy {
        arguments?.getString(ARG_AD_PLACE_NAME) ?: ""
    }

    @Inject
    lateinit var appAdPlaceName: IAppProviderAdPlaceName

    @Inject
    lateinit var adsManager: AdsManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    private var viewBinding: DialogNativeFakeInterstitialBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        val placeNameApp = appAdPlaceName.findAdPlaceName(adPlaceNameString)
        val placeNameCore = CoreAdPlaceName.fromKey(adPlaceNameString)
        if (placeNameApp != null && placeNameCore != NONE && requireContext().isAppDebuggable()) {
            Toasty.error(
                requireContext(),
                "Conflict adPlaname core and app ${placeNameCore.name}, please remove it from app module"
            ).show()
            Timber.e("Conflict adPlaname core and app ${placeNameCore.name}, please remove it from app module")
            onClose?.invoke()
            dismissAllowingStateLoss()
            return
        }
        val placeName = placeNameApp ?: placeNameCore
        if (placeName == NONE) {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        val nativeHolder = adsManager.getNativeHolder(requireActivity(), placeName)
        val nativeAd = nativeHolder?.nativeAd
        if (nativeHolder == null || nativeAd == null) {
            dismissAllowingStateLoss()
            onClose?.invoke()
            return
        }
        val adResource = AdLoadBannerNativeUiResource.NativeAdLoaded(
            nativeAd = nativeAd,
            adPlaceName = placeName,
            nativeAdPlace = nativeHolder.adPlace as NativeAdPlace
        )

        viewBinding?.nativeInterstitial?.processAdResource(
            adResource = adResource,
            placeName = placeName
        )

        viewBinding?.nativeInterstitial?.onClose = {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }

        viewBinding?.root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.setPadding(0, systemBars.top, 0, systemBars.bottom)
                insets
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = DialogNativeFakeInterstitialBinding.inflate(inflater, container, false)
        return viewBinding?.root
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        applyFullScreen()
    }

    override fun onResume() {
        super.onResume()
        applyFullScreen()
        viewBinding?.nativeInterstitial?.resumeCloseCountDown()
    }

    override fun onPause() {
        viewBinding?.nativeInterstitial?.pauseCloseCountDown()
        super.onPause()
    }

    override fun onDestroyView() {
        viewBinding?.nativeInterstitial?.pauseCloseCountDown()
        viewBinding = null
        super.onDestroyView()
    }

    private fun applyFullScreen() {
        if (isHideNavigationBar) {
            dialog?.window?.let { window ->
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                WindowCompat.setDecorFitsSystemWindows(window, false)

                val controller = WindowInsetsControllerCompat(window, window.decorView)

                controller.hide(
                    WindowInsetsCompat.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }


    inline fun <T : Any> collectFlowOn(
        flow: Flow<T>,
        lifecycleScope: LifecycleCoroutineScope,
        lifecycleOwner: LifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
        crossinline onResult: (t: T) -> Unit,
    ) {
        lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(lifecycleState) {
                flow.collect {
                    onResult.invoke(it)
                }
            }
        }
    }


}
