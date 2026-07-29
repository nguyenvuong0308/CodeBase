package com.core.config.domain.data

interface IAdPlaceName {
    val name: String
}

// Dùng cho native_config lồng trong interstitial khi place_name chỉ tồn tại trên Firebase.
data class RemoteAdPlaceName(
    override val name: String
) : IAdPlaceName

sealed class CoreAdPlaceName(
    override val name: String
) : IAdPlaceName {
    object NONE : CoreAdPlaceName("")
    object APP_REOPEN : CoreAdPlaceName("reopen_app")

    companion object {
        val ALL: List<CoreAdPlaceName> by lazy {
            CoreAdPlaceName::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }

        fun fromKey(key: String): CoreAdPlaceName {
            return ALL.find { it.name == key } ?: NONE
        }
    }
}
