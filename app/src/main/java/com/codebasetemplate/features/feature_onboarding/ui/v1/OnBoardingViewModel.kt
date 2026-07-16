package com.codebasetemplate.features.feature_onboarding.ui.v1

import androidx.lifecycle.SavedStateHandle
import com.codebasetemplate.features.feature_onboarding.ui.model.OnBoardingItem
import com.codebasetemplate.required.ads.AppAdPlaceName
import com.core.ads.domain.AdsManager
import com.core.baseui.BaseSharedViewModel
import com.core.config.domain.RemoteConfigRepository
import com.core.config.domain.data.AppConfig
import com.core.config.domain.data.CoreAdPlaceName
import com.core.preference.PurchasePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    remoteConfigRepository: RemoteConfigRepository,
    handle: SavedStateHandle,
) : BaseSharedViewModel<OnBoardingEvent>(
    remoteConfigRepository, handle
) {
    @Inject
    lateinit var purchasePreferences: PurchasePreferences

    @Inject
    lateinit var adsManager: AdsManager
    private val introData by lazy {
        remoteConfigRepository.getAppConfig().introDataV3.takeIf { it.isNotEmpty() } ?: arrayListOf(
            AppConfig.DEFINE_INTRO_HAVE_ADS,
            AppConfig.DEFINE_INTRO_HAVE_ADS,
            AppConfig.DEFINE_INTRO_HAVE_ADS
        )
    }
    val itemsOnboarding = ArrayList<OnBoardingItem>()

    fun setup() {
        itemsOnboarding.apply {
            var indexIntro = 0
            var realPosition = 0
            introData.forEachIndexed { _, defineIntro ->

                when (defineIntro) {
                    AppConfig.DEFINE_INTRO_HAVE_ADS -> {
                        add(
                            OnBoardingItem.Item(
                                position = indexIntro,
                                isShowAds = !purchasePreferences.isUserVip(),
                                isPageEnd = false,
                                realPosition = realPosition
                            )
                        )
                        indexIntro++
                        realPosition++
                    }

                    AppConfig.DEFINE_INTRO_NO_ADS -> {
                        add(
                            OnBoardingItem.Item(
                                position = indexIntro,
                                isShowAds = false,
                                isPageEnd = false,
                                realPosition = realPosition
                            )
                        )
                        indexIntro++
                        realPosition++
                    }

                    AppConfig.DEFINE_INTRO_FULL_AD -> {
                        val placeName = when(indexIntro) {
                            0 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_1
                            1 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_2
                            2 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_3
                            3 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_4
                            4 -> AppAdPlaceName.ANCHORED_ONBOARDING_BOTTOM_V3_5
                            else -> CoreAdPlaceName.NONE
                        }
                        if (!adsManager.isNotAbleToVisibleAdsToUser(placeName)) {
                            add(
                                OnBoardingItem.FullNativeItem(indexIntro),
                            )
                        }
                        indexIntro++
                    }
                }
            }
            itemsOnboarding.lastOrNull { it is OnBoardingItem.Item }?.isPageEnd = true
        }
    }

    override fun navigateActionBack() {
        navigateTo(OnBoardingEvent.BackEvent)
    }
}