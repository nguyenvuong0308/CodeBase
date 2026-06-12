package com.core.baseui.adapter

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.core.baseui.executor.AppExecutors

/**
 * A generic RecyclerView adapter that uses Data Binding & DiffUtil.
 *
 * @param <T> Type of the items in the list
 * @param <V> The type of the ViewDataBinding
</V></T> */
abstract class BaseListAdapter<T, V : BaseViewHolder<T>>(
    appExecutors: AppExecutors,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseViewHolder<T>>(
    AsyncDifferConfig.Builder<T>(diffCallback)
        .setBackgroundThreadExecutor(appExecutors.diskIO())
        .build()
) {
    var OnItemClickCallBack: ((position: Int, data: T) -> Unit)? = null
    protected val TYPE_NORMAL = 1
    protected val TYPE_ADS = 2
    protected val TYPE_LOAD_MORE = 3
    protected val TYPE_RETRY = 4

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        holder.bindData(position, getItem(position))
    }

}
