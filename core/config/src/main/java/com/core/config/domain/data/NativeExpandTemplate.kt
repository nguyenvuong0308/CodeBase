package com.core.config.domain.data

enum class NativeExpandTemplate(val key: String) {
    V1("native_expand_v1"),
    V2("native_expand_v2"),
    ;

    companion object {
        fun getBy(key: String?): NativeExpandTemplate {
            return when (key?.trim()?.lowercase()) {
                V2.key -> V2
                else -> V1
            }
        }
    }
}
