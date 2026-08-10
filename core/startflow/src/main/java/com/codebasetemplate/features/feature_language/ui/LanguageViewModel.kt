package com.codebasetemplate.features.feature_language.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.config.domain.RemoteConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) : ViewModel() {
    private val _initDataAndNextScreen = MutableLiveData<Boolean>()
    val initDataAndNextScreen: LiveData<Boolean> = _initDataAndNextScreen


    fun startInitAndNextScreen() {
        viewModelScope.launch {
            delay((remoteConfigRepository.getLanguageActivityConfig().timeShowLoadingLfo ?: 3) * 1000L)
            _initDataAndNextScreen.postValue(true)
        }
    }

}
