package com.codebasetemplate.features.feature_demo_frame_mvvm.frame_list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.codebasetemplate.Navigator
import com.codebasetemplate.core.base_ui.model.LoadMoreUiModel
import com.codebasetemplate.core.base_ui.model.RetryUiModel
import com.codebasetemplate.databinding.CoreFragmentNativeInListBinding
import com.codebasetemplate.features.feature_demo_frame_mvvm.frame_list.ui.adapter.FrameListAdapter
import com.codebasetemplate.features.main.ui.host.MainHostEvent
import com.codebasetemplate.features.main.ui.host.MainHostViewModel
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.required.shortcut.AppScreenType
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.fragment.BaseChildOfHostFragment
import com.core.baseui.fragment.ScreenType
import com.core.baseui.fragment.collectFlowOn
import com.core.baseui.toolbar.CoreToolbarView
import com.core.config.domain.data.IAdPlaceName
import com.core.utilities.gone
import com.core.utilities.result.processResult
import com.core.utilities.util.OnScrollLoadMore
import com.core.utilities.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FrameListFragment :
    BaseChildOfHostFragment<CoreFragmentNativeInListBinding, MainHostEvent, MainHostViewModel>() {

    private val _viewModel: FrameListViewModel by viewModels()

    @Inject
    lateinit var frameAdapter: FrameListAdapter
    private val onScrollLoadMore: OnScrollLoadMore by lazy {
        OnScrollLoadMore(
            onLoadMore = {
                _viewModel.loadMoreFrameItems()
            }
        )
    }

    override fun bindingProvider(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): CoreFragmentNativeInListBinding {
        return CoreFragmentNativeInListBinding.inflate(inflater, container, false)
    }

    override val hostViewModel: MainHostViewModel by viewModels(ownerProducer = { requireParentFragment() })
    override val screenType: ScreenType
        get() = AppScreenType.NativeInList


    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)
        viewBinding.run {
            rcvSimple.apply {
                adapter = frameAdapter
                addOnScrollListener(onScrollLoadMore)
                frameAdapter.onRetryLoad = {
                    _viewModel.loadMoreFrameItems()
                }
                frameAdapter.OnItemClickCallBack = { position, data ->
                    Navigator.startFrameDetail(requireActivity(), data.identify)
                }
            }

            toolbar.onToolbarListener = object : CoreToolbarView.OnToolbarListener {
                override fun onBack() {
                    hostViewModel.navigateTo(MainHostEvent.ActionBack)
                }
            }
        }
    }

    override fun providerBannerNativeAdPlaceName(): List<IAdPlaceName> {
        return listOf(AppAdPlaceName.ANCHORED_NATIVE_IN_LIST_TEST)
    }

    override fun onBannerNativeResult(adResource: AdLoadBannerNativeUiResource) {
        if (adResource.commonAdPlaceName == AppAdPlaceName.ANCHORED_NATIVE_IN_LIST_TEST) {
            frameAdapter.setAdsResource(adResource)
        }
    }

    override fun handleObservable() {
        collectFlowOn(_viewModel.items) {
            it.processResult(
                onFirstLoading = {
                    viewBinding.progressLoading.visible()
                },
                onStandby = {
                    viewBinding.progressLoading.visible()
                },
                onFirstLoadError = {
                    viewBinding.progressLoading.gone()
                    viewBinding.lnError.visible()
                },
                onResult = {
                    viewBinding.progressLoading.gone()
                    if (it.isLoadingMore) {
                        frameAdapter.submitList(it.data.toMutableList().apply {
                            add(LoadMoreUiModel())
                        })
                    } else if (it.loadMoreError != null) {
                        frameAdapter.submitList(it.data.toMutableList().apply {
                            add(RetryUiModel())
                        })
                    } else {
                        frameAdapter.submitList(it.data)
                    }
                }
            )
        }
    }
}