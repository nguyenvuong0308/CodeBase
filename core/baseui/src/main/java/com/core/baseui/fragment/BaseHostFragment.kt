package com.core.baseui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.core.ads.domain.AdsManager
import com.core.baseui.BaseSharedViewModel
import com.core.baseui.ext.TransitionType
import com.core.baseui.ext.handleAdd
import com.core.baseui.ext.handleReplace
import com.core.baseui.ext.popBackStack
import com.core.baseui.ext.viewBinding
import com.core.baseui.navigator.NavigatorEvent
import com.core.config.domain.RemoteConfigRepository
import com.core.utilities.checkIfFragmentAttached
import com.core.utilities.isLoaderShowing
import com.core.utilities.manager.NetworkConnectionManager
import com.core.utilities.manager.isNetworkConnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

abstract class BaseHostFragment<B: ViewBinding, E : NavigatorEvent, VM : BaseSharedViewModel<E>> : Fragment(), CoroutineScope {

    private var _viewBinding: B? = null

    protected val viewBinding get() = _viewBinding
        ?: throw RuntimeException("Should only use binding after onCreateView and before onDestroyView")

    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    companion object {
        const val TAG = "Fragment"
    }

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            this@BaseHostFragment.handleOnBackPressed()
        }
    }

    abstract val containerId: Int

    abstract val hostViewModel: VM

    private var previousNetworkConnection = true

    open fun handleOnBackPressed() {
        if (requireActivity().isLoaderShowing()) {
            return
        }
        if (requireActivity().isFinishing || requireActivity().isDestroyed || parentFragmentManager.isStateSaved) {
            return
        }
        if (childFragmentManager.backStackEntryCount > 1) {
            childFragmentManager.popBackStack()
        } else {
            handleLastBackStack()
        }
    }

    open fun handleLastBackStack() {
        requireActivity().popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinding = getViewBinding(inflater, container)
        return _viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            Log.i(TAG, "${this::class.java.simpleName} onViewCreated savedInstanceState is null")
        } else {
            Log.i(
                TAG,
                "${this::class.java.simpleName} onViewCreated savedInstanceState is non null"
            )
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        if (savedInstanceState == null) {
            showFirstScreen()
        }
        handleObservable()
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "${this::class.java.simpleName} onResume")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "${this::class.java.simpleName} onResume")
        hostViewModel.isActivityResume = true
        if (hostViewModel.needHandleEventWhenResume) {
            hostViewModel.needHandleEventWhenResume = false
            onNavigateTo(hostViewModel.currentEvent)
        }
    }

    override fun onPause() {
        super.onPause()
        hostViewModel.isActivityResume = false
        Log.i(TAG, "${this::class.java.simpleName} onResume")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "${this::class.java.simpleName} onResume")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
        Log.i(TAG, "${this::class.java.simpleName} onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "${this::class.java.simpleName} onResume")
    }

    open fun showFirstScreen() {}

    @CallSuper
    open fun onNavigateTo(event: E) {
        hostViewModel.currentEvent = event
    }

    open fun handleObservable() {
        collectFlowOn(hostViewModel.navigateToFlow) { event ->
            if (hostViewModel.isActivityResume) {
                onNavigateTo(event)
            } else {
                hostViewModel.needHandleEventWhenResume = true
            }
        }

        collectFlowOn(adsManager.isDisableAdDueManyClickFlow, Lifecycle.State.STARTED) {
            preloadAds()
        }

        // Consent granted after this screen was shown: reload the ads it skipped.
        collectFlowOn(adsManager.adsBecameAvailableFlow, Lifecycle.State.STARTED) {
            preloadAds()
        }

        collectFlowOn(networkConnectionManager.isNetworkConnectedFlow, Lifecycle.State.RESUMED) {
            CoroutineScope(coroutineContext).launch {
                delay(1000)
                checkIfFragmentAttached {
                    val isNetworkConnected = requireActivity().isNetworkConnected()
                    if (isNetworkConnected && !previousNetworkConnection) {
                        preloadAds()
                    }
                    previousNetworkConnection = isNetworkConnected
                }
            }
        }
    }

    open fun preloadAds() {}

    fun replaceFragment(
        fragment: Fragment,
        transitionType: TransitionType? = null,
        isEnableDuplicateFragment: Boolean = false,
        addToBackStack: Boolean = true,
    ) {
        childFragmentManager.handleReplace(
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
    ) {
        childFragmentManager.handleAdd(
            containerId = containerId,
            transitionType = transitionType,
            fragment = fragment,
            isEnableDuplicateFragment = isEnableDuplicateFragment,
            addToBackStack = addToBackStack
        )
    }
}