package com.codebasetemplate.features.feature_demo_frame_mvvm.frame_list.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.codebasetemplate.core.base_ui.model.AdItemUI
import com.codebasetemplate.core.base_ui.model.LoadMoreUiModel
import com.codebasetemplate.core.base_ui.model.RetryUiModel
import com.codebasetemplate.databinding.CoreFrameItemBinding
import com.codebasetemplate.databinding.CoreItemAdsBinding
import com.codebasetemplate.databinding.CoreItemLoadMoreBinding
import com.codebasetemplate.databinding.CoreItemRetryBinding
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.codebasetemplate.shared.frame.ui.model.FrameModel
import com.codebasetemplate.utils.diffutil.SimpleDiffUtil
import com.core.ads.domain.AdLoadBannerNativeUiResource
import com.core.baseui.BaseItemUI
import com.core.baseui.adapter.BaseListAdapter
import com.core.baseui.adapter.BaseViewHolder
import com.core.baseui.executor.AppExecutors
import com.core.utilities.setOnSingleClick
import javax.inject.Inject

class FrameListAdapter @Inject constructor(
    appExecutors: AppExecutors,
    diffCallback: SimpleDiffUtil
) : BaseListAdapter<BaseItemUI, BaseViewHolder<BaseItemUI>>(appExecutors, diffCallback) {
    private var adsResource: AdLoadBannerNativeUiResource? = null
    var onRetryLoad: (() -> Unit)? = null

    fun setAdsResource(adsResource: AdLoadBannerNativeUiResource) {
        this.adsResource = adsResource
        for (i in 0 until itemCount) {
            if (getItemViewType(i) == TYPE_ADS) {
                notifyItemChanged(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<BaseItemUI> {
        return if (viewType == TYPE_ADS) {
            AdsItemViewHolder(
                CoreItemAdsBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        } else if (viewType == TYPE_LOAD_MORE) {
            LoadMoreViewHolder(
                CoreItemLoadMoreBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

        } else if (viewType == TYPE_RETRY) {
            RetryViewHolder(
                CoreItemRetryBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        } else {
            FrameViewHolder(
                CoreFrameItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        }
    }

    inner class AdsItemViewHolder(private val _viewBinding: CoreItemAdsBinding) :
        BaseViewHolder<BaseItemUI>(_viewBinding) {
        override fun bindData(position: Int, data: BaseItemUI) {
            _viewBinding.run {
                adsResource?.let {
                    layoutBannerNative.processAdResourceOnRecyclerView(
                        it,
                        AppAdPlaceName.ANCHORED_NATIVE_IN_LIST_TEST,
                        isHideNativeBannerWhenNetworkError = false
                    )
                }
            }
        }

    }

    inner class FrameViewHolder(private val _viewBinding: CoreFrameItemBinding) :
        BaseViewHolder<BaseItemUI>(_viewBinding) {
        override fun bindData(position: Int, data: BaseItemUI) {
            if (data is FrameModel) {
                _viewBinding.run {
                    Glide.with(itemView.context).load(data.imageUrl).into(ivThumb)
                    viewContainer.setOnSingleClick {
                        OnItemClickCallBack?.invoke(position, data)
                    }
                }
            }
        }
    }

    inner class LoadMoreViewHolder(private val _viewBinding: CoreItemLoadMoreBinding) :
        BaseViewHolder<BaseItemUI>(_viewBinding) {
        override fun bindData(position: Int, data: BaseItemUI) {
        }
    }

    inner class RetryViewHolder(private val _viewBinding: CoreItemRetryBinding) :
        BaseViewHolder<BaseItemUI>(_viewBinding) {
        override fun bindData(position: Int, data: BaseItemUI) {
            _viewBinding.tvRetry.setOnClickListener {
                onRetryLoad?.invoke()
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        val data = getItem(position)
        return if (data is AdItemUI) {
            TYPE_ADS
        } else if (data is LoadMoreUiModel) {
            TYPE_LOAD_MORE
        } else if (data is RetryUiModel) {
            TYPE_RETRY
        } else {
            TYPE_NORMAL
        }
    }
}