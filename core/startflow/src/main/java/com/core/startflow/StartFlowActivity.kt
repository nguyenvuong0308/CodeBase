package com.core.startflow

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.core.baseui.BaseActivity
import com.core.baseui.RequireTurnOnNetworkBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

abstract class StartFlowActivity<VB : ViewBinding> : BaseActivity<VB>() {
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setupAfterOnBackPressed()
            }
        }
        onBackPressedCallback?.let {
            onBackPressedDispatcher.addCallback(this, it)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let {
            val config = it.resources.configuration
            if (config.fontScale != 1.0f) {
                config.fontScale = 1.0f
                super.attachBaseContext(it.createConfigurationContext(config))
                return
            }
        }
        super.attachBaseContext(newBase)
    }

    open fun setupAfterOnBackPressed() {
        callSystemBack()
    }

    protected fun callSystemBack() {
        onBackPressedCallback?.isEnabled = false
        lifecycleScope.launch(Dispatchers.Main) {
            yield()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setBackPressEnabled(enabled: Boolean) {
        onBackPressedCallback?.isEnabled = enabled
    }

    fun addOnBackPressedCallback(owner: LifecycleOwner, action: () -> Unit) {
        onBackPressedDispatcher.addCallback(owner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = action()
        })
    }

    protected fun showRequireTurnOnNetworkBottomSheetFragment(
        onRetry: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        RequireTurnOnNetworkBottomSheetFragment().apply {
            this.onRetry = onRetry
            this.onCancel = onCancel
        }.show(
            supportFragmentManager,
            RequireTurnOnNetworkBottomSheetFragment::class.java.simpleName + System.currentTimeMillis()
        )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return try {
            super.dispatchKeyEvent(event)
        } catch (e: SecurityException) {
            if (e.message?.contains("CLOSE_SYSTEM_DIALOGS") == true) {
                analyticsManager.logEvent("CLOSE_SYSTEM_DIALOGS_${this.javaClass.simpleName}")
                true
            } else {
                throw e
            }
        }
    }
}
