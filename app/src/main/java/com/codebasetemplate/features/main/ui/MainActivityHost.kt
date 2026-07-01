package com.codebasetemplate.features.main.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.doOnAttach
import com.codebasetemplate.BuildConfig
import com.codebasetemplate.core.base_ui.DialogExit
import com.codebasetemplate.databinding.CoreActivityMainBinding
import com.codebasetemplate.features.main.ui.event.MainFeatureEvent
import com.codebasetemplate.features.main.ui.host.MainHostFragment
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.shortcut.AppShortCut
import com.codebasetemplate.required.update.InAppUpdateImpl
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.HostBaseActivity
import com.core.config.domain.data.IAdPlaceName
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivityHost : HostBaseActivity<CoreActivityMainBinding, MainFeatureEvent, MainShareViewModel>() {
    private var dialogExit: DialogExit? = null
    override fun bindingProvider(inflater: LayoutInflater): CoreActivityMainBinding {
        return CoreActivityMainBinding.inflate(inflater)
    }

    private val isPreloadBannerNativeExit by lazy {
        remoteConfigRepository.getAppConfig().isPreloadBannerNativeExit
    }

    /*Dữ liệu từ short cut*/
    private val targetScreenFromShortCut by lazy {
        intent?.extras?.getString(AppShortCut.KEY_SHORTCUT_TARGET_SCREEN, "")
    }

    override val containerId: Int
        get() = viewBinding.mainContainer.id
    override val sharedViewModel: MainShareViewModel by viewModels()

    @Inject
    lateinit var inAppUpdateImpl: InAppUpdateImpl

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult -> }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        viewBinding.mainContainer.doOnAttach {
            inAppUpdateImpl.checkForUpdate(
                activity = this,
                launcher = activityResultLauncher,
                rootView = it
            )
        }

        customOptionTopicFCM()
    }

    /**
     * Tùy chỉnh subscribeToTopic
     */
    private fun customOptionTopicFCM() {
        requestPostNotificationsIfNeeded { granted, permanentlyDenied ->
            when {
                granted -> {
                    // Set auto init
                    Firebase.messaging.isAutoInitEnabled = true

                    if (BuildConfig.DEBUG) {
                        // Set subscribe chỉ nhận notification với topic cài đặt
                        FirebaseMessaging.getInstance().subscribeToTopic("test").addOnCompleteListener {
                            Log.e("FCM", "FirebaseMessaging: addOnCompleteListener")
                        }.addOnFailureListener { e ->
                            Log.e("FCM", "Subscribe FAIL", e)
                        }
                        // Lấy token, dùng khi test gửi noti đến 1 thiết bị
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                            if (t.isSuccessful) Log.e("FCM", "token=${t.result}")
                            else Log.e("FCM", "getToken FAIL", t.exception)
                        }
                    }
                }

                permanentlyDenied -> {
                    // nothing
                }

                else -> {
                    // nothing
                }
            }
        }
    }

    fun requestPostNotificationsIfNeeded(onResult: (granted: Boolean, permanentlyDenied: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true, false)
            return
        }
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        requestPermission(arrayOf(perm)) { result ->
            val granted = result[perm] == true
            val permanentlyDenied = !granted && requiredOpenSettingPermission(arrayOf(perm))
            onResult(granted, permanentlyDenied)
        }
    }

    override fun showFirstScreen() {
        onNavigateTo(MainFeatureEvent.OpenMain)
    }

    override fun onNavigateTo(event: MainFeatureEvent) {
        super.onNavigateTo(event)
        when(event) {
            MainFeatureEvent.ActionBack -> {
                dialogExit?.dismiss()
                dialogExit = DialogExit()
                dialogExit?.show(supportFragmentManager, "DialogExit_${System.currentTimeMillis()}")
                loadBannerOrNativeAds(AppAdPlaceName.ANCHORED_EXIT, oneTimeLoad = true)
                dialogExit?.onExit = {
                    dialogExit = null

                    adsManager.removeNativeAds(AppAdPlaceName.ANCHORED_EXIT)

                    finish()
                }
            }
            MainFeatureEvent.OpenMain -> {
                replaceFragment(MainHostFragment())
            }
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return if(isPreloadBannerNativeExit) {
            listOf(AppAdPlaceName.ANCHORED_EXIT)
        } else listOf()
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        super.onBannerNativeResult(adResource)
        dialogExit?.setBannerNativeAd(adResource)
    }

    override fun onDestroy() {
        inAppUpdateImpl.cancelFlexible()
        super.onDestroy()
    }
}
