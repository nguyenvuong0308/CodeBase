package com.codebasetemplate.features.feature_demo_banner_native.ui

internal object NativeGalleryPreviewConfig {
    const val EXTRA_TEMPLATE_KEY = "template_key"
    const val EXTRA_COLLAPSIBLE = "collapsible"
    const val EXTRA_EXPAND_TEMPLATE = "expand_template"
    const val NATIVE_PIP_KEY = "native_pip"

    private val fullScreenTemplateKeys = setOf(
        "full_cta_bottom",
        "full_cta_bottom_onboarding",
        "full_cta_top",
        "full_cta_right",
        "full_interstitial_v1",
        "full_interstitial_v2",
        "full_interstitial_v3",
    )

    fun requiresFullScreen(templateKey: String): Boolean {
        return templateKey in fullScreenTemplateKeys
    }

    fun loadingDescription(templateKey: String): String = "native-gallery-loading:$templateKey"

    fun readyDescription(templateKey: String): String = "native-gallery-ready:$templateKey"

    fun errorDescription(templateKey: String): String = "native-gallery-error:$templateKey"
}
