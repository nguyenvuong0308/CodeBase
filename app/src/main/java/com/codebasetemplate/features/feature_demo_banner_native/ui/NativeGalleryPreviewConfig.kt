package com.codebasetemplate.features.feature_demo_banner_native.ui

import com.core.config.domain.data.NativeExpandTemplate
import com.core.config.domain.data.NativeTemplateSize

internal data class NativeGalleryPreviewArguments(
    val templateKey: String,
    val collapsible: Boolean = false,
    val expandTemplateKey: String? = null,
)

internal data class NativeGalleryOption(
    val group: String,
    val title: String,
    val arguments: NativeGalleryPreviewArguments,
) {
    val id: String = buildString {
        append(arguments.templateKey)
        if (arguments.collapsible) {
            append(':')
            append(arguments.expandTemplateKey)
        }
    }
}

internal object NativeGalleryPreviewConfig {
    const val EXTRA_TEMPLATE_KEY = "template_key"
    const val EXTRA_COLLAPSIBLE = "collapsible"
    const val EXTRA_EXPAND_TEMPLATE = "expand_template"
    const val NATIVE_PIP_KEY = "native_pip"

    val options: List<NativeGalleryOption> =
        NativeTemplateSize.builtInTemplates.map { template ->
            NativeGalleryOption(
                group = template.groupName(),
                title = template.key.toDisplayName(),
                arguments = NativeGalleryPreviewArguments(templateKey = template.key),
            )
        } + listOf(
            NativeGalleryOption(
                group = "Special",
                title = "Collapsible · V1",
                arguments = NativeGalleryPreviewArguments(
                    templateKey = NativeTemplateSize.MediumCtaBottom.key,
                    collapsible = true,
                    expandTemplateKey = NativeExpandTemplate.V1.key,
                ),
            ),
            NativeGalleryOption(
                group = "Special",
                title = "Collapsible · V2",
                arguments = NativeGalleryPreviewArguments(
                    templateKey = NativeTemplateSize.MediumCtaBottom.key,
                    collapsible = true,
                    expandTemplateKey = NativeExpandTemplate.V2.key,
                ),
            ),
            NativeGalleryOption(
                group = "Special",
                title = "Picture in Picture",
                arguments = NativeGalleryPreviewArguments(templateKey = NATIVE_PIP_KEY),
            ),
        )

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

    private fun NativeTemplateSize.groupName(): String = when {
        key.startsWith("small_") || key == NativeTemplateSize.Small.key ||
            key == NativeTemplateSize.MiniCtaRight.key -> "Small"

        key.startsWith("medium") -> "Medium"
        key.startsWith("large") -> "Large"
        else -> "Full screen"
    }

    private fun String.toDisplayName(): String =
        split('_').joinToString(" ") { word ->
            word.replaceFirstChar { character -> character.uppercase() }
        }
}
