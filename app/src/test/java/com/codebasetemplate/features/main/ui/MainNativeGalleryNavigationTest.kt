package com.codebasetemplate.features.main.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class MainNativeGalleryNavigationTest {

    @Test
    fun `main menu exposes the native gallery destination`() {
        val layout = parseXml("src/main/res/layout/core_fragment_main.xml")
        val menuItem = layout.getElementsByTagName("androidx.appcompat.widget.LinearLayoutCompat")
            .asElements()
            .singleOrNull { element ->
                element.androidAttribute("id") == "@+id/nativeGalleryLayout"
            }

        assertNotNull("Main must contain the native gallery menu item", menuItem)
        val labels = checkNotNull(menuItem)
            .getElementsByTagName("TextView")
            .asElements()
            .map { it.androidAttribute("text") }
        assertTrue(labels.contains("@string/native_gallery_selector_title"))

        val fragmentSource = projectFile(
            "src/main/java/com/codebasetemplate/features/main/ui/MainChildOfHostFragment.kt"
        ).readText()
        assertTrue(fragmentSource.contains("nativeGalleryLayout.setOnSingleClick"))
        assertTrue(
            fragmentSource.contains(
                "Intent(requireContext(), NativeGalleryActivity::class.java)"
            )
        )
    }

    @Test
    fun `main manifest registers gallery and preview activities`() {
        val manifest = parseXml("src/main/AndroidManifest.xml")
        val activityNames = manifest.getElementsByTagName("activity")
            .asElements()
            .map { it.androidAttribute("name") }

        assertEquals(
            1,
            activityNames.count {
                it == ".features.feature_demo_banner_native.ui.NativeGalleryActivity"
            },
        )
        assertEquals(
            1,
            activityNames.count {
                it == ".features.feature_demo_banner_native.ui.NativeGalleryPreviewActivity"
            },
        )
    }

    private fun parseXml(relativePath: String) =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile(relativePath))

    private fun projectFile(relativePath: String): File =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath")

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).mapNotNull { index -> item(index) as? Element }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
