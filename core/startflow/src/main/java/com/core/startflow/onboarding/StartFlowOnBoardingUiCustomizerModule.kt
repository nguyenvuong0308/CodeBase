package com.core.startflow.onboarding

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.core.startflow.onboarding.v3.OnBoardingV3FullAdsPageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3PageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3UiCustomizer
import com.core.startflow.onboarding.v1.OnBoardingV1PageRenderer
import com.core.startflow.onboarding.v1.OnBoardingV1UiCustomizer
import com.core.startflow.onboarding.v2.OnBoardingV2PageRenderer
import com.core.startflow.onboarding.v2.OnBoardingV2UiCustomizer

@Module
@InstallIn(SingletonComponent::class)
abstract class StartFlowOnBoardingUiCustomizerModule {
    @Multibinds
    abstract fun bindOnBoardingUiCustomizers(): Set<OnBoardingUiCustomizer>

    @Multibinds
    abstract fun bindOnBoardingContentProviders(): Set<OnBoardingContentProvider>

    @Multibinds
    abstract fun bindOnBoardingV1UiCustomizers(): Set<OnBoardingV1UiCustomizer>

    @Multibinds
    abstract fun bindOnBoardingV1PageRenderers(): Set<OnBoardingV1PageRenderer>

    @Multibinds
    abstract fun bindOnBoardingV2UiCustomizers(): Set<OnBoardingV2UiCustomizer>

    @Multibinds
    abstract fun bindOnBoardingV2PageRenderers(): Set<OnBoardingV2PageRenderer>

    @Multibinds
    abstract fun bindOnBoardingV3UiCustomizers(): Set<OnBoardingV3UiCustomizer>

    @Multibinds
    abstract fun bindOnBoardingV3PageRenderers(): Set<OnBoardingV3PageRenderer>

    @Multibinds
    abstract fun bindOnBoardingV3FullAdsPageRenderers(): Set<OnBoardingV3FullAdsPageRenderer>
}
