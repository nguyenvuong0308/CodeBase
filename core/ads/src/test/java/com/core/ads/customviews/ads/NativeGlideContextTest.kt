package com.core.ads.customviews.ads

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeGlideContextTest {

    @Test
    fun `all native custom Glide requests use application context`() {
        val glideCalls = nativeCustomSourceRoot()
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { sourceFile ->
                GLIDE_WITH_REGEX.findAll(sourceFile.readText()).map { match ->
                    sourceFile.name to match.groupValues[1].trim()
                }
            }
            .toList()

        assertTrue("Expected native custom views to contain Glide requests", glideCalls.isNotEmpty())
        glideCalls.forEach { (fileName, requestContext) ->
            assertEquals(
                "$fileName must not bind Glide to an Activity-backed View",
                "context.applicationContext",
                requestContext,
            )
        }
    }

    private fun nativeCustomSourceRoot(): File =
        sequenceOf(
            File("src/main/java/com/core/ads/customviews/ads"),
            File("core/ads/src/main/java/com/core/ads/customviews/ads"),
        ).firstOrNull(File::isDirectory)
            ?: error("Cannot find native custom source directory")

    private companion object {
        val GLIDE_WITH_REGEX = Regex("""Glide\.with\(([^)]+)\)""")
    }
}
