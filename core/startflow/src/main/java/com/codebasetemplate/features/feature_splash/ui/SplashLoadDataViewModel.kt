package com.codebasetemplate.features.feature_splash.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashLoadDataViewModel @Inject constructor(): ViewModel() {
    private val _initData = MutableLiveData<Boolean>()
    val initData: LiveData<Boolean> = _initData

    var isInitData = false

    fun initData() {
        viewModelScope.launch(Dispatchers.IO) {
            /**Load cái gì ở đây, chỉ nên load offline nhẹ nhàng thôi, tránh ảnh hưởng đến trải nghiệm người dùng*/
            delay(1000)
            _initData.postValue(true)
        }
    }

}