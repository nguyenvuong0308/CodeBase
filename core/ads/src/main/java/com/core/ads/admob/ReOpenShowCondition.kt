package com.core.ads.admob

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReOpenShowCondition @Inject constructor(){
    /**
     * Mục đích chủ yếu là để chặn hiển thị quảng cáo trong các trường hợp xin quyền hoặc mở các intent ra ngoài ứng dụng khi trở về app sẽ không hiển thị quảng cáo
     * true -> Có thể hiển thị quảng cáo
     * false -> Không được hiển thị quảng cáo
     */
    var isCanShow : ()-> Boolean = { true }


}