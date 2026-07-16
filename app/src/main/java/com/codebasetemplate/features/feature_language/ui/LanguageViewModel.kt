package com.codebasetemplate.features.feature_language.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebasetemplate.required.firebase.GetDataFromRemoteUseCaseImpl
import com.core.utilities.getCurrentLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(private val getDataFromRemoteUseCaseImpl: GetDataFromRemoteUseCaseImpl): ViewModel() {
    private val _initDataAndNextScreen = MutableLiveData<Boolean>()
    val initDataAndNextScreen: LiveData<Boolean> = _initDataAndNextScreen


    fun startInitAndNextScreen() {
        viewModelScope.launch {
            delay((getDataFromRemoteUseCaseImpl.languageActivityConfig.time_show_loading_lfo ?: 3) * 1000L)
            _initDataAndNextScreen.postValue(true)
        }
    }

}