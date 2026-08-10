package com.codebasetemplate.required.firebase

import com.core.config.data.RemoteConfigService
import com.core.config.domain.GetDataFromRemoteConfigUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDataFromRemoteUseCaseImpl @Inject constructor(): GetDataFromRemoteConfigUseCase {

    override fun invoke(remoteConfig: RemoteConfigService) {
    }
}
