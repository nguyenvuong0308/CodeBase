package com.codebasetemplate.features.feature_reward

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import com.codebasetemplate.databinding.CoreActivityRewardTestBinding
import com.core.utilities.setOnSingleClick
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RewardTestActivity : BaseAdsActivity<CoreActivityRewardTestBinding>() {

    override fun bindingProvider(inflater: LayoutInflater): CoreActivityRewardTestBinding {
        return CoreActivityRewardTestBinding.inflate(inflater)
    }

    override fun initViews(savedInstanceState: Bundle?) {
        super.initViews(savedInstanceState)

        viewBinding.rivCardGiftCard.setOnSingleClick {
            unlock(callbackSuccess = {
                Log.d("TAG5", "initViews: callbackSuccess")
            }, callbackFailed = { isNoAds ->
                Log.d("TAG5", "initViews: callbackFailed.isNoAds = $isNoAds")
            })
        }
    }

    override fun cancelRewarded() {
        Log.d("TAG5", "cancelRewarded: ")
    }
}