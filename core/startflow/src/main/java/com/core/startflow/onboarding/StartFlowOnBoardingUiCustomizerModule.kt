package com.core.startflow.onboarding

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StartFlowOnBoardingUiCustomizerModule {
    @Multibinds
    abstract fun bindOnBoardingUiCustomizers(): Set<OnBoardingUiCustomizer>

    @Multibinds
    abstract fun bindOnBoardingContentProviders(): Set<OnBoardingContentProvider>
}
