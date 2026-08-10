package com.codebasetemplate.features.feature_splash.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.utilities.util.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads app-specific, lightweight offline data before the splash flow starts.
 *
 * This ViewModel intentionally belongs to the app module so each application can customize the
 * startup work without adding app-specific dependencies to core:startflow.
 */
@HiltViewModel
class SplashLoadDataViewModel @Inject constructor() : ViewModel() {
    private val _initData = MutableLiveData<Boolean>()
    val initData: LiveData<Boolean> = _initData

    var isInitData = false

    fun initData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadData()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Unable to initialize optional splash data")
            } finally {
                // App startup must not get stuck if optional local initialization fails.
                _initData.postValue(true)
            }
        }
    }

    private suspend fun loadData() {
        /**
         * Load only lightweight offline app data here. Avoid network requests or expensive
         * work that would delay the user's splash experience.
         */
    }
}
