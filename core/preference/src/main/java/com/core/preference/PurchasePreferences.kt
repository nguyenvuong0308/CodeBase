package com.core.preference

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchasePreferences @Inject constructor(@ApplicationContext private val applicationContext: Context) {

    var isVipDebug = false
    private val currentKeyVipList = mutableListOf<String>()

    val changeVipState = MutableLiveData<Boolean>()
    private val prefs = SharedPrefs(applicationContext, name = "Purchase", gson = Gson())

    var isProLifeTime by prefs.preference(defaultValue = false, key = KEY_IS_PRO_LIFE_TIME) {
        changeVipState.value = isUserVip()
    }

    var isProByYear by prefs.preference(defaultValue = false, key = KEY_IS_PRO_BY_YEAR) {
        changeVipState.value = isUserVip()
    }

    var isProByMonth by prefs.preference(defaultValue = false, key = KEY_IS_PRO_BY_MONTH) {
        changeVipState.value = isUserVip()
    }

    var isProByWeek by prefs.preference(defaultValue = false, key = KEY_IS_PRO_BY_WEEK) {
        changeVipState.value = isUserVip()
    }

    init {
        currentKeyVipList.add(KEY_IS_PRO_BY_WEEK)
        currentKeyVipList.add(KEY_IS_PRO_BY_MONTH)
        currentKeyVipList.add(KEY_IS_PRO_BY_YEAR)
        currentKeyVipList.add(KEY_IS_PRO_LIFE_TIME)
    }

    fun isVipForever() = isProLifeTime

    /**
     * Kiểm tra xem người dùng đã mua vip chưa
     */
    fun isUserVip(): Boolean {
        if (isVipDebug) return true
        return currentKeyVipList.any { keyVip ->
            prefs.get(keyVip, false)
        }
    }

    fun isUserNotVip(): Boolean {
        return !isUserVip()
    }

    fun saveBoughtState(key: String, value: Boolean) {
        prefs.put(key, value)
    }

    fun isBought(key: String): Boolean {
        return prefs.get(key, false)
    }

    /**
     * Đăng ký danh sách các key vip được sử dụng trong ứng dụng
     */
    fun addKeyVipList(keyVipList: MutableList<String>) {
        currentKeyVipList.addAll(keyVipList)
    }

    fun registerKeyVipList(keyVipList: MutableList<String>) {
        currentKeyVipList.clear()
        currentKeyVipList.addAll(keyVipList)
    }

    companion object {
        private const val KEY_IS_PRO_LIFE_TIME = "CORE_KEY_IS_PRO_LIFE_TIME"

        private const val KEY_IS_PRO_BY_YEAR = "CORE_KEY_IS_PRO_BY_YEAR"

        private const val KEY_IS_PRO_BY_MONTH = "CORE_KEY_IS_PRO_BY_MONTH"

        private const val KEY_IS_PRO_BY_WEEK = "CORE_KEY_IS_PRO_BY_WEEK"
    }
}