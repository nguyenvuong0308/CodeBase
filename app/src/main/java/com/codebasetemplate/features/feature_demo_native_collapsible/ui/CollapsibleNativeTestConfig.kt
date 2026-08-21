package com.codebasetemplate.features.feature_demo_native_collapsible.ui

import com.core.config.domain.data.NativeAdPlace

internal fun NativeAdPlace.asForcedCollapsibleTestPlace(): NativeAdPlace {
    return copy(isNativeCollapsible = true)
}

internal class CollapsibleNativeTestControls(
    private val setExpanded: (Boolean) -> Unit,
) {
    fun expand() {
        setExpanded(true)
    }

    fun collapse() {
        setExpanded(false)
    }
}
