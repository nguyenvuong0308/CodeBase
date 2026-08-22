package com.core.baseui

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.core.config.domain.RemoteConfigRepository
import com.core.utilities.util.hideNavigationBar
import javax.inject.Inject

/**
 * Các class kết thừa cần thêm annotation @AndroidEntryPoint*/
abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    private var _viewBinding: VB? = null

    protected val viewBinding
        get() = _viewBinding
            ?: throw RuntimeException("Should only use binding after onCreateView and before onDestroyView")

    protected abstract fun bindingProvider(inflater: LayoutInflater, container: ViewGroup?): VB

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    val isHideNavigationBar: Boolean by lazy {
        remoteConfigRepository.getAppConfig().isHideNavigationBar
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinding = bindingProvider(inflater, container)
        return _viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        initView()

        if (isHideNavigationBar) {
            requireDialog().hideNavigationBar()
        }
    }

    abstract fun initView()

    open fun setupDialog() {

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
}

fun DialogFragment.safeDismiss() {
    if (isAdded && !requireActivity().isFinishing) {
        dismissAllowingStateLoss()
    }
}