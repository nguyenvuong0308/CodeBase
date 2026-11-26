package com.core.ads

/** App-owned UMP test settings. Ignored when the application is not debuggable. */
data class ConsentDebugConfig(
    val forceEea: Boolean = false,
    /** UMP hashed device IDs from Logcat; emulators are already test devices. */
    val testDeviceHashedIds: List<String> = emptyList(),
)
